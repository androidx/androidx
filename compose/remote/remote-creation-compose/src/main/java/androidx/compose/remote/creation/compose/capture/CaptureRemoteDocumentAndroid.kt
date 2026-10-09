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
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

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
): CapturedDocument {
    val configuration = context.resources.configuration
    val document =
        captureSingleRemoteDocument(
            creationDisplayInfo = creationDisplayInfo,
            remoteDensity = remoteDensity,
            layoutDirection = layoutDirection,
            clock = clock,
            profile = profile,
            writerCallback = writerEvents,
            platformImageProvider = AndroidPlatformImageProvider,
        ) {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides configuration,
                LocalFontWeightAdjustment provides platformFontWeightAdjustment(configuration),
                LocalIs24HourFormat provides DateFormat.is24HourFormat(context),
                content = content,
            )
        }
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
        content = content,
    )

/**
 * Like the public [captureRemoteDocument], but with [updateThrottle] deciding how often documents
 * are emitted instead of [CaptureUpdateThrottle.Default].
 *
 * TODO(b/567847315): Make the update strategy public together with [CaptureUpdateThrottle].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun captureRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo,
    updateThrottle: CaptureUpdateThrottle,
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
): Flow<ByteArray> {
    val configuration = context.resources.configuration
    val resolvedLayoutDirection =
        layoutDirection ?: toLayoutDirection(configuration.layoutDirection)
    return captureRemoteDocument(
        creationDisplayInfo = creationDisplayInfo,
        updateThrottle = updateThrottle,
        remoteDensity = remoteDensity,
        layoutDirection = resolvedLayoutDirection,
        clock = clock,
        profile = profile,
        writerCallback = writerEvents,
        platformImageProvider = AndroidPlatformImageProvider,
        coroutineContext = coroutineContext,
    ) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalFontWeightAdjustment provides platformFontWeightAdjustment(configuration),
            LocalIs24HourFormat provides DateFormat.is24HourFormat(context),
            content = content,
        )
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
