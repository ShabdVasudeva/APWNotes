package apw.android.notes.user

import com.google.firebase.auth.FirebaseUser

sealed class AuthState {
    object Idle: AuthState()
    object Loading: AuthState()
    data class Success(val user: FirebaseUser): AuthState()
    data class Failed(val message: String): AuthState()
}