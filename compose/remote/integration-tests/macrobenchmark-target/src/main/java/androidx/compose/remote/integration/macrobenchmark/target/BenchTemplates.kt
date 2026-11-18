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

package androidx.compose.remote.integration.macrobenchmark.target

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteFlowRow
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.layout.RemoteTime
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BenchTemplate(val key: String) {
    AppCard("appCard"),
    Headline("headline"),
    Poll("poll"),
    Live("live"),
    Stateful("stateful"),
}

internal val FEED_CARD_HEIGHT = 112.dp
internal val FEED_CARD_HEIGHT_RDP = 112.rdp

internal suspend fun loadFeedTemplates(
    context: Context,
    templates: List<BenchTemplate>,
    globalScope: Boolean = false,
): Map<BenchTemplate, ByteArray> {
    val result = LinkedHashMap<BenchTemplate, ByteArray>()
    for (template in templates.distinct()) {
        result[template] =
            BenchDocuments.getOrCapture(
                context = context,
                name = "feed_${template.key}",
                globalScope = globalScope,
            ) {
                RemoteTemplateContent(template, globalScope = globalScope)
            }
    }
    return result
}

@RemoteComposable
@Composable
private fun RemoteTemplateContent(template: BenchTemplate, globalScope: Boolean) {
    when (template) {
        BenchTemplate.AppCard -> RemoteAppCard()
        BenchTemplate.Headline -> RemoteHeadlineCard()
        BenchTemplate.Poll -> RemotePollCard(liveBadge = false, globalScope = globalScope)
        BenchTemplate.Live -> RemotePollCard(liveBadge = true, globalScope = globalScope)
        BenchTemplate.Stateful -> RemoteStatefulCard(globalScope = globalScope)
    }
}

@RemoteComposable
@Composable
private fun RemoteAppCard() {
    RemoteRow(
        modifier =
            RemoteModifier.fillMaxWidth()
                .height(FEED_CARD_HEIGHT_RDP)
                .padding(horizontal = 12.rdp, vertical = 6.rdp)
                .clip(RemoteRoundedCornerShape(12.rdp))
                .background(Color(0xFFF3F4F6).rc)
                .padding(12.rdp)
                .semantics { contentDescription = "card_appCard".rs },
        verticalAlignment = RemoteAlignment.CenterVertically,
    ) {
        RemoteBox(
            modifier =
                RemoteModifier.size(56.rdp)
                    .clip(RemoteRoundedCornerShape(10.rdp))
                    .background(Color(0xFF1E88E5).rc)
        )
        RemoteColumn(
            modifier = RemoteModifier.fillMaxWidth().padding(start = 12.rdp),
            verticalArrangement = RemoteArrangement.Center,
        ) {
            RemoteText(
                text = "Remote Compose Card".rs,
                fontSize = 16.rsp,
                color = Color(0xFF111827).rc,
            )
            RemoteText(
                text = "Template-driven feed item with rounded clip".rs,
                fontSize = 13.rsp,
                color = Color(0xFF4B5563).rc,
                modifier = RemoteModifier.padding(top = 4.rdp),
            )
        }
    }
}

@RemoteComposable
@Composable
private fun RemoteHeadlineCard() {
    RemoteColumn(
        modifier =
            RemoteModifier.fillMaxWidth()
                .height(FEED_CARD_HEIGHT_RDP)
                .padding(horizontal = 12.rdp, vertical = 6.rdp)
                .clip(RemoteRoundedCornerShape(12.rdp))
                .background(Color(0xFFFFFFFF).rc)
                .padding(12.rdp)
                .semantics { contentDescription = "card_headline".rs },
        verticalArrangement = RemoteArrangement.SpaceBetween,
    ) {
        RemoteText(
            text = "TOP STORY".rs,
            fontSize = 11.rsp,
            color = Color(0xFFD32F2F).rc,
        )
        RemoteText(
            text = "AndroidX Macrobenchmarks for Remote Feeds".rs,
            fontSize = 16.rsp,
            color = Color(0xFF111827).rc,
        )
        RemoteFlowRow(
            modifier = RemoteModifier.fillMaxWidth(),
            horizontalArrangement = RemoteArrangement.spacedBy(6.rdp),
        ) {
            listOf("Compose", "Remote", "Perf", "Feed").forEach { tag ->
                RemoteBox(
                    modifier =
                        RemoteModifier.clip(RemoteRoundedCornerShape(6.rdp))
                            .background(Color(0xFFE5E7EB).rc)
                            .padding(horizontal = 8.rdp, vertical = 2.rdp)
                ) {
                    RemoteText(text = tag.rs, fontSize = 11.rsp, color = Color(0xFF374151).rc)
                }
            }
        }
    }
}

