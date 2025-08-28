/*
 * Copyright (C) 2025 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.state

import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.player.view.state.RemoteDomains
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Abstract base class for all remote bitmap representations in Compose Remote, this class extends
 * [RemoteState<Bitmap>].
 *
 * @property state The [RemoteComposeCreationState] associated with this bitmap, allowing access to
 *   the remote document for registration.
 * @property hasConstantValue A boolean indicating whether this [RemoteBitmap] will always evaluate
 *   to the same [value].
 */
abstract class RemoteBitmap
internal constructor(
    val state: RemoteComposeCreationState?,
    override val hasConstantValue: Boolean,
) : RemoteState<Bitmap> {

    // @Deprecated("Use getIdForCreationState directly")
    // TODO: re-enable this asap
    val id: Int
        get() {
            // FallbackCreationState.state.platform.log(
            //     Platform.LogCategory.TODO,
            //     "Use RemoteBitmap.getIdForCreationState directly"
            // )
            return getIdForCreationState(FallbackCreationState.state)
        }

    companion object {
        /**
         * Creates a [RemoteBitmap] instance from a [Bitmap] value. This factory method can be used
         * with or without an explicit [RemoteComposeCreationState].
         *
         * @param v The [Bitmap] value.
         * @param state An optional [RemoteComposeCreationState] to associate with this bitmap. If
         *   not provided, the bitmap will be added to the document when its ID is requested.
         * @return A [RemoteBitmap] representing the provided bitmap.
         */
        @JvmOverloads
        operator fun invoke(v: Bitmap, state: RemoteComposeCreationState? = null): RemoteBitmap {
            return MutableRemoteBitmap(state, mutableStateOf(v), false) { creationState ->
                creationState.document.addBitmap(v)
            }
        }

        /**
         * Creates a named [RemoteBitmap] with an initial value. Named remote bitmaps can be set via
         * AndroidRemoteContext.setNamedBitmap.
         *
         * @param name The unique name for this remote bitmap.
         * @param initialValue The initial [Bitmap] value for the named remote bitmap.
         * @return A [RemoteBitmap] representing the named bitmap.
         */
        fun createNamedRemoteBitmap(
            name: String,
            initialValue: Bitmap,
            state: RemoteComposeCreationState,
        ): RemoteBitmap =
            MutableRemoteBitmap(state, mutableStateOf(initialValue), false) { creationState ->
                creationState.document.addNamedBitmap(name, initialValue)
            }
    }
}

/**
 * A mutable implementation of [RemoteBitmap] that holds its value in a [MutableState<Bitmap>].
 *
 * @property state The [RemoteComposeCreationState] associated with this bitmap.
 * @property content The underlying [MutableState<Bitmap>] that stores the actual bitmap value.
 * @property hasConstantValue A boolean indicating whether this [MutableRemoteBitmap] is expected to
 *   remain constant. For mutable states, this is typically `false`.
 * @property idProvider A lambda that provides the unique ID for this mutable bitmap within the
 *   [RemoteComposeCreationState]. This ID is used to identify the bitmap in the remote document.
 */
class MutableRemoteBitmap(
    state: RemoteComposeCreationState?,
    private val content: MutableState<Bitmap>,
    hasConstantValue: Boolean,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteBitmap(state, hasConstantValue), MutableRemoteState<Bitmap> {

    override fun writeToDocument(creationState: RemoteComposeCreationState) =
        idProvider(creationState)

    override var value: Bitmap
        get() {
            return content.value
        }
        set(newValue) {
            content.value = newValue
        }

    override operator fun component1(): Bitmap = value

    override operator fun component2(): (Bitmap) -> Unit = { newValue -> content.value = newValue }
}

/**
 * A Composable function to remember and provide a mutable remote bitmap value.
 *
 * @param name The unique name for this remote bitmap, used for identification in the remote
 *   document.
 * @param domain The domain of the remote bitmap (defaults to [RemoteDomains.USER]).
 * @param value A lambda that provides the initial [Bitmap] value for this remote bitmap.
 * @return A [RemoteBitmap] instance, which initially evaluates to the return value of [value]
 */
@Composable
fun rememberRemoteBitmapValue(
    name: String,
    domain: RemoteDomains = RemoteDomains.USER,
    value: () -> Bitmap,
): RemoteBitmap {
    val state = LocalRemoteComposeCreationState.current
    return remember(name) {
        val initial = value()
        MutableRemoteBitmap(state, mutableStateOf(initial), false) { creationState ->
            creationState.document.addNamedBitmap("$domain:$name", initial)
        }
    }
}
