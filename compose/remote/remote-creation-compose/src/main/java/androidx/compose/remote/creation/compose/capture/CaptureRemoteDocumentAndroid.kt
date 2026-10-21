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

@file:OptIn(ExperimentalRemoteCreationComposeApi::class)
@file:JvmName("CaptureRemoteDocumentKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.text.format.DateFormat
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.annotation.RestrictTo
import androidx.collection.MutableScatterMap
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.capture.headless.HeadlessLifecycleOwner
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.LocalRetainedValuesStoreProvider
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.savedstate.SavedState
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.write
import java.io.Serializable
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Capture a single RemoteCompose document from the specified [content] Composable by rendering it
 * once inside a virtual display.
 *
 * This is a suspending function that performs the composition and rendering, returning a
 * [CapturedDocument] which contains the serialized bytes and metadata.
 *
 * @param context The Android [Context] to use.
 * @param creationDisplayInfo Details about the virtual display to capture for (size, density,
 *   etc.). Defaults to display metrics derived from [context].
 * @param remoteDensity The logical screen density and font scale to use for unit conversions.
 *   Defaults to density derived from [creationDisplayInfo]. Note: If passing custom values, they
 *   should typically match the density and font scale specified in [creationDisplayInfo] to avoid
 *   layout scaling discrepancies.
 * @param layoutDirection The layout direction (LTR or RTL) to use. Defaults to the layout direction
 *   of [context]'s configuration.
 * @param clock The clock used for the composition timeline. Defaults to [RemoteClock.SYSTEM].
 * @param profile The writing profile that determines supported operations. Defaults to
 *   [RcPlatformProfiles.ANDROIDX].
 * @param writerEvents Callback to handle non-serializable events (e.g. pending intents).
 * @param content The Composable content to render and capture.
 * @return A [CapturedDocument] containing the serialized document bytes.
 */
public suspend fun captureSingleRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo = createCreationDisplayInfo(context),
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection =
        toLayoutDirection(context.resources.configuration.layoutDirection),
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerEvents: WriterEvents = WriterEvents(),
    content: @Composable @RemoteComposable () -> Unit,
): CapturedDocument =
    captureSingleRemoteDocument(
        context = context,
        creationDisplayInfo = creationDisplayInfo,
        remoteDensity = remoteDensity,
        layoutDirection = layoutDirection,
        clock = clock,
        profile = profile,
        writerEvents = writerEvents,
        savedState = null,
        retainedValuesStore = null,
        content = content,
    )

/**
 * Like the public [captureSingleRemoteDocument], but with an optional [savedState] used to
 * initialize and save [LocalSaveableStateRegistry] during the capture so values held with
 * [rememberSaveable] can be saved and restored across captures, and an optional
 * [retainedValuesStore] provided as [LocalRetainedValuesStore] so values held with [retain] are
 * preserved across captures.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun captureSingleRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo = createCreationDisplayInfo(context),
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection =
        toLayoutDirection(context.resources.configuration.layoutDirection),
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerEvents: WriterEvents = WriterEvents(),
    savedState: SavedState? = null,
    retainedValuesStore: RetainedValuesStore? = null,
    content: @Composable @RemoteComposable () -> Unit,
): CapturedDocument {
    val lifecycleOwner = HeadlessLifecycleOwner(savedState)
    val saveableStateRegistry = savedState?.let {
        SaveableStateRegistry(it.toSaveableStateMap(), ::canBeSavedToBundle)
    }
    val document =
        captureSingleRemoteDocument(
            creationDisplayInfo = creationDisplayInfo,
            remoteDensity = remoteDensity,
            layoutDirection = layoutDirection,
            clock = clock,
            profile = profile,
            writerCallback = writerEvents,
            platformImageProvider = AndroidPlatformImageProvider,
            onDispose = {
                savedState?.write { clear() }
                if (savedState != null && saveableStateRegistry != null) {
                    savedState.saveSaveableState(saveableStateRegistry.performSave())
                }
                lifecycleOwner.performSave()
                lifecycleOwner.destroy()
            },
            content =
                withAndroidLocals(
                    context = context,
                    creationDisplayInfo = creationDisplayInfo,
                    lifecycleOwner = lifecycleOwner,
                    saveableStateRegistry = saveableStateRegistry,
                    retainedValuesStore = retainedValuesStore,
                    content = content,
                ),
        )
    return CapturedDocument(document, writerEvents.pendingIntents, writerEvents.lambdas)
}

/**
 * Capture a stream of RemoteCompose documents by rendering the specified [content] Composable in a
 * virtual display and emitting the resulting byte arrays whenever recomposition occurs and the
 * layout visually changes.
 *
 * This API allows capturing dynamic Compose content (e.g., containing animations, transitions, or
 * state updates) as a Flow of serialized document byte arrays.
 *
 * Crucially, recomposition is handled cleanly, and duplicate documents (where nothing visually
 * changed in the layout tree) are automatically filtered out, so new byte arrays are only emitted
 * when the document actually changes.
 *
 * Remote documents are expected to change rarely; animate with remote expressions rather than
 * recomposition. Content that updates faster than a few documents in quick succession followed by
 * about one per second is throttled, and a warning is logged. The latest state is still emitted
 * once the throttle allows.
 *
 * @param context The Android [Context] to use.
 * @param creationDisplayInfo Details about the virtual display to capture for (size, density,
 *   etc.).
 * @param remoteDensity The logical screen density and font scale to use for unit conversions.
 *   Defaults to density derived from [creationDisplayInfo]. Note: If passing custom values, they
 *   should typically match the density and font scale specified in [creationDisplayInfo] to avoid
 *   layout scaling discrepancies.
 * @param layoutDirection The layout direction (LTR or RTL) to use. Defaults to the layout direction
 *   of [context]'s configuration when `null`.
 * @param writerEvents Callback to handle non-serializable events (e.g. pending intents).
 * @param clock The clock used for the recomposer timeline. Defaults to [RemoteClock.SYSTEM].
 * @param profile The writing profile that determines supported operations. Defaults to
 *   [RcPlatformProfiles.ANDROIDX].
 * @param coroutineContext The CoroutineContext to run recomposition and rendering on. Defaults to
 *   [Dispatchers.Default].
 * @param content The Composable content to render and capture.
 * @return A [Flow] of [ByteArray]s containing the serialized RemoteCompose documents.
 */
