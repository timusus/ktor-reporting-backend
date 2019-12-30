package database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.datetime

object Reports : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val userId = (integer("user_id") references Users.id)
    val title = varchar("title", length = 50)
    val description = text("description")
    val votes = integer("votes").default(1)
    val appVersion = varchar("app_version", 50)
    val deviceName = varchar("device_name", 50)
    val osVersion = varchar("os_version", 50)
    val dateCreated = datetime("created_at")
    val dateResolved = datetime("resolved_at").nullable()
}