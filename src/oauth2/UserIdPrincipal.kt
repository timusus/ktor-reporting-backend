package oauth2

import com.simplecityapps.users.User
import io.ktor.auth.Principal

data class UserPrincipal(val user: User): Principal
