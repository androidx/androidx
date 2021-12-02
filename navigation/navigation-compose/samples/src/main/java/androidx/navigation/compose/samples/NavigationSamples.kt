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

package androidx.navigation.compose.samples

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Sampled
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object Profile : Screen("profile", R.string.profile)
    object Dashboard : Screen("dashboard", R.string.dashboard)
    object Scrollable : Screen("scrollable", R.string.scrollable)
    object Dialog : Screen("dialog", R.string.dialog)
}

@Composable
fun BasicNav() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Profile.route) {
        composable(Screen.Profile.route) { Profile(navController) }
        composable(Screen.Dashboard.route) { Dashboard(navController) }
        composable(Screen.Scrollable.route) { Scrollable(navController) }
        dialog(Screen.Dialog.route) { DialogContent(navController) }
    }
}

@Composable
fun NestedNavStartDestination() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "nested") {
        navigation(startDestination = Screen.Profile.route, route = "nested") {
            composable(Screen.Profile.route) { Profile(navController) }
        }
        composable(Screen.Dashboard.route) { Dashboard(navController) }
        composable(Screen.Scrollable.route) { Scrollable(navController) }
        dialog(Screen.Dialog.route) { DialogContent(navController) }
    }
}

@Composable
fun NestedNavInGraph() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Profile.route) {
        composable(Screen.Profile.route) { Profile(navController) }
        navigation(startDestination = "nested", route = Screen.Dashboard.route) {
            composable("nested") { Dashboard(navController) }
        }
        composable(Screen.Scrollable.route) { Scrollable(navController) }
        dialog(Screen.Dialog.route) { DialogContent(navController) }
    }
}

@Sampled
@Composable
fun NavScaffold() {
    val navController = rememberNavController()
    Scaffold { innerPadding ->
        NavHost(navController, Screen.Profile.route, Modifier.padding(innerPadding)) {
            composable(Screen.Profile.route) { Profile(navController) }
            composable(Screen.Dashboard.route) { Dashboard(navController) }
            composable(Screen.Scrollable.route) { Scrollable(navController) }
            dialog(Screen.Dialog.route) { DialogContent(navController) }
        }
    }
}

@Composable
fun NavWithArgs() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Profile.route) {
        composable(Screen.Profile.route) { Profile(navController) }
        composable(
            Screen.Dashboard.route,
            arguments = listOf(navArgument("userId") { defaultValue = "no value given" })
        ) { backStackEntry ->
            Dashboard(navController, backStackEntry.arguments?.getString("userId"))
        }
    }
}

@Sampled
@Composable
fun NestedNavInGraphWithArgs() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Profile.route) {
        composable(Screen.Profile.route) { Profile(navController) }
        navigation(
            startDestination = "nested",
            route = Screen.Dashboard.route,
            // This value will be sent to the start destination of the graph when you navigate to
            // this graph
            arguments = listOf(navArgument("userId") { defaultValue = "no value given" })
        ) {
            composable(
                "nested",
                // We don't need to set a default value here because the start destination will
                // automatically receive the arguments of its parent graph
                arguments = listOf(navArgument("userId") { })
            ) {
                Dashboard(navController)
            }
        }
    }
}

@Composable
fun Profile(navController: NavHostController) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = stringResource(Screen.Profile.resourceId))
        NavigateButton(stringResource(Screen.Dashboard.resourceId)) {
            navController.navigate(Screen.Dashboard.route)
        }
        Divider(color = Color.Black)
        NavigateButton(stringResource(Screen.Scrollable.resourceId)) {
            navController.navigate(Screen.Scrollable.route)
        }
        Divider(color = Color.Black)
        NavigateButton(stringResource(Screen.Dialog.resourceId)) {
            navController.navigate(Screen.Dialog.route)
        }
        Spacer(Modifier.weight(1f))
        NavigateBackButton(navController)
    }
}

@Composable
fun Dashboard(navController: NavController, title: String? = null) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = title ?: stringResource(Screen.Dashboard.resourceId))
        Spacer(Modifier.weight(1f))
        NavigateBackButton(navController)
    }
}

@Composable
fun Scrollable(navController: NavController) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        NavigateButton(stringResource(Screen.Dashboard.resourceId)) {
            navController.navigate(Screen.Dashboard.route)
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(phrases) { phrase ->
                Text(phrase, fontSize = 30.sp)
            }
        }
        NavigateBackButton(navController)
    }
}

@Composable
fun DialogContent(navController: NavController) {
    val dialogWidth = 300.dp
    val dialogHeight = 300.dp
    Column(Modifier.size(dialogWidth, dialogHeight).background(Color.White).padding(8.dp)) {
        NavigateBackButton(navController)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(phrases) { phrase ->
                Text(phrase, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun NavigateButton(
    text: String,
    listener: () -> Unit = { }
) {
    Button(
        onClick = listener,
        colors = ButtonDefaults.buttonColors(backgroundColor = LightGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Navigate to $text")
    }
}

@Composable
fun NavigateBackButton(navController: NavController) {
    // Use LocalLifecycleOwner.current as a proxy for the NavBackStackEntry
    // associated with this Composable
    if (navController.currentBackStackEntry == LocalLifecycleOwner.current &&
        navController.previousBackStackEntry != null
    ) {
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(backgroundColor = LightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Go to Previous screen")
        }
    }
}

private val phrases = listOf(
    "Easy As Pie",
    "Wouldn't Harm a Fly",
    "No-Brainer",
    "Keep On Truckin'",
    "An Arm and a Leg",
    "Down To Earth",
    "Under the Weather",
    "Up In Arms",
    "Cup Of Joe",
    "Not the Sharpest Tool in the Shed",
    "Ring Any Bells?",
    "Son of a Gun",
    "Hard Pill to Swallow",
    "Close But No Cigar",
    "Beating a Dead Horse",
    "If You Can't Stand the Heat, Get Out of the Kitchen",
    "Cut To The Chase",
    "Heads Up",
    "Goody Two-Shoes",
    "Fish Out Of Water",
    "Cry Over Spilt Milk",
    "Elephant in the Room",
    "There's No I in Team",
    "Poke Fun At",
    "Talk the Talk",
    "Know the Ropes",
    "Fool's Gold",
    "It's Not Brain Surgery",
    "Fight Fire With Fire",
    "Go For Broke"
)

@Serializable
@Parcelize
@Suppress("BanParcelableUsage")
data class SearchParameters(val searchQuery: String, val filters: List<String>) : Parcelable

class SearchParametersType : NavType<SearchParameters>(isNullableAllowed = false) {
    override fun put(bundle: Bundle, key: String, value: SearchParameters) {
        bundle.putParcelable(key, value)
    }

    override fun get(bundle: Bundle, key: String): SearchParameters {
        return bundle.getParcelable<SearchParameters>(key) as SearchParameters
    }

    override fun parseValue(value: String): SearchParameters {
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        return Json.decodeFromString(value)
    }

    // Only required when using Navigation 2.4.0-alpha07 and lower
    override val name = "SearchParameters"
}