public fun captureRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo,
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection? = null,
    writerEvents: WriterEvents = WriterEvents(),
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    content: @Composable @RemoteComposable () -> Unit,
): Flow<ByteArray> =
    captureRemoteDocument(
        context = context,
        creationDisplayInfo = creationDisplayInfo,
        updateThrottle = CaptureUpdateThrottle.Default,
        remoteDensity = remoteDensity,
        layoutDirection = layoutDirection,
        writerEvents = writerEvents,
        clock = clock,
        profile = profile,
        coroutineContext = coroutineContext,
        savedState = null,
        retainedValuesStore = null,
        content = content,
    )

/**
 * Like the public [captureRemoteDocument], but with [updateThrottle] deciding how often documents
 * are emitted instead of [CaptureUpdateThrottle.Default], an optional [savedState], and an optional
 * [retainedValuesStore].
 *
 * TODO(b/567847315): Make the update strategy public together with [CaptureUpdateThrottle].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun captureRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo,
    updateThrottle: CaptureUpdateThrottle = CaptureUpdateThrottle.Default,
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection? = null,
    writerEvents: WriterEvents = WriterEvents(),
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    savedState: SavedState? = null,
    retainedValuesStore: RetainedValuesStore? = null,
    content: @Composable @RemoteComposable () -> Unit,
): Flow<ByteArray> = flow {
    val resolvedLayoutDirection =
        layoutDirection ?: toLayoutDirection(context.resources.configuration.layoutDirection)
    val lifecycleOwner = HeadlessLifecycleOwner(savedState)
    val saveableStateRegistry = savedState?.let {
        SaveableStateRegistry(it.toSaveableStateMap(), ::canBeSavedToBundle)
    }
    emitAll(
        captureRemoteDocument(
            creationDisplayInfo = creationDisplayInfo,
            updateThrottle = updateThrottle,
            remoteDensity = remoteDensity,
            layoutDirection = resolvedLayoutDirection,
            clock = clock,
            profile = profile,
            writerCallback = writerEvents,
            platformImageProvider = AndroidPlatformImageProvider,
            coroutineContext = coroutineContext,
            onDispose = {
                savedState?.write { clear() }
                if (savedState != null && saveableStateRegistry != null) {
                    savedState.saveSaveableState(saveableStateRegistry.performSave())
                }
                lifecycleOwner.performSave()
                lifecycleOwner.destroy()
            },
            content =
                withAndroidLocals(
                    context = context,
                    creationDisplayInfo = creationDisplayInfo,
                    lifecycleOwner = lifecycleOwner,
                    saveableStateRegistry = saveableStateRegistry,
                    retainedValuesStore = retainedValuesStore,
                    content = content,
                ),
        )
    )
}

/**
 * Wraps [content] so that the Android-specific CompositionLocals a capture needs ([LocalContext],
 * [LocalConfiguration], [LocalFontWeightAdjustment] and [LocalIs24HourFormat]) are provided from
 * [context].
 */
