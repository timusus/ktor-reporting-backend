package oauth2

import com.fasterxml.jackson.annotation.JsonProperty

sealed class OAuthResponse {
    class Success(
        @JsonProperty("access_token") val accessToken: String,
        @JsonProperty("token_type") val tokenType: String,
        @JsonProperty("expires_in") val expiresIn: Int,
        @JsonProperty("refresh_token") val refreshToken: String?,
        @JsonProperty("scope") val scope: String?
    ) : OAuthResponse()

    class Error(
        @JsonProperty("error") val error: OAuthError,
        @JsonProperty("error_description") val errorDescription: String? = null,
        @JsonProperty("error_uri") val errorUri: String? = null
    ) : OAuthResponse()
}