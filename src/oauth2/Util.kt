package oauth2

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.sun.deploy.Environment
import io.ktor.util.InternalAPI
import io.ktor.util.decodeBase64String
import java.util.*

/**
 * Retrieves Base064 encoded clientId/clientSecret credentials from an Auth Header
 */
@UseExperimental(InternalAPI::class)
fun extractBasicAuth(authHeader: String): Pair<String, String> {
    val entries = authHeader.removePrefix("Basic ").decodeBase64String().split(":")
    return Pair(entries.first(), entries.last())
}

/**
 * Retrieves Base064 encoded clientId/clientSecret credentials from an Auth Header
 */
@UseExperimental(InternalAPI::class)
fun extractBearerToken(authHeader: String): String {
    return authHeader.removePrefix("Bearer ").decodeBase64String()
}

/**
 * @param expiration time to expiration, in seconds
 */
fun createAccessToken(subject: String, expiration: Int, userJson: String? = null): String {
    return JWT.create()
        .withIssuedAt(Date())
        .withExpiresAt(Date(Date().time + expiration * 1000L))
        .withIssuer("shuttle-backend")
        .withSubject("access")
        .apply { userJson?.let { withClaim("usr", userJson) } }
        .sign(Algorithm.HMAC256(Environment.getenv("JWT_SECRET")))!!
}

/**
 * @param expiration time to expiration, in seconds
 */
fun createRefreshToken(expiration: Int, userJson: String): String {
    return JWT.create()
        .withIssuedAt(Date())
        .withExpiresAt(Date(Date().time + expiration * 1000L))
        .withIssuer("shuttle-backend")
        .withSubject("refresh")
        .withClaim("usr", userJson)
        .sign(Algorithm.HMAC256(Environment.getenv("JWT_SECRET")))!!
}