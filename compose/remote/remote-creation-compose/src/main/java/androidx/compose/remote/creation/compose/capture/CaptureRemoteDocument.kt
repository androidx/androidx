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

@file:OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalRemoteCreationComposeApi::class,
)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Looper
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteComposeApplier
import androidx.compose.remote.creation.compose.layout.RemoteRootNode
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.trace
import androidx.core.graphics.createBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tracing.traceAsync
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadLocalRandom
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

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
 * @param layoutDirection The layout direction (LTR or RTL) to use. Defaults to the system layout
 *   direction.
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
    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    // Limit the recomposer dispatcher to parallelism of 1 so that recomposition and frame clock
    // events run sequentially on multi-threaded dispatchers (skipping Unconfined dispatchers which
    // do not support limitedParallelism, Main dispatchers which are already single-threaded, and
    // non-CoroutineDispatcher ContinuationInterceptors such as ApplyingContinuationInterceptor in
    // Compose UI tests).
    val interceptor = currentCoroutineContext()[ContinuationInterceptor]
    val recomposerContext =
        when {
            interceptor == null ->
                currentCoroutineContext() +
                    Dispatchers.Default.limitedParallelism(
                        parallelism = 1,
                        name = "captureSingleRemoteDocument",
                    )
            Looper.myLooper() == Looper.getMainLooper() -> currentCoroutineContext()
            interceptor is MainCoroutineDispatcher -> currentCoroutineContext()
            interceptor is CoroutineDispatcher &&
                interceptor.isDispatchNeeded(EmptyCoroutineContext) ->
                currentCoroutineContext() +
                    interceptor.limitedParallelism(
                        parallelism = 1,
                        name = "captureSingleRemoteDocument",
                    )
            else -> currentCoroutineContext()
        }
    val recomposer = Recomposer(recomposerContext)
    val composition = Composition(applier, recomposer)
    // Provide a thread-safe HeadlessLifecycleOwner in the RESUMED state so that composables
    // observing LocalLifecycleOwner do not crash with IllegalStateException when capturing on a
    // background thread.
    val lifecycleOwner = HeadlessLifecycleOwner()

    try {
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
                writerEvents = writerEvents,
                layoutDirection = layoutDirection,
                remoteDensity = remoteDensity,
            )

        val initialSize = creationState.document.buffer.buffer.size()

        composition.setContent {
            CompositionLocalProvider(
                LocalRemoteComposeCreationState provides creationState,
                LocalInspectionMode provides creationDisplayInfo.isInspectionMode,
                LocalDensity provides
                    Density(
                        creationDisplayInfo.density.density,
                        creationDisplayInfo.density.fontScale,
                    ),
                LocalRemoteDensity provides remoteDensity,
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalLayoutDirection provides layoutDirection,
                LocalFontWeightAdjustment provides
                    platformFontWeightAdjustment(context.resources.configuration),
                LocalLifecycleOwner provides lifecycleOwner,
                content = content,
            )
        }

        coroutineScope {
            try {
                traceAsync(
                    "CaptureRemoteDocument:captureSingleRemoteDocument:compositionInitialization",
                    ThreadLocalRandom.current().nextInt(),
                ) {
                    lateinit var frameClock: BroadcastFrameClock
                    frameClock = BroadcastFrameClock {
                        launch(recomposerContext) { frameClock.sendFrame(clock.nanoTime()) }
                    }
                    launch(recomposerContext + frameClock) {
                        recomposer.runRecomposeAndApplyChanges()
                    }

                    recomposer.currentState.filter { it == Recomposer.State.Idle }.first()
                }
            } finally {
                recomposer.cancel()
            }
        }

        val document = Snapshot.withMutableSnapshot {
            val recordingCanvas =
                RecordingCanvas(createBitmap(1, 1)).apply {
                    setRemoteComposeCreationState(creationState)
                }

            val remoteCanvas = RemoteCanvas(recordingCanvas)

            if (RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled) {
                check(creationState.document.buffer.buffer.size() == initialSize) {
                    "Document was written to during composition. Expected size $initialSize, got ${creationState.document.buffer.buffer.size()}"
                }
            }

            trace("CaptureRemoteDocument:captureSingleRemoteDocument:rootNodeRender") {
                rootNode.render(creationState, remoteCanvas)
                recordingCanvas.flush()
            }

            creationState.document.encodeToByteArray()
        }

        return CapturedDocument(document, writerEvents.pendingIntents, writerEvents.lambdas)
    } finally {
        lifecycleOwner.destroy()
        composition.dispose()
        recomposer.cancel()
    }
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
 * @param creationDisplayInfo Details about the virtual display to capture for (size, density,
 *   etc.).
 * @param remoteDensity The logical screen density and font scale to use for unit conversions.
 *   Defaults to density derived from [creationDisplayInfo]. Note: If passing custom values, they
 *   should typically match the density and font scale specified in [creationDisplayInfo] to avoid
 *   layout scaling discrepancies.
 * @param layoutDirection The layout direction (LTR or RTL) to use. Defaults to LTR.
 * @param writerEvents Callback to handle non-serializable events (e.g. pending intents).
 * @param context The Android [Context] to use.
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
): Flow<ByteArray> = flow {
    require(RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled) {
        "captureRemoteDocument requires isEnforceCleanRecompositionEnabled to be true"
    }
    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    val interceptor = coroutineContext[ContinuationInterceptor]
    val limitedCoroutineContext =
        if (
            interceptor is CoroutineDispatcher &&
                interceptor !is MainCoroutineDispatcher &&
                interceptor.isDispatchNeeded(EmptyCoroutineContext)
        ) {
            coroutineContext +
                interceptor.limitedParallelism(parallelism = 1, name = "captureRemoteDocument")
        } else {
            coroutineContext
        }

    val recomposerContext = currentCoroutineContext() + limitedCoroutineContext
    val recomposer = Recomposer(recomposerContext)
    val composition = Composition(applier, recomposer)
    val lifecycleOwner = HeadlessLifecycleOwner()

    try {
        val layoutDirection =
            (layoutDirection ?: toLayoutDirection(context.resources.configuration.layoutDirection))
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
                writerEvents = writerEvents,
                layoutDirection = layoutDirection,
                remoteDensity = remoteDensity,
            )

        val initialSize = creationState.document.buffer.buffer.size()

        composition.setContent {
            CompositionLocalProvider(
                LocalRemoteComposeCreationState provides creationState,
                LocalInspectionMode provides creationDisplayInfo.isInspectionMode,
                LocalDensity provides
                    Density(
                        creationDisplayInfo.density.density,
                        creationDisplayInfo.density.fontScale,
                    ),
                LocalRemoteDensity provides remoteDensity,
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalLayoutDirection provides layoutDirection,
                LocalFontWeightAdjustment provides
                    platformFontWeightAdjustment(context.resources.configuration),
                LocalLifecycleOwner provides lifecycleOwner,
                content = content,
            )
        }

        coroutineScope {
            lateinit var frameClock: BroadcastFrameClock
            frameClock = BroadcastFrameClock {
                launch(recomposerContext) { frameClock.sendFrame(clock.nanoTime()) }
            }

            launch(recomposerContext + frameClock) { recomposer.runRecomposeAndApplyChanges() }

            val documentFlow =
                recomposer.currentState
                    .filter { it == Recomposer.State.Idle }
                    .mapLatest {
                        Snapshot.withMutableSnapshot {
                            creationState.document =
                                profile.create(
                                    creationDisplayInfo.toCreationDisplayInfo(),
                                    writerEvents,
                                )
                            creationState.expressionCache.clear()
                            creationState.intExpressionCache.clear()
                            creationState.remoteVariableToId.clear()
                            creationState.floatArrayCache.clear()
                            creationState.longArrayCache.clear()
                            val recordingCanvas =
                                RecordingCanvas(createBitmap(1, 1)).apply {
                                    setRemoteComposeCreationState(creationState)
                                }

                            val remoteCanvas = RemoteCanvas(recordingCanvas)

                            check(creationState.document.buffer.buffer.size() == initialSize) {
                                "Document was written to during composition. Expected size $initialSize, got ${creationState.document.buffer.buffer.size()}"
                            }

                            rootNode.render(creationState, remoteCanvas)

                            creationState.document.encodeToByteArray()
                        }
                    }
                    .distinctUntilChanged { old, new -> old.contentEquals(new) }
            emitAll(documentFlow)
        }
    } finally {
        lifecycleOwner.destroy()
        composition.dispose()
        recomposer.cancel()
    }
}

