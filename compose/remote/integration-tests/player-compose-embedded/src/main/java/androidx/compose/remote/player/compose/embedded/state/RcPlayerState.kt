/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded.state

import android.graphics.Bitmap
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.player.compose.embedded.LocalComponentValueStateMap
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalCurrentTimeMillis
import androidx.compose.remote.player.compose.embedded.LocalGraphContext
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.getFloatExpressionsReflection
import androidx.compose.remote.player.compose.embedded.getRemoteContextReflection
import androidx.compose.remote.player.compose.embedded.getVariableIdReflection
import androidx.compose.remote.player.compose.embedded.resolveBitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

public class DirectUpdateVariableSupport(public val id: Int, public val update: () -> Unit) :
    VariableSupport {
    override fun updateVariables(context: RemoteContext) {
        update()
    }

    override fun registerListening(context: RemoteContext) {
        context.listensTo(id, this)
    }

    override fun markDirty() {
        update()
    }

    override fun toString(): String {
        return "RcPlayer VariableSupport[$id]"
    }
}

@Composable
internal fun rememberRemoteStringAsState(id: Int): State<String> {
    val document = LocalCoreDocument.current
    val context = LocalRemoteContext.current

    // Derived text (TextFromFloat / TextMerge / TextLookup / …): resolve via the pure-Compose
    // graph.
    val graph = LocalGraphContext.current
    if (graph != null && graph.isComputed(id)) {
        return remember(graph, id) { derivedStateOf { graph.getText(id) ?: "" } }
    }
    // Plain text: reactive read of the snapshot-backed data store.
    return remember(document, id) { derivedStateOf { context.getText(id) ?: "" } }
}

@Composable
internal fun rememberMutableRemoteStringAsState(id: Int): MutableState<String> {
    val document = LocalCoreDocument.current
    val context = LocalRemoteContext.current
    return remember(document, id) {
        object : MutableState<String> {
            override var value: String
                // Read/write the snapshot-backed data store directly: the getter is reactive and
                // the
                // setter's overrideData invalidates readers (no mutableStateOf mirror or listener).
                get() = context.getText(id) ?: ""
                set(v) {
                    if ((context.getText(id) ?: "") != v) {
                        document.remoteComposeState.overrideData(id, v)
                    }
                }

            override fun component1(): String = value

            override fun component2(): (String) -> Unit = { value = it }
        }
    }
}

@Composable
internal fun rememberRemoteIntAsState(id: Int): State<Int> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState

    // Computed integer (IntegerExpression or e.g. TextLength): resolve via the pure-Compose graph.
    val graph = LocalGraphContext.current
    if (graph != null && graph.isComputed(id)) {
        return remember(graph, id) { derivedStateOf { graph.getInteger(id) } }
    }
    // Plain variable: reactive read of the snapshot-backed integer store.
    return remember(document, id) { derivedStateOf { context.getInteger(id) } }
}

@Composable
internal fun rememberRemoteFloatAsState(id: Int): State<Float> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState

    if (
        id == RemoteContext.ID_TIME_IN_SEC ||
            id == RemoteContext.ID_TIME_IN_MIN ||
            id == RemoteContext.ID_TIME_IN_HR
    ) {
        val timeMillisState = LocalCurrentTimeMillis.current
        return remember(timeMillisState) {
            derivedStateOf {
                val timeMillis = timeMillisState.value
                when (id) {
                    RemoteContext.ID_TIME_IN_SEC -> timeMillis / 1000f
                    RemoteContext.ID_TIME_IN_MIN -> timeMillis / 60000f
                    RemoteContext.ID_TIME_IN_HR -> timeMillis / 3600000f
                    else -> 0f
                }
            }
        }
    }

    val componentValueStates = LocalComponentValueStateMap.current
    val compState = componentValueStates[id]
    if (compState != null) {
        return compState
    }

    // Animation-bearing expressions resolve as a Compose-native animated State (Animatable). Plain
    // (non-animated) FloatExpressions fall through to the graph below — one evaluation path.
    val expression = document.getFloatExpressionsReflection()[id]
    if (expression != null && expression.mFloatAnimation != null) {
        return rememberAnimatedRemoteFloat(id)
    }

    // Computed float (FloatExpression or e.g. ImageAttribute): resolve via the pure-Compose graph.
    val graph = LocalGraphContext.current
    if (graph != null && graph.isComputed(id)) {
        return remember(graph, id) { derivedStateOf { graph.getFloat(id) } }
    }
    // Plain variable: reactive read of the snapshot-backed scalar store (no listener bridge).
    return remember(document, id) { derivedStateOf { context.getFloat(id) } }
}