@RemoteComposable
@Composable
private fun RemotePollCard(liveBadge: Boolean, globalScope: Boolean) {
    val share0 = remember { createNamedRemoteFloat("share0", 120f) }.withGlobalScopeIf(globalScope)
    val share1 = remember { createNamedRemoteFloat("share1", 80f) }.withGlobalScopeIf(globalScope)
    val share2 = remember { createNamedRemoteFloat("share2", 50f) }.withGlobalScopeIf(globalScope)

    RemoteColumn(
        modifier =
            RemoteModifier.fillMaxWidth()
                .height(FEED_CARD_HEIGHT_RDP)
                .padding(horizontal = 12.rdp, vertical = 6.rdp)
                .clip(RemoteRoundedCornerShape(12.rdp))
                .background(Color(0xFFF9FAFB).rc)
                .padding(12.rdp)
                .semantics {
                    contentDescription = (if (liveBadge) "card_live" else "card_poll").rs
                },
        verticalArrangement = RemoteArrangement.SpaceBetween,
    ) {
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth(),
            horizontalArrangement = RemoteArrangement.SpaceBetween,
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteText(
                text = (if (liveBadge) "Live Poll" else "Quick Poll").rs,
                fontSize = 14.rsp,
                color = Color(0xFF111827).rc,
            )
            if (liveBadge) {
                val pulse = (RemoteTime().ContinuousSec() * 2f.rf) % 2f.rf
                val badgeColor = if (liveBadge) Color(0xFFDC2626).rc else Color(0xFF6B7280).rc
                RemoteBox(
                    modifier =
                        RemoteModifier.width(120f.rf + pulse * 10f.rf)
                            .height(18.rdp)
                            .clip(RemoteRoundedCornerShape(9.rdp))
                            .background(badgeColor),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    RemoteText(text = "LIVE".rs, fontSize = 10.rsp, color = Color.White.rc)
                }
            }
        }
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth().height(12.rdp),
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.width(share0).fillMaxHeight().background(Color(0xFF2563EB).rc)
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(share1).fillMaxHeight().background(Color(0xFF10B981).rc)
            )
            RemoteBox(
                modifier =
                    RemoteModifier.width(share2).fillMaxHeight().background(Color(0xFFF59E0B).rc)
            )
        }
        RemoteText(
            text = "Updated from host state".rs,
            fontSize = 11.rsp,
            color = Color(0xFF6B7280).rc,
        )
    }
}

