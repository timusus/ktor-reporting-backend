package com.simplecityapps.reporting

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import oauth2.UserPrincipal

fun Route.reporting(reportService: ReportService) {

    authenticate("oauth2-access-token") {
        route("/api/v1/report") {
            post("/") {
                try {
                    call.authentication.principal<UserPrincipal>()?.let { principal ->
                        val reportRequestBody = call.receive<ReportService.ReportCreationPayload>()
                        reportService.submitReport(principal.user.id, reportRequestBody)?.let { report ->
                            call.respond(HttpStatusCode.OK, report)
                        } ?: call.respond(HttpStatusCode.BadRequest, "Report not created")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
                }
            }
            get("/{id}") {
                call.parameters["id"]?.toIntOrNull()?.let { id ->
                    reportService.getReport(id)?.let { report ->
                        call.respond(HttpStatusCode.OK, report)
                        return@get
                    }
                }

                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}