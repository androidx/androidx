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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.ImageAttribute.IMAGE_HEIGHT
import androidx.compose.remote.core.operations.ImageAttribute.IMAGE_WIDTH
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.player.core.state.RemoteDomains
import androidx.compose.runtime.Composable

/**
 * Abstract base class for all remote bitmap representations in Compose Remote, this class extends
 * [RemoteState<Bitmap>].
 *
 * @property state The [RemoteComposeCreationState] associated with this bitmap, allowing access to
 *   the remote document for registration.
 * @property hasConstantValue A boolean indicating whether this [RemoteBitmap] will always evaluate
 *   to the same value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class RemoteBitmap
internal constructor(
    public val state: RemoteComposeCreationState?,
    public override val constantValue: Bitmap?,
) : BaseRemoteState<Bitmap>() {

    // @Deprecated("Use getIdForCreationState directly")
    // TODO: re-enable this asap
    public val id: Int
        get() {
            // FallbackCreationState.state.platform.log(
            //     Platform.LogCategory.TODO,
            //     "Use RemoteBitmap.getIdForCreationState directly"
            // )
            return getIdForCreationState(FallbackCreationState.state)
        }

    /** The width of the bitmap as represented in the remote document. */
    public val width: RemoteFloat
        get() {
            val width =
                state?.document?.bitmapAttribute(id, IMAGE_WIDTH)
                    ?: throw IllegalStateException(
                        "Bitmap width is not available in the remote document."
                    )
            return RemoteFloat(width)
        }

    /** The height of the bitmap as represented in the remote document. */
    public val height: RemoteFloat
        get() {
            val height =
                state?.document?.bitmapAttribute(id, IMAGE_HEIGHT)
                    ?: throw IllegalStateException(
                        "Bitmap height is not available in the remote document."
                    )
            return RemoteFloat(height)
        }

    public companion object {
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
        public operator fun invoke(
            v: Bitmap,
            state: RemoteComposeCreationState? = null,
        ): RemoteBitmap {
            return MutableRemoteBitmap(state, v) { creationState ->
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
        public fun createNamedRemoteBitmap(
            name: String,
            initialValue: Bitmap,
            state: RemoteComposeCreationState,
        ): RemoteBitmap =
            MutableRemoteBitmap(state, constantValue = null) { creationState ->
                creationState.document.addNamedBitmap(name, initialValue)
            }
    }
}

/**
 * A mutable implementation of [RemoteBitmap] that holds its value in a [MutableState<Bitmap>].
 *
 * @property state The [RemoteComposeCreationState] associated with this bitmap.
 * @property constantValue The [Bitmap] this [RemoteColor] always evaluates to, if any, or null if
 *   it's not constant.
 * @property idProvider A lambda that provides the unique ID for this mutable bitmap within the
 *   [RemoteComposeCreationState]. This ID is used to identify the bitmap in the remote document.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MutableRemoteBitmap(
    state: RemoteComposeCreationState?,
    constantValue: Bitmap?,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteBitmap(state, constantValue), MutableRemoteState<Bitmap> {

    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider(creationState)
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
public fun rememberRemoteBitmapValue(
    name: String,
    domain: RemoteDomains = RemoteDomains.USER,
    value: () -> Bitmap,
): RemoteBitmap {
    val state = LocalRemoteComposeCreationState.current
    return rememberNamedState(name, domain) {
        val initial = value()
        MutableRemoteBitmap(state, constantValue = null) { creationState ->
            creationState.document.addNamedBitmap("$domain:$name", initial)
        }
    }
}

@Composable
public fun rememberRemoteBitmap(
    name: String,
    domain: RemoteDomains = RemoteDomains.USER,
    url: String,
    width: Int = 1,
    height: Int = 1,
): RemoteBitmap {
    val state = LocalRemoteComposeCreationState.current
    return rememberNamedState(name, domain) {
        // We create a bitmap of the specified dimensions as a placeholder. The actual bitmap will
        // be loaded from the URL on the remote side. Providing accurate dimensions can prevent
        // unnecessary relayouts.
        MutableRemoteBitmap(state, constantValue = null) { creationState ->
            creationState.document.addNamedBitmapUrl("$domain:$name", url)
        }
    }
}
