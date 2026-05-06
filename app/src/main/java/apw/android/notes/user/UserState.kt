package apw.android.notes.user

data class UserState(
    val isLoggedIn: Boolean = false,
    val isGuest: Boolean = true,
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null
)