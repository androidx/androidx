/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.compose.remote.frontend.capture

import android.content.Context
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.PlatformProfile
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.frontend.layout.RemoteComposable
import androidx.compose.remote.frontend.state.FallbackCreationState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

data class Connection(
    val width: Int = Resources.getSystem().displayMetrics.widthPixels,
    val height: Int = Resources.getSystem().displayMetrics.heightPixels,
    val density: Int = Resources.getSystem().displayMetrics.densityDpi,
) {
    val size: Size
        get() = Size(width.toFloat(), height.toFloat())
}

@Composable
fun rememberRemoteDocument(
    size: Size = displaySize(),
    onCreate: ((CoreDocument) -> Unit)? = null,
    content: @Composable () -> Unit,
): MutableState<CoreDocument?> {
    return rememberRemoteDocument(size, onCreate, CoreDocument.DOCUMENT_API_LEVEL, 0, content)
}

@Composable
fun rememberRemoteDocument(
    size: Size = displaySize(),
    onCreate: ((CoreDocument) -> Unit)? = null,
    apiLevel: Int,
    profiles: Int,
    content: @Composable () -> Unit,
): MutableState<CoreDocument?> {
    val doc: MutableState<CoreDocument?> = remember { mutableStateOf(null) }
    val densityDpi = LocalConfiguration.current.densityDpi
    val connection = Connection(size.width.toInt(), size.height.toInt(), densityDpi)
    val done = remember { mutableStateOf(false) }
    RemoteComposeCapture(
        LocalContext.current,
        connection,
        true,
        { view, writer ->
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
        @Composable {},
        apiLevel,
        profiles,
        @Composable { content() },
    )
    return doc
}

@Composable
fun rememberAsyncRemoteDocument(
    size: Size = displaySize(),
    content: @Composable (MutableState<Boolean>) -> Unit,
): MutableState<CoreDocument?> {
    return rememberAsyncRemoteDocument(size, CoreDocument.DOCUMENT_API_LEVEL, 0, content)
}

@Composable
fun rememberAsyncRemoteDocument(
    size: Size = displaySize(),
    apiLevel: Int,
    profiles: Int,
    content: @Composable (MutableState<Boolean>) -> Unit,
): MutableState<CoreDocument?> {
    val doc: MutableState<CoreDocument?> = remember { mutableStateOf(null) }
    val densityDpi = LocalConfiguration.current.densityDpi
    val connection =
        Connection(width = size.width.toInt(), height = size.height.toInt(), density = densityDpi)
    val done = remember { mutableStateOf(false) }
    val readyToCapture = remember { mutableStateOf(false) }
    RemoteComposeCapture(
        LocalContext.current,
        connection,
        false,
        { view, writer ->
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
        @Composable {},
        apiLevel,
        profiles,
        @Composable { content(readyToCapture) },
    )
    return doc
}

@Composable
fun displaySize(): Size {
    return with(LocalDensity.current) {
        DpSize(
                LocalConfiguration.current.screenWidthDp.dp,
                LocalConfiguration.current.screenHeightDp.dp,
            )
            .toSize()
    }
}

/**
 * Encapsulate the overall capture process of running a composable function in a virtual display.
 * The remoteComposeExecution() function will run inside a CaptureComposeView, capturing its output
 * via a RecordingCanvas
 */
class RemoteComposeCapture(
    context: Context,
    connection: Connection,
    val immediateCapture: Boolean = true,
    val onPaint: (View, RemoteComposeWriter) -> Boolean,
    val onCaptureReady: @Composable () -> Unit,
    val apiLevel: Int,
    val profiles: Int,
    val content: @Composable () -> Unit,
    val remoteComposeExecution: @Composable (CaptureComposeView, @Composable () -> Unit) -> Unit =
        { captureComposeView, contentWrapper ->
            RemoteComposeExecution(
                captureComposeView,
                connection.size,
                apiLevel,
                profiles,
                contentWrapper,
            )
        },
) {
    constructor(
        context: Context,
        connection: Connection,
        immediateCapture: Boolean = true,
        onPaint: (View, RemoteComposeWriter) -> Boolean,
        onCaptureReady: @Composable () -> Unit,
        profile: Profile,
        content: @Composable () -> Unit,
    ) : this(
        context,
        connection,
        immediateCapture,
        onPaint,
        onCaptureReady,
        profile.apiLevel,
        profile.operationsProfiles,
        content,
        remoteComposeExecution =
            @Composable { captureComposeView, contentWrapper ->
                RemoteComposeExecution(captureComposeView, connection.size, profile, contentWrapper)
            },
    )

    fun newSize(width: Int, height: Int) {
        resizableLayout.layoutParams = FrameLayout.LayoutParams(width, height)
    }

    private var resizableLayout: ResizableLayout

    init {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val virtualDisplay =
            displayManager.createVirtualDisplay(
                "Projection",
                connection.width,
                connection.height,
                connection.density,
                SurfaceView(context).holder.surface,
                0,
            )

        val presentation = SecondaryDisplay(context, virtualDisplay.display)
        val captureComposeView =
            CaptureComposeView(presentation.context, immediateCapture, onPaint, onCaptureReady)
        captureComposeView.apply {
            setContent { remoteComposeExecution(captureComposeView) { content.invoke() } }
        }
        presentation.show()
        resizableLayout = presentation.resizeLayout
        resizableLayout.use(captureComposeView)
    }
}

@Composable
fun RemoteComposeExecution(
    captureComposeView: CaptureComposeView,
    size: Size,
    apiLevel: Int,
    profiles: Int,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val platform = LocalPlatform.current

    val remoteComposeCreationState = remember {
        RemoteComposeCreationState(platform, density.density, size, apiLevel, profiles)
    }
    CompositionLocalProvider(LocalRemoteComposeCreationState provides remoteComposeCreationState) {
        FallbackCreationState.state = remoteComposeCreationState
        captureComposeView.setRemoteComposeState(remoteComposeCreationState)
        content.invoke()
    }
}

@Composable
fun RemoteComposeExecution(
    captureComposeView: CaptureComposeView,
    size: Size,
    profile: Profile,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    val remoteComposeCreationState = remember {
        RemoteComposeCreationState(density.density, size, profile)
    }
    CompositionLocalProvider(LocalRemoteComposeCreationState provides remoteComposeCreationState) {
        FallbackCreationState.state = remoteComposeCreationState
        captureComposeView.setRemoteComposeState(remoteComposeCreationState)
        content.invoke()
    }
}

/**
 * Record a RemoteComposeDocument from a composable function without creating a SecondaryDisplay.
 */
@Composable
fun RememberRemoteDocumentInline(
    profile: Profile = PlatformProfile.ANDROIDX,
    onDocument: (CoreDocument) -> Unit,
    content: @RemoteComposable @Composable () -> Unit,
) {
    val generated = remember { mutableStateOf(false) }
    if (!generated.value) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val connection = Connection()

                CaptureComposeView(
                        context = context,
                        immediateCapture = true,
                        onPaint = { view, writer ->
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
                            RemoteComposeExecution(this, connection.size, profile, content)
                        }
                    }
            },
        )
    }
}
