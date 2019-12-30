package com.simplecityapps.users

data class UserRegistration(val name: String, val email: String, val password: String) {
    val credentials = Credentials(email, password)
}

data class Credentials(val email: String, val password: String)