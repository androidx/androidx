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
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap.Companion.createNamedRemoteImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

/**
 * Abstract base class for all remote bitmap representations in Compose Remote, this class extends
 * [RemoteState<ImageBitmap>].
 *
 * `RemoteImageBitmap` represents an image value that can be a constant, a named variable, or an
 * offscreen buffer.
 */
@Stable
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

    public companion object {
        /**
         * Creates a [RemoteImageBitmap] instance from a [ImageBitmap] value.
         *
         * @param value The [ImageBitmap] value.
         * @return A [RemoteImageBitmap] representing the provided bitmap.
         */
        public operator fun invoke(value: ImageBitmap): RemoteImageBitmap {
            return MutableRemoteImageBitmap(value, cacheKey = RemoteConstantCacheKey(value)) {
                creationState ->
                creationState.addBitmap(value)
            }
        }

        /**
         * Creates a [RemoteImageBitmap] from a URL.
         *
         * @param url The URL of the image.
         * @return A [RemoteImageBitmap] representing the image from the URL.
         */
        public operator fun invoke(url: String): RemoteImageBitmap {
            return MutableRemoteImageBitmap(
                constantValueOrNull = null,
                cacheKey = RemoteConstantCacheKey(url),
            ) { creationState ->
                creationState.document.addBitmapUrl(url)
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
                creationState.addNamedBitmap(
                    domain.prefixed(name),
                    defaultValue,
                )
            }

        /**
         * Creates a named [RemoteImageBitmap] from a lambda providing the initial [ImageBitmap].
         *
         * @param name A unique name to identify this state within its [domain].
         * @param domain The domain for the named state. Defaults to [RemoteState.Domain.User].
         * @param value A lambda that provides the initial [ImageBitmap].
         * @return A [RemoteImageBitmap] representing the named bitmap.
         */
        @JvmStatic
        public fun createNamedRemoteImageBitmap(
            name: String,
            domain: RemoteState.Domain = RemoteState.Domain.User,
            value: () -> ImageBitmap,
        ): RemoteImageBitmap {
            val bitmap = value()
            return MutableRemoteImageBitmap(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                creationState.addNamedBitmap(
                    domain.prefixed(name),
                    bitmap,
                )
            }
        }

        /**
         * Sentinel [IntSize] (`0x0`) indicating that a URL-backed [RemoteImageBitmap] should derive
         * its dimensions from the loaded image at runtime rather than enforcing fixed bounds.
         */
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public val IntrinsicSize: IntSize = IntSize.Zero

        /**
         * Creates a named [RemoteImageBitmap] backed by an image [url].
         *
         * Defaults to `1x1` dimensions for compatibility with existing players. Pass
         * [IntrinsicSize] to the `size` overload to instruct updated players to take the actual
         * size from the loaded image.
         *
         * @param name unique name to identify this state within its [domain]
         * @param url URL of the image
         * @param domain domain for the named state, defaults to [RemoteState.Domain.User]
         * @return a [RemoteImageBitmap] for the named bitmap
         */
        @JvmStatic
        public fun createNamedRemoteImageBitmap(
            name: String,
            url: String,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteImageBitmap = createNamedRemoteImageBitmap(name, url, IntSize(1, 1), domain)

        /**
         * Creates a named [RemoteImageBitmap] backed by an image [url] with explicit [size].
         *
         * Note: Passing [IntrinsicSize] (`0x0`) for [size] instructs the player to use the loaded
         * image's intrinsic dimensions.
         *
         * @param name unique name to identify this state within its [domain]
         * @param url URL of the image
         * @param size expected dimensions of the bitmap in pixels, or [IntrinsicSize] to use the
         *   loaded image's dimensions
         * @param domain domain for the named state, defaults to [RemoteState.Domain.User]
         * @return a [RemoteImageBitmap] for the named bitmap
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createNamedRemoteImageBitmap(
            name: String,
            url: String,
            size: IntSize,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteImageBitmap {
            return MutableRemoteImageBitmap(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
            ) { creationState ->
                val id = creationState.document.addBitmapUrl(url, size.width, size.height)
                creationState.document.setNamedVariable(
                    id,
                    domain.prefixed(name),
                    NamedVariable.IMAGE_TYPE,
                )
                id
            }
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
 * A mutable implementation of [RemoteImageBitmap] that holds its value in a
 * [MutableRemoteState<ImageBitmap>].
 */
public class MutableRemoteImageBitmap
@RememberInComposition
internal constructor(
    constantValueOrNull: ImageBitmap?,
    cacheKey: RemoteStateCacheKey?,
    private val idProvider: (creationState: RemoteComposeCreationState) -> Int,
) :
    RemoteImageBitmap(constantValueOrNull, cacheKey ?: RemoteStateInstanceKey()),
    MutableRemoteState<ImageBitmap> {

    /**
     * Creates a [MutableRemoteImageBitmap] with an [initialValue].
     *
     * @param initialValue The initial [ImageBitmap] value.
     */
    @RememberInComposition
    public constructor(
        initialValue: ImageBitmap
    ) : this(
        constantValueOrNull = initialValue,
        cacheKey = RemoteStateInstanceKey(),
        idProvider = { creationState ->
            creationState.addBitmap(initialValue)
        },
    )

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
        @RememberInComposition
        public operator fun invoke(initialValue: ImageBitmap): MutableRemoteImageBitmap {
            return MutableRemoteImageBitmap(initialValue)
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
@Deprecated(
    message = "Use remember { MutableRemoteImageBitmap(initialValue) } instead.",
    replaceWith =
        ReplaceWith(
            "remember { MutableRemoteImageBitmap(initialValue) }",
            "androidx.compose.runtime.remember",
        ),
)
@RemoteComposable
@Composable
public fun rememberMutableRemoteImageBitmap(initialValue: ImageBitmap): MutableRemoteImageBitmap {
    return remember { MutableRemoteImageBitmap(initialValue) }
}

/**
 * Remembers a remote bitmap from a URL.
 *
 * @param url The URL of the image.
 * @return A [RemoteImageBitmap] representing the image from the URL.
 */
@RemoteComposable
@Composable
public fun rememberRemoteImageBitmap(url: String): RemoteImageBitmap {
    return remember(url) { RemoteImageBitmap(url) }
}

/**
 * Remembers a named [RemoteImageBitmap] from a lambda providing the initial [ImageBitmap].
 *
 * @param name The unique name for this remote bitmap.
 * @param domain The domain of the named bitmap (defaults to [RemoteState.Domain.User]).
 * @param value A lambda that provides the initial [ImageBitmap].
 * @return A [RemoteImageBitmap] representing the named remote bitmap.
 */
@RemoteComposable
@Composable
public fun rememberNamedRemoteImageBitmap(
    name: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
    value: () -> ImageBitmap,
): RemoteImageBitmap {
    return remember(name, domain) { createNamedRemoteImageBitmap(name, domain, value) }
}

/**
 * Remembers a named [RemoteImageBitmap] loaded from a [url].
 *
 * @param name unique name to identify this state within its [domain]
 * @param url URL of the image
 * @param domain domain for the named state, defaults to [RemoteState.Domain.User]
 * @return a [RemoteImageBitmap] for the named bitmap from [url]
 */
@RemoteComposable
@Composable
public fun rememberNamedRemoteImageBitmap(
    name: String,
    url: String,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteImageBitmap {
    return remember(name, domain) { createNamedRemoteImageBitmap(name, url, domain) }
}

/**
 * Remembers a named [RemoteImageBitmap] loaded from a [url] with explicit [size].
 *
 * @param name unique name to identify this state within its [domain]
 * @param url URL of the image
 * @param size expected dimensions of the bitmap in pixels, or [RemoteImageBitmap.IntrinsicSize] to
 *   use the loaded image's dimensions
 * @param domain domain for the named state, defaults to [RemoteState.Domain.User]
 * @return a [RemoteImageBitmap] for the named bitmap from [url]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberNamedRemoteImageBitmap(
    name: String,
    url: String,
    size: IntSize,
    domain: RemoteState.Domain = RemoteState.Domain.User,
): RemoteImageBitmap {
    return remember(name, domain, size) { createNamedRemoteImageBitmap(name, url, size, domain) }
}

/** Extension property to convert a [ImageBitmap] to a [RemoteImageBitmap]. */
public val ImageBitmap.rb: RemoteImageBitmap
    get() {
        return MutableRemoteImageBitmap(
            constantValueOrNull = this,
            cacheKey = RemoteConstantCacheKey(this),
        ) { creationState ->
            creationState.addBitmap(this)
        }
    }