@RemoteComposable
@Composable
private fun RemoteStatefulCard(globalScope: Boolean) {
    val stateIndex = remember { createNamedRemoteInt("state", 0) }.withGlobalScopeIf(globalScope)
    RemoteBox(
        modifier =
            RemoteModifier.fillMaxWidth()
                .height(FEED_CARD_HEIGHT_RDP)
                .padding(horizontal = 12.rdp, vertical = 6.rdp)
                .clip(RemoteRoundedCornerShape(12.rdp))
                .background(Color(0xFFEEF2FF).rc)
                .padding(12.rdp)
                .semantics { contentDescription = "card_stateful".rs }
    ) {
        RemoteStateLayout(
            currentState = stateIndex,
            0,
            1,
            2,
            modifier = RemoteModifier.fillMaxSize(),
        ) { state ->
            when (state) {
                0 ->
                    RemoteRow(
                        modifier = RemoteModifier.fillMaxSize(),
                        verticalAlignment = RemoteAlignment.CenterVertically,
                    ) {
                        RemoteBox(
                            modifier = RemoteModifier.size(36.rdp).background(Color(0xFF4F46E5).rc)
                        )
                        RemoteText(
                            text = "State 0: Idle".rs,
                            fontSize = 15.rsp,
                            modifier = RemoteModifier.padding(start = 12.rdp),
                        )
                    }
                1 ->
                    RemoteRow(
                        modifier = RemoteModifier.fillMaxSize(),
                        verticalAlignment = RemoteAlignment.CenterVertically,
                    ) {
                        RemoteBox(
                            modifier = RemoteModifier.size(36.rdp).background(Color(0xFF059669).rc)
                        )
                        RemoteText(
                            text = "State 1: Active".rs,
                            fontSize = 15.rsp,
                            modifier = RemoteModifier.padding(start = 12.rdp),
                        )
                    }
                else ->
                    RemoteRow(
                        modifier = RemoteModifier.fillMaxSize(),
                        verticalAlignment = RemoteAlignment.CenterVertically,
                    ) {
                        RemoteBox(
                            modifier = RemoteModifier.size(36.rdp).background(Color(0xFFD97706).rc)
                        )
                        RemoteText(
                            text = "State 2: Complete".rs,
                            fontSize = 15.rsp,
                            modifier = RemoteModifier.padding(start = 12.rdp),
                        )
                    }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ComposeTwinCard(
    template: BenchTemplate,
    share0: Float,
    share1: Float,
    share2: Float,
    stateIndex: Int,
) {
    when (template) {
        BenchTemplate.AppCard -> {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(FEED_CARD_HEIGHT)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(12.dp)
                        .semantics { contentDescription = "card_appCard" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier.size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E88E5))
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = "Remote Compose Card", fontSize = 16.sp, color = Color(0xFF111827))
                    Text(
                        text = "Template-driven feed item with rounded clip",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        BenchTemplate.Headline -> {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(FEED_CARD_HEIGHT)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFFFFF))
                        .padding(12.dp)
                        .semantics { contentDescription = "card_headline" },
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "TOP STORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F),
                )
                Text(
                    text = "AndroidX Macrobenchmarks for Remote Feeds",
                    fontSize = 16.sp,
                    color = Color(0xFF111827),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("Compose", "Remote", "Perf", "Feed").forEach { tag ->
                        Box(
                            modifier =
                                Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE5E7EB))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = tag, fontSize = 11.sp, color = Color(0xFF374151))
                        }
                    }
                }
            }
        }
        BenchTemplate.Poll,
        BenchTemplate.Live -> {
            val isLive = template == BenchTemplate.Live
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(FEED_CARD_HEIGHT)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9FAFB))
                        .padding(12.dp)
                        .semantics {
                            contentDescription = if (isLive) "card_live" else "card_poll"
                        },
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isLive) "Live Poll" else "Quick Poll",
                        fontSize = 14.sp,
                        color = Color(0xFF111827),
                    )
                    if (isLive) {
                        val transition = rememberInfiniteTransition(label = "liveBadge")
                        val pulse by
                            transition.animateFloat(
                                initialValue = 0f,
                                targetValue = 2f,
                                animationSpec =
                                    infiniteRepeatable(
                                        animation =
                                            tween(durationMillis = 1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart,
                                    ),
                                label = "liveBadgeWidth",
                            )
                        Box(
                            modifier =
                                Modifier.width(44.dp + (pulse * 4f).dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color(0xFFDC2626)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "LIVE", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier.width(share0.dp).fillMaxHeight().background(Color(0xFF2563EB))
                    )
                    Box(
                        modifier =
                            Modifier.width(share1.dp).fillMaxHeight().background(Color(0xFF10B981))
                    )
                    Box(
                        modifier =
                            Modifier.width(share2.dp).fillMaxHeight().background(Color(0xFFF59E0B))
                    )
                }
                Text(text = "Updated from host state", fontSize = 11.sp, color = Color(0xFF6B7280))
            }
        }
        BenchTemplate.Stateful -> {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(FEED_CARD_HEIGHT)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF))
                        .padding(12.dp)
                        .semantics { contentDescription = "card_stateful" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (color, label) =
                    when (stateIndex % 3) {
                        0 -> Color(0xFF4F46E5) to "State 0: Idle"
                        1 -> Color(0xFF059669) to "State 1: Active"
                        else -> Color(0xFFD97706) to "State 2: Complete"
                    }
                Box(modifier = Modifier.size(36.dp).background(color))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = label, fontSize = 15.sp)
            }
        }
    }
}