@Composable
internal fun rememberRemoteFloatAsState(value: Float): State<Float> {
    return if (Utils.isVariable(value)) {
        rememberRemoteFloatAsState(Utils.idFromNan(value))
    } else {
        rememberUpdatedState(value)
    }
}

/**
 * Reactive resolver for a long-typed data value (DATA_LONG / `getLong`). Mirrors
 * [rememberRemoteIntAsState] for `Long`; previously long data had no typed reactive resolver.
 */
@Composable
internal fun rememberRemoteLongAsState(id: Int): State<Long> {
    val document = LocalCoreDocument.current
    val context = LocalRemoteContext.current
    val initialValue = context.getLong(id)
    val state = remember(document, id) { mutableLongStateOf(initialValue) }

    DisposableEffect(document, id) {
        val listener =
            DirectUpdateVariableSupport(id) {
                val v = context.getLong(id)
                if (state.longValue != v) state.longValue = v
            }
        document.remoteComposeState.listenToVar(id, listener)
        onDispose {}
    }
    return state
}

@Composable
internal fun rememberRemoteColorAsState(id: Int): State<Color> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState
    // Derived color (ColorExpression / ColorAttribute): resolve via the pure-Compose graph.
    val graph = LocalGraphContext.current
    if (graph != null && graph.isComputed(id)) {
        return remember(graph, id) { derivedStateOf { Color(graph.getColor(id)) } }
    }
    // Plain variable: reactive read of the snapshot-backed color store.
    return remember(document, id) { derivedStateOf { Color(context.getColor(id)) } }
}

@Composable
internal fun rememberRemoteBitmapAsState(id: Int): State<Bitmap?> {
    val document = LocalCoreDocument.current
    val remoteContext = LocalRemoteContext.current
    // Lazy decode: an Image component composing here is the "drawn" trigger. Decode once in a keyed
    // remember (the snapshot write happens here, outside the derived read), then track the
    // snapshot-backed data store so a later host swap of the bitmap recomposes — no listener
    // bridge.
    remember(document, id) { resolveBitmap(remoteContext, id) }
    return remember(document, id) {
        derivedStateOf { remoteContext.mRemoteComposeState.getFromId(id) as? Bitmap }
    }
}

@Composable
internal fun rememberMutableRemoteIntAsState(id: Int): MutableState<Int> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState
    return remember(document, id) {
        object : MutableState<Int> {
            // Read/write the snapshot-backed integer store directly (no mirror/listener). The
            // setter
            // uses overrideInteger so the value lands in the integer store the getter reads — the
            // old
            // overrideData wrote the *data* store, which the getter never saw.
            override var value: Int
                get() = context.getInteger(id)
                set(v) {
                    if (context.getInteger(id) != v) context.overrideInteger(id, v)
                }

            override fun component1(): Int = value

            override fun component2(): (Int) -> Unit = { value = it }
        }
    }
}

@Composable
internal fun rememberMutableRemoteFloatAsState(id: Int): MutableState<Float> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState
    return remember(document, id) {
        object : MutableState<Float> {
            override var value: Float
                get() = context.getFloat(id)
                set(v) {
                    if (context.getFloat(id) != v) context.overrideFloat(id, v)
                }

            override fun component1(): Float = value

            override fun component2(): (Float) -> Unit = { value = it }
        }
    }
}

@Composable
internal fun rememberMutableRemoteColorAsState(id: Int): MutableState<Color> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState
    return remember(document, id) {
        object : MutableState<Color> {
            override var value: Color
                get() = Color(context.getColor(id))
                set(v) {
                    val argb = v.toArgb()
                    if (context.getColor(id) != argb) context.overrideColor(id, argb)
                }

            override fun component1(): Color = value

            override fun component2(): (Color) -> Unit = { value = it }
        }
    }
}

// Named helpers

@Composable
internal fun rememberNamedRemoteStringAsState(name: String): MutableState<String> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState.getRemoteContextReflection()!!
    val id = context.getVariableIdReflection(name)
    return rememberMutableRemoteStringAsState(id)
}

@Composable
internal fun rememberNamedRemoteIntAsState(name: String): MutableState<Int> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState.getRemoteContextReflection()!!
    val id = context.getVariableIdReflection(name)
    return rememberMutableRemoteIntAsState(id)
}

@Composable
internal fun rememberNamedRemoteFloatAsState(name: String): MutableState<Float> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState.getRemoteContextReflection()!!
    val id = context.getVariableIdReflection(name)
    return rememberMutableRemoteFloatAsState(id)
}

@Composable
internal fun rememberNamedRemoteColorAsState(name: String): MutableState<Color> {
    val document = LocalCoreDocument.current
    val context = document.remoteComposeState.getRemoteContextReflection()!!
    val id = context.getVariableIdReflection(name)
    return rememberMutableRemoteColorAsState(id)
}
