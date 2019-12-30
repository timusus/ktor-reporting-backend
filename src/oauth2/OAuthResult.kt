package oauth2

sealed class OAuthResult {
    class Success(val grant: Grant) : OAuthResult()
    class Error(val error: OAuthError) : OAuthResult()
}
