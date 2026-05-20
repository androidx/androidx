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

package androidx.compose.remote.integration.view.demos.dsl

import android.graphics.Color
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocumentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun DemoAnchorTextPreview() {
    RemoteDocumentPreview(RemoteDocument(demoAnchorText()))
}

@Suppress("RestrictedApiAndroidX")
fun demoAnchorText(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Box(modifier = Modifier.fillMaxSize().background(Color.DKGRAY).padding(4.rdp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()
                val cx = w * 0.5f
                val v1 = cx - 20f
                val v2 = cx + 20f
                val l1 = h * 0.2f
                val l2 = h * 0.4f
                val l3 = h * 0.6f
                val l4 = h * 0.8f
                val l5 = h * 0.9f

                applyPaint { setColor(Color.WHITE) }
                drawRect(0f.rf, 0f.rf, w, h)

                applyPaint { setColor(Color.RED) }
                drawLine(0f.rf, l1, w, l1)
                drawLine(0f.rf, l2, w, l2)
                drawLine(0f.rf, l3, w, l3)
                drawLine(0f.rf, l4, w, l4)
                drawLine(v1, 0f.rf, v1, h)
                drawLine(v2, 0f.rf, v2, h)

                val dur = 10f
                val sec = continuousSeconds()
                val t = sec % (dur * 3f)

                // Simulating PINGPONG(v, max) as ((v % (2*max)) - max).abs()
                val vX = sec * 2f
                val pingpongX = ((vX % 4f) - 2f).abs()
                val animatX = pingpongX - 1f

                val vY = sec * 3f
                val pingpongY = ((vY % 4f) - 2f).abs()
                val animatY = pingpongY - 1f

                val flag1 = Rc.TextAnchorMask.MEASURE_EVERY_TIME
                val flag2 = Rc.TextAnchorMask.BASELINE_RELATIVE

                applyPaint {
                    setColor(Color.BLUE)
                    setTextSize(64f)
                }

                val strId = remoteText("flip plop")

                val t1 = remoteText("X Right top X")
                val textLeftTop = remoteText("X Left top X")
                val textRightCenter = remoteText("X Right center X")
                val textLeftCenter = remoteText("X Left center X")
                val textRightBottom = remoteText("X Right bottom X")
                val textLeftBottom = remoteText("X Left bottom X")
                val textRightBaseline = remoteText("X Right baseline X")
                val textLeftBaseline = remoteText("X Left baseline X")

                conditionalOperations(Rc.Condition.LT, t, dur.rf) {
                    drawTextAnchored(t1, v1, l1, 1f.rf, 1f.rf, 0)
                    drawTextAnchored(textLeftTop, v2, l1, (-1f).rf, 1f.rf, 0)
                    drawTextAnchored(textRightCenter, v1, l2, 1f.rf, 0f.rf, 0)
                    drawTextAnchored(textLeftCenter, v2, l2, (-1f).rf, 0f.rf, 0)
                    drawTextAnchored(textRightBottom, v1, l3, 1f.rf, (-1f).rf, 0)
                    drawTextAnchored(textLeftBottom, v2, l3, (-1f).rf, (-1f).rf, 0)
                    drawTextAnchored(textRightBaseline, v1, l4, 1f.rf, 0f.rf, flag2)
                    drawTextAnchored(textLeftBaseline, v2, l4, (-1f).rf, 0f.rf, flag2)
                }

                conditionalOperations(Rc.Condition.GT, t, dur.rf) {
                    drawTextAnchored(strId, v1, l1, 1f.rf, 1f.rf, 0)
                    drawTextAnchored(strId, v1, l2, 1f.rf, 0f.rf, 0)
                    drawTextAnchored(strId, v1, l3, 1f.rf, (-1f).rf, 0)
                    drawTextAnchored(strId, v1, l4, 1f.rf, 0f.rf, flag2)

                    conditionalOperations(Rc.Condition.GT, t, (dur * 2f).rf) {
                        applyPaint {
                            setColor(Color.BLUE)
                            setTextSize(128f)
                        }
                    }

                    drawTextAnchored(strId, v2, l1, (-1f).rf, 1f.rf, flag1)
                    drawTextAnchored(strId, v2, l2, (-1f).rf, 0f.rf, flag1)
                    drawTextAnchored(strId, v2, l3, (-1f).rf, (-1f).rf, flag1)
                    drawTextAnchored(strId, v2, l4, (-1f).rf, 0f.rf, flag2 or flag1)
                    drawTextAnchored(strId, v2, l5, animatX, animatY, flag1)
                }

                applyPaint { setColor(Color.BLACK) }
            }
        }
    }
}
