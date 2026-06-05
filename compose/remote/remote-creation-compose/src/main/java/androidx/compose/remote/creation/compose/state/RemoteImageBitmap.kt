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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.ImageAttribute.IMAGE_HEIGHT
import androidx.compose.remote.core.operations.ImageAttribute.IMAGE_WIDTH
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

/**
 * Abstract base class for all remote bitmap representations in Compose Remote, this class extends
 * [RemoteState<ImageBitmap>].
 *
 * `RemoteImageBitmap` represents an image value that can be a constant, a named variable, or an
 * offscreen buffer.
 */
public abstract class RemoteImageBitmap
internal constructor(
    public override val constantValueOrNull: ImageBitmap?,
    cacheKey: RemoteStateCacheKey,
) : BaseRemoteState<ImageBitmap>(cacheKey) {

    internal enum class OperationKey {
        Width,
        Height,
    }

    /** The width of the bitmap as represented in the remote document. */
    public val width: RemoteFloat
        get() {
            return RemoteFloatExpression(
                null,
                cacheKey = RemoteOperationCacheKey.create(OperationKey.Width, this),
            ) { creationState ->
                floatArrayOf(
                    creationState.document.bitmapAttribute(
                        getIdForCreationState(creationState),
                        IMAGE_WIDTH,
                    )
                )
            }
        }

    /** The height of the bitmap as represented in the remote document. */
    public val height: RemoteFloat
        get() {
            return RemoteFloatExpression(
                null,
                cacheKey = RemoteOperationCacheKey.create(OperationKey.Height, this),
            ) { creationState ->
                floatArrayOf(
                    creationState.document.bitmapAttribute(
                        getIdForCreationState(creationState),
                        IMAGE_HEIGHT,
                    )
                )
            }
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a [RemoteImageBitmap] instance from a [ImageBitmap] value. This factory method
         * can be used with or without an explicit [RemoteComposeCreationState].
         *
         * @param value The [ImageBitmap] value.
         * @return A [RemoteImageBitmap] representing the provided bitmap.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public operator fun invoke(value: ImageBitmap): MutableRemoteImageBitmap {
            return MutableRemoteImageBitmap(value, cacheKey = RemoteConstantCacheKey(value)) {
                creationState ->
                creationState.document.addBitmap(value.asAndroidBitmap())
            }
        }

        /**
         * Creates a [RemoteImageBitmap] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteImageBitmap] referencing the ID.
         */
        internal fun createForId(id: Int): RemoteImageBitmap =
            MutableRemoteImageBitmap(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id)) {
                id
            }

        /**
         * Creates a named [RemoteImageBitmap] with an initial value.
         *
         * Named remote bitmaps can be set via AndroidRemoteContext.setNamedBitmap.
         *
         * @param name A unique name to identify this state within its [domain].
         * @param defaultValue The initial [ImageBitmap] value for the named remote bitmap.
         * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
         * @return A [RemoteImageBitmap] representing the named bitmap.
         */
        @JvmStatic
        public fun createNamedRemoteImageBitmap(
            name: String,
            defaultValue: ImageBitmap,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteImageBitmap =
            MutableRemoteImageBitmap(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                creationState.document.addNamedBitmap(
                    domain.prefixed(name),
                    defaultValue.asAndroidBitmap(),
                )
            }

        /**
         * Creates a [RemoteImageBitmap] with the specified [width] and [height].
         *
         * @param width The width of the [RemoteImageBitmap] to create
         * @param height The height of the [RemoteImageBitmap] to create
         * @return A [RemoteImageBitmap] with the specified [width] and [height].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createOffscreenRemoteBitmap(width: Int, height: Int): RemoteImageBitmap =
            object : RemoteImageBitmap(null, RemoteStateInstanceKey()) {
                public override val constantValueOrNull: ImageBitmap? = null

                public override fun writeToDocument(
                    creationState: RemoteComposeCreationState
                ): Int = creationState.document.createBitmap(width, height)
            }
    }
}

/**
 * A mutable implementation of [RemoteImageBitmap] that holds its value in a [MutableState<Bitmap>].
 */
public class MutableRemoteImageBitmap
internal constructor(
    constantValueOrNull: ImageBitmap?,
    cacheKey: RemoteStateCacheKey?,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) :
    RemoteImageBitmap(constantValueOrNull, cacheKey ?: RemoteStateInstanceKey()),
    MutableRemoteState<ImageBitmap> {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider(creationState)

    public companion object {
        /**
         * Creates a new mutable state (allocates an ID).
         *
         * @param initialValue The initial value for the state.
         * @return A new [MutableRemoteImageBitmap] instance.
         */
        public operator fun invoke(initialValue: ImageBitmap): MutableRemoteImageBitmap {
            return MutableRemoteImageBitmap(initialValue, cacheKey = RemoteStateInstanceKey()) {
                creationState ->
                creationState.document.addBitmap(initialValue.asAndroidBitmap())
            }
        }

        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteImageBitmap] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Int): MutableRemoteImageBitmap =
            MutableRemoteImageBitmap(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id)) {
                id
            }
    }
}

/**
 * Factory for mutable remote bitmap state.
 *
 * @param initialValue The initial [ImageBitmap] value.
 * @return A [MutableRemoteImageBitmap] instance that will be remembered across recompositions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberMutableRemoteImageBitmap(initialValue: ImageBitmap): MutableRemoteImageBitmap {
    return remember {
        MutableRemoteImageBitmap(constantValueOrNull = null, cacheKey = RemoteStateInstanceKey()) {
            creationState ->
            creationState.document.addBitmap(initialValue.asAndroidBitmap())
        }
    }
}

/**
 * Remembers a named remote bitmap expression.
 *
 * @param name The unique name for this remote bitmap.
 * @param domain The domain of the named bitmap (defaults to [RemoteState.Domain.User]).
 * @param value A lambda that provides the [RemoteImageBitmap] expression.
 * @return A [RemoteImageBitmap] representing the named remote bitmap expression.
 */
@Composable
public fun rememberNamedRemoteImageBitmap(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> ImageBitmap,
): RemoteImageBitmap {
    return rememberNamedState(name, domain) {
        val bitmap = value()
        MutableRemoteImageBitmap(
            constantValueOrNull = null,
            cacheKey = RemoteNamedCacheKey(domain, name),
        ) { creationState ->
            creationState.document.addNamedBitmap(domain.prefixed(name), bitmap.asAndroidBitmap())
        }
    }
}

/** A Composable function to remember and provide a **named** remote bitmap from a URL. */
@Composable
public fun rememberNamedRemoteImageBitmap(
    name: String,
    url: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteImageBitmap {
    return rememberNamedState(name, domain) {
        // We create a bitmap of the specified dimensions as a placeholder. The actual bitmap will
        // be loaded from the URL on the remote side. Providing accurate dimensions can prevent
        // unnecessary relayouts.
        MutableRemoteImageBitmap(
            constantValueOrNull = null,
            cacheKey = RemoteNamedCacheKey(domain, name),
        ) { creationState ->
            creationState.document.addNamedBitmapUrl(domain.prefixed(name), url)
        }
    }
}

/** Extension property to convert a [ImageBitmap] to a [RemoteImageBitmap]. */
public val ImageBitmap.rb: RemoteImageBitmap
    get() {
        return MutableRemoteImageBitmap(
            constantValueOrNull = this,
            cacheKey = RemoteConstantCacheKey(this),
        ) { creationState ->
            creationState.document.addBitmap(this.asAndroidBitmap())
        }
    }
