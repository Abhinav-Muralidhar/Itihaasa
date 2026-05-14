package com.itihaasa.nammakathey.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
            ItihaasaAuthLogo()
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Sign in to begin your Karnataka story journey",
                color = RoyalIndigo,
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 27.sp
            )
            Text(
                text = "Your district, badges, ranks, and story progress stay connected to your account.",
                color = RoyalIndigo.copy(alpha = 0.72f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))

            val loginCardShape = RoundedCornerShape(12.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = loginCardShape,
                        ambientColor = Color(0x1A000000),
                        spotColor = Color(0x1A000000)
                    ),
                color = ParchmentLight,
                shape = loginCardShape,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalIndigo,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (uiState.isLoading) "Please wait..." else if (uiState.mode == AuthMode.SignUp) "Create Account" else "Sign In")
                    }

                    OutlinedButton(
                        onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = RoyalIndigo
                        ),
                        border = BorderStroke(1.dp, RoyalIndigo.copy(alpha = 0.16f))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_g),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("Continue with Google", color = RoyalIndigo)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItihaasaAuthLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            "itihaasa".forEach { char ->
                AuthLogoLetter(char)
            }
        }
        Text(
            text = "\u0CA8\u0CAE\u0CCD\u0CAE \u0C95\u0CA5\u0CC6",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = HeritageOchre.copy(alpha = 0.86f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthLogoLetter(char: Char) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(10.dp)
                .size(width = 19.dp, height = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (char == 'i') {
                Box(
                    modifier = Modifier
                        .size(width = 7.dp, height = 9.dp)
                        .background(HeritageOchre, RoundedCornerShape(999.dp))
                )
            }
        }
        Text(
            text = if (char == 'i') "\u0131" else char.toString(),
            fontFamily = FontFamily.Serif,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = RoyalIndigo
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
            .background(Color(0xFFF0E5CD), RoundedCornerShape(16.dp))
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
            if (selected) RoyalIndigo else Color(0xFFEADCC2),
            RoundedCornerShape(12.dp)
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
            focusedBorderColor = RoyalIndigo.copy(alpha = 0.20f),
            unfocusedBorderColor = RoyalIndigo.copy(alpha = 0.20f),
            focusedLabelColor = RoyalIndigo,
            cursorColor = HeritageOchre
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
