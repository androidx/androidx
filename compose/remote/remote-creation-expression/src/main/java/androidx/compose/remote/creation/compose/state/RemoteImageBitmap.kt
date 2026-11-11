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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition

/**
 * Abstract base class for all remote bitmap representations in Compose Remote.
 *
 * `RemoteImageBitmap` represents an image value that can be a constant, a named variable, or an
 * offscreen buffer.
 */
@Stable
public open class RemoteImageBitmap
internal constructor(
    public override val constantValueOrNull: Any?,
    cacheKey: RemoteStateCacheKey,
    internal val idProvider: ((creationState: RemoteComposeCreationState) -> Int)? = null,
) : BaseRemoteState<Any>(cacheKey) {

    internal override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider?.invoke(creationState)
            ?: error(
                "RemoteImageBitmap subclass must implement writeToDocument or supply idProvider"
            )

    internal enum class OperationKey {
        Width,
        Height,
    }

    /** The width of the bitmap as represented in the remote document. */
    public val width: RemoteFloat
        get() {
            return RemoteFloat(
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
            return RemoteFloat(
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
         * Creates a [RemoteImageBitmap] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteImageBitmap] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: Int): RemoteImageBitmap =
            RemoteImageBitmap(
                constantValueOrNull = null,
                cacheKey = RemoteStateIdKey(id),
                idProvider = { id },
            )

        /**
         * Creates a named [RemoteImageBitmap] backed by an image [url].
         *
         * Defaults to `1x1` dimensions for compatibility with existing players. Pass explicit width
         * and height or use the `IntSize` extension overload to instruct updated players to take
         * the actual size from the loaded image.
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
        ): RemoteImageBitmap = createNamedRemoteImageBitmap(name, url, 1, 1, domain)

        /**
         * Creates a named [RemoteImageBitmap] backed by an image [url] with explicit [width] and
         * [height].
         *
         * @param name unique name to identify this state within its [domain]
         * @param url URL of the image
         * @param width expected width of the bitmap in pixels, or 0 to use the loaded image's
         *   dimensions
         * @param height expected height of the bitmap in pixels, or 0 to use the loaded image's
         *   dimensions
         * @param domain domain for the named state, defaults to [RemoteState.Domain.User]
         * @return a [RemoteImageBitmap] for the named bitmap
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createNamedRemoteImageBitmap(
            name: String,
            url: String,
            width: Int,
            height: Int,
            domain: RemoteState.Domain = RemoteState.Domain.User,
        ): RemoteImageBitmap =
            RemoteImageBitmap(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
                idProvider = { creationState ->
                    val id = creationState.document.addBitmapUrl(url, width, height)
                    creationState.document.setNamedVariable(
                        id,
                        domain.prefixed(name),
                        NamedVariable.IMAGE_TYPE,
                    )
                    id
                },
            )

        /**
         * Creates a [RemoteImageBitmap] with the specified [width] and [height].
         *
         * @param width The width of the [RemoteImageBitmap] to create
         * @param height The height of the [RemoteImageBitmap] to create
         * @return A [RemoteImageBitmap] with the specified [width] and [height].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createOffscreenRemoteBitmap(width: Int, height: Int): RemoteImageBitmap =
            RemoteImageBitmap(
                constantValueOrNull = null,
                cacheKey = RemoteStateInstanceKey(),
                idProvider = { creationState ->
                    creationState.document.createBitmap(width, height)
                },
            )
    }
}

/**
 * A mutable implementation of [RemoteImageBitmap] that holds its value in a
 * [MutableRemoteState<Any>].
 */
public class MutableRemoteImageBitmap
@RememberInComposition
internal constructor(
    cacheKey: RemoteStateCacheKey,
    public override val constantValueOrNull: Any?,
    idProvider: (creationState: RemoteComposeCreationState) -> Int,
) : RemoteImageBitmap(constantValueOrNull, cacheKey, idProvider), MutableRemoteState<Any> {

    internal companion object {
        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteImageBitmap] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Int): MutableRemoteImageBitmap =
            MutableRemoteImageBitmap(
                cacheKey = RemoteStateIdKey(id),
                constantValueOrNull = null,
            ) {
                id
            }
    }
}

/**
 * Creates a [MutableRemoteImageBitmap] with the given [constantValueOrNull], [cacheKey], and
 * [idProvider].
 */
@RememberInComposition
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun MutableRemoteImageBitmap(
    constantValueOrNull: Any?,
    cacheKey: RemoteStateCacheKey? = null,
    idProvider: (creationState: RemoteComposeCreationState) -> Int,
): MutableRemoteImageBitmap =
    MutableRemoteImageBitmap(
        cacheKey = cacheKey ?: RemoteStateInstanceKey(),
        constantValueOrNull = constantValueOrNull,
        idProvider = idProvider,
    )

/**
 * Creates a [RemoteImageBitmap] from a [url].
 *
 * @param url The URL of the image.
 * @return A [RemoteImageBitmap] representing the image from the URL.
 */
public fun RemoteImageBitmap(url: String): RemoteImageBitmap =
    RemoteImageBitmap(
        constantValueOrNull = null,
        cacheKey = RemoteConstantCacheKey(url),
        idProvider = { creationState -> creationState.document.addBitmapUrl(url) },
    )
