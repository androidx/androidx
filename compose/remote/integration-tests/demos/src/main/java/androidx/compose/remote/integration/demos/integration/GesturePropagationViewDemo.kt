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

package androidx.compose.remote.integration.demos.integration

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.combinedClickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.foundation.layout.RemoteSpacer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.core.action.NamedActionHandler
import androidx.compose.remote.player.core.action.StateUpdaterActionCallback
import androidx.compose.remote.player.core.state.StateUpdater
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private const val REMOTE_COMPOSE_CLICK = "remote_compose_click"
private const val REMOTE_COMPOSE_DOUBLE_CLICK = "remote_compose_double_click"
private const val REMOTE_COMPOSE_LONG_CLICK = "remote_compose_long_click"

@Suppress(
    "RestrictedApiAndroidX"
) // Referring to RemoteText, combinedClickable, remote-core, remote-creation-core
@Composable
fun GesturePropagationViewDemo() {
    val experimentalProfile =
        Profile(
            RcPlatformProfiles.ANDROIDX.apiLevel,
            RcPlatformProfiles.ANDROIDX.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
            RcPlatformProfiles.ANDROIDX.platform,
            RcPlatformProfiles.ANDROIDX.profileFactory,
        )

    RemoteFrameLayoutDemo(modifier = Modifier.fillMaxSize(), profile = experimentalProfile) {
        val clickCount = rememberMutableRemoteInt(0)
        val doubleClickCount = rememberMutableRemoteInt(0)
        val longClickCount = rememberMutableRemoteInt(0)
        RemoteColumn(
            modifier = RemoteModifier.fillMaxSize().padding(5.rdp),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            RemoteText("Clickable:".rs, color = Color.Black.rc)
            RemoteRow {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(80.rdp)
                            .background(RemoteColor(Color.Red))
                            .clickable(hostAction(REMOTE_COMPOSE_CLICK.rs)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("HostAction.".rs)
                }
                RemoteSpacer(modifier = RemoteModifier.width(5.rdp))
                RemoteBox(
                    modifier =
                        RemoteModifier.size(80.rdp)
                            .background(RemoteColor(Color.Green))
                            .clickable(valueChange(clickCount, clickCount + 1)),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("ValueChange.".rs, color = Color.Black.rc)
                }
            }
            RemoteSpacer(modifier = RemoteModifier.height(10.rdp))
            RemoteText("CombinedClickable:".rs, color = Color.Black.rc)
            RemoteRow {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(80.rdp)
                            .background(RemoteColor(Color.Red))
                            .combinedClickable(
                                onClick = hostAction(REMOTE_COMPOSE_CLICK.rs),
                                onDoubleClick = hostAction(REMOTE_COMPOSE_DOUBLE_CLICK.rs),
                                onLongClick = hostAction(REMOTE_COMPOSE_LONG_CLICK.rs),
                            ),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("HostAction.".rs)
                }
                RemoteSpacer(modifier = RemoteModifier.width(5.rdp))
                RemoteBox(
                    modifier =
                        RemoteModifier.size(80.rdp)
                            .background(RemoteColor(Color.Green))
                            .combinedClickable(
                                onClick = valueChange(clickCount, clickCount + 1),
                                onDoubleClick = valueChange(doubleClickCount, doubleClickCount + 1),
                                onLongClick = valueChange(longClickCount, longClickCount + 1),
                            ),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText("ValueChange.".rs, color = Color.Black.rc)
                }
            }
            RemoteSpacer(modifier = RemoteModifier.height(10.rdp))
            RemoteText(
                "ValueChange click counter: ".rs + clickCount.toRemoteString(),
                color = Color.Black.rc,
            )
            RemoteText(
                "ValueChange double click counter: ".rs + doubleClickCount.toRemoteString(),
                color = Color.Black.rc,
            )
            RemoteText(
                "ValueChange long click counter: ".rs + longClickCount.toRemoteString(),
                color = Color.Black.rc,
            )
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
private fun RemoteFrameLayoutDemo(
    modifier: Modifier = Modifier,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    content: @Composable @RemoteComposable () -> Unit,
) {
    var documentState by remember { mutableStateOf<RemoteDocument?>(null) }
    val context = LocalContext.current
    val creationDisplayInfo = createCreationDisplayInfo()

    LaunchedEffect(Unit) {
        val captured =
            captureSingleRemoteDocument(
                creationDisplayInfo = creationDisplayInfo,
                context = context,
                profile = profile,
                content = content,
            )
        documentState = RemoteDocument(captured.bytes)
    }

    if (documentState != null) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                var viewClickCounter = 0
                var viewDoubleClickCounter = 0
                var viewLongClickCounter = 0
                var rcClickCounter = 0
                var rcDoubleClickCounter = 0
                var rcLongClickCounter = 0

                val density = ctx.resources.displayMetrics.density
                fun dp(dpValue: Int): Int = (dpValue * density).toInt()

                val root =
                    LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(16), dp(16), dp(16), dp(16))
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                    }

                val t1 =
                    TextView(ctx).apply {
                        text =
                            "Gestures on the red boxes should increase the Remote Compose gesture counter. "
                        setTextColor(AndroidColor.BLACK)
                    }
                val t2 =
                    TextView(ctx).apply {
                        text =
                            "Gestures on the green boxes should increase the ValueChange counter. "
                        setTextColor(AndroidColor.BLACK)
                    }
                val t3 =
                    TextView(ctx).apply {
                        text =
                            "Gestures outside the colored boxes should propagate to the parent View and increase its counters. "
                        setTextColor(AndroidColor.BLACK)
                    }
                root.addView(t1)
                root.addView(t2)
                root.addView(t3)

                val spacer =
                    View(ctx).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10))
                    }
                root.addView(spacer)

                fun createCell(text: String, alignment: Int = Gravity.CENTER): TextView {
                    return TextView(ctx).apply {
                        this.text = text
                        gravity = alignment
                        setTextColor(AndroidColor.BLACK)
                        setPadding(dp(4), dp(4), dp(4), dp(4))
                        background =
                            GradientDrawable().apply { setStroke(dp(1), AndroidColor.BLACK) }
                    }
                }

                fun createRow(): LinearLayout {
                    return LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams =
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                        weightSum = 4f
                    }
                }

                val table =
                    LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams =
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                    }

                val headerRow =
                    createRow().apply {
                        addView(
                            createCell(" ").apply {
                                layoutParams =
                                    LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f,
                                    )
                            }
                        )
                        addView(
                            createCell("Click").apply {
                                layoutParams =
                                    LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f,
                                    )
                            }
                        )
                        addView(
                            createCell("Double").apply {
                                layoutParams =
                                    LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f,
                                    )
                            }
                        )
                        addView(
                            createCell("Long").apply {
                                layoutParams =
                                    LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f,
                                    )
                            }
                        )
                    }
                table.addView(headerRow)

                val viewClickCell =
                    createCell("0").apply {
                        layoutParams =
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    }
                val viewDoubleClickCell =
                    createCell("0").apply {
                        layoutParams =
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    }
                val viewLongClickCell =
                    createCell("0").apply {
                        layoutParams =
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    }
                val viewRow =
                    createRow().apply {
                        addView(
                            createCell("View", Gravity.START or Gravity.CENTER_VERTICAL).apply {
                                layoutParams =
                                    LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f,
                                    )
                            }
                        )
                        addView(viewClickCell)
                        addView(viewDoubleClickCell)
                        addView(viewLongClickCell)
                    }
                table.addView(viewRow)

                val rcClickCell =
                    createCell("0").apply {
                        layoutParams =
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    }
                val rcDoubleClickCell =
                    createCell("0").apply {
                        layoutParams =
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    }
                val rcLongClickCell =
                    createCell("0").apply {
                        layoutParams =
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    }
                val rcRow =
                    createRow().apply {
                        addView(
                            createCell("Remote Compose", Gravity.START or Gravity.CENTER_VERTICAL)
                                .apply {
                                    layoutParams =
                                        LinearLayout.LayoutParams(
                                            0,
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            1f,
                                        )
                                }
                        )
                        addView(rcClickCell)
                        addView(rcDoubleClickCell)
                        addView(rcLongClickCell)
                    }
                table.addView(rcRow)

                root.addView(table)

                fun updateTable() {
                    viewClickCell.text = "$viewClickCounter"
                    viewDoubleClickCell.text = "$viewDoubleClickCounter"
                    viewLongClickCell.text = "$viewLongClickCounter"
                    rcClickCell.text = "$rcClickCounter"
                    rcDoubleClickCell.text = "$rcDoubleClickCounter"
                    rcLongClickCell.text = "$rcLongClickCounter"
                }

                val parentFrameLayout =
                    FrameLayout(ctx).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                                .apply { setMargins(dp(10), dp(10), dp(10), dp(10)) }
                        background =
                            GradientDrawable().apply { setStroke(dp(1), AndroidColor.BLUE) }
                        var lastClickTime = 0L
                        val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()
                        setOnClickListener {
                            val currentTime = SystemClock.uptimeMillis()
                            if (
                                lastClickTime != 0L &&
                                    currentTime - lastClickTime <= doubleTapTimeout
                            ) {
                                viewDoubleClickCounter++
                                lastClickTime = 0L
                            } else {
                                viewClickCounter++
                                lastClickTime = currentTime
                            }
                            updateTable()
                        }
                        setOnLongClickListener {
                            viewLongClickCounter++
                            updateTable()
                            true
                        }
                    }

                val player =
                    RemoteComposePlayer(ctx).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                    }
                parentFrameLayout.addView(player)
                root.addView(parentFrameLayout)

                root.tag =
                    DemoViewHolder(
                        player,
                        onNamedAction = { action ->
                            when (action) {
                                REMOTE_COMPOSE_CLICK -> rcClickCounter++
                                REMOTE_COMPOSE_DOUBLE_CLICK -> rcDoubleClickCounter++
                                REMOTE_COMPOSE_LONG_CLICK -> rcLongClickCounter++
                            }
                            updateTable()
                        },
                    )

                root
            },
            update = { root ->
                val holder = root.tag as? DemoViewHolder ?: return@AndroidView
                holder.player.setDocument(documentState!!)
                holder.player.document.document.clearActionCallbacks()
                holder.player.document.document.addActionCallback(
                    object :
                        StateUpdaterActionCallback(
                            holder.player.stateUpdater,
                            object : NamedActionHandler {
                                override fun execute(
                                    name: String,
                                    value: Any?,
                                    stateUpdater: StateUpdater,
                                ) {
                                    holder.onNamedAction(name)
                                }
                            },
                        ) {}
                )
            },
        )
    }
}

@Suppress("RestrictedApiAndroidX")
private class DemoViewHolder(val player: RemoteComposePlayer, val onNamedAction: (String) -> Unit)
