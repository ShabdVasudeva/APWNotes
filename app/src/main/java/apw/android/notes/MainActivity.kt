package apw.android.notes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import apw.android.notes.ui.composables.CreateNoteScreen
import apw.android.notes.ui.composables.LoginScreen
import apw.android.notes.ui.composables.NotesScreen
import apw.android.notes.ui.theme.APWNotesTheme
import apw.android.notes.user.SessionViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Toast.makeText(applicationContext, "${FirebaseAuth.getInstance().currentUser?.email}",
            Toast.LENGTH_SHORT).show()
        setContent {
            val navController = rememberNavController()
            val sessionViewModel: SessionViewModel = viewModel()
            val startDestination: String = if(FirebaseAuth.getInstance().currentUser != null) "notes" else "login"
            sessionViewModel.setUser(FirebaseAuth.getInstance().currentUser)
            APWNotesTheme {
                NavHost(navController, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(
                            viewModel = viewModel(),
                            sessionViewModel = sessionViewModel,
                            onSkipClick = {
                                navController.navigate("notes") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        ) {
                            navController.navigate("notes") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                    composable("notes") {
                        NotesScreen(
                            sessionViewModel = sessionViewModel,
                            onNavigateToAdd = {
                                navController.navigate("add_note")
                            }
                        )
                    }

                    composable("add_note") {
                        CreateNoteScreen {

                        }
                    }
                }
            }
        }
    }
}