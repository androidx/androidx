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
@file:JvmName("RemoteComposeCreationStateKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.collection.MutableObjectIntMap
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.state.RemoteStateCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.nanIdContentEquals
import androidx.compose.remote.creation.compose.state.nanIdContentHashCode
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.LayoutDirection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteComposeCreationState : RemoteStateScope {

    override val parentScope: RemoteComposeCreationState
        get() = this

    public val creationDisplayInfo: RemoteCreationDisplayInfo
    public val profile: Profile
    public override lateinit var remoteDensity: RemoteDensity
    public override lateinit var layoutDirection: LayoutDirection
    public final override val densityBehavior: RemoteDensityBehavior

    /**
     * Ids of float expressions already written to [document], keyed by their lowered RPN array and
     * optional animation. Used to write each distinct expression only once.
     */
    internal val floatExpressionIds: MutableObjectIntMap<LoweredFloatExpressionKey> =
        MutableObjectIntMap()

    /**
     * Ids of integer expressions already written to [document], keyed by their lowered RPN array.
     * Used to write each distinct expression only once.
     */
    internal val intExpressionIds: MutableObjectIntMap<LoweredIntExpressionKey> =
        MutableObjectIntMap()
    public var ready: Boolean = true
    public override lateinit var document: RemoteComposeWriter
    internal val remoteVariableToId: MutableObjectIntMap<RemoteStateCacheKey> =
        MutableObjectIntMap()
    internal val floatArrayCache: HashMap<RemoteStateCacheKey, FloatArray> = HashMap()
    internal val longArrayCache: HashMap<RemoteStateCacheKey, LongArray> = HashMap()

    internal val platformImageProvider: PlatformImageProvider

    public fun addBitmap(image: ImageBitmap): Int = platformImageProvider.addBitmap(document, image)

    public fun addNamedBitmap(name: String, image: ImageBitmap): Int =
        platformImageProvider.addNamedBitmap(document, name, image)

    internal inline fun getOrPutFloatArray(
        key: RemoteStateCacheKey,
        crossinline compute: () -> FloatArray,
    ): FloatArray = floatArrayCache.getOrPut(key) { compute() }

    internal inline fun getOrPutLongArray(
        key: RemoteStateCacheKey,
        crossinline compute: () -> LongArray,
    ): LongArray = longArrayCache.getOrPut(key) { compute() }

    /**
     * Returns the id of a float expression with the same lowered [array] and [animation] that was
     * already written to [document], or calls [write] to write it and caches the resulting id.
     */
    internal inline fun getOrPutFloatExpressionId(
        array: FloatArray,
        animation: FloatArray?,
        crossinline write: () -> Int,
    ): Int = floatExpressionIds.getOrPut(LoweredFloatExpressionKey(array, animation)) { write() }

    /**
     * Returns the id of an integer expression with the same lowered [array] that was already
     * written to [document], or calls [write] to write it and caches the resulting id.
     */
    internal inline fun getOrPutIntExpressionId(
        array: LongArray,
        crossinline write: () -> Int,
    ): Int = intExpressionIds.getOrPut(LoweredIntExpressionKey(array)) { write() }

    /** Clears all caches tied to the current [document]. */
    internal fun clearDocumentCaches() {
        floatExpressionIds.clear()
        intExpressionIds.clear()
        remoteVariableToId.clear()
        floatArrayCache.clear()
        longArrayCache.clear()
    }

    internal inline fun getOrPutVariableId(
        key: RemoteStateCacheKey,
        crossinline compute: () -> Int,
    ): Int {
        val id = remoteVariableToId.getOrDefault(key, -1)
        if (id == -1) {
            val nextId = compute()
            remoteVariableToId.put(key, nextId)
            return nextId
        }
        return id
    }

    public val time: MutableState<Long> = mutableLongStateOf(0L)

    public val platform: RcPlatformServices
        get() = profile.platform

    public constructor(
        creationDisplayInfo: RemoteCreationDisplayInfo,
        profile: Profile,
        writerCallback: Any?,
        remoteDensity: RemoteDensity = RemoteDensity.from(creationDisplayInfo),
        layoutDirection: LayoutDirection,
        platformImageProvider: PlatformImageProvider,
    ) {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        document = profile.create(creationDisplayInfo.toCreationDisplayInfo(), writerCallback)
        this.remoteDensity = remoteDensity
        this.layoutDirection = layoutDirection
        this.densityBehavior = creationDisplayInfo.densityBehavior
        this.platformImageProvider = platformImageProvider
    }

    public constructor(
        creationDisplayInfo: RemoteCreationDisplayInfo,
        contentDescription: String?,
        profile: Profile,
        platformImageProvider: PlatformImageProvider,
    ) {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        document = profile.create(creationDisplayInfo.toCreationDisplayInfo(), null)
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
        this.layoutDirection = LayoutDirection.Ltr
        this.densityBehavior = creationDisplayInfo.densityBehavior
        this.platformImageProvider = platformImageProvider
    }

    public constructor(
        creationDisplayInfo: RemoteCreationDisplayInfo,
        profile: Profile,
        writer: RemoteComposeWriter,
        platformImageProvider: PlatformImageProvider,
    ) {
        this.creationDisplayInfo = creationDisplayInfo
        this.profile = profile
        this.document = writer
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
        this.layoutDirection = LayoutDirection.Ltr
        this.densityBehavior = creationDisplayInfo.densityBehavior
        this.platformImageProvider = platformImageProvider
    }

    public constructor(
        size: Size,
        profile: Profile,
        platformImageProvider: PlatformImageProvider,
    ) {
        this.profile = profile
        this.creationDisplayInfo =
            RemoteCreationDisplayInfo(size.width.toInt(), size.height.toInt(), 160, 1.0f)
        this.document = profile.create(creationDisplayInfo.toCreationDisplayInfo(), null)
        this.remoteDensity = RemoteDensity.from(creationDisplayInfo)
        this.layoutDirection = LayoutDirection.Ltr
        this.densityBehavior = creationDisplayInfo.densityBehavior
        this.platformImageProvider = platformImageProvider
    }
}

/**
 * Key for a lowered float expression: its RPN [array] plus optional [animation].
 *
 * The arrays contain NaN-encoded variable ids and operators, so equality uses [nanIdContentEquals];
 * [FloatArray.contentEquals] would treat e.g. `[a, b, ADD]` and `[c, d, MUL]` as equal.
 */
internal class LoweredFloatExpressionKey(
    private val array: FloatArray,
    private val animation: FloatArray?,
) {
    private val hashCode = 31 * array.nanIdContentHashCode() + animation.nanIdContentHashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoweredFloatExpressionKey) return false
        return hashCode == other.hashCode &&
            array.nanIdContentEquals(other.array) &&
            animation.nanIdContentEquals(other.animation)
    }

    override fun hashCode(): Int = hashCode
}

