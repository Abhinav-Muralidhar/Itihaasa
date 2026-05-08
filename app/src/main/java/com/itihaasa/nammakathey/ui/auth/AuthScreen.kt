package com.itihaasa.nammakathey.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.itihaasa.nammakathey.R
import com.itihaasa.nammakathey.ui.theme.Charcoal
import com.itihaasa.nammakathey.ui.theme.HeritageOchre
import com.itihaasa.nammakathey.ui.theme.Parchment
import com.itihaasa.nammakathey.ui.theme.ParchmentLight
import com.itihaasa.nammakathey.ui.theme.RoyalIndigo

@Composable
fun AuthScreen(
    onBackClick: () -> Unit,
    onAuthComplete: (Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
                .idToken
        }.getOrNull()?.let { token ->
            viewModel.signInWithGoogle(token, onAuthComplete)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = RoyalIndigo)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HeritageSeal()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Namma Kathey",
                fontFamily = FontFamily.Serif,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalIndigo
            )
            Text(
                text = "Enter your heritage journey",
                fontSize = 14.sp,
                color = Charcoal.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ParchmentLight,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .border(1.dp, HeritageOchre.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AuthModeToggle(
                        mode = uiState.mode,
                        onModeChange = viewModel::setMode
                    )

                    if (uiState.mode == AuthMode.SignUp) {
                        AuthTextField(
                            value = uiState.name,
                            onValueChange = viewModel::updateName,
                            label = "Name"
                        )
                    }
                    AuthTextField(
                        value = uiState.email,
                        onValueChange = viewModel::updateEmail,
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )
                    AuthTextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = "Password",
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )

                    uiState.errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.submitEmail(onAuthComplete) },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalIndigo),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (uiState.isLoading) "Please wait..." else if (uiState.mode == AuthMode.SignUp) "Create Account" else "Sign In")
                    }

                    OutlinedButton(
                        onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continue with Google", color = RoyalIndigo)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeritageSeal() {
    Box(
        modifier = Modifier
            .size(84.dp)
            .background(RoyalIndigo, CircleShape)
            .border(3.dp, HeritageOchre, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NK",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Parchment
        )
    }
}

@Composable
private fun AuthModeToggle(
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Parchment, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AuthModeButton("Sign in", mode == AuthMode.SignIn, Modifier.weight(1f)) {
            onModeChange(AuthMode.SignIn)
        }
        AuthModeButton("Create", mode == AuthMode.SignUp, Modifier.weight(1f)) {
            onModeChange(AuthMode.SignUp)
        }
    }
}

@Composable
private fun AuthModeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.background(
            if (selected) HeritageOchre else Color.Transparent,
            RoundedCornerShape(7.dp)
        )
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else RoyalIndigo,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    icon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = icon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HeritageOchre,
            unfocusedBorderColor = RoyalIndigo.copy(alpha = 0.28f),
            focusedLabelColor = RoyalIndigo,
            cursorColor = HeritageOchre
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
