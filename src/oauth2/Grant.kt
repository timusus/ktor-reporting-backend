package oauth2

sealed class Grant {
    data class ClientCredentials(
        val clientId: String,
        val clientSecret: String
    ) : Grant()

    data class Password(
        val clientId: String,
        val clientSecret: String,
        val username: String,
        val password: String
    ) : Grant()

    data class RefreshToken(
        val clientId: String,
        val clientSecret: String,
        val refreshToken: String
    ) : Grant()
}