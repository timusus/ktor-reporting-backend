package oauth2

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.fasterxml.jackson.module.kotlin.readValue
import com.simplecityapps.JsonMapper
import com.simplecityapps.users.User
import com.sun.deploy.Environment
import io.ktor.application.call
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.util.InternalAPI

@UseExperimental(InternalAPI::class)
class OAuth2Configuration(name: String) : AuthenticationProvider.Configuration(name) {
    init {
        pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->

            var cause: AuthenticationFailedCause? = null

            // Validate access token
            context.call.request.header(HttpHeaders.Authorization)?.let { authHeader ->
                val token = extractBearerToken(authHeader)
                try {

                    val jwt = JWT.require(Algorithm.HMAC256(Environment.getenv("JWT_SECRET")))
                        .build()
                        .verify(token)

                    if (jwt.subject != "access") {
                        throw JWTVerificationException("Invalid subject")
                    }

                    if (jwt.issuer != "shuttle-backend") {
                        throw JWTVerificationException("Invalid issuer")
                    }

                    jwt.claims["usr"]?.asString()?.let { userJson ->
                        val user: User = JsonMapper.defaultMapper.readValue(userJson)
                        context.principal = UserPrincipal(user)
                    } ?: throw JWTVerificationException("Invalid usr claim")

                } catch (e: JWTVerificationException) {
                    cause = AuthenticationFailedCause.InvalidCredentials
                }
            } ?: run {
                cause = AuthenticationFailedCause.NoCredentials
            }

            cause?.let { cause ->
                context.challenge("OAuth", cause) { challenge ->
                    call.respond(HttpStatusCode.BadRequest, OAuthResponse.Error(OAuthError.InvalidGrant))
                    challenge.complete()
                }
            }
        }
    }
}