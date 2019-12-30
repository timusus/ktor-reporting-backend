package com.simplecityapps.users

import java.io.Serializable

data class User(
    val id: Int,
    val name: String,
    val email: String
) : Serializable