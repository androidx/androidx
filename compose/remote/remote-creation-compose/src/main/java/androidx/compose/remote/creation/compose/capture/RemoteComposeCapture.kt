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
@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.hardware.display.VirtualDisplay
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags.isRemoteApplierEnabled
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.v2.captureSingleRemoteDocumentV2
import androidx.compose.remote.creation.compose.widgets.toLayoutDirection
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

@Composable
public fun rememberRemoteDocument(
    creationDisplayInfo: CreationDisplayInfo = createCreationDisplayInfo(),
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerEvents: WriterEvents = WriterEvents(),
    onCreate: ((CoreDocument) -> Unit)? = null,
    content: @Composable () -> Unit,
): MutableState<CoreDocument?> {
    val layoutDirection = LocalLayoutDirection.current
    val doc: MutableState<CoreDocument?> = remember { mutableStateOf(null) }
    if (isRemoteApplierEnabled) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val document =
                captureSingleRemoteDocumentV2(
                    creationDisplayInfo = createCreationDisplayInfo(context),
                    layoutDirection = layoutDirection,
                    context = context,
                    content = content,
                    profile = profile,
                )
            val coreDocument =
                CoreDocument().apply {
                    initFromBuffer(
                        RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
                    )
                }
            if (onCreate != null) {
                onCreate(coreDocument)
            }
            doc.value = coreDocument
        }
    } else {
        val done = remember { mutableStateOf(false) }
        val context = LocalContext.current
        val virtualDisplay = rememberVirtualDisplay(creationDisplayInfo)
        RemoteComposeCapture(
            context = context,
            virtualDisplay = virtualDisplay,
            creationDisplayInfo = creationDisplayInfo,
            layoutDirection = layoutDirection,
            immediateCapture = true,
            onPaint = { _, writer ->
                if (!done.value) {
                    val buffer = writer.buffer()
                    val bufferSize = writer.bufferSize()
                    val inputStream = ByteArrayInputStream(buffer, 0, bufferSize)
                    val document = CoreDocument()
                    val rcBuffer = RemoteComposeBuffer.fromInputStream(inputStream)
                    document.initFromBuffer(rcBuffer)
                    doc.value = document
                    done.value = true
                    if (onCreate != null) {
                        onCreate(document)
                    }
                }
                done.value
            },
            onCaptureReady = @Composable {},
            profile = profile,
            writerEvents = writerEvents,
            content = content,
        )
    }
    return doc
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberRemoteDocument(
    size: Size,
    onCreate: ((CoreDocument) -> Unit)? = null,
    content: @Composable () -> Unit,
): MutableState<CoreDocument?> {
    return rememberRemoteDocument(
        creationDisplayInfo =
            CreationDisplayInfo(
                size.width.toInt(),
                size.height.toInt(),
                LocalConfiguration.current.densityDpi,
            ),
        onCreate = onCreate,
        content = content,
    )
}

@Composable
public fun rememberAsyncRemoteDocument(
    creationDisplayInfo: CreationDisplayInfo = createCreationDisplayInfo(),
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerEvents: WriterEvents? = null,
    content: @Composable (MutableState<Boolean>) -> Unit,
): MutableState<CoreDocument?> {
    val doc: MutableState<CoreDocument?> = remember { mutableStateOf(null) }
    val done = remember { mutableStateOf(false) }
    val readyToCapture = remember { mutableStateOf(false) }

    val virtualDisplay = rememberVirtualDisplay(creationDisplayInfo)

    RemoteComposeCapture(
        context = LocalContext.current,
        virtualDisplay = virtualDisplay,
        creationDisplayInfo = creationDisplayInfo,
        layoutDirection = LocalLayoutDirection.current,
        immediateCapture = false,
        onPaint = { view: View, writer: RemoteComposeWriter ->
            if (readyToCapture.value && !done.value) {
                val buffer = writer.buffer()
                val bufferSize = writer.bufferSize()
                val inputStream = ByteArrayInputStream(buffer, 0, bufferSize)
                val document = CoreDocument()
                val rcBuffer = RemoteComposeBuffer.fromInputStream(inputStream)
                document.initFromBuffer(rcBuffer)
                doc.value = document
                done.value = true
            }
            done.value
        },
        onCaptureReady = {},
        profile = profile,
        writerEvents = writerEvents,
        content = @Composable { content(readyToCapture) },
    )
    return doc
}

