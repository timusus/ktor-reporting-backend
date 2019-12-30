package users

import com.simplecityapps.users.Credentials
import com.simplecityapps.users.User
import com.simplecityapps.users.UserRegistration
import database.DatabaseManager.dbQuery
import database.Users
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt

class UserService {

    suspend fun registerUser(userRegistration: UserRegistration): User? {
        val id = dbQuery {
            Users.insert { users ->
                users[name] = userRegistration.name
                users[email] = userRegistration.credentials.email
                users[hash] = BCrypt.hashpw(userRegistration.credentials.password, BCrypt.gensalt(10))
            }
        } get Users.id

        return getUser(id)
    }

    suspend fun getUser(id: Int): User? = dbQuery {
        Users.select { (Users.id eq id) }
            .mapNotNull { row -> User(row[Users.id], row[Users.name], row[Users.email]) }
            .singleOrNull()
    }

    suspend fun getUser(credentials: Credentials): User? = dbQuery {
        Users.select { Users.email.eq(credentials.email) }
            .mapNotNull { row ->
                if (BCrypt.checkpw(credentials.password, row[Users.hash])) {
                    User(row[Users.id], row[Users.name], row[Users.email])
                } else {
                    null
                }
            }
            .singleOrNull()
    }

    /**
     * @param token a Base-64 encoded refresh token
     */
    suspend fun setRefreshToken(userId: Int, token: String) = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[refreshToken] = token
        }
    }

    /**
     * A Base-64 encoded refresh token
     */
    suspend fun getRefreshToken(userId: Int): String? = dbQuery {
        Users.select { (Users.id eq userId) }
            .mapNotNull { row -> row[Users.refreshToken] }
            .singleOrNull()
    }
}