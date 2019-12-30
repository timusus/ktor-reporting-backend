package com.simplecityapps.reporting

import java.io.Serializable
import java.time.LocalDateTime

data class Report(
    val id: Int,
    val userId: Int,
    val title: String,
    val description: String,
    val votes: Int,
    val appVersion: String,
    val deviceName: String,
    val osVersion: String,
    val dateCreated: LocalDateTime,
    val dateResolved: LocalDateTime?
) : Serializable