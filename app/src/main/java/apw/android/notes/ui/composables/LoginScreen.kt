package apw.android.notes.ui.composables

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import apw.android.notes.R
import apw.android.notes.ui.theme.APWNotesTheme
import apw.android.notes.user.AuthState
import apw.android.notes.user.AuthViewModel
import apw.android.notes.user.SessionViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    sessionViewModel: SessionViewModel,
    viewModel: AuthViewModel = viewModel(),
    onSkipClick: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context: Context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (val currentAuthState = authState) {
            is AuthState.Success -> {
                Toast.makeText(
                    context,
                    "Success Login: ${currentAuthState.user.email}",
                    Toast.LENGTH_LONG
                ).show()
                sessionViewModel.setUser(currentAuthState.user)
                onLoginSuccess()
            }
            is AuthState.Failed -> {
                val toast = Toast(context)
                toast.setText("Error: ${currentAuthState.message}")
                toast.show()
            }
            else -> {}
        }
    }

    LoginContent(
        scope = viewModel.viewModelScope,
        onSignIn = { viewModel.signIn(it) },
        onSkipClick = onSkipClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent(
    scope: CoroutineScope,
    onSignIn: (String) -> Unit,
    onSkipClick: () -> Unit
) {
    val context: Context = LocalContext.current
    val isDark: Boolean = isSystemInDarkTheme()
    val credentialManager: CredentialManager = CredentialManager.create(context)
    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(stringResource(R.string.default_web_client_id))
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    val snackbarHostState = SnackbarHostState()
    val gradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.background
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = gradient
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "APW Notes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 17.dp),
                color = if (isDark) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(0.5f))

            LottieAnim()

            Spacer(modifier = Modifier.weight(0.3f))

            Text(
                text = "Your own personal space",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = if (isDark) 0.85f else 0.7f
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Capture ideas, stay organized, and transform small daily actions into meaningful progress that leads to bigger achievements.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = if (isDark) 0.85f else 0.7f
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val result = credentialManager.getCredential(context, request)
                            val credential = result.credential
                            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleCredential: GoogleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                val idToken: String = googleCredential.idToken
                                onSignIn(idToken)
                            }
                        } catch (e: NoCredentialException) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        } catch (e: GetCredentialCancellationException) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (isDark) 2.dp else 6.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.primary,
                    contentColor = if (isDark)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Sign-Up or Login", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onSkipClick) {
                Text(
                    "Continue as guest",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
@Preview(showSystemUi = true)
fun ShowLoginScreen() {
    APWNotesTheme {
        LoginContent(
            scope = rememberCoroutineScope(),
            onSignIn = {},
            onSkipClick = {}
        )
    }
}

@Composable
fun LottieAnim() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.animation)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(300.dp)
    )
}
