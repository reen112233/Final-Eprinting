package com.example.eprinting.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.eprinting.data.UserRole
import com.example.eprinting.data.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


// -------------------
// Auth State
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: UserRole) : AuthState()
    data class Error(val message: String) : AuthState()
}

// -------------------
// Sign Up Data
data class SignUpData(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val userType: String = "CUSTOMER",
    val shopName: String = ""
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()


    val currentUserUid: String?
        get() = auth.currentUser?.uid

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _signUpData = MutableStateFlow(SignUpData())
    val signUpData: StateFlow<SignUpData> = _signUpData

    private val _currentUserData = MutableStateFlow(UserProfile())
    val currentUserData: StateFlow<UserProfile> = _currentUserData

    private val _currentUserEmail = MutableStateFlow(auth.currentUser?.email ?: "")
    val currentUserEmail: StateFlow<String> = _currentUserEmail


    fun updateSignUpData(data: SignUpData) {
        _signUpData.value = data
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    // -------------------
    // Customer update
    fun updateCustomerData(name: String, gcash: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update(mapOf("name" to name, "gcash" to gcash))
    }

    // -------------------
    // Owner update
    fun updateOwnerData(
        name: String,
        gcash: String,
        contactNumber: String,
        shopName: String,
        shopLocation: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update(
                mapOf(
                    "name" to name,
                    "gcash" to gcash,
                    "contactNumber" to contactNumber,
                    "shopName" to shopName,
                    "shopLocation" to shopLocation
                )
            )
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser ?: return

        val credential = com.google.firebase.auth.EmailAuthProvider
            .getCredential(user.email ?: return, currentPassword)

        // 🔐 Re-authenticate
        user.reauthenticate(credential)
            .addOnSuccessListener {

                // ✅ Update password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        onResult(true, null)
                    }
                    .addOnFailureListener {
                        onResult(false, it.message)
                    }
            }
            .addOnFailureListener {
                onResult(false, "Current password is incorrect")
            }
    }




    // -------------------
    // Load current user
    fun loadCurrentUser() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val email = auth.currentUser?.email ?: ""
                _currentUserEmail.value = email // update the flow
                _currentUserData.value = UserProfile(
                    uid = uid,
                    name = doc.getString("name") ?: "",
                    gcash = doc.getString("gcash") ?: "",
                    email = email,
                    contactNumber = doc.getString("contactNumber") ?: "",
                    shopName = doc.getString("shopName") ?: "",
                    shopLocation = doc.getString("shopLocation") ?: "",
                    role = doc.getString("role") ?: "CUSTOMER"
                )
            }
    }


    // -------------------
    // Sign up
    fun signUp(email: String, password: String, userType: String, shopName: String = "") {
        _authState.value = AuthState.Loading
        val signUpData = _signUpData.value

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: run {
                    _authState.value = AuthState.Error("Failed to create user")
                    return@addOnSuccessListener
                }

                val name = signUpData.name

                // ------------------- Update displayName -------------------
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                ).addOnCompleteListener { profileUpdateTask ->
                    if (!profileUpdateTask.isSuccessful) {
                        Log.e("AuthViewModel", "Failed to update displayName")
                    }
                }

                // ------------------- Save user info in Firestore -------------------
                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "userType" to userType,
                    "shopName" to if (userType == "OWNER") shopName else ""
                )

                val role = if (userType == "OWNER") UserRole.OWNER else UserRole.CUSTOMER

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        _authState.value = AuthState.Success(role)  // ✅ pass the role here
                    }
                    .addOnFailureListener { e ->
                        _authState.value = AuthState.Error("Failed to save user data: ${e.message}")
                    }

                    .addOnFailureListener { e ->
                        _authState.value = AuthState.Error(e.message ?: "Sign up failed")
                    }
            }
    }

        // -------------------
        // Login
        fun login(email: String, password: String) {
            _authState.value = AuthState.Loading
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val roleString = doc.getString("role") ?: "CUSTOMER"
                            val role = UserRole.fromString(roleString)
                            _authState.value = AuthState.Success(role)
                        }
                        .addOnFailureListener {
                            _authState.value = AuthState.Error("Failed to load user")
                        }
                }
                .addOnFailureListener {
                    _authState.value = AuthState.Error(it.message ?: "Login failed")
                }
        }

        fun logout() {
            auth.signOut()
        }

}
