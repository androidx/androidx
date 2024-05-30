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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable class NumberedDestination(val number: Int)

@Composable
fun NavPopUpToDemo() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = NumberedDestination(1)) {
        composable<NumberedDestination> {
            NumberedScreen(navController, it.toRoute<NumberedDestination>().number)
        }
    }
}

@Composable
fun NumberedScreen(navController: NavController, number: Int) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        val next = number + 1
        if (number < 5) {
            Button(
                onClick = { navController.navigate(NumberedDestination(next)) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Navigate to Screen $next")
            }
        }
        Text("This is screen $number", Modifier.weight(1f))
        if (navController.previousBackStackEntry != null) {
            val firstScreen = NumberedDestination(1)
            Button(
                onClick = {
                    navController.navigate(firstScreen) {
                        popUpTo(firstScreen) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "PopUpTo Screen 1")
            }
        }
    }
}
