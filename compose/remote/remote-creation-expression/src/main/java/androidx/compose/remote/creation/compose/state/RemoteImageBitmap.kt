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
import androidx.compose.remote.document.RemoteBitmapId
import androidx.compose.remote.document.floatArrayOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import kotlin.reflect.KClass

/** Provides a [RemoteBitmapId] for a [RemoteImageBitmap] when written into [creationState]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface BitmapIdProvider {
    public fun provide(creationState: RemoteComposeCreationState): RemoteBitmapId
}

/**
 * Read-only supertype for all remote bitmap representations in Compose Remote.
 *
 * [RemoteImageBitmap] represents an image value that can be a constant, a named variable, or an
 * offscreen buffer.
 *
 * Comparing [RemoteImageBitmap] values with `==` checks structural equality (matching constant
 * values or expression DAGs), whereas [MutableRemoteImageBitmap] instances compare by identity; see
 * [RemoteValue].
 */
@Stable
public sealed interface RemoteImageBitmap : RemoteValue<Any> {
    /** The width of the bitmap as represented in the remote document. */
    public val width: RemoteFloat

    /** The height of the bitmap as represented in the remote document. */
    public val height: RemoteFloat

    public companion object {
        /**
         * Creates a [RemoteImageBitmap] referencing a remote ID.
         *
         * @param id The remote ID.
         * @return A [RemoteImageBitmap] referencing the ID.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createForId(id: Int): RemoteImageBitmap =
            RemoteImageBitmapImpl(
                constantValueOrNull = null,
                cacheKey = RemoteStateIdKey(id),
                idProvider = { RemoteBitmapId(id) },
            )

        /**
         * Creates a named [RemoteImageBitmap] backed by an image [url].
         *
         * Defaults to `1x1` dimensions for compatibility with existing players. Pass explicit width
         * and height to the overloaded factory to instruct updated players to take the actual size
         * from the loaded image.
         *
         * @param name unique name to identify this state within its [domain]
         * @param url URL of the image
         * @param domain domain for the named state, defaults to [RemoteValue.Domain.User]
         * @return a [RemoteImageBitmap] for the named bitmap
         */
        @JvmStatic
        public fun createNamedRemoteImageBitmap(
            name: String,
            url: String,
            domain: RemoteValue.Domain = RemoteValue.Domain.User,
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
         * @param domain domain for the named state, defaults to [RemoteValue.Domain.User]
         * @return a [RemoteImageBitmap] for the named bitmap
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun createNamedRemoteImageBitmap(
            name: String,
            url: String,
            width: Int,
            height: Int,
            domain: RemoteValue.Domain = RemoteValue.Domain.User,
        ): RemoteImageBitmap =
            RemoteImageBitmapImpl(
                constantValueOrNull = null,
                cacheKey = RemoteNamedCacheKey(domain, name),
                idProvider = { creationState ->
                    val bitmapId = creationState.document.addBitmapUrl(url, width, height)
                    creationState.document.setNamedVariable(
                        bitmapId.id,
                        domain.prefixed(name),
                        NamedVariable.IMAGE_TYPE,
                    )
                    bitmapId
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
            RemoteImageBitmapImpl(
                constantValueOrNull = null,
                cacheKey = RemoteStateInstanceKey(),
                idProvider = { creationState ->
                    creationState.document.createBitmap(width, height)
                },
            )
    }
}

private enum class ImageBitmapOperationKey : RemoteOperation {
    Width,
    Height;

    override fun toDebugString(args: List<RemoteStateCacheKey>): String =
        when (this) {
            Width -> "${args[0].toOperandString(100)}.width"
            Height -> "${args[0].toOperandString(100)}.height"
        }

    override fun reconstruct(args: List<RemoteValue<*>>): RemoteValue<*> =
        when (this) {
            Width -> (args[0] as RemoteImageBitmap).width
            Height -> (args[0] as RemoteImageBitmap).height
        }

    override val resultKind: RemoteValueKind
        get() = RemoteValueKind.FLOAT

    override fun isResultWholeNumber(args: List<RemoteStateCacheKey>): Boolean = true
}

private abstract class BaseRemoteImageBitmap(cacheKey: RemoteStateCacheKey) :
    BaseRemoteValue<Any>(cacheKey), RemoteImageBitmap {
    override val equalityType: KClass<*>
        get() = RemoteImageBitmap::class

    override val valueKind: RemoteValueKind
        get() = RemoteValueKind.IMAGE_BITMAP

    override fun createForIdOrNull(id: Int): RemoteValue<*> = RemoteImageBitmap.createForId(id)

    override val width: RemoteFloat
        get() =
            RemoteFloat(
                null,
                cacheKey = RemoteOperationCacheKey.create(ImageBitmapOperationKey.Width, this),
            ) { creationState ->
                floatArrayOf(
                    creationState.document.bitmapAttribute(
                        RemoteBitmapId(getId(creationState)),
                        IMAGE_WIDTH.toInt(),
                    )
                )
            }

    override val height: RemoteFloat
        get() =
            RemoteFloat(
                null,
                cacheKey = RemoteOperationCacheKey.create(ImageBitmapOperationKey.Height, this),
            ) { creationState ->
                floatArrayOf(
                    creationState.document.bitmapAttribute(
                        RemoteBitmapId(getId(creationState)),
                        IMAGE_HEIGHT.toInt(),
                    )
                )
            }
}

private class RemoteImageBitmapImpl(
    override val constantValueOrNull: Any?,
    cacheKey: RemoteStateCacheKey,
    private val idProvider: BitmapIdProvider,
) : BaseRemoteImageBitmap(cacheKey) {
    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider.provide(creationState).id
}

/**
 * A mutable implementation of [RemoteImageBitmap] that holds its value in a
 * [MutableRemoteValue<Any>].
 *
 * Distinct [MutableRemoteImageBitmap] instances represent separate mutable variables and compare by
 * identity with `==`.
 */
@Stable
public sealed interface MutableRemoteImageBitmap : RemoteImageBitmap, MutableRemoteValue<Any> {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Maps an existing mutable ID to a state instance.
         *
         * @param id The existing mutable ID.
         * @return A [MutableRemoteImageBitmap] instance mapping to the ID.
         */
        internal fun createMutableForId(id: Int): MutableRemoteImageBitmap =
            MutableRemoteImageBitmapImpl(
                cacheKey = RemoteStateIdKey(id),
                constantValueOrNull = null,
            ) {
                RemoteBitmapId(id)
            }
    }
}

private class MutableRemoteImageBitmapImpl(
    cacheKey: RemoteStateCacheKey,
    override val constantValueOrNull: Any?,
    private val idProvider: BitmapIdProvider,
) : BaseRemoteImageBitmap(cacheKey), MutableRemoteImageBitmap {
    override fun writeToDocument(creationState: RemoteComposeCreationState): Int =
        idProvider.provide(creationState).id

    override fun adaptToType(candidate: RemoteValue<*>): RemoteValue<*> =
        if (candidate is MutableRemoteImageBitmap) candidate else this
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
    idProvider: BitmapIdProvider,
): MutableRemoteImageBitmap =
    MutableRemoteImageBitmapImpl(
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
    RemoteImageBitmapImpl(
        constantValueOrNull = null,
        cacheKey = RemoteConstantCacheKey(url),
        idProvider = { creationState -> creationState.document.addBitmapUrl(url) },
    )
