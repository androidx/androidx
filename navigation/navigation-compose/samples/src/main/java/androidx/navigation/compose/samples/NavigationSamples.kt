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

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import java.util.UUID
import kotlin.reflect.typeOf
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable
object Profile {
    val resourceId: Int = R.string.profile
}

@Serializable
object Scrollable {
    val resourceId: Int = R.string.scrollable
}

@Serializable
object Dialog {
    val resourceId: Int = R.string.dialog
}

@Serializable
data class Dashboard(val userId: String? = "no value given") {
    companion object {
        val resourceId: Int = R.string.dashboard
    }
}

@Serializable object Nested

@Serializable data class NestedWithArg(val userId: String? = "default nested arg")

@Composable
fun BasicNav() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Profile) {
        composable<Profile> { Profile(navController) }
        composable<Dashboard>(
            enterTransition = {
                if (initialState.destination.hasRoute<Scrollable>()) {
                    // Slide in when entering from Scrollable
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)
                } else {
                    null
                }
            },
            popExitTransition = {
                if (targetState.destination.hasRoute<Scrollable>()) {
                    // Slide out when popping back to Scrollable
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End)
                } else {
                    null
                }
            }
        ) {
            Dashboard(navController)
        }
        composable<Scrollable>(
            exitTransition = {
                if (targetState.destination.hasRoute<Dashboard>()) {
                    // Slide out when navigating to Dashboard
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start)
                } else {
                    null
                }
            },
            popEnterTransition = {
                if (initialState.destination.hasRoute<Dashboard>()) {
                    // Slide back in when returning from Dashboard
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End)
                } else {
                    null
                }
            }
        ) {
            Scrollable(navController)
        }
        dialog<Dialog> { DialogContent(navController) }
    }
}

@Composable
fun NestedNavStartDestination() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Nested) {
        navigation<Nested>(startDestination = Profile) {
            composable<Profile> { Profile(navController) }
        }
        composable<Dashboard> { Dashboard(navController) }
        composable<Scrollable> { Scrollable(navController) }
        dialog<Dialog> { DialogContent(navController) }
    }
}

@Composable
fun NestedNavInGraph() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Profile) {
        composable<Profile> { Profile(navController) }
        navigation<Dashboard>(startDestination = Nested) {
            composable<Nested> { Dashboard(navController) }
        }
        composable<Scrollable> { Scrollable(navController) }
        dialog<Dialog> { DialogContent(navController) }
    }
}

@Sampled
@Composable
fun NavScaffold() {
    val navController = rememberNavController()
    Scaffold { innerPadding ->
        NavHost(navController, Profile, Modifier.padding(innerPadding)) {
            composable<Profile> { Profile(navController) }
            composable<Dashboard> { Dashboard(navController) }
            composable<Scrollable> { Scrollable(navController) }
            dialog<Dialog> { DialogContent(navController) }
        }
    }
}

@Sampled
@Composable
fun NavWithArgsInNestedGraph() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Profile) {
        composable<Profile> { ProfileWithArgs(navController) }
        navigation<Dashboard>(startDestination = NestedWithArg::class) {
            composable<NestedWithArg> {
                // argument from parent graph Destination.Dashboard will automatically be
                // bundled into the start destination's arguments
                val userId = it.toRoute<NestedWithArg>().userId
                Dashboard(navController, userId)
            }
        }
    }
}

@Composable
fun Profile(navController: NavHostController) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = stringResource(Profile.resourceId))
        NavigateButton(stringResource(Dashboard.resourceId)) { navController.navigate(Dashboard()) }
        Divider(color = Color.Black)
        NavigateButton(stringResource(Scrollable.resourceId)) { navController.navigate(Scrollable) }
        Divider(color = Color.Black)
        NavigateButton(stringResource(Dialog.resourceId)) { navController.navigate(Dialog) }
        Spacer(Modifier.weight(1f))
        NavigateBackButton(navController)
    }
}

@Composable
fun ProfileWithArgs(navController: NavController) {
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
        NavigateButton("Dashboard with userId") { navController.navigate(Dashboard(state.value)) }
    }
}

@Composable
fun Dashboard(navController: NavController, title: String? = null) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = title ?: stringResource(Dashboard.resourceId))
        Spacer(Modifier.weight(1f))
        NavigateBackButton(navController)
    }
}

@Composable
fun Scrollable(navController: NavController) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        NavigateButton(stringResource(Dashboard.resourceId)) { navController.navigate(Dashboard()) }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(phrases) { phrase -> Text(phrase, fontSize = 30.sp) }
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
            items(phrases) { phrase -> Text(phrase, fontSize = 16.sp) }
        }
    }
}

@Composable
fun NavigateButton(text: String, listener: () -> Unit = {}) {
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
    if (
        navController.currentBackStackEntry == LocalLifecycleOwner.current &&
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

@Preview
@Composable
fun NavHostPreview() {
    CompositionLocalProvider(
        LocalInspectionMode provides true,
    ) {
        Box(Modifier.fillMaxSize().background(Color.Red)) {
            NavHost(navController = rememberNavController(), startDestination = "home") {
                composable("home") {
                    Box(Modifier.fillMaxSize().background(Color.Blue)) {
                        Text(text = "test", modifier = Modifier.testTag("text"))
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun ThirdPartySerializableType() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = User(WrappedUUID(UUID.randomUUID()))) {
        composable<User>(typeMap = mapOf(typeOf<WrappedUUID>() to WrappedUUIDNavType)) { entry ->
            val uuid = entry.toRoute<User>().id.uuid
            Box(Modifier.fillMaxSize().background(Color.Blue)) {
                Text(text = "uuid: $uuid", modifier = Modifier.testTag("text"))
            }
        }
    }
}

@Serializable class User(val id: WrappedUUID)

@Serializable class WrappedUUID(@Serializable(with = UUIDSerializer::class) val uuid: UUID)

object WrappedUUIDNavType : NavType<WrappedUUID>(isNullableAllowed = false) {
    override fun put(bundle: Bundle, key: String, value: WrappedUUID) {
        bundle.putString(key, value.uuid.toString())
    }

    override fun get(bundle: Bundle, key: String): WrappedUUID? =
        WrappedUUID(UUID.fromString(bundle.getString(key)))

    override fun parseValue(value: String): WrappedUUID = WrappedUUID(UUID.fromString(value))

    override fun serializeAsValue(value: WrappedUUID): String = Uri.encode(value.uuid.toString())
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
}

private val phrases =
    listOf(
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

    @Suppress("DEPRECATION")
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
