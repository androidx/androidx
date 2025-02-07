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

package androidx.navigation3.samples

import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation3.LocalNavAnimatedContentScope
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer

@Serializable
object Profile {
    val resourceId: Int = R.string.profile
}

@Serializable
object Scrollable {
    val resourceId: Int = R.string.scrollable
}

@Serializable
object DialogBase {
    val resourceId: Int = R.string.dialog
}

@Serializable
data class Dashboard(val userId: String? = "no value given") {
    companion object {
        val resourceId: Int = R.string.dashboard
    }
}

@Serializable object CatList

@Serializable data class CatDetail(val cat: Cat)

@Serializable
data class Cat(@DrawableRes val imageId: Int, val name: String, val description: String)

@Composable
fun Profile(viewModel: ProfileViewModel, navigateTo: (Any) -> Unit, onBack: () -> Unit) {
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
fun Scrollable(navigateTo: (Any) -> Unit, onBack: () -> Unit) {
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CatList(sharedScope: SharedTransitionScope, onClick: (cat: Cat) -> Unit) {
    Column {
        catList.forEach { cat: Cat ->
            Row(Modifier.clickable { onClick(cat) }) {
                with(sharedScope) {
                    val imageModifier =
                        Modifier.size(100.dp)
                            .sharedElement(
                                sharedScope.rememberSharedContentState(key = cat.imageId),
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                    Image(painterResource(cat.imageId), cat.description, imageModifier)
                    Text(cat.name)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CatDetail(cat: Cat, sharedScope: SharedTransitionScope, onBack: () -> Unit) {
    Column {
        Box {
            with(sharedScope) {
                val imageModifier =
                    Modifier.size(300.dp)
                        .sharedElement(
                            sharedScope.rememberSharedContentState(key = cat.imageId),
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current
                        )
                Image(painterResource(cat.imageId), cat.description, imageModifier)
            }
        }
        Text(cat.name)
        Text(cat.description)
        NavigateBackButton(onBack)
    }
}

@Composable
fun NavigateButton(text: String, listener: () -> Unit = {}) {
    Button(
        onClick = listener,
        colors = ButtonDefaults.buttonColors(containerColor = LightGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Navigate to $text")
    }
}

@Composable
fun NavigateBackButton(onBack: () -> Unit) {
    Button(
        onClick = onBack,
        colors = ButtonDefaults.buttonColors(containerColor = LightGray),
        modifier = Modifier.fillMaxWidth()
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
        "Go For Broke"
    )

private val catList: List<Cat> =
    listOf(
        Cat(R.drawable.cat_1, "happy", "cat lying down"),
        Cat(R.drawable.cat_2, "lucky", "cat playing"),
        Cat(R.drawable.cat_3, "chocolate cake", "cat upside down"),
    )

@Composable
fun <T : Any> rememberMutableStateListOf(vararg elements: T): SnapshotStateList<Any> {
    return rememberSaveable(saver = snapshotStateListSaver(serializableListSaver())) {
        elements.toList().toMutableStateList()
    }
}

inline fun <reified T : Any> serializableListSaver(
    serializer: KSerializer<T> = UnsafePolymorphicSerializer()
) =
    listSaver(
        save = { list -> list.map { encodeToSavedState(serializer, it) } },
        restore = { list -> list.map { decodeFromSavedState(serializer, it) } }
    )

@Suppress("UNCHECKED_CAST")
fun <T> snapshotStateListSaver(
    listSaver: Saver<List<T>, out Any> = autoSaver()
): Saver<SnapshotStateList<T>, Any> =
    with(listSaver as Saver<List<T>, Any>) {
        Saver(
            save = { state ->
                // We use toMutableList() here to ensure that save() is
                // sent a list that is saveable by default (e.g., something
                // that autoSaver() can handle)
                save(state.toList().toMutableList())
            },
            restore = { state -> restore(state)?.toMutableStateList() }
        )
    }

@OptIn(InternalSerializationApi::class)
class UnsafePolymorphicSerializer<T : Any> : KSerializer<T> {

    override val descriptor =
        buildClassSerialDescriptor("PolymorphicData") {
            element(elementName = "type", serialDescriptor<String>())
            element(elementName = "payload", buildClassSerialDescriptor("Any"))
        }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        return decoder.decodeStructure(descriptor) {
            val className = decodeStringElement(descriptor, decodeElementIndex(descriptor))
            val classRef = Class.forName(className).kotlin
            val serializer = classRef.serializer()

            decodeSerializableElement(descriptor, decodeElementIndex(descriptor), serializer) as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeStructure(descriptor) {
            val className = value::class.java.canonicalName!!
            encodeStringElement(descriptor, index = 0, className)
            val serializer = value::class.serializer() as KSerializer<T>
            encodeSerializableElement(descriptor, index = 1, serializer, value)
        }
    }
}