/** Key for a lowered integer expression, identified by its RPN [array]. */
internal class LoweredIntExpressionKey(private val array: LongArray) {
    private val hashCode = array.contentHashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoweredIntExpressionKey) return false
        return hashCode == other.hashCode && array.contentEquals(other.array)
    }

    override fun hashCode(): Int = hashCode
}

private object NoOpPlatformImageProvider : PlatformImageProvider {
    override fun addBitmap(document: RemoteComposeWriter, image: ImageBitmap): Int = -1

    override fun addNamedBitmap(
        document: RemoteComposeWriter,
        name: String,
        image: ImageBitmap,
    ): Int = -1
}

// Density and Size should be taken from Compose in this mode
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NoRemoteCompose :
    RemoteComposeCreationState(
        RemoteCreationDisplayInfo(1, 1, 160, 1.0f),
        null,
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            0,
            RcPlatformServices.None,
            { creationDisplayInfo, profile, callback ->
                RemoteComposeWriter(creationDisplayInfo, null, profile, callback)
            },
        ),
        NoOpPlatformImageProvider,
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PlatformImageProvider {
    public fun addBitmap(document: RemoteComposeWriter, image: ImageBitmap): Int

    public fun addNamedBitmap(document: RemoteComposeWriter, name: String, image: ImageBitmap): Int
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalRemoteComposeCreationState: ProvidableCompositionLocal<RemoteComposeCreationState> =
    compositionLocalOf {
        NoRemoteCompose()
    }
