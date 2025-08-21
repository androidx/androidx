/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.demos.autofill

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** Autofill sample displaying navigation capabilities. */
@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun AutofillNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("submit") { SubmittedScreen(navController) }
        composable("scrolling-then-register") { ScrollingRegisterScreen(navController) }
        composable("register-then-scrolling") { RegisterThenScrollScreen(navController) }
    }
}

/** Home screen that the sample app will land on. */
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        content = { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Registration Button
                Button(
                    onClick = { navController.navigate("register") },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Register",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to registration screen.")
                }

                // Scrolling registration button 1
                Button(
                    onClick = { navController.navigate("scrolling-then-register") },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to scrolling",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to scrolling registration screen 1.")
                }

                // Scrolling registration button 2
                Button(
                    onClick = { navController.navigate("register-then-scrolling") },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to scrolling",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to scrolling registration screen 2.")
                }

                Spacer(modifier = Modifier.height(16.dp))
                val homescreenText =
                    """ 
                    This is the Home Screen. From here, you can navigate to the registration screen,
                    or scrolling login screens. Scrolling screen 1 has a username field, then 
                    scrolling content, then a password field. Scrolling screen 2 has credentials, 
                    then content. After registering credentials, you may then return to use the 
                    login page.
                    """
                        .trimIndent()
                Text(text = homescreenText)

                Spacer(modifier = Modifier.height(16.dp))
                // Login Button
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to login",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to the login screen.")
                }
            }
        }
    )
}

/** Submitted screen that all registration routes will lead to. */
@Composable
fun SubmittedScreen(navController: NavController) {
    Scaffold(
        content = { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Back Button
                Button(
                    onClick = { navController.navigate("home") },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to Home")
                }

                // Descriptive Text
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text =
                        "This is the Success Screen. You can only go back to the home screen from here."
                )
            }
        }
    )
}

/**
 * Registration screen for new credentials — navigating away from this page should trigger the save
 * dialog when new credentials are entered or existing credentials are updated.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegisterScreen(navController: NavController) {
    TwoButtonNavigationScaffold(navController, null, "home", content = { RegisterScreenContent() })
}

/**
 * Login screen that should trigger autofill options to appear. This is meant for entering in
 * credentials that have already been saved with a password manager.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LoginScreen(navController: NavController) {
    TwoButtonNavigationScaffold(navController, "home", "submit", content = { LoginScreenContent() })
}

/** Registration screen that has content in between two autofillable components. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScrollingRegisterScreen(navController: NavController) {
    TwoButtonNavigationScaffold(
        navController,
        null,
        "home",
        content = { ScrollingRegisterScreenContent() },
    )
}

/** Registration screen that has autofillable components followed by scrolling content. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegisterThenScrollScreen(navController: NavController) {
    TwoButtonNavigationScaffold(
        navController,
        null,
        "home",
        content = { RegisterThenScrollScreenContent() },
    )
}

// ============================================================================================
// Screen content
// ============================================================================================

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegisterScreenContent() {
    var showPassword by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "This is the register screen. Toggle to show the passworld field.")

        // Enter Credentials -------------------------------------------------
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Enter your username below:")
        Spacer(modifier = Modifier.height(8.dp))

        NavigationDemoTextField(contentType = ContentType.NewUsername)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showPassword, onCheckedChange = { showPassword = it })
            Text("Show password field.")
        }

        if (showPassword) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Enter your password below:")
            Spacer(modifier = Modifier.height(8.dp))
            NavigationDemoTextField(contentType = ContentType.NewPassword)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LoginScreenContent() {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text =
                "This is the Login Screen. You can go back to the Home " +
                    "Screen or enter submit your credentials below."
        )

        // Enter Credentials -------------------------------------------------
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Enter your username and password below:")
        Spacer(modifier = Modifier.height(8.dp))
        NavigationDemoTextField(contentType = ContentType.Username)
        NavigationDemoTextField(contentType = ContentType.Password)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScrollingRegisterScreenContent() {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        // Enter Credentials -------------------------------------------------
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Enter your username and password below:")
        Spacer(modifier = Modifier.height(8.dp))
        NavigationDemoTextField(contentType = ContentType.NewUsername)

        repeat(50) {
            Text(
                text =
                    "Filler content between username and password. Scrolling past " +
                        "the username should not trigger save dialog."
            )
        }

        NavigationDemoTextField(contentType = ContentType.NewPassword)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegisterThenScrollScreenContent() {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Spacer(modifier = Modifier.height(16.dp))
        // Enter Credentials -------------------------------------------------
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Enter your username and password below:")
        Spacer(modifier = Modifier.height(8.dp))
        NavigationDemoTextField(contentType = ContentType.NewUsername)
        NavigationDemoTextField(contentType = ContentType.NewPassword)

        repeat(50) {
            Text(
                text =
                    "Filler content after username and password. " +
                        "Scrolling past the credentials should trigger save dialog."
            )
        }
    }
}

// ============================================================================================
// Helper functions
// ============================================================================================

@Composable
fun NavigationDemoTextField(
    modifier: Modifier = Modifier,
    state: TextFieldState = remember { TextFieldState() },
    contentType: ContentType,
    textStyle: TextStyle = LocalTextStyle.current.copy(color = Color.White),
) {
    BasicTextField(
        state = state,
        modifier =
            modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                this.contentType = contentType
            },
        textStyle = textStyle,
    )
}

/** Template scaffold for the navigation demo with two buttons: forward and backwards. */
@Composable
fun TwoButtonNavigationScaffold(
    navController: NavController,
    forwardRoute: String? = null,
    backwardRoute: String,
    content: @Composable () -> Unit,
) {
    Scaffold(
        content = { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Navigation Button backwards
                Button(
                    onClick = { navController.navigate(backwardRoute) },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go to $backwardRoute",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to $backwardRoute.")
                }

                if (forwardRoute != null) {
                    // Navigation Button forwards
                    Button(
                        onClick = { navController.navigate(forwardRoute) },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go to $forwardRoute",
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Go to $forwardRoute screen")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                content()
            }
        }
    )
}
