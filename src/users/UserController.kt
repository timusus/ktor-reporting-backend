package users

import com.simplecityapps.users.UserRegistration
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route

fun Route.user(userService: UserService) {

    route("/api/v1/register") {
        authenticate("oauth2-client-credentials") {
            post("/") {
                try {
                    val userRegistration = call.receive<UserRegistration>()
                    userService.registerUser(userRegistration)?.let { user ->
                        call.respond(HttpStatusCode.Created, user)
                    } ?: run {
                        call.respond(HttpStatusCode.Forbidden, "Registration failed")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.Forbidden, "Registration failed (${e.localizedMessage})")
                }
            }
        }
    }
}