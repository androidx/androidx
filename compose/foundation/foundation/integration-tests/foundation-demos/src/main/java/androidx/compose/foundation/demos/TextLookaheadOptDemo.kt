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

package androidx.compose.foundation.demos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TextLookaheadOptDemo() {
    var isExpanded by remember { mutableStateOf(false) }
    var autoPlay by remember { mutableStateOf(true) }
    val selectionState = rememberSelectionState()

    LaunchedEffect(Unit) {
        delay(500)
        selectionState.selectAll()
    }

    LaunchedEffect(autoPlay) {
        if (autoPlay) {
            while (true) {
                delay(3000)
                isExpanded = !isExpanded
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "spinner_trans")
    val rotation by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "spinner_rot",
        )

    val gradientBrush =
        Brush.sweepGradient(
            listOf(Color(0xFFFF007F), Color(0xFF00FFFF), Color(0xFF7F00FF), Color(0xFFFF007F))
        )

    // Inline content replacing the 'o' inside "L[o]ading..."
    val inlineContent =
        mapOf(
            "gradient_o" to
                InlineTextContent(
                    Placeholder(
                        width = 16.sp,
                        height = 16.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    )
                ) {
                    Canvas(modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = rotation }) {
                        drawArc(
                            brush = gradientBrush,
                            startAngle = 0f,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
        )

    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xFF0C0C14))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                        .clickable { autoPlay = !autoPlay }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicText(
                    text = if (autoPlay) "Autoplay: ON" else "Autoplay: OFF",
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF00FFFF),
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }

            Box(
                modifier =
                    Modifier.background(Color(0xFF2A2A3C), RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicText(
                    text = if (isExpanded) "Shrink (2.0s)" else "Expand (2.0s)",
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SharedTransitionLayout {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Button starts on left when small, expands to center
                MorphingTextSharedElement(
                    title = "Centered Text Morph (Lookahead)",
                    isExpanded = isExpanded,
                    startAlign = TextAlign.Center,
                    endAlign = TextAlign.Center,
                    startAlignment = Alignment.CenterStart,
                    endAlignment = Alignment.Center,
                    startColor = Color(0xFF00FFFF),
                    endColor = Color(0xFFFF007F),
                    key = "centered_text_morph",
                    inlineContent = inlineContent,
                    selectionState = selectionState,
                    onClick = { isExpanded = !isExpanded },
                )
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.MorphingTextSharedElement(
    title: String,
    isExpanded: Boolean,
    startAlign: TextAlign,
    endAlign: TextAlign,
    startAlignment: Alignment,
    endAlignment: Alignment,
    startColor: Color,
    endColor: Color,
    key: String,
    inlineContent: Map<String, InlineTextContent>,
    selectionState: androidx.compose.foundation.text.selection.SelectionState,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        BasicText(
            text = title,
            style =
                TextStyle(fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 6.dp),
        )

        SelectionContainer(state = selectionState) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                label = key,
            ) { expanded ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (expanded) endAlignment else startAlignment,
                ) {
                    BasicText(
                        text =
                            buildAnnotatedString {
                                append("L")
                                appendInlineContent("gradient_o", "o")
                                append("ading...")
                            },
                        style =
                            TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = if (expanded) endAlign else startAlign,
                            ),
                        inlineContent = inlineContent,
                        modifier =
                            Modifier.sharedElement(
                                    rememberSharedContentState(key = key),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = BoundsTransform { _, _ -> tween(2000) },
                                )
                                .then(
                                    if (expanded) Modifier.fillMaxWidth()
                                    else Modifier.width(160.dp)
                                )
                                .border(
                                    2.dp,
                                    if (expanded) endColor else startColor,
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(12.dp),
                    )
                }
            }
        }
    }
}
