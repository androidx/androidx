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

package androidx.compose.remote.player.compose

import androidx.compose.foundation.Canvas
import androidx.compose.remote.core.RemoteClock.nanoTime
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.player.compose.context.ComposePaintContext
import androidx.compose.remote.player.compose.context.ComposeRemoteContext
import androidx.compose.remote.player.view.RemoteComposeDocument
import androidx.compose.remote.player.view.action.NamedActionHandler
import androidx.compose.remote.player.view.action.StateUpdaterActionCallback
import androidx.compose.remote.player.view.player.platform.SettingsRetriever
import androidx.compose.remote.player.view.state.StateUpdater
import androidx.compose.remote.player.view.state.StateUpdaterImpl
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import java.time.Clock

/**
 * This is a player for a [androidx.compose.remote.player.view.RemoteComposeDocument].
 *
 * <p>It displays the document as well as providing the integration with the Android system (e.g.
 * passing sensor values, etc.). It also exposes player APIs that allows to control how the document
 * is displayed as well as reacting to document events.
 */
@Composable
internal fun RemoteComposePlayer(
    document: RemoteComposeDocument,
    modifier: Modifier = Modifier,
    theme: Int = -1,
    debugMode: Int = 0,
    clock: Clock = SystemClock(),
    onNamedAction: (name: String, value: Any?, stateUpdater: StateUpdater) -> Unit = { _, _, _ -> },
) {
    var start by remember(document) { mutableLongStateOf(System.nanoTime()) }
    var lastAnimationTime by remember(document) { mutableFloatStateOf(0.1f) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val remoteContext by
        remember(document) {
            val composeRemoteContext = ComposeRemoteContext(SystemClock())
            document.initializeContext(composeRemoteContext)
            composeRemoteContext.a11yAnimationEnabled = SettingsRetriever.animationsEnabled(context)
            composeRemoteContext.setDebug(debugMode)
            composeRemoteContext.theme = theme
            composeRemoteContext.setHaptic(haptic)
            composeRemoteContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME, -Float.MAX_VALUE)
            mutableStateOf<RemoteContext>(composeRemoteContext)
        }

    val coreDocument = document.document
    coreDocument.addActionCallback(
        object :
            StateUpdaterActionCallback(
                StateUpdaterImpl(remoteContext),
                object : NamedActionHandler {
                    override fun execute(name: String, value: Any?, stateUpdater: StateUpdater) {
                        onNamedAction.invoke(name, value, stateUpdater)
                    }
                },
            ) {}
    )

    val dragHappened = remember { mutableStateOf(false) }
    Canvas(
        modifier =
            modifier.pointerInput(remoteContext) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (i in 0 until event.changes.size) {
                            val change = event.changes[i]
                            if (change.changedToDown()) {
                                val x = change.position.x
                                val y = change.position.y
                                val time = remoteContext.animationTime
                                remoteContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME, time)
                                coreDocument.touchDown(remoteContext, x, y)
                                dragHappened.value = false
                                change.consume()
                            }
                            if (change.changedToUp()) {
                                val x = change.position.x
                                val y = change.position.y
                                val time = remoteContext.animationTime
                                remoteContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME, time)
                                coreDocument.touchUp(remoteContext, x, y, 0f, 0f)
                                if (!dragHappened.value) {
                                    coreDocument.onClick(remoteContext, x, y)
                                }
                                change.consume()
                            }
                            if (change.positionChanged()) {
                                val x = change.position.x
                                val y = change.position.y
                                val time = remoteContext.animationTime
                                remoteContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME, time)
                                coreDocument.touchDrag(remoteContext, x, y)
                                dragHappened.value = true
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        drawIntoCanvas {
            it.save()

            if (remoteContext.isAnimationEnabled) {
                val nanoStart = nanoTime(clock)
                val animationTime: Float = (nanoStart - start) * 1E-9f
                remoteContext.animationTime = animationTime
                remoteContext.loadFloat(RemoteContext.ID_ANIMATION_TIME, animationTime)
                val loopTime: Float = animationTime - lastAnimationTime
                remoteContext.loadFloat(RemoteContext.ID_ANIMATION_DELTA_TIME, loopTime)
                lastAnimationTime = animationTime
                remoteContext.currentTime = clock.millis()
            }

            remoteContext.density = density
            remoteContext.mWidth = size.width
            remoteContext.mHeight = size.height

            remoteContext.loadFloat(RemoteContext.ID_FONT_SIZE, 30f)

            remoteContext.setPaintContext(
                ComposePaintContext(remoteContext as ComposeRemoteContext, it)
            )
            document.paint(remoteContext, theme)
            it.restore()
        }
    }
}
