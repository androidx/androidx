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

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.DataListFloat
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastForEach
import java.io.ByteArrayInputStream

/**
 * State holder for an embedded [RcPlayer], providing reactive access to named document variables
 * (floats, integers, booleans, strings, colors, bitmaps) backed by Compose snapshot state.
 *
 * Typical usage with two-way state delegation:
 * ```
 * val playerState = remember(document) { RcPlayerState(document) }
 * var progress by playerState.floatState("progress")
 * RcPlayer(state = playerState)
 * ```
 *
 * Variable names are resolved according to [defaultPrefix]:
 * - If `name` contains a colon (`:`), it is treated as a fully-qualified variable name and used
 *   exactly as provided, ignoring any prefix.
 * - If `prefix` is `null` or empty, `name` is used without prefix and without colon.
 * - If `prefix` is non-null (defaulting to `"USER"`), `"$prefix:$name"` is preferred if defined in
 *   the document, falling back to unprefixed `name` if defined without prefix, or `"$prefix:$name"`
 *   otherwise.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RcPlayerState {
    /** The underlying [CoreDocument] managed by this state. */
    public val document: CoreDocument

    /** The default prefix applied to variable names (e.g. `"USER"`, or `null` for none). */
    public val defaultPrefix: String?

    public fun floatState(name: String, prefix: String? = defaultPrefix): MutableState<Float>

    public fun intState(name: String, prefix: String? = defaultPrefix): MutableState<Int>

    public fun booleanState(name: String, prefix: String? = defaultPrefix): MutableState<Boolean>

    public fun stringState(name: String, prefix: String? = defaultPrefix): MutableState<String>

    public fun colorState(name: String, prefix: String? = defaultPrefix): MutableState<Color>

    public fun floatArrayState(
        name: String,
        prefix: String? = defaultPrefix,
    ): MutableState<FloatArray>

    public fun bitmapState(name: String, prefix: String? = defaultPrefix): MutableState<Bitmap?>

    /** Clears any override applied to [name] and restores its authored document default. */
    public fun clearOverride(name: String, prefix: String? = defaultPrefix)
}

/**
 * Creates an [RcPlayerState] for the given [document].
 *
 * @param document The [CoreDocument] to bind.
 * @param defaultPrefix The default variable name prefix (default is `"USER"`). Pass `null` for no
 *   prefix.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcPlayerState(
    document: CoreDocument,
    defaultPrefix: String? = "USER",
): RcPlayerState = RcPlayerStateImpl(document, defaultPrefix)

/**
 * Creates an [RcPlayerState] for the given [capturedDocument].
 *
 * @param capturedDocument The [CapturedDocument] to bind.
 * @param defaultPrefix The default variable name prefix (default is `"USER"`). Pass `null` for no
 *   prefix.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RcPlayerState(
    capturedDocument: CapturedDocument,
    defaultPrefix: String? = "USER",
): RcPlayerState {
    RemoteImageSupport.enableEncodedImageReferences()
    val coreDoc =
        CoreDocument(RemoteClock.SYSTEM).apply {
            ByteArrayInputStream(capturedDocument.bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    return RcPlayerStateImpl(coreDoc, defaultPrefix)
}

/** Creates and remembers an [RcPlayerState] for the given [document]. */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberRcPlayerState(
    document: CoreDocument,
    defaultPrefix: String? = "USER",
): RcPlayerState = remember(document, defaultPrefix) { RcPlayerState(document, defaultPrefix) }

/** Creates and remembers an [RcPlayerState] for the given [capturedDocument]. */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberRcPlayerState(
    capturedDocument: CapturedDocument,
    defaultPrefix: String? = "USER",
): RcPlayerState =
    remember(capturedDocument, defaultPrefix) { RcPlayerState(capturedDocument, defaultPrefix) }

