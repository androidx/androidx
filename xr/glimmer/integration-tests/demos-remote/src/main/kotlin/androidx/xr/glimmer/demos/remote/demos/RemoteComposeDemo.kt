/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.xr.glimmer.demos.remote.demos

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.RemoteDensityBehavior
import androidx.compose.remote.creation.compose.capture.captureRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberRemoteDocumentState(
    content: @Composable @RemoteComposable () -> Unit
): State<ByteArray?> {
    val context = LocalContext.current
    val flow = remember(context) { remoteDocumentFlow(context, content) }
    return flow.collectAsStateWithLifecycle(null)
}

internal fun remoteDocumentFlow(
    context: Context,
    content: @Composable @RemoteComposable () -> Unit,
): Flow<ByteArray> {
    return captureRemoteDocument(
        context = context,
        creationDisplayInfo =
            createCreationDisplayInfo(
                context = context,
                densityBehavior = RemoteDensityBehavior.Dp,
            ),
        profile = RemoteGlimmerProfile,
        content = content,
    )
}

@Composable
@SuppressLint("RestrictedApiAndroidX")
internal fun OutlinedRemoteDocumentPlayer(
    documentBytes: ByteArray?,
    modifier: Modifier = Modifier,
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
    update: (RemoteComposePlayer) -> Unit = {},
) {
    // Conversion: ByteArray -> RemoteDocument -> CoreDocument.
    val coreDocument = remember { mutableStateOf<CoreDocument?>(null) }
    if (documentBytes != null) {
        LaunchedEffect(documentBytes) {
            withContext(Dispatchers.Default) {
                coreDocument.value = RemoteDocument(documentBytes).document
            }
        }
    }

    // Outline the boundaries of the RC player to highlight where the RC content is rendered.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Remote Compose content:",
            color = Color.Red,
            style = GlimmerTheme.typography.caption,
        )
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Red),
            contentAlignment = Alignment.TopCenter,
        ) {
            val coreDocumentValue = coreDocument.value
            if (coreDocumentValue != null) {
                RemoteDocumentPlayer(
                    document = coreDocumentValue,
                    update = update,
                    onNamedAction = onNamedAction,
                    documentWidth = maxWidth.value.toInt(),
                    documentHeight = maxHeight.value.toInt(),
                )
            } else {
                ProgressIndicator(Modifier.size(80.dp))
            }
        }
    }
}

@Composable
internal fun SaveRemoteDocumentButton(
    fileName: String,
    documentBytes: ByteArray?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saveLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/remote-compose-doc")
        ) { uri ->
            if (uri != null && documentBytes != null) {
                scope.launch(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(documentBytes)
                    }
                }
            }
        }

    Button(
        onClick = { saveLauncher.launch(fileName) },
        enabled = documentBytes != null,
        modifier = modifier.padding(24.dp),
    ) {
        Text("Save as .rc file")
    }
}

@Composable
private fun ProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Dp = 12.dp,
    sweepAngle: Float = 90f,
    durationMillis: Int = 1000,
) {
    val transition = rememberInfiniteTransition(label = "rotatingArc")
    val rotation by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "rotation",
        )
    Canvas(modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val halfStroke = stroke.width / 2f
        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
        val topLeft = Offset(halfStroke, halfStroke)
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}

@SuppressLint("RestrictedApiAndroidX")
private val RemoteGlimmerProfile: Profile =
    Profile(
        CoreDocument.DOCUMENT_API_LEVEL,
        PROFILE_ANDROIDX,
        AndroidxRcPlatformServices(),
        {
            // Get all operations supported by the player
            Operations.getOperations(CoreDocument.DOCUMENT_API_LEVEL, PROFILE_ANDROIDX)
                ?.keySet()
                .orEmpty()
                .filter { opId ->
                    // Exclude CORE_TEXT to force bitmap font rendering
                    opId != Operations.CORE_TEXT && opId != Operations.DRAW_TEXT_RUN
                }
                .toSet()
        },
    ) { displayInfo, profile, callback ->
        RemoteComposeWriterAndroid(displayInfo, null, profile, callback)
    }
