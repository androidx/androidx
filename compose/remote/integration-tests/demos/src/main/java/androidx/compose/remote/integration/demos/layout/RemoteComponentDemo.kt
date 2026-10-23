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

package androidx.compose.remote.integration.demos.layout

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteScrollState
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.tooling.preview.RemoteComponentPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteComponentDemo(onDocument: (CoreDocument) -> Unit = {}) {
    RemoteDemo(modifier = Modifier.fillMaxSize(), onDocument = onDocument) {
        RemoteComponentDemoContent()
    }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComponentPreview
@Composable
@RemoteComposable
fun RemoteComponentDemoContent() {
    val scrollState = remember { RemoteScrollState() }
    RemoteColumn(modifier = RemoteModifier.fillMaxSize().background(Color(0xFFF5F5F5).rc)) {
        RemoteText(
            text = "Scroll to observe rootX / rootY update relative to the document:".rs,
            modifier = RemoteModifier.padding(12.rdp),
            color = Color(0xFF212121).rc,
        )
        RemoteColumn(
            modifier =
                RemoteModifier.verticalScroll(scrollState)
                    .fillMaxWidth()
                    .weight(1f.rf)
                    .padding(12.rdp),
            verticalArrangement = RemoteArrangement.spacedBy(16.rdp),
        ) {
            RemoteComponentInspectorBox(
                label = "Full-Width Box",
                backgroundColor = Color(0xFFE3F2FD).rc,
                accentColor = Color(0xFF1565C0).rc,
                modifier = RemoteModifier.fillMaxWidth().height(150.rdp),
            )

            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth(),
                horizontalArrangement = RemoteArrangement.spacedBy(12.rdp),
            ) {
                RemoteComponentInspectorBox(
                    label = "Row Item 1",
                    backgroundColor = Color(0xFFE8F5E9).rc,
                    accentColor = Color(0xFF2E7D32).rc,
                    modifier = RemoteModifier.weight(1f.rf).height(150.rdp),
                )
                RemoteComponentInspectorBox(
                    label = "Row Item 2",
                    backgroundColor = Color(0xFFFFF3E0).rc,
                    accentColor = Color(0xFFEF6C00).rc,
                    modifier = RemoteModifier.weight(1f.rf).height(150.rdp),
                )
            }

            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxWidth().background(Color(0xFFEDE7F6).rc).padding(24.rdp)
            ) {
                RemoteComponentInspectorBox(
                    label = "Nested Padded Box",
                    backgroundColor = Color(0xFFD1C4E9).rc,
                    accentColor = Color(0xFF4527A0).rc,
                    modifier = RemoteModifier.fillMaxWidth().height(150.rdp),
                )
            }

            repeat(4) { index ->
                RemoteComponentInspectorBox(
                    label = "Scroll Card #${index + 1}",
                    backgroundColor = Color(0xFFFCE4EC).rc,
                    accentColor = Color(0xFFC2185B).rc,
                    modifier = RemoteModifier.fillMaxWidth().height(150.rdp),
                )
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
private fun RemoteComponentInspectorBox(
    label: String,
    backgroundColor: RemoteColor,
    accentColor: RemoteColor,
    modifier: RemoteModifier = RemoteModifier,
) {
    RemoteBox(
        modifier =
            modifier.background(backgroundColor).drawWithContent {
                val comp = remote.component
                val w = comp.width
                val h = comp.height
                val cx = comp.centerX
                val cy = comp.centerY
                val x = comp.x
                val y = comp.y
                val rootX = comp.rootX
                val rootY = comp.rootY

                val guidePaint = RemotePaint {
                    color = accentColor.copy(alpha = 0.35f.rf)
                    style = PaintingStyle.Stroke
                    strokeWidth = 2f.rf
                }
                val centerPaint = RemotePaint {
                    color = accentColor.copy(alpha = 0.6f.rf)
                    style = PaintingStyle.Fill
                }
                val textPaint = RemotePaint {
                    color = accentColor
                    textSize = 26f.rf
                }

                // Draw border using width & height
                drawRect(
                    paint = guidePaint,
                    topLeft = RemoteOffset(0f.rf, 0f.rf),
                    size = RemoteSize(w, h),
                )

                // Draw center crosshairs using centerX & centerY
                drawLine(
                    paint = guidePaint,
                    start = RemoteOffset(0f.rf, cy),
                    end = RemoteOffset(w, cy),
                )
                drawLine(
                    paint = guidePaint,
                    start = RemoteOffset(cx, 0f.rf),
                    end = RemoteOffset(cx, h),
                )
                drawCircle(
                    paint = centerPaint,
                    radius = 6f.rf,
                    center = RemoteOffset(cx, cy),
                )

                drawContent()

                val sizeText =
                    "w=".rs +
                        w.toRemoteInt().toRemoteString() +
                        " h=".rs +
                        h.toRemoteInt().toRemoteString() +
                        " cx=".rs +
                        cx.toRemoteInt().toRemoteString() +
                        " cy=".rs +
                        cy.toRemoteInt().toRemoteString()

                val posText =
                    "x=".rs +
                        x.toRemoteInt().toRemoteString() +
                        " y=".rs +
                        y.toRemoteInt().toRemoteString() +
                        " rootX=".rs +
                        rootX.toRemoteInt().toRemoteString() +
                        " rootY=".rs +
                        rootY.toRemoteInt().toRemoteString()

                drawAnchoredText(
                    text = sizeText,
                    anchorX = cx,
                    anchorY = cy - 20f.rf,
                    paint = textPaint,
                )
                drawAnchoredText(
                    text = posText,
                    anchorX = cx,
                    anchorY = cy + 28f.rf,
                    paint = textPaint,
                )
            },
        contentAlignment = RemoteAlignment.TopStart,
    ) {
        RemoteText(
            text = label.rs,
            modifier = RemoteModifier.padding(8.rdp),
            color = accentColor,
        )
    }
}

@Preview
@Composable
private fun RemoteComponentDemoPreview() {
    RemoteComponentDemo()
}
