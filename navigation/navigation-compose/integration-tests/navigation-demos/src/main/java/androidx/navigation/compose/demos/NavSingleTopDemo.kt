/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.compose.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.samples.NavigateButton

@Composable
fun NavSingleTopDemo() {
    val navController = rememberNavController()
    val query = rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        TextField(
            value = query.value,
            onValueChange = { query.value = it },
            placeholder = { Text("Search") }
        )
        NavigateButton("Search") {
            navController.navigate("search/" + query.value) {
                launchSingleTop = true
            }
        }
        NavHost(navController, startDestination = "start") {
            composable("start") { StartScreen() }
            composable("search/{query}") { backStackEntry ->
                SearchResultScreen(
                    backStackEntry.arguments!!.getString("query", "no query entered")
                )
            }
        }
    }
}

@Composable
fun StartScreen() {
    Divider(color = Color.Black)
    Text(text = "Start a search above")
}

@Composable
fun SearchResultScreen(query: String) {
    Text("You searched for $query")
}
