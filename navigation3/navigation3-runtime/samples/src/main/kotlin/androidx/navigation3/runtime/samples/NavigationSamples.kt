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

package androidx.navigation3.runtime.samples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object Profile : NavKey {
    val resourceId: Int = R.string.profile
}

class ProfileViewModel : ViewModel() {
    val name = "no user"
}

@Serializable
object Scrollable : NavKey {
    val resourceId: Int = R.string.scrollable
}

@Serializable
object DialogBase : NavKey {
    val resourceId: Int = R.string.dialog
}

@Serializable
data class Dashboard(val userId: String? = "no value given") : NavKey {
    companion object {
        val resourceId: Int = R.string.dashboard
    }
}

@Composable
fun Profile(viewModel: ProfileViewModel, navigateTo: (NavKey) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = "${viewModel.name} ${stringResource(Profile.resourceId)}")
        NavigateButton(stringResource(Dashboard.resourceId)) { navigateTo(Dashboard()) }
        Divider(color = Color.Black)
        NavigateButton(stringResource(Scrollable.resourceId)) { navigateTo(Scrollable) }
        Divider(color = Color.Black)
        NavigateButton(stringResource(DialogBase.resourceId)) { navigateTo(DialogBase) }
        Spacer(Modifier.weight(1f))
        NavigateBackButton(onBack)
    }
}

@Composable
fun Dashboard(title: String? = null, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = title ?: stringResource(Dashboard.resourceId))
        Spacer(Modifier.weight(1f))
        NavigateBackButton(onBack)
    }
}

@Composable
fun Scrollable(navigateTo: (NavKey) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        NavigateButton(stringResource(Dashboard.resourceId)) { navigateTo(Dashboard()) }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(phrases) { phrase -> Text(phrase, fontSize = 30.sp) }
        }
        NavigateBackButton(onBack)
    }
}

@Composable
fun DialogBase(onClick: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.dialog))
        Button(onClick = onClick) { Text("Show Dialog") }
        Spacer(Modifier.weight(1f))
        NavigateBackButton(onBack)
    }
}

@Composable
fun DialogContent(onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        val dialogWidth = 300.dp
        val dialogHeight = 300.dp
        Column(Modifier.size(dialogWidth, dialogHeight).background(Color.White).padding(8.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(phrases) { phrase -> Text(phrase, fontSize = 16.sp) }
            }
        }
    }
}

@Composable
fun NavigateButton(text: String, listener: () -> Unit = {}) {
    Button(
        onClick = listener,
        colors = ButtonDefaults.buttonColors(containerColor = LightGray),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Navigate to $text")
    }
}

@Composable
fun NavigateBackButton(onBack: () -> Unit) {
    Button(
        onClick = onBack,
        colors = ButtonDefaults.buttonColors(containerColor = LightGray),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Go to Previous screen")
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
        "Go For Broke",
    )
