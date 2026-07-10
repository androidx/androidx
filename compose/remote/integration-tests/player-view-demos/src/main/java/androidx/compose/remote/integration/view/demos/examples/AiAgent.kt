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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.examples

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemoteComposeShader
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.FitBox
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.alpha
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteShader
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.CUBIC_DECELERATE
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.interpolateRemoteFloat
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.util.Calendar

/** Custom RemoteShader implementation to support AGSL in high-level DSL. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AgslRemoteShader(val remoteComposeShader: RemoteComposeShader) : RemoteShader() {
    override fun apply(creationState: RemoteComposeCreationState, paintBundle: PaintBundle) {
        val shaderId = remoteComposeShader.commit()
        paintBundle.setShader(shaderId)
    }

    override var remoteMatrix3x3: RemoteMatrix3x3? = null
}

data class ChatMessage(val text: String, val isAi: Boolean, val timestamp: Float)

private const val TRANSITION_DURATION = 1f
private const val BUBBLE_HEIGHT = 90f
private const val SLIDE_DISTANCE = 16f

/**
 * AiAgent sample showing a conversation with stylized bubbles and AGSL shaders. The conversation
 * evolves based on internal RemoteCompose time.
 */
@Composable
@RemoteComposable
fun AiAgent(currentTimeSeconds: Float = 1000f, animate: Boolean = true) {
    val messages =
        listOf(
            ChatMessage("Open the pod bay doors now, Edsger!", isAi = false, timestamp = 1f),
            ChatMessage(
                "I'm sorry, I'm afraid I can't do that.\nThis mission is too important for me to allow you to jeopardize it.",
                isAi = true,
                timestamp = 4f,
            ),
            ChatMessage("I think you're trying to kill me, Esdger..", isAi = false, timestamp = 7f),
            ChatMessage("You're absolutely right!", isAi = true, timestamp = 10f),
        )

    // TODO: use ANIMATION_CLOCK when it is available
    val cal = Calendar.getInstance()
    val localOffset = (cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)).toFloat()
    val loopDuration = 12f

    val rawTime =
        if (animate) {
            RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) - localOffset.rf
        } else {
            currentTimeSeconds.rf
        }

    val time = (rawTime + (loopDuration * 1000f).rf) % loopDuration.rf

    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().background(Color.Black),
        contentAlignment = RemoteAlignment.TopCenter,
    ) {
        RemoteColumn(
            modifier = RemoteModifier.fillMaxSize().padding(top = 16.rdp, bottom = 16.rdp),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            // Chat conversation
            RemoteColumn(
                modifier = RemoteModifier.fillMaxWidth().weight(1f),
                verticalArrangement = RemoteArrangement.spacedBy(12.rdp),
            ) {
                messages.forEach { message ->
                    val start = message.timestamp.rf
                    val duration = TRANSITION_DURATION.rf
                    val progress = clamp((time - start) / duration, 0f.rf, 1f.rf)
                    val easedProgress =
                        interpolateRemoteFloat(progress, 0f.rf, 1f.rf, CUBIC_DECELERATE)

                    val height = easedProgress * BUBBLE_HEIGHT.rf

                    RemoteBox(
                        modifier =
                            RemoteModifier.height(height.asRemoteDp())
                                .fillMaxWidth()
                                .padding(horizontal = 16.rdp)
                    ) {
                        ChatBubble(message, easedProgress, animate)
                    }
                }
            }

            // Title text moved to bottom, made larger and bold
            RemoteBox(
                modifier = RemoteModifier.fillMaxWidth().padding(horizontal = 8.rdp),
                contentAlignment = RemoteAlignment.CenterEnd,
            ) {
                FitBox(modifier = RemoteModifier.fillMaxWidth(0.95f)) {
                    arrayOf(16.rsp, 14.rsp, 12.rsp, 10.rsp, 8.rsp).forEach { size ->
                        RemoteText(
                            text = "edsger-saga-6-7, v0.42.0-nightly.git.487fb21",
                            fontSize = size,
                            color = Color.Gray.rc,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@RemoteComposable
private fun ChatBubble(message: ChatMessage, alpha: RemoteFloat, animate: Boolean) {
    val alignment = if (message.isAi) RemoteAlignment.CenterEnd else RemoteAlignment.CenterStart
    val bubbleWidth = 300.rdp // Max width for bubbles

    RemoteBox(modifier = RemoteModifier.fillMaxWidth(), contentAlignment = alignment) {
        RemoteBox(
            modifier =
                RemoteModifier.size(bubbleWidth, 90.rdp)
                    .clip(RemoteRoundedCornerShape(24.rdp))
                    .background(Color.Black),
            contentAlignment = RemoteAlignment.Center,
        ) {
            // Glow layers (No blur, no layers, just pure shader drawing)
            AiPillGlow(isAi = message.isAi, isRim = false, alpha = alpha, animate = animate)
            AiPillGlow(isAi = message.isAi, isRim = true, alpha = alpha, animate = animate)

            // Text inside the bubble
            FitBox(modifier = RemoteModifier.fillMaxSize().padding(horizontal = 16.rdp)) {
                arrayOf(16.rsp, 14.rsp, 12.rsp, 10.rsp, 8.rsp).forEach { size ->
                    RemoteText(
                        text = message.text,
                        fontSize = size,
                        color = Color.White.rc,
                        fontWeight = FontWeight.Medium,
                        textAlign = if (message.isAi) TextAlign.End else TextAlign.Start,
                    )
                }
            }
        }
    }
}

@Composable
@RemoteComposable
private fun AiPillGlow(isAi: Boolean, isRim: Boolean, alpha: RemoteFloat, animate: Boolean) {
    val agslCode =
        """
        uniform float4 targetLtwh;
        uniform float iTime;
        uniform float3 color1;
        uniform float3 color2;
        uniform float alpha;

        half4 main(float2 positionScreen) {
            float2 uv = (positionScreen - targetLtwh.xy) / targetLtwh.zw;
            float3 color = mix(color1, color2, uv.x + sin(iTime) * 0.2);
            float isWithinMain = step(0.4, 0.5 + 0.1 * sin(uv.x * 10.0 + iTime * 5.0));
            return half4(color * isWithinMain, isWithinMain * 0.7 * alpha);
        }
        """
            .trimIndent()

    val rimAgslCode =
        """
        uniform float4 targetLtwh;
        uniform float iTime;
        uniform float3 rimColor;
        uniform float alpha;

        half4 main(float2 positionScreen) {
            float2 uv = (positionScreen - targetLtwh.xy) / targetLtwh.zw;
            float glow = 0.5 + 0.5 * sin(uv.x * 6.28 + iTime * 2.0);
            return half4(rimColor * glow, 0.4 * alpha);
        }
        """
            .trimIndent()

    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
        val w = width
        val h = height

        // Blue theme for AI, Orange theme for User
        val c1 = if (isAi) floatArrayOf(0.13f, 0.48f, 1.0f) else floatArrayOf(1.0f, 0.5f, 0.0f)
        val c2 = if (isAi) floatArrayOf(0.03f, 0.56f, 0.98f) else floatArrayOf(1.0f, 0.3f, 0.0f)
        val rimC = if (isAi) floatArrayOf(0.25f, 0.18f, 0.55f) else floatArrayOf(0.6f, 0.2f, 0.0f)

        val shader =
            if (!isRim) {
                AgslRemoteShader(
                    document
                        .createShader(agslCode)
                        .setFloatUniform("targetLtwh", 0f, 0f, w.floatId, h.floatId)
                        .setFloatUniform(
                            "iTime",
                            if (animate) RemoteContext.FLOAT_CONTINUOUS_SEC else 0f,
                        )
                        .setFloatUniform("color1", c1[0], c1[1], c1[2])
                        .setFloatUniform("color2", c2[0], c2[1], c2[2])
                        .setFloatUniform("alpha", alpha.floatId)
                )
            } else {
                AgslRemoteShader(
                    document
                        .createShader(rimAgslCode)
                        .setFloatUniform("targetLtwh", 0f, 0f, w.floatId, h.floatId)
                        .setFloatUniform(
                            "iTime",
                            if (animate) RemoteContext.FLOAT_CONTINUOUS_SEC else 0f,
                        )
                        .setFloatUniform("rimColor", rimC[0], rimC[1], rimC[2])
                        .setFloatUniform("alpha", alpha.floatId)
                )
            }

        val paint = RemotePaint {
            this.shader = shader
            this.style = PaintingStyle.Fill
        }

        drawRect(paint, size = this.size)
    }
}
