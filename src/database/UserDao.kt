package database

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val name = varchar("name", length = 50)
    val email = varchar("email", length = 50).uniqueIndex()
    val hash = varchar("hash", length = 255)
    val refreshToken = text("refresh_token").nullable()
}