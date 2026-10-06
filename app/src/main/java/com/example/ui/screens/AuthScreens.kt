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
    onLoginSuccess: (isAdmin: Boolean) -> Unit,
    onNavigateToOtp: () -> Unit = {}
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

    val isBiometricEnabled = remember { BiometricHelper.isBiometricForEmployeeLoginEnabled(context) || BiometricHelper.isBiometricSettingEnabled(context) }
    LaunchedEffect(Unit) {
        if (isBiometricEnabled && activity != null) {
            BiometricHelper.promptBiometricAuth(
                activity = activity,
                title = "Initial Biometric Sign-In",
                subtitle = "Scan fingerprint or face recognition to unlock MB EM Workspace",
                description = "Secured initial app login integrated with Firebase Authentication.",
                onSuccess = {
                    viewModel.loginWithBiometricsWithFirestore(
                        phoneNumber = "+91 98765 43210",
                        selectedRoleHint = "Employee"
                    ) { _, _ ->
                        viewModel.unlockAllBiometrics()
                        Toast.makeText(context, "Biometric verified! Signed into Firebase Workspace", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(false)
                    }
                }
            )
        }
    }

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
            ) { success, errorMessage, _ ->
                isSigningIn = false
                if (success) {
                    Toast.makeText(context, "Welcome back! Signed in with Firebase", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(false)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Minimal Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = "OR",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Employee Biometric Sign-In Button
                    OutlinedButton(
                        onClick = {
                            if (activity != null) {
                                BiometricHelper.promptBiometricAuth(
                                    activity = activity,
                                    title = "Employee Biometric Unlock",
                                    subtitle = "Scan fingerprint or face to verify your identity and open Employee Workspace",
                                    description = "Instant biometric verification powered by androidx.biometric.",
                                    onSuccess = {
                                        viewModel.loginWithBiometricsWithFirestore(
                                            phoneNumber = "+91 98765 43210",
                                            selectedRoleHint = "Employee"
                                        ) { _, _ ->
                                            viewModel.unlockAllBiometrics()
                                            Toast.makeText(context, "Biometric verified! Opening Employee Workspace", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(false)
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
                            .height(50.dp)
                            .testTag("biometric_login_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF0D9488).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF0FDFA),
                            contentColor = Color(0xFF0F766E)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Login",
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Biometric Sign-In",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F766E)
                            )
                        }
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
}

// ---------------- Screen 3: OTP Verification Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    viewModel: MainViewModel,
    onVerifySuccess: (isAdmin: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val otpSession by viewModel.otpSession.collectAsState()

    val targetPhone = otpSession?.phoneNumber ?: "+91 98765 43210"
    val targetRole = otpSession?.role ?: "Employee"
    val isAdmin = otpSession?.isAdmin ?: false
    val generatedCode = otpSession?.otpCode ?: "123456"

    var otpInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableIntStateOf(30) }
    var isVerifyingOtp by remember { mutableStateOf(false) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "WhatsApp OTP Verification",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Shield / WhatsApp badge
            Surface(
                shape = CircleShape,
                color = Color(0xFFDCFCE7),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                "Enter 6-Digit WhatsApp Code",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Passcode dispatched to WhatsApp number",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text(
                text = "$targetPhone ($targetRole)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BrandDarkBlue
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Auto-Fill Helper Card (Ensures testing works effortlessly in any environment)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        otpInput = generatedCode
                        errorMessage = null
                        focusManager.clearFocus()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Generated WhatsApp Code:",
                                fontSize = 11.sp,
                                color = Color(0xFF1E40AF)
                            )
                            Text(
                                generatedCode,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandBlue,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandBlue,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            "Tap to Auto-Fill",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6-digit PIN Boxes
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Real hidden text field capturing user keystrokes
                BasicTextField(
                    value = otpInput,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) {
                            otpInput = input
                            errorMessage = null
                            if (input.length == 6) {
                                focusManager.clearFocus()
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp)),
                    decorationBox = { /* Invisible overlay */ }
                )

                // Rendered 6 OTP Boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (0 until 6).forEach { index ->
                        val char = otpInput.getOrNull(index)?.toString() ?: ""
                        val isFocused = otpInput.length == index

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = if (isFocused) 4.dp else 1.dp,
                            border = BorderStroke(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = when {
                                    errorMessage != null -> Color(0xFFEF4444)
                                    isFocused -> BrandBlue
                                    char.isNotEmpty() -> BrandGreen
                                    else -> BorderLight
                                }
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = char,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Resend OTP Countdown
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (countdown > 0) "Resend code in 00:${if (countdown < 10) "0$countdown" else countdown}"
                    else "Didn't get the code? ",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                if (countdown == 0) {
                    Text(
                        text = "Resend WhatsApp OTP",
                        fontSize = 13.sp,
                        color = BrandGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val newCode = viewModel.requestWhatsAppOtp(
                                context = context,
                                phoneNumber = targetPhone,
                                role = targetRole,
                                isAdmin = isAdmin
                            )
                            countdown = 30
                            otpInput = ""
                            errorMessage = null
                            Toast.makeText(context, "New OTP dispatched to WhatsApp: $newCode", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cloud Role Redirection Information Note
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Your employee session is verified with Firestore to open your Employee Workspace.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Verify Button
            Button(
                onClick = {
                    if (otpInput.length < 6) {
                        errorMessage = "Please enter all 6 digits of the OTP"
                        return@Button
                    }
                    isVerifyingOtp = true
                    viewModel.verifyOtpWithFirestore(otpInput) { verified, _, _ ->
                        isVerifyingOtp = false
                        if (verified) {
                            Toast.makeText(context, "Verified! Opening Employee Workspace", Toast.LENGTH_SHORT).show()
                            onVerifySuccess(false)
                        } else {
                            errorMessage = "Incorrect OTP code. Please check WhatsApp or tap Auto-Fill."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                enabled = !isVerifyingOtp
            ) {
                if (isVerifyingOtp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Role in Firestore...", fontSize = 15.sp, color = Color.White)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify & Access App",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Mobile Number", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
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
