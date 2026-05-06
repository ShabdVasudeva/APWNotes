package apw.android.notes.user

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel: ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        val user = firebaseAuth.currentUser
        if (user != null) {
            _authState.value = AuthState.Success(user)
        }
    }

    fun signIn(id: String) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(id, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if(it.isSuccessful) {
                val user: FirebaseUser = firebaseAuth.currentUser!!
                _authState.value = AuthState.Success(user)
            } else {
                _authState.value = AuthState.Failed(it.exception?.message ?: "Login Failed")
            }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        _authState.value = AuthState.Idle
    }
}