package database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseManager {

    lateinit var database: Database

    fun initialise() {
        database = Database.connect(
            "jdbc:mysql://0.0.0.0:33060/reporting",
            driver = "com.mysql.jdbc.Driver",
            user = "app",
            password = "password"
        )

        transaction {
            addLogger(StdOutSqlLogger)

            SchemaUtils.create(Users)
            SchemaUtils.create(Reports)
        }
    }

    suspend fun <T> dbQuery(
        block: suspend () -> T
    ): T =
        newSuspendedTransaction { block() }
}