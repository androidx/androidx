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

package androidx.navigation3.demos

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.navEntryDecorator
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.Scene
import androidx.navigation3.ui.SceneStrategy
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import kotlin.collections.forEach
import kotlin.math.max
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** The [SharedTransitionScope] of the [NavDisplay]. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalNavSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope> =
    compositionLocalOf {
        throw IllegalStateException(
            "Unexpected access to LocalNavSharedTransitionScope. You must provide a " +
                "SharedTransitionScope from a call to SharedTransitionLayout() or " +
                "SharedTransitionScope()"
        )
    }

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalUuidApi::class)
@Composable
fun HierarchicalSceneSample() {
    /**
     * A [NavEntryDecorator] that wraps each entry in a shared element that is controlled by the
     * [Scene].
     */
    val sharedEntryInSceneNavEntryDecorator =
        navEntryDecorator<Any> { entry ->
            with(LocalNavSharedTransitionScope.current) {
                Box(
                    Modifier.sharedElement(
                        rememberSharedContentState(entry.contentKey),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                    )
                ) {
                    entry.Content()
                }
            }
        }

    var columns by rememberSaveable { mutableIntStateOf(1) }

    val backStack: MutableList<ColorEntry> =
        rememberSerializable(serializer = SnapshotStateListSerializer<ColorEntry>()) {
            mutableStateListOf(ColorEntry(Random.nextColor(), Uuid.random()))
        }
    val sceneStrategy = remember(columns) { HierarchicalSceneStrategy<ColorEntry>(columns) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { columns = max(1, columns - 1) }) { Text("-") }
            Text("Columns: $columns")
            Button(onClick = { columns += 1 }) { Text("+") }
            Button(onClick = { backStack.add(ColorEntry(Random.nextColor(), Uuid.random())) }) {
                Text("Add new color")
            }
        }
        SharedTransitionLayout {
            CompositionLocalProvider(LocalNavSharedTransitionScope provides this) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                    entryDecorators =
                        listOf(
                            sharedEntryInSceneNavEntryDecorator,
                            rememberSceneSetupNavEntryDecorator(),
                            rememberSavedStateNavEntryDecorator(),
                        ),
                    sceneStrategy = sceneStrategy,
                ) {
                    NavEntry(key = it, contentKey = it.id) { entry ->
                        Box(
                            modifier = Modifier.background(entry.color).fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(entry.color.toString())

                                var counter by rememberSaveable { mutableIntStateOf(0) }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { counter-- }) { Text("-") }
                                    Text(
                                        "rememberSaveable counter: $counter",
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Button(onClick = { counter++ }) { Text("+") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Random.nextColor() =
    Color(red = nextInt(256), green = nextInt(256), blue = nextInt(256))

@Serializable
@OptIn(ExperimentalUuidApi::class)
private data class ColorEntry(
    val color: @Serializable(with = ColorSerializer::class) Color,
    val id: Uuid,
)

private object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("androidx.compose.ui.graphics.Color", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Color {
        val longValue = decoder.decodeLong()
        return Color(longValue.toULong())
    }

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeLong(value.value.toLong())
    }
}

private class HierarchicalScene<T : Any>(
    private val navEntries: List<NavEntry<T>?>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = navEntries.filterNotNull()
    override val key: Any = navEntries.map { it?.contentKey }

    override val content: @Composable () -> Unit = {
        Row {
            navEntries.forEach { navEntry ->
                Box(Modifier.weight(1f)) {
                    if (navEntry != null) {
                        key(navEntry.contentKey) { navEntry.Content() }
                    }
                }
            }
        }
    }
}

private class HierarchicalSceneStrategy<T : Any>(private val columns: Int) : SceneStrategy<T> {
    @Composable
    override fun calculateScene(
        entries: List<NavEntry<T>>,
        onBack: (count: Int) -> Unit,
    ): Scene<T> {
        val includedEntries = entries.takeLast(columns)
        return remember(columns, includedEntries) {
            HierarchicalScene(
                List(columns, includedEntries::getOrNull),
                previousEntries =
                    if (entries.size > columns) {
                        entries.dropLast(1)
                    } else {
                        emptyList()
                    },
            )
        }
    }
}