private fun withAndroidLocals(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo,
    lifecycleOwner: HeadlessLifecycleOwner,
    saveableStateRegistry: SaveableStateRegistry?,
    retainedValuesStore: RetainedValuesStore?,
    content: @Composable @RemoteComposable () -> Unit,
): @Composable @RemoteComposable () -> Unit {
    val configuration = context.resources.configuration
    return {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalInspectionMode provides creationDisplayInfo.isInspectionMode,
            LocalDensity provides
                Density(
                    creationDisplayInfo.density.density,
                    creationDisplayInfo.density.fontScale,
                ),
            LocalLifecycleOwner provides lifecycleOwner,
            LocalSavedStateRegistryOwner provides lifecycleOwner,
            LocalSaveableStateRegistry provides saveableStateRegistry,
            LocalFontWeightAdjustment provides platformFontWeightAdjustment(configuration),
            LocalIs24HourFormat provides DateFormat.is24HourFormat(context),
        ) {
            if (retainedValuesStore != null) {
                LocalRetainedValuesStoreProvider(retainedValuesStore, content)
            } else {
                content()
            }
        }
    }
}

internal fun platformFontWeightAdjustment(configuration: Configuration): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (configuration.fontWeightAdjustment != Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED) {
            configuration.fontWeightAdjustment
        } else {
            0
        }
    } else {
        0
    }

private const val SAVEABLE_STATE_KEY =
    "androidx.compose.remote.creation.compose.capture.SaveableStateRegistry"

@Suppress("DEPRECATION")
internal fun SavedState.toSaveableStateMap(): Map<String, List<Any?>>? = read {
    getParcelableOrNull<ParcelableMapHolder>(SAVEABLE_STATE_KEY)?.map
}

internal fun SavedState.saveSaveableState(map: Map<String, List<Any?>>) {
    write {
        if (map.isEmpty()) {
            remove(SAVEABLE_STATE_KEY)
        } else {
            putParcelable(SAVEABLE_STATE_KEY, ParcelableMapHolder(map))
        }
    }
}

/**
 * Checks whether [value] can be stored in an Android `Bundle`, matching Compose UI's
 * `DisposableSaveableStateRegistry`.
 */
internal fun canBeSavedToBundle(value: Any): Boolean {
    if (value is SnapshotMutableState<*>) {
        val policy = value.policy
        if (
            policy !== neverEqualPolicy<Any?>() &&
                policy !== structuralEqualityPolicy<Any?>() &&
                policy !== referentialEqualityPolicy<Any?>()
        ) {
            return false
        }
        val stateValue = value.value
        return stateValue == null || canBeSavedToBundle(stateValue)
    }
    // Kotlin lambdas implement Serializable on the JVM but cannot be saved to a Bundle.
    if (value is Function<*> && value is Serializable) {
        return false
    }
    if (value is Parcelable || value is Serializable) {
        return true
    }
    @Suppress("USELESS_IS_CHECK") // SizeF is not Parcelable before API 31.
    if (value is Binder || value is Size || value is SizeF || value is SparseArray<*>) {
        return true
    }
    return false
}

/**
 * Holder wrapping a `SaveableStateRegistry` state [Map], matching Compose UI's
 * `DisposableSaveableStateRegistry.ParcelableMapHolder`.
 *
 * Across captures in the same process, [ParcelableMapHolder] is kept by reference in memory inside
 * the `Bundle`. Across process death, it serializes entries via `Parcel.writeValue`.
 */
@Suppress("AsCollectionCall", "BanParcelableUsage", "UNCHECKED_CAST")
private class ParcelableMapHolder(val map: Map<String, List<Any?>>) : Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(map.size)
        map.forEach { (key, value) ->
            parcel.writeString(key)
            parcel.writeValue(value)
        }
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableMapHolder> =
            object : Parcelable.ClassLoaderCreator<ParcelableMapHolder> {
                override fun createFromParcel(
                    parcel: Parcel,
                    loader: ClassLoader?,
                ): ParcelableMapHolder {
                    val classLoader = loader ?: ParcelableMapHolder::class.java.classLoader
                    val size = parcel.readInt()
                    val map = MutableScatterMap<String, List<Any?>>(initialCapacity = size)
                    for (i in 0 until size) {
                        val key = parcel.readString() ?: continue
                        val value = parcel.readValue(classLoader) as List<Any?>
                        map[key] = value
                    }
                    return ParcelableMapHolder(map.asMap())
                }

                override fun createFromParcel(parcel: Parcel): ParcelableMapHolder =
                    createFromParcel(parcel, loader = null)

                override fun newArray(size: Int): Array<ParcelableMapHolder?> = arrayOfNulls(size)
            }
    }
}
