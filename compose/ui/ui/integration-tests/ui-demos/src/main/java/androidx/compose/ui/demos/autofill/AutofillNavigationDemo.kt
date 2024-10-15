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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Preview
@Composable
fun AutofillNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("submit") { SubmittedScreen(navController) }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        content = { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Navigation Button
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Login"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to Login")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text =
                        "This is the Home Screen. From here, you can navigate to the Login Screen."
                )
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(navController: NavController) {
    val autofillManager = LocalAutofillManager.current

    Scaffold(
        content = { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Back Button -------------------------------------------------
                Button(
                    onClick = {
                        navController.navigate("home")
                        autofillManager?.cancel()
                    },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to Home")
                }

                // Submit Button -------------------------------------------------
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        navController.navigate("submit")
                        autofillManager?.commit()
                    },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Submit"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text =
                        """This is the Login Screen. You can go back to the Home Screen or 
                            |enter submit your credentials below."""
                            .trimMargin()
                )

                // Enter Credentials -------------------------------------------------
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Enter your username and password below:")
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                            ContentType.Username
                        }
                )

                BasicTextField(
                    state = remember { TextFieldState() },
                    modifier =
                        Modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                            ContentType.Username
                        }
                )
            }
        }
    )
}

@Composable
fun SubmittedScreen(navController: NavController) {
    Scaffold(
        content = { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Back Button
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Login"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to Login")
                }

                // Descriptive Text
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text =
                        """This is the Success Screen. You can only go back to the 
                            |Login Screen from here."""
                            .trimMargin()
                )
            }
        }
    )
}
