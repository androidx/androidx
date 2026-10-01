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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    public override val constantValueOrNull: Any?,
    cacheKey: RemoteStateCacheKey,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val idProvider: ((creationState: RemoteComposeCreationState) -> Int)? = null,
) : BaseRemoteState<Any>(cacheKey) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        constantValueOrNull: Any?,
        cacheKey: RemoteStateCacheKey,
    ) : this(
        constantValueOrNull = constantValueOrNull,
        cacheKey = cacheKey,
        idProvider = null,
    )

    /**
     * Creates a [RemoteImageBitmap] from a URL.
     *
     * @param url The URL of the image.
     */
    public constructor(
        url: String
    ) : this(
        constantValueOrNull = null,
        cacheKey = RemoteConstantCacheKey(url),
        idProvider = { creationState -> creationState.document.addBitmapUrl(url) },
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    public override val constantValueOrNull: Any?,
    cacheKey: RemoteStateCacheKey?,
    idProvider: (creationState: RemoteComposeCreationState) -> Int,
) :
    RemoteImageBitmap(constantValueOrNull, cacheKey ?: RemoteStateInstanceKey(), idProvider),
    MutableRemoteState<Any> {

    public companion object {
        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteImageBitmap] instance mapping to the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createMutableForId(id: Int): MutableRemoteImageBitmap =
            MutableRemoteImageBitmap(constantValueOrNull = null, cacheKey = RemoteStateIdKey(id)) {
                id
            }
    }
}
