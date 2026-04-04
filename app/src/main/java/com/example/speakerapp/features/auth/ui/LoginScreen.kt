package com.example.speakerapp.features.auth.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
// OR if you have the extended library working:
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speakerapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val serverClientId = stringResource(id = R.string.default_web_client_id).trim()
    val hasValidServerClientId = remember(serverClientId) {
        serverClientId.isNotBlank() &&
                serverClientId != "REPLACE_WITH_WEB_CLIENT_ID" &&
                serverClientId.endsWith(".apps.googleusercontent.com")
    }

    val googleSignInClient = remember(serverClientId, hasValidServerClientId) {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (hasValidServerClientId) {
            gsoBuilder.requestIdToken(serverClientId)
        }
        GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.login(it) }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google sign-in failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF6FAFA))) {
        // Decorative Background Elements
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-100).dp)
                .size(300.dp)
                .blur(100.dp)
                .background(Color(0xFF135F6B).copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(250.dp)
                .blur(80.dp)
                .background(Color(0xFFCDE7ED).copy(alpha = 0.1f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Brand Section
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF004650), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = Color.White)
                }
                Text(
                    "SafeEar",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF004650),
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "The Vigilant\nSanctuary.",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 52.sp
                ),
                color = Color(0xFF181C1D)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(32.dp)) {
                    Text(
                        "SafeEar Login",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF181C1D)
                    )
                    Text(
                        "Access your secure acoustic dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF3F4949)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Google Login
                    Button(
                        onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F4F4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Google Icon placeholder or simple text for brevity
                            Text("Continue with Google", color = Color(0xFF181C1D), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Divider
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Divider(modifier = Modifier.weight(1f), color = Color(0xFFBEC8C9).copy(alpha = 0.3f))
                        Text("OR", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6F7979), fontWeight = FontWeight.Bold)
                        Divider(modifier = Modifier.weight(1f), color = Color(0xFFBEC8C9).copy(alpha = 0.3f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tab Switch
                    Surface(
                        color = Color(0xFFF0F4F4),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row {
                            TabButton("Sign In", !isSignUp, Modifier.weight(1f)) { isSignUp = false }
                            TabButton("Sign Up", isSignUp, Modifier.weight(1f)) { isSignUp = true }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Form
                    if (isSignUp) {
                        LoginTextField("Display Name", displayName, { displayName = it }, "Your name")
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    LoginTextField("Email Address", email, { email = it }, "name@company.com")
                    Spacer(modifier = Modifier.height(16.dp))
                    LoginTextField(
                        "Password",
                        password,
                        { password = it },
                        "••••••••",
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onVisibilityToggle = { passwordVisible = !passwordVisible }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (isSignUp) viewModel.registerWithEmail(email, password, displayName)
                            else viewModel.loginWithEmail(email, password)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004650)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Secure Login", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Local Security Note
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = Color(0xFF9DF898).copy(alpha = 0.1f),
                            shape = CircleShape
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF005312))
                                Text("LOCAL SECURITY ACTIVE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF005312), fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Your biometric data and acoustic fingerprints never leave your device. SafeEar uses End-to-End Local Processing for ultimate privacy.",
                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                            color = Color(0xFF3F4949),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (uiState.error != null) {
                Text(uiState.error ?: "", color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun TabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.padding(4.dp),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF004650) else Color(0xFF3F4949)
            )
        }
    }
}

@Composable
fun LoginTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onVisibilityToggle: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF3F4949), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF3F4949).copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFEBEFEF),
                unfocusedContainerColor = Color(0xFFEBEFEF),
                focusedBorderColor = Color(0xFF004650),
                unfocusedBorderColor = Color.Transparent
            ),
            visualTransformation = if (isPassword && !passwordVisible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onVisibilityToggle) {
                        Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF3F4949))
                    }
                }
            } else null
        )
    }
}
