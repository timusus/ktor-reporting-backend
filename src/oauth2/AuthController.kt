package oauth2

import users.UserService
import com.auth0.jwt.JWT
import com.fasterxml.jackson.module.kotlin.readValue
import com.simplecityapps.JsonMapper
import com.sun.deploy.Environment
import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.header
import io.ktor.request.receiveParameters
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.InternalAPI
import io.ktor.util.decodeBase64String
import io.ktor.util.encodeBase64
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.simplecityapps.users.Credentials
import com.simplecityapps.users.User
import java.util.*


@UseExperimental(InternalAPI::class)
fun Route.auth(userService: UserService) {

    route("/v1/oauth/access_token") {
        post {
            try {
                call.response.header(HttpHeaders.CacheControl, CacheControl.NoStore(null).toString())
                call.response.header(HttpHeaders.Pragma, CacheControl.NoCache(null).toString())

                call.request.header(HttpHeaders.Authorization)?.let { authHeader ->
                    val (clientId, clientSecret) = extractBasicAuth(authHeader)
                    if (clientId != Environment.getenv("CLIENT_ID") || clientSecret != Environment.getenv("CLIENT_SECRET")) {
                        call.respond(HttpStatusCode.Unauthorized, OAuthResponse.Error(OAuthError.InvalidClient))
                        return@post
                    }

                    when (val result = call.receiveParameters().toGrant(clientId, clientSecret)) {
                        is OAuthResult.Success -> {
                            when (val grant = result.grant) {
                                is Grant.ClientCredentials -> {
                                    handleClientCredentialsGrant()
                                }
                                is Grant.Password -> {
                                    handleClientCredentialsGrant(userService, grant)
                                }
                                is Grant.RefreshToken -> {
                                    handleRefreshTokenGrant(grant, userService)
                                }
                            }
                        }
                        is OAuthResult.Error -> {
                            call.respond(if (result.error == OAuthError.InvalidClient) HttpStatusCode.Unauthorized else HttpStatusCode.BadRequest, OAuthResponse.Error(OAuthError.InvalidGrant))
                        }
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Access token failed (${e.localizedMessage})")
            }
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleClientCredentialsGrant() {
    val accessTokenExpiration = 15 * 60 // 15 minutes
    val accessToken = createAccessToken("registration", accessTokenExpiration, null)

    call.respond(HttpStatusCode.OK, OAuthResponse.Success(accessToken, "bearer", accessTokenExpiration, null, null))
}

@UseExperimental(InternalAPI::class)
private suspend fun PipelineContext<Unit, ApplicationCall>.handleRefreshTokenGrant(
    grant: Grant.RefreshToken,
    userService: UserService
): Unit? {
    val refreshToken = JWT.decode(grant.refreshToken.decodeBase64String())

    return if (refreshToken.expiresAt > Date()) { // The refresh token hasn't expired
        refreshToken.claims["usr"]?.asString()?.let { userJson ->
            val user = JsonMapper.defaultMapper.readValue<User>(userJson)
            if (userService.getRefreshToken(user.id)?.equals(grant.refreshToken) == true) { // The refresh token matches
                val accessTokenExpiration = 60 * 60 // 1 hour
                val accessToken = createAccessToken("access", accessTokenExpiration, userJson).encodeBase64()

                call.respond(HttpStatusCode.OK, OAuthResponse.Success(accessToken, "bearer", accessTokenExpiration, grant.refreshToken, null))
            } else {
                call.respond(HttpStatusCode.BadRequest, OAuthResponse.Error(OAuthError.InvalidRequest))
            }
        }
    } else {
        call.respond(HttpStatusCode.BadRequest, OAuthResponse.Error(OAuthError.InvalidRequest))
    }
}

@UseExperimental(InternalAPI::class)
private suspend fun PipelineContext<Unit, ApplicationCall>.handleClientCredentialsGrant(
    userService: UserService,
    grant: Grant.Password
): Unit? {
    return userService.getUser(Credentials(grant.username, grant.password))?.let { user ->
        withContext(Dispatchers.IO) {
            val userJson = JsonMapper.defaultMapper.writeValueAsString(user)
            val accessTokenExpiration = 60 * 60 // 1 hour
            val accessToken = createAccessToken("access", accessTokenExpiration, userJson).encodeBase64()

            val refreshTokenExpiration = 60 * 60 * 24 * 60 // 60 days
            val refreshToken = createRefreshToken(refreshTokenExpiration, userJson).encodeBase64()

            userService.setRefreshToken(user.id, refreshToken)

            call.respond(HttpStatusCode.OK, OAuthResponse.Success(accessToken, "bearer", accessTokenExpiration, refreshToken, null))
        }
    } ?: run {
        call.respond(HttpStatusCode.BadRequest, OAuthResponse.Error(OAuthError.InvalidGrant))
    }
}

private fun Parameters.toGrant(clientId: String?, clientSecret: String?): OAuthResult {

    val grantType = this["grant_type"]

    if (clientId == null || clientSecret == null) {
        return OAuthResult.Error(OAuthError.InvalidClient)
    }

    return when (grantType) {
        "client_credentials" -> {
            return OAuthResult.Success(Grant.ClientCredentials(clientId, clientSecret))
        }
        "password" -> {
            val username = this["username"]
            val password = this["password"]
            if (username == null || password == null) {
                throw DeserializationException(Throwable("Missing username or password"))
            }
            OAuthResult.Success(Grant.Password(clientId, clientSecret, username, password))
        }
        "refresh_token" -> {
            val refreshToken = this["refresh_token"] ?: throw DeserializationException(Throwable("Missing refresh token"))
            OAuthResult.Success(Grant.RefreshToken(clientId, clientSecret, refreshToken))
        }
        else -> {
            OAuthResult.Error(OAuthError.UnsupportedGrantType)
        }
    }
}
