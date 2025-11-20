/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Preview
@Composable
fun MultiPageLoginDemo() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val navController = rememberNavController()
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = "UsernameScreen",
        ) {
            composable("UsernameScreen") { Screen1(navController) }
            composable("PasswordScreen") { Screen2(navController) }
            composable("SubmitScreen") { Screen3() }
        }
    }
}

@Composable
fun Screen1(navController: NavController) {
    var username by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Text("Enter username")
        TextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
        )

        Button(onClick = { navController.navigate("PasswordScreen") }) {
            Text("Go to password screen")
        }
    }
}

@Composable
fun Screen2(navController: NavController) {
    Column(Modifier.fillMaxSize()) {
        var password by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Text("Enter password")
        TextField(
            value = password,
            onValueChange = { password = it },
            modifier =
                Modifier.fillMaxWidth().focusRequester(focusRequester).semantics {
                    contentType = ContentType.Password
                },
        )

        Button(onClick = { navController.navigate("SubmitScreen") }) { Text("Go to submit screen") }
    }
}

@Composable
fun Screen3() {
    Column(Modifier.fillMaxSize()) { Text("Credentials have been submitted.") }
}
