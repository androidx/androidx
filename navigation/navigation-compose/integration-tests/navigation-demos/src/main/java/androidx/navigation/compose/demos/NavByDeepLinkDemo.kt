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

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.samples.Dashboard
import androidx.navigation.compose.samples.Profile
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute

@Composable
fun NavByDeepLinkDemo() {
    val navController = rememberNavController()
    val basePath = "https://example.com"
    NavHost(navController, startDestination = Profile) {
        composable<Profile> { ProfileWithDeepLink(navController, "$basePath?userId=") }
        composable<Dashboard>(
            // use the same args from Destination.Dashboard with custom uri base path
            deepLinks = listOf(navDeepLink<Dashboard>(basePath))
        ) { backStackEntry ->
            val dashboard = backStackEntry.toRoute<Dashboard>()
            Dashboard(navController, dashboard.userId)
        }
    }
}

@Composable
fun ProfileWithDeepLink(navController: NavController, uri: String) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = stringResource(Profile.resourceId))
        Divider(color = Color.Black)
        val state = rememberSaveable { mutableStateOf("") }
        Box {
            TextField(
                value = state.value,
                onValueChange = { state.value = it },
                placeholder = { Text("Enter userId here") }
            )
        }
        Divider(color = Color.Black)
        Button(
            // navigate with deeplink
            onClick = { navController.navigate(Uri.parse(uri + state.value)) },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Navigate By DeepLink")
        }
    }
}