private class HeadlessLifecycleOwner : LifecycleOwner {
    @Volatile private var state = Lifecycle.State.RESUMED
    private val observers = CopyOnWriteArrayList<LifecycleObserver>()

    override val lifecycle: Lifecycle =
        object : Lifecycle() {
            override val currentState: State
                get() = state

            override fun addObserver(observer: LifecycleObserver) {
                if (state == State.DESTROYED) {
                    return
                }
                observers.add(observer)
                if (observer is DefaultLifecycleObserver) {
                    observer.onCreate(this@HeadlessLifecycleOwner)
                    observer.onStart(this@HeadlessLifecycleOwner)
                    observer.onResume(this@HeadlessLifecycleOwner)
                }
                if (observer is LifecycleEventObserver) {
                    observer.onStateChanged(this@HeadlessLifecycleOwner, Event.ON_CREATE)
                    observer.onStateChanged(this@HeadlessLifecycleOwner, Event.ON_START)
                    observer.onStateChanged(this@HeadlessLifecycleOwner, Event.ON_RESUME)
                }
            }

            override fun removeObserver(observer: LifecycleObserver) {
                observers.remove(observer)
            }
        }

    fun destroy() {
        state = Lifecycle.State.DESTROYED
        for (observer in observers.toTypedArray()) {
            if (observer is DefaultLifecycleObserver) {
                if (observers.contains(observer)) observer.onPause(this)
                if (observers.contains(observer)) observer.onStop(this)
                if (observers.contains(observer)) observer.onDestroy(this)
            }
            if (observer is LifecycleEventObserver) {
                if (observers.contains(observer)) {
                    observer.onStateChanged(this, Lifecycle.Event.ON_PAUSE)
                }
                if (observers.contains(observer)) {
                    observer.onStateChanged(this, Lifecycle.Event.ON_STOP)
                }
                if (observers.contains(observer)) {
                    observer.onStateChanged(this, Lifecycle.Event.ON_DESTROY)
                }
            }
        }
        observers.clear()
    }
}

private fun CreationDisplayInfo.toRemote(
    fontScale: Float,
    isInspectionMode: Boolean = false,
): RemoteCreationDisplayInfo =
    RemoteCreationDisplayInfo(
        width = this.width,
        height = this.height,
        densityDpi = this.densityDpi,
        fontScale = fontScale,
        isInspectionMode = isInspectionMode,
    )

private fun platformFontWeightAdjustment(configuration: Configuration): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (configuration.fontWeightAdjustment != Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED) {
            configuration.fontWeightAdjustment
        } else {
            0
        }
    } else {
        0
    }
