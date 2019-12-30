package com.simplecityapps.reporting

import database.DatabaseManager
import database.Reports
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime

class ReportService {

    data class ReportCreationPayload(val title: String, val description: String, val appVersion: String, val deviceName: String, val osVersion: String)

    suspend fun submitReport(userId: Int, reportCreationPayload: ReportCreationPayload): Report? {
        val id = DatabaseManager.dbQuery {
            Reports.insert { reports ->
                reports[Reports.userId] = userId
                reports[Reports.title] = reportCreationPayload.title
                reports[Reports.description] = reportCreationPayload.description
                reports[Reports.appVersion] = reportCreationPayload.appVersion
                reports[Reports.deviceName] = reportCreationPayload.deviceName
                reports[Reports.osVersion] = reportCreationPayload.osVersion
                reports[Reports.dateCreated] = LocalDateTime.now()
            }
        } get Reports.id

        return getReport(id)
    }

    suspend fun getReport(id: Int): Report? = DatabaseManager.dbQuery {
        Reports.select { (Reports.id eq id) }
            .mapNotNull { row -> row.fromReport() }
            .singleOrNull()
    }

    private fun ResultRow.fromReport(): Report {
        return Report(
            this[Reports.id],
            this[Reports.userId],
            this[Reports.title],
            this[Reports.description],
            this[Reports.votes],
            this[Reports.appVersion],
            this[Reports.deviceName],
            this[Reports.osVersion],
            this[Reports.dateCreated],
            this[Reports.dateResolved]
        )
    }
}