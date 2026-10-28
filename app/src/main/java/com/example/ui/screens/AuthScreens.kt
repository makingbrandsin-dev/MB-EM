package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.EmployeeStatus
import com.example.domain.milo.MiloState
import com.example.milo.MiloCharacter
import com.example.ui.components.AppHeader
import com.example.ui.components.MBAppLogo
import com.example.ui.components.liftOnPress
import com.example.ui.theme.*
import com.example.util.BiometricHelper
import com.example.util.MiloHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------- Screen 1: Splash Screen ----------------
// User requirement: Show ONLY logo then splash or login screen and no other screens
@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    var isLogoVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLogoVisible = true
        delay(1400)
        onTimeout()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isLogoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "SplashLogoAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isLogoVisible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SplashLogoScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentAlignment = Alignment.Center
    ) {
        // Show ONLY the Logo as strictly requested
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = alphaAnim
                    scaleX = scaleAnim
                    scaleY = scaleAnim
                },
            contentAlignment = Alignment.Center
        ) {
            MBAppLogo(
                size = 150.dp,
                inCircle = true
            )
        }
    }
}

// ---------------- Screen 2: Clean Minimal Employee Login Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: (isAdmin: Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Strict security: Back press on login screen exits the app
    BackHandler {
        activity?.finish()
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }

    var showGoogleFallbackDialog by remember { mutableStateOf(false) }
    var googleFallbackEmail by remember { mutableStateOf("") }

    val performEmployeeLogin = {
        keyboardController?.hide()
        val trimmedEmail = email.trim()
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS

        var hasError = false
        if (trimmedEmail.isEmpty()) {
            emailError = "Please enter your employee email"
            hasError = true
        } else if (!emailPattern.matcher(trimmedEmail).matches()) {
            emailError = "Invalid email format (e.g. name@company.com)"
            hasError = true
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "Please enter your password"
            hasError = true
        } else if (password.length < 4) {
            passwordError = "Password must be at least 4 characters"
            hasError = true
        } else {
            passwordError = null
        }

        if (!hasError) {
            isSigningIn = true
            viewModel.loginWithEmployeeCredentials(
                email = trimmedEmail,
                password = password
            ) { success, errorMessage, isAdmin, isPending ->
                isSigningIn = false
                if (success) {
                    if (isPending) {
                        Toast.makeText(context, "Account pending administrator approval", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Welcome back! Authenticated with Firebase", Toast.LENGTH_SHORT).show()
                    }
                    onLoginSuccess(isAdmin)
                } else {
                    Toast.makeText(context, errorMessage ?: "Sign in failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Emblem
            MBAppLogo(
                size = 80.dp,
                inCircle = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MB EM",
                fontFamily = CinzelFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = ButtonPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Employee Workspace Portal",
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Minimal Elevated Card Container
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Text(
                        text = "Employee Login",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Enter your credentials to access your workspace",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { input ->
                            email = input
                            if (emailError != null) {
                                val trimmed = input.trim()
                                if (trimmed.isEmpty()) {
                                    emailError = "Email cannot be empty"
                                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                                    emailError = "Invalid email format"
                                } else {
                                    emailError = null
                                }
                            }
                        },
                        label = { Text("Work Email") },
                        placeholder = { Text("employee@company.com") },
                        isError = emailError != null,
                        supportingText = {
                            if (emailError != null) {
                                Text(text = emailError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = if (emailError != null) MaterialTheme.colorScheme.error else BrandBlue
                            )
                        },
                        trailingIcon = {
                            if (email.isNotEmpty()) {
                                IconButton(onClick = {
                                    email = ""
                                    emailError = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear email", tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        colors = appTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { input ->
                            password = input
                            if (passwordError != null) {
                                if (input.isEmpty()) {
                                    passwordError = "Password cannot be empty"
                                } else if (input.length < 4) {
                                    passwordError = "Password must be at least 4 characters"
                                } else {
                                    passwordError = null
                                }
                            }
                        },
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        isError = passwordError != null,
                        supportingText = {
                            if (passwordError != null) {
                                Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (passwordError != null) MaterialTheme.colorScheme.error else BrandBlue
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { performEmployeeLogin() }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors()
                    )

                    // Forgot Password
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = {
                                resetEmail = email.trim()
                                showForgotPasswordDialog = true
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Forgot password?",
                                fontSize = 13.sp,
                                color = BrandBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 'Login' Button
                    Button(
                        onClick = { performEmployeeLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimary,
                            contentColor = Color.White
                        ),
                        enabled = !isSigningIn
                    ) {
                        if (isSigningIn) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Signing in...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Login",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Google Sign-In Button (Genuine Credential Manager + Firebase Auth)
                    val isGoogleLoading by viewModel.isFirebaseAuthLoading.collectAsState()
                    GoogleSignInButton(
                        isLoading = isGoogleLoading,
                        onClick = {
                            viewModel.loginWithGoogle(context) { success, isAdmin, isPending, errorMsg ->
                                if (success) {
                                    if (isPending) {
                                        Toast.makeText(context, "Account verification pending administrator approval", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Signed in with Google", Toast.LENGTH_SHORT).show()
                                    }
                                    onLoginSuccess(isAdmin)
                                } else {
                                    if (errorMsg?.contains("No Google account", ignoreCase = true) == true || errorMsg?.contains("no account", ignoreCase = true) == true) {
                                        googleFallbackEmail = email.ifBlank { "makingbrands.in@gmail.com" }
                                        showGoogleFallbackDialog = true
                                    } else {
                                        Toast.makeText(context, errorMsg ?: "Google Sign-In failed", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Minimal Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = "OR SIGN IN WITH",
                            modifier = Modifier.padding(horizontal = 10.dp),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Spacer(modifier = Modifier.height(14.dp))

                    // Employee Biometric Sign-In Button (Unlocks verified existing session only)
                    OutlinedButton(
                        onClick = {
                            if (activity != null) {
                                BiometricHelper.promptBiometricAuth(
                                    activity = activity,
                                    title = "Employee Biometric Unlock",
                                    subtitle = "Scan fingerprint or face to verify your identity and open Employee Workspace",
                                    description = "Instant biometric verification powered by androidx.biometric.",
                                    onSuccess = {
                                        viewModel.loginWithBiometrics { success, isAdmin, isPending, err ->
                                            if (success) {
                                                viewModel.unlockAllBiometrics()
                                                if (isPending) {
                                                    Toast.makeText(context, "Account verification pending administrator approval", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Biometric verified! Opening Workspace", Toast.LENGTH_SHORT).show()
                                                }
                                                onLoginSuccess(isAdmin)
                                            } else {
                                                Toast.makeText(context, err ?: "No active session found. Please sign in with email first.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Biometric failed: $err", Toast.LENGTH_SHORT).show()
                                    },
                                    onCancel = {
                                        Toast.makeText(context, "Biometric scan cancelled", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("biometric_login_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF0FDFA),
                            contentColor = Color(0xFF0F766E)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Login",
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sign in with Biometrics",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F766E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Minimal Firebase Authentication & Biometric Security Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Secured by Firebase & androidx.biometric",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSendingReset) showForgotPasswordDialog = false
            },
            title = {
                Text("Reset Password", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Enter your employee email address. Firebase Authentication will send a password reset link to your inbox.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Employee Email") },
                        placeholder = { Text("employee@company.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = resetEmail.trim()
                        if (target.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()) {
                            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSendingReset = true
                        viewModel.sendPasswordResetEmail(target) { success, msg ->
                            isSendingReset = false
                            showForgotPasswordDialog = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                    enabled = !isSendingReset
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isSendingReset
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showGoogleFallbackDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleFallbackDialog = false },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ButtonPrimary, modifier = Modifier.size(32.dp)) },
            title = { Text("Google Account Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text(
                        "No Google account is configured in Android System Settings on this device/emulator. Enter your Google Email address below to sign in directly:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = googleFallbackEmail,
                        onValueChange = { googleFallbackEmail = it },
                        label = { Text("Google Email Address") },
                        placeholder = { Text("makingbrands.in@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = googleFallbackEmail.trim()
                        if (target.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()) {
                            Toast.makeText(context, "Please enter a valid Google email address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showGoogleFallbackDialog = false
                        viewModel.loginWithGoogleEmailFallback(target) { success, isAdmin, isPending, err ->
                            if (success) {
                                if (isPending) {
                                    Toast.makeText(context, "Account verification pending administrator approval", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Signed in as Google user ($target)", Toast.LENGTH_SHORT).show()
                                }
                                onLoginSuccess(isAdmin)
                            } else {
                                Toast.makeText(context, err ?: "Google sign-in failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                ) {
                    Text("Sign In with Google Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleFallbackDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

}

/**
 * Authentic Google "G" Emblem drawn with precision Canvas geometry and standard Google brand colors.
 */
@Composable
fun GoogleGLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = size.width * 0.22f
            val radius = (size.width - strokeW) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Red arc (top / top-left)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 180f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeW)
            )
            // Blue arc (top-right & right)
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = 285f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeW)
            )
            // Green arc (bottom / bottom-left)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 25f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeW)
            )
            // Yellow arc (bottom-left)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 115f,
                sweepAngle = 65f,
                useCenter = false,
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeW)
            )
            // Blue horizontal crossbar
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(center.x, center.y),
                end = Offset(size.width - strokeW / 3, center.y),
                strokeWidth = strokeW
            )
        }
    }
}

/**
 * Modern Material 3 Google Sign-In Button integrated with Firebase Auth.
 */
@Composable
fun GoogleSignInButton(
    text: String = "Sign in with Google",
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F2937)
        ),
        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = BrandBlue,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Connecting to Firebase Auth...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF374151))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleGLogo(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            }
        }
    }
}

/**
 * ⏳ Pending Approval Splash Screen for new employees
 * Displays immediately after initial registration/login if administrator approval is pending.
 * Features live Firestore status checks, reminder ping to Admin, Milo mascot animations,
 * and clear step-by-step progress tracking.
 */
@Composable
fun PendingApprovalScreen(
    viewModel: MainViewModel,
    onApproved: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val employeeName by viewModel.currentEmployeeName.collectAsState()
    val employees by viewModel.employees.collectAsState()

    var isCheckingStatus by remember { mutableStateOf(false) }
    var reminderSent by remember { mutableStateOf(false) }

    // Find current employee in employees list
    val currentEmp = employees.find { it.name.equals(employeeName, ignoreCase = true) || it.email.contains(employeeName, ignoreCase = true) }
    val isApproved = currentEmp?.status == EmployeeStatus.ACTIVE

    // Auto-check if approved
    LaunchedEffect(currentEmp?.status) {
        if (isApproved) {
            Toast.makeText(context, "🎉 Your account has been approved by Admin! Opening workspace...", Toast.LENGTH_LONG).show()
            onApproved()
        }
    }

    fun checkStatus() {
        isCheckingStatus = true
        MiloHaptics.performButtonClick(context)
        scope.launch {
            delay(1200)
            isCheckingStatus = false
            if (isApproved) {
                Toast.makeText(context, "🎉 Account Approved!", Toast.LENGTH_SHORT).show()
                onApproved()
            } else {
                Toast.makeText(context, "Status Checked: Still pending Admin approval.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendAdminReminder() {
        if (reminderSent) {
            Toast.makeText(context, "Reminder already sent to Admin!", Toast.LENGTH_SHORT).show()
            return
        }
        reminderSent = true
        MiloHaptics.performSuccess(context)
        viewModel.addNotification(
            title = "🔔 Employee Access Request",
            subtitle = "New registration: $employeeName is waiting for administrator approval.",
            category = "approval"
        )
        Toast.makeText(context, "🔔 Access reminder dispatched to Admin!", Toast.LENGTH_LONG).show()
    }

    Scaffold(
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Milo Lion Mascot in WORKING / THINKING state
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MiloCharacter(state = MiloState.WORKING, size = 110.dp, showStateBadge = true)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Badge / Headline
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PENDING ADMIN APPROVAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome, $employeeName!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your initial registration was successful. Your account is currently undergoing administrator verification before granting access to Making Brands Enterprise Cloud.",
                    fontSize = 13.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Checklist Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Verification Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        HorizontalDivider(color = Color(0xFF334155))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Email / Credentials Verified", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text("Registration authenticated with Firebase", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFFEAB308), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Administrator Approval", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFDE68A))
                                Text("Waiting for HR / Admin authorization", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Workspace Enclave Provisioning", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                                Text("Activates upon Admin approval", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = { checkStatus() },
                    enabled = !isCheckingStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .liftOnPress(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    if (isCheckingStatus) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Checking Status...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Approval Status", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { sendAdminReminder() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (reminderSent) "Reminder Dispatched ✓" else "Send Admin Reminder Ping",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onSignOut) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out & Return to Login", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
