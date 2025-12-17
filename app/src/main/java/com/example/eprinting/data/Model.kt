import android.net.http.UrlRequest
import java.security.Timestamp

// User role
sealed class UserRole {

    object CUSTOMER : UserRole()
    object OWNER : UserRole()

    // Optional: convert string to role
    companion object {
        fun fromString(value: String): UserRole = when(value) {
            "CUSTOMER" -> CUSTOMER
            "OWNER" -> OWNER
            else -> throw IllegalArgumentException("Invalid UserRole: $value")
        }
    }

    override fun toString(): String = when(this) {
        CUSTOMER -> "CUSTOMER"
        OWNER -> "OWNER"
    }
}


// User model
data class User(
    val name: String = "",
    val email: String = "",
    val gcash: String = "",
    val role: String = "CUSTOMER"
)


data class UserProfile(
    val name: String = "",
    val gcash: String = ""
)


// Optional: printing shop
data class PrintingShop(
    val id: String = "",
    val name: String = "",
    val location: String = ""
)

// Paper type
data class PaperType(
    val id: String = "",
    val name: String = "",
    val pricePerCopy: Double = 0.0
)