@Composable
public fun rememberVirtualDisplay(creationDisplayInfo: CreationDisplayInfo): VirtualDisplay {
    val context = LocalContext.current
    val virtualDisplay = remember { DisplayPool.allocate(context, creationDisplayInfo) }
    DisposableEffect(Unit) { onDispose { DisplayPool.release(virtualDisplay) } }
    return virtualDisplay
}

@Composable
public fun displaySize(): Size {
    // Note: Usage of LocalConfiguration.current.screenWidthDp was replaced due to
    // `ConfigurationScreenWidthHeight` lint rule
    // TODO: Consider to remove this function at all
    return LocalWindowInfo.current.containerSize.toSize()
}

/**
 * Encapsulate the overall capture process of running a composable function in a virtual display.
 * The remoteComposeExecution() function will run inside a CaptureComposeView, capturing its output
 * via a RecordingCanvas
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeCapture(
    context: Context,
    virtualDisplay: VirtualDisplay,
    creationDisplayInfo: CreationDisplayInfo,
    layoutDirection: LayoutDirection? = null,
    public val immediateCapture: Boolean = true,
    public val onPaint: (View, RemoteComposeWriter) -> Boolean,
    public val onCaptureReady: @Composable () -> Unit,
    public val profile: Profile,
    public val writerEvents: WriterEvents?,
    public val content: @Composable () -> Unit,
    public val remoteComposeExecution:
        @Composable
        (CaptureComposeView, @Composable () -> Unit) -> Unit =
        { captureComposeView, contentWrapper ->
            RemoteComposeExecution(
                captureComposeView = captureComposeView,
                creationDisplayInfo = creationDisplayInfo,
                layoutDirection =
                    layoutDirection
                        ?: toLayoutDirection(context.resources.configuration.layoutDirection),
                profile = profile,
                writerEvents = writerEvents,
                content = contentWrapper,
            )
        },
) {
    public constructor(
        context: Context,
        virtualDisplay: VirtualDisplay,
        creationDisplayInfo: CreationDisplayInfo,
        layoutDirection: LayoutDirection? = null,
        immediateCapture: Boolean = true,
        onPaint: (View, RemoteComposeWriter) -> Boolean,
        onCaptureReady: @Composable () -> Unit,
        apiLevel: Int,
        profiles: Int,
        content: @Composable () -> Unit,
        remoteComposeExecution: @Composable (CaptureComposeView, @Composable () -> Unit) -> Unit =
            { captureComposeView, contentWrapper ->
                RemoteComposeExecution(
                    captureComposeView = captureComposeView,
                    creationDisplayInfo = creationDisplayInfo,
                    layoutDirection =
                        layoutDirection
                            ?: toLayoutDirection(context.resources.configuration.layoutDirection),
                    profile = RcPlatformProfiles.ANDROIDX,
                    writerEvents = null,
                    content = contentWrapper,
                )
            },
    ) : this(
        context = context,
        virtualDisplay = virtualDisplay,
        creationDisplayInfo = creationDisplayInfo,
        layoutDirection = layoutDirection,
        immediateCapture = immediateCapture,
        onPaint = onPaint,
        onCaptureReady = onCaptureReady,
        profile = RcPlatformProfiles.ANDROIDX,
        writerEvents = null,
        content = content,
        remoteComposeExecution = remoteComposeExecution,
    )

    public fun newSize(width: Int, height: Int) {
        resizableLayout.layoutParams = FrameLayout.LayoutParams(width, height)
    }

    private var resizableLayout: ResizableLayout

    init {

        val presentation = SecondaryDisplay(context, virtualDisplay.display)
        val captureComposeView =
            CaptureComposeView(presentation.context, immediateCapture, onPaint, onCaptureReady)
        captureComposeView.apply {
            setContent {
                val effectiveLayoutDirection = layoutDirection ?: LocalLayoutDirection.current
                CompositionLocalProvider(LocalLayoutDirection provides effectiveLayoutDirection) {
                    remoteComposeExecution(captureComposeView) { content.invoke() }
                }
            }
        }
        presentation.show()
        resizableLayout = presentation.resizeLayout
        resizableLayout.use(captureComposeView)
    }
}

@Composable
public fun RemoteComposeExecution(
    captureComposeView: CaptureComposeView,
    creationDisplayInfo: CreationDisplayInfo,
    layoutDirection: LayoutDirection,
    profile: Profile,
    writerEvents: WriterEvents?,
    content: @Composable () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val remoteComposeCreationState = remember {
        RemoteComposeCreationState(
            creationDisplayInfo = creationDisplayInfo,
            profile = profile,
            writerEvents = writerEvents,
            layoutDirection = layoutDirection,
        )
    }
    CompositionLocalProvider(
        LocalRemoteComposeCreationState provides remoteComposeCreationState,
        LocalLayoutDirection provides layoutDirection,
    ) {
        captureComposeView.setRemoteComposeState(remoteComposeCreationState)
        content.invoke()
    }
}

@Composable
public fun RemoteComposeExecution(
    captureComposeView: CaptureComposeView,
    size: Size,
    apiLevel: Int,
    profiles: Int,
    content: @Composable () -> Unit,
) {
    val platform = LocalRcPlatformServices.current

    val remoteComposeCreationState = remember {
        RemoteComposeCreationState(platform, size, apiLevel, profiles)
    }
    CompositionLocalProvider(LocalRemoteComposeCreationState provides remoteComposeCreationState) {
        captureComposeView.setRemoteComposeState(remoteComposeCreationState)
        content.invoke()
    }
}

@Composable
public fun RemoteComposeExecution(
    captureComposeView: CaptureComposeView,
    size: Size,
    profile: Profile,
    content: @Composable () -> Unit,
) {
    val remoteComposeCreationState = remember { RemoteComposeCreationState(size, profile) }
    CompositionLocalProvider(LocalRemoteComposeCreationState provides remoteComposeCreationState) {
        captureComposeView.setRemoteComposeState(remoteComposeCreationState)
        content.invoke()
    }
}

/**
 * Record a RemoteComposeDocument from a composable function without creating a SecondaryDisplay.
 */
