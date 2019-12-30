package com.simplecityapps

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.simplecityapps.JsonMapper.defaultMapper
import com.simplecityapps.reporting.ReportService
import com.simplecityapps.reporting.reporting
import com.sun.deploy.Environment
import database.DatabaseManager
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.basic
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.Routing
import oauth2.OAuth2Configuration
import oauth2.auth
import users.UserService
import users.user

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

val databaseManager = DatabaseManager

object JsonMapper {
    // automatically installs the Kotlin module
    val defaultMapper: ObjectMapper = jacksonObjectMapper()

    init {
        defaultMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
        defaultMapper.registerModule(KotlinModule())
    }
}

@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    databaseManager.initialise()

    val userService = UserService()
    val reportingService = ReportService()

    install(DefaultHeaders)

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(defaultMapper))
    }

    install(Authentication) {

        // OAuth Client Credentials
        basic(name = "oauth2-client-credentials") {
            validate { credentials ->
                if (credentials.name == Environment.getenv("CLIENT_ID") && credentials.password == Environment.getenv("CLIENT_SECRET")) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }

        // OAuth Access Token
        register(AuthenticationProvider(OAuth2Configuration("oauth2-access-token")))
    }

    install(Routing) {
        auth(userService)
        user(userService)
        reporting(reportingService)
    }
}