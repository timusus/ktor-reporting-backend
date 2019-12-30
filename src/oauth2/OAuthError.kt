package oauth2

import com.fasterxml.jackson.annotation.JsonValue

enum class OAuthError(@get:JsonValue val type: String) {
    InvalidRequest("invalid_request"),
    InvalidClient("invalid_client"),
    InvalidGrant("invalid_grant"),
    UnauthorizedClient("unauthorized_client"),
    UnsupportedGrantType("unsupported_grant_type"),
    InvalidScope("invalid_scope")
}


