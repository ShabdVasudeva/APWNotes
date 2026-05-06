package apw.android.notes.user

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionViewModel : ViewModel() {

    private val _user = MutableStateFlow(UserState())
    val user: StateFlow<UserState> = _user

    fun setUser(firebaseUser: FirebaseUser?) {
        _user.value = if (firebaseUser != null) {
            UserState(
                isLoggedIn = true,
                isGuest = false,
                name = firebaseUser.displayName,
                email = firebaseUser.email,
                photoUrl = firebaseUser.photoUrl?.toString()
            )
        } else {
            UserState(isGuest = true)
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        _user.value = UserState(isGuest = true)
    }
}