@Composable
public fun RememberRemoteDocumentInline(
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    onDocument: (CoreDocument) -> Unit,
    content: @RemoteComposable @Composable () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    if (isRemoteApplierEnabled) {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val document =
                captureSingleRemoteDocumentV2(
                    creationDisplayInfo = createCreationDisplayInfo(context),
                    layoutDirection = layoutDirection,
                    context = context,
                    content = content,
                )
            onDocument.invoke(
                CoreDocument().apply {
                    initFromBuffer(
                        RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(document.bytes))
                    )
                }
            )
        }
    } else {

        val generated = remember { mutableStateOf(false) }
        if (!generated.value) {
            val creationDisplayInfo = createCreationDisplayInfo()
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    CaptureComposeView(
                            context = context,
                            immediateCapture = true,
                            onPaint = { _, writer ->
                                val buffer = writer.buffer()
                                val bufferSize = writer.bufferSize()
                                val inputStream = ByteArrayInputStream(buffer, 0, bufferSize)
                                val coreDocument = CoreDocument()
                                val rcBuffer = RemoteComposeBuffer.fromInputStream(inputStream)
                                coreDocument.initFromBuffer(rcBuffer)
                                onDocument(coreDocument)
                                true
                            },
                            onCaptureReady = {},
                        )
                        .apply {
                            setContent {
                                RemoteComposeExecution(
                                    captureComposeView = this,
                                    creationDisplayInfo = creationDisplayInfo,
                                    layoutDirection = layoutDirection,
                                    profile = profile,
                                    writerEvents = null,
                                    content = content,
                                )
                            }
                        }
                },
            )
        }
    }
}