internal class RcPlayerStateImpl(
    override val document: CoreDocument,
    override val defaultPrefix: String?,
) : RcPlayerState {
    internal val preprocessed: DocumentPreprocessResult = preprocessDocument(document)
    internal val remoteContext: AndroidRemoteContext =
        initializePlayerRemoteContext(
            document,
            document.clock,
            preprocessed,
        )

    internal val currentTimeMillisState: MutableFloatState = mutableFloatStateOf(0f)
    internal val graphContext: GraphContext =
        GraphContext(
                realState = remoteContext.mRemoteComposeState as SnapshotRemoteComposeState,
                computedOps = preprocessed.computedOpIndex,
                timeMillis = currentTimeMillisState,
                clock = document.clock,
            )
            .also { gc ->
                gc.setTypefaceResolver(remoteContext.typefaceResolver)
            }

    private val initialFloats = mutableMapOf<Int, Float>()
    private val initialInts = mutableMapOf<Int, Int>()
    private val initialColors = mutableMapOf<Int, Int>()
    private val initialStrings = mutableMapOf<Int, String>()
    private val initialBitmaps = mutableMapOf<Int, Bitmap>()
    private val initialFloatArrays = mutableMapOf<Int, FloatArray>()

    private val floatStates = mutableMapOf<String, MutableState<Float>>()
    private val intStates = mutableMapOf<String, MutableState<Int>>()
    private val booleanStates = mutableMapOf<String, MutableState<Boolean>>()
    private val stringStates = mutableMapOf<String, MutableState<String>>()
    private val colorStates = mutableMapOf<String, MutableState<Color>>()
    private val floatArrayStates = mutableMapOf<String, MutableState<FloatArray>>()
    private val bitmapStates = mutableMapOf<String, MutableState<Bitmap?>>()

    init {
        preprocessed.constantOps.fastForEach { op ->
            if (op is NamedVariable) {
                val id = op.mVarId
                when (op.mVarType) {
                    NamedVariable.FLOAT_TYPE -> {
                        initialFloats[id] =
                            if (graphContext.isComputed(id)) {
                                graphContext.getFloat(id)
                            } else {
                                remoteContext.mRemoteComposeState.getFloat(id)
                            }
                    }
                    NamedVariable.INT_TYPE -> {
                        initialInts[id] =
                            if (graphContext.isComputed(id)) {
                                graphContext.getInteger(id)
                            } else {
                                remoteContext.mRemoteComposeState.getInteger(id)
                            }
                    }
                    NamedVariable.COLOR_TYPE -> {
                        initialColors[id] =
                            if (graphContext.isComputed(id)) {
                                graphContext.getColor(id)
                            } else {
                                remoteContext.mRemoteComposeState.getColor(id)
                            }
                    }
                    NamedVariable.STRING_TYPE -> {
                        val text =
                            if (graphContext.isComputed(id)) {
                                graphContext.getText(id)
                            } else {
                                remoteContext.getText(id)
                            }
                        text?.let { initialStrings[id] = it }
                    }
                    NamedVariable.IMAGE_TYPE -> {
                        resolveBitmap(remoteContext, id)?.let {
                            initialBitmaps[id] = it
                        }
                    }
                    NamedVariable.FLOAT_ARRAY_TYPE -> {
                        remoteContext.mRemoteComposeState.getFloats(id)?.let {
                            initialFloatArrays[id] = it
                        }
                    }
                }
            }
        }
    }

    private fun resolveName(name: String, prefix: String?): String {
        if (name.contains(':') || prefix.isNullOrEmpty()) {
            return name
        }
        val prefixed = "$prefix:$name"
        if (remoteContext.getVariableIdReflection(prefixed) != -1) {
            return prefixed
        }
        if (remoteContext.getVariableIdReflection(name) != -1) {
            return name
        }
        return prefixed
    }

    private inline fun <T> getOrDefault(
        name: String,
        prefix: String?,
        default: T,
        get: (Int) -> T,
    ): T {
        val resolved = resolveName(name, prefix)
        val id = remoteContext.getVariableIdReflection(resolved)
        return if (id != -1) get(id) else default
    }

    private fun <T> getOrCreateState(
        cache: MutableMap<String, MutableState<T>>,
        name: String,
        prefix: String?,
        getter: (String) -> T,
        setter: (String, T) -> Unit,
    ): MutableState<T> {
        val resolved = resolveName(name, prefix)
        return cache.getOrPut(resolved) {
            DelegatedMutableState(resolved, getter, setter)
        }
    }

    private inline fun restoreInitial(
        name: String,
        prefix: String?,
        clearContext: (String) -> Unit = {},
        restore: (SnapshotRemoteComposeState, Int) -> Unit,
    ) {
        val resolved = resolveName(name, prefix)
        clearContext(resolved)
        val id = remoteContext.getVariableIdReflection(resolved)
        if (id != -1) {
            (remoteContext.mRemoteComposeState as? SnapshotRemoteComposeState)?.let {
                restore(it, id)
            }
        }
    }

    private fun setFloat(name: String, value: Float, prefix: String?) {
        remoteContext.setNamedFloatOverride(resolveName(name, prefix), value)
    }

    private fun getFloat(name: String, prefix: String?): Float =
        getOrDefault(name, prefix, 0f) { graphContext.getFloat(it) }

    private fun clearFloat(name: String, prefix: String?) =
        restoreInitial(name, prefix, remoteContext::clearNamedFloatOverride) { state, id ->
            initialFloats[id]?.let { state.overrideFloat(id, it) }
            state.clearFloatOverride(id)
        }

    override fun floatState(name: String, prefix: String?): MutableState<Float> =
        getOrCreateState(
            floatStates,
            name,
            prefix,
            { getFloat(it, null) },
            { n, v -> setFloat(n, v, null) },
        )

    private fun setInt(name: String, value: Int, prefix: String?) {
        remoteContext.setNamedIntegerOverride(resolveName(name, prefix), value)
    }

    private fun getInt(name: String, prefix: String?): Int =
        getOrDefault(name, prefix, 0) { graphContext.getInteger(it) }

    private fun clearInt(name: String, prefix: String?) =
        restoreInitial(name, prefix, remoteContext::clearNamedIntegerOverride) { state, id ->
            initialInts[id]?.let { state.overrideInteger(id, it) }
            state.clearIntegerOverride(id)
        }

    override fun intState(name: String, prefix: String?): MutableState<Int> =
        getOrCreateState(
            intStates,
            name,
            prefix,
            { getInt(it, null) },
            { n, v -> setInt(n, v, null) },
        )

    private fun setBoolean(name: String, value: Boolean, prefix: String?) {
        remoteContext.setNamedBooleanOverride(resolveName(name, prefix), value)
    }

    private fun getBoolean(name: String, prefix: String?): Boolean = getInt(name, prefix) != 0

    private fun clearBoolean(name: String, prefix: String?) =
        restoreInitial(name, prefix, remoteContext::clearNamedBooleanOverride) { state, id ->
            initialInts[id]?.let { state.overrideInteger(id, it) }
            state.clearIntegerOverride(id)
        }

    override fun booleanState(name: String, prefix: String?): MutableState<Boolean> =
        getOrCreateState(
            booleanStates,
            name,
            prefix,
            { getBoolean(it, null) },
            { n, v -> setBoolean(n, v, null) },
        )

    private fun setString(name: String, value: String, prefix: String?) {
        remoteContext.setNamedStringOverride(resolveName(name, prefix), value)
    }

    private fun getString(name: String, prefix: String?): String? =
        getOrDefault(name, prefix, null) { graphContext.getText(it) }

    private fun clearString(name: String, prefix: String?) =
        restoreInitial(name, prefix, remoteContext::clearNamedStringOverride) { state, id ->
            initialStrings[id]?.let { state.overrideData(id, it) }
            state.clearDataOverride(id)
        }

    override fun stringState(name: String, prefix: String?): MutableState<String> =
        getOrCreateState(
            stringStates,
            name,
            prefix,
            { getString(it, null) ?: "" },
            { n, v -> setString(n, v, null) },
        )

    private fun setColor(name: String, color: Color, prefix: String?) {
        setColor(name, color.toArgb(), prefix)
    }

    private fun setColor(name: String, color: Int, prefix: String?) {
        remoteContext.setNamedColorOverride(resolveName(name, prefix), color)
    }

    private fun getColor(name: String, prefix: String?): Color = Color(getColorInt(name, prefix))

    private fun getColorInt(name: String, prefix: String?): Int =
        getOrDefault(name, prefix, 0) { graphContext.getColor(it) }

    private fun clearColor(name: String, prefix: String?) =
        restoreInitial(name, prefix) { state, id ->
            initialColors[id]?.let { state.overrideColor(id, it) }
        }

    override fun colorState(name: String, prefix: String?): MutableState<Color> =
        getOrCreateState(
            colorStates,
            name,
            prefix,
            { getColor(it, null) },
            { n, v -> setColor(n, v, null) },
        )

    private fun setBitmap(name: String, bitmap: Bitmap, prefix: String?) {
        remoteContext.setNamedDataOverride(resolveName(name, prefix), bitmap)
    }

    private fun getBitmap(name: String, prefix: String?): Bitmap? =
        getOrDefault(name, prefix, null) { resolveBitmap(remoteContext, it) }

    private fun clearBitmap(name: String, prefix: String?) =
        restoreInitial(name, prefix, remoteContext::clearNamedDataOverride) { state, id ->
            initialBitmaps[id]?.let { state.overrideData(id, it) }
            state.clearDataOverride(id)
        }

    override fun bitmapState(name: String, prefix: String?): MutableState<Bitmap?> =
        getOrCreateState(
            bitmapStates,
            name,
            prefix,
            { getBitmap(it, null) },
            { n, v -> if (v != null) setBitmap(n, v, null) else clearBitmap(n, null) },
        )

    private fun setFloatArray(name: String, value: FloatArray, prefix: String?) {
        val resolved = resolveName(name, prefix)
        val id = remoteContext.getVariableIdReflection(resolved)
        if (id != -1) {
            remoteContext.mRemoteComposeState.addCollection(id, DataListFloat(id, value))
            remoteContext.mRemoteComposeState.markVariableDirty(id)
        }
    }

    private fun getFloatArray(name: String, prefix: String?): FloatArray =
        getOrDefault(name, prefix, FloatArray(0)) {
            remoteContext.mRemoteComposeState.getFloats(it) ?: FloatArray(0)
        }

    private fun clearFloatArray(name: String, prefix: String?) =
        restoreInitial(name, prefix) { state, id ->
            initialFloatArrays[id]?.let {
                state.addCollection(id, DataListFloat(id, it))
                state.markVariableDirty(id)
            }
        }

    override fun floatArrayState(name: String, prefix: String?): MutableState<FloatArray> =
        getOrCreateState(
            floatArrayStates,
            name,
            prefix,
            { getFloatArray(it, null) },
            { n, v -> setFloatArray(n, v, null) },
        )

    override fun clearOverride(name: String, prefix: String?) {
        clearFloat(name, prefix)
        clearInt(name, prefix)
        clearBoolean(name, prefix)
        clearString(name, prefix)
        clearBitmap(name, prefix)
        clearColor(name, prefix)
        clearFloatArray(name, prefix)
    }
}

private class DelegatedMutableState<T>(
    private val name: String,
    private val getter: (String) -> T,
    private val setter: (String, T) -> Unit,
) : MutableState<T> {
    override var value: T
        get() = getter(name)
        set(v) {
            setter(name, v)
        }

    override fun component1(): T = value

    override fun component2(): (T) -> Unit = { value = it }
}
