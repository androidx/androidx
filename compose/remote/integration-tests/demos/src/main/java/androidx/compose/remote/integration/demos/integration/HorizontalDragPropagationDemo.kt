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
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

private enum class HostContainerMode(val label: String) {
    PAGER("HorizontalPager"),
    ROW("Horizontal Row"),
    VIEW("HorizontalScrollView"),
}

@Suppress("RestrictedApiAndroidX")
@Composable
fun HorizontalDragPropagationDemo() {
    var selectedModeIndex by remember { mutableIntStateOf(0) }
    val modes = HostContainerMode.entries

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)) {
        Text(
            text = "Horizontal Drag Propagation Demo",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Black,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Instructions & Expected Behavior:",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        "• Scenario: The host UI scrolls horizontally, while the Remote Compose Player has a vertically scrolling document.\n" +
                            "• Expected: Horizontal drag over Remote Compose Player should scroll the host container horizontally. Vertical drag should scroll the Remote Compose list vertically.\n" +
                            "• Test: Try dragging horizontally starting INSIDE the blue box vs starting OUTSIDE it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PrimaryTabRow(selectedTabIndex = selectedModeIndex) {
            modes.forEachIndexed { index, mode ->
                Tab(
                    selected = selectedModeIndex == index,
                    onClick = { selectedModeIndex = index },
                    text = { Text(mode.label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (modes[selectedModeIndex]) {
            HostContainerMode.PAGER -> HorizontalPagerHostDemo(modifier = Modifier.fillMaxSize())
            HostContainerMode.ROW -> HorizontalScrollRowHostDemo(modifier = Modifier.fillMaxSize())
            HostContainerMode.VIEW ->
                HorizontalScrollViewHostDemo(modifier = Modifier.fillMaxSize())
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
private fun HorizontalPagerHostDemo(modifier: Modifier = Modifier) {
    val pageCount = 3
    val pagerState = rememberPagerState { pageCount }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                enabled = pagerState.currentPage > 0,
            ) {
                Text("< Prev")
            }

            Text(
                text = "Page ${pagerState.currentPage + 1} of $pageCount",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage < pageCount - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                enabled = pagerState.currentPage < pageCount - 1,
            ) {
                Text("Next >")
            }
        }

        Text(
            text =
                if (pagerState.currentPage == 1) {
                    "Try swiping horizontally starting INSIDE the blue box to go to Page 1 or 3:"
                } else {
                    "Swipe horizontally to navigate to Page 2 (Remote Compose Player):"
                },
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
            when (page) {
                0 -> {
                    HostPageCard(
                        title = "Host Page 1 (Left)",
                        subtitle =
                            "Swipe left / drag to the left to reach Page 2 with the Remote Compose Player.",
                        containerColor = Color(0xFFFFF3E0),
                        borderColor = Color(0xFFFFB74D),
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    )
                }
                1 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Host Page 2: Remote Compose Player (Vertical Scroll)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF1976D2),
                            )
                            Text(
                                text =
                                    "Start horizontal drag INSIDE the blue box below to attempt paging:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .weight(1f)
                                        .border(2.dp, Color(0xFF1976D2), RoundedCornerShape(8.dp))
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                            ) {
                                RemoteDemo(modifier = Modifier.fillMaxSize()) {
                                    VerticalScrollDemoContent()
                                }
                            }
                        }
                    }
                }
                2 -> {
                    HostPageCard(
                        title = "Host Page 3 (Right)",
                        subtitle =
                            "Swipe right / drag to the right to return to Page 2 with the Remote Compose Player.",
                        containerColor = Color(0xFFE8F5E9),
                        borderColor = Color(0xFF81C784),
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    )
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX", "FrequentlyChangingValue")
@Composable
private fun HorizontalScrollRowHostDemo(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier) {
        Text(
            text = "Horizontal Scroll Offset: ${scrollState.value} / ${scrollState.maxValue} px",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Text(
            text = "Drag horizontally on Card 2 (blue border) to attempt scrolling the host Row:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HostPageCard(
                title = "Host Card 1 (Left)",
                subtitle = "Scroll right to see Card 2 with Remote Compose Player.",
                containerColor = Color(0xFFFFF3E0),
                borderColor = Color(0xFFFFB74D),
                modifier = Modifier.width(300.dp).fillMaxHeight(),
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.width(320.dp).fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Card 2: Remote Compose Player",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF1976D2),
                    )
                    Text(
                        text = "Drag horizontally starting inside the blue box:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .weight(1f)
                                .border(2.dp, Color(0xFF1976D2), RoundedCornerShape(8.dp))
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                    ) {
                        RemoteDemo(modifier = Modifier.fillMaxSize()) {
                            VerticalScrollDemoContent()
                        }
                    }
                }
            }

            HostPageCard(
                title = "Host Card 3 (Right)",
                subtitle = "Scroll left to return to Card 2.",
                containerColor = Color(0xFFE8F5E9),
                borderColor = Color(0xFF81C784),
                modifier = Modifier.width(300.dp).fillMaxHeight(),
            )
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
private fun HorizontalScrollViewHostDemo(modifier: Modifier = Modifier) {
    var documentState by remember { mutableStateOf<RemoteDocument?>(null) }
    val context = LocalContext.current
    val creationDisplayInfo = createCreationDisplayInfo()

    LaunchedEffect(Unit) {
        val captured =
            captureSingleRemoteDocument(
                creationDisplayInfo = creationDisplayInfo,
                context = context,
                content = { VerticalScrollDemoContent() },
            )
        documentState = RemoteDocument(captured.bytes)
    }

    Column(modifier = modifier) {
        Text(
            text = "Native Android View: HorizontalScrollView hosting RemoteComposePlayer",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Text(
            text =
                "Drag horizontally starting on the center player to attempt scrolling the HorizontalScrollView:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        if (documentState != null) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { ctx ->
                    val density = ctx.resources.displayMetrics.density
                    fun dp(dpValue: Int): Int = (dpValue * density).toInt()

                    val scrollView =
                        HorizontalScrollView(ctx).apply {
                            layoutParams =
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                        }

                    val linearLayout =
                        LinearLayout(ctx).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams =
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                        }

                    fun createCardView(
                        title: String,
                        subtitle: String,
                        bgColor: Int,
                    ): LinearLayout {
                        return LinearLayout(ctx).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams =
                                LinearLayout.LayoutParams(
                                        dp(300),
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                                    .apply { setMargins(dp(8), dp(8), dp(8), dp(8)) }
                            setPadding(dp(16), dp(16), dp(16), dp(16))
                            background =
                                GradientDrawable().apply {
                                    setColor(bgColor)
                                    cornerRadius = dp(8).toFloat()
                                }

                            val tvTitle =
                                TextView(ctx).apply {
                                    text = title
                                    textSize = 18f
                                    setTextColor(AndroidColor.BLACK)
                                }
                            val tvSub =
                                TextView(ctx).apply {
                                    text = subtitle
                                    textSize = 14f
                                    setTextColor(AndroidColor.DKGRAY)
                                    setPadding(0, dp(4), 0, 0)
                                }
                            val tvHint =
                                TextView(ctx).apply {
                                    text =
                                        "Notice: In standard Android View ScrollView, dragging vertically scrolls the list, and dragging horizontally scrolls the parent HorizontalScrollView."
                                    textSize = 12f
                                    setTextColor(0xFF2E7D32.toInt())
                                    setPadding(0, dp(4), 0, dp(8))
                                }
                            addView(tvTitle)
                            addView(tvSub)
                            addView(tvHint)

                            val verticalScrollView =
                                ScrollView(ctx).apply {
                                    layoutParams =
                                        LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            0,
                                            1f,
                                        )
                                }

                            val scrollContent =
                                LinearLayout(ctx).apply {
                                    orientation = LinearLayout.VERTICAL
                                    layoutParams =
                                        ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                        )
                                }

                            for (i in 1..20) {
                                val itemView =
                                    TextView(ctx).apply {
                                        text = "View Item #$i (Vertical Scroll)"
                                        textSize = 14f
                                        setTextColor(AndroidColor.BLACK)
                                        setPadding(dp(12), dp(12), dp(12), dp(12))
                                        background =
                                            GradientDrawable().apply {
                                                setColor(AndroidColor.WHITE)
                                                cornerRadius = dp(4).toFloat()
                                            }
                                        layoutParams =
                                            LinearLayout.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                                )
                                                .apply { setMargins(0, dp(4), 0, dp(4)) }
                                    }
                                scrollContent.addView(itemView)
                            }

                            verticalScrollView.addView(scrollContent)
                            addView(verticalScrollView)
                        }
                    }

                    val leftCard =
                        createCardView(
                            title = "View Panel 1 (Left)",
                            subtitle = "Scroll right to reach the RemoteComposePlayer panel.",
                            bgColor = 0xFFFFF3E0.toInt(),
                        )
                    linearLayout.addView(leftCard)

                    val playerContainer =
                        LinearLayout(ctx).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams =
                                LinearLayout.LayoutParams(
                                        dp(320),
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                                    .apply { setMargins(dp(8), dp(8), dp(8), dp(8)) }
                            setPadding(dp(8), dp(8), dp(8), dp(8))
                            background =
                                GradientDrawable().apply {
                                    setColor(AndroidColor.WHITE)
                                    cornerRadius = dp(8).toFloat()
                                    setStroke(dp(2), 0xFF1976D2.toInt())
                                }

                            val tvTitle =
                                TextView(ctx).apply {
                                    text = "Panel 2: RemoteComposePlayer"
                                    textSize = 16f
                                    setTextColor(0xFF1976D2.toInt())
                                    gravity = Gravity.CENTER_HORIZONTAL
                                }
                            val tvSub =
                                TextView(ctx).apply {
                                    text = "Drag horizontally starting over player:"
                                    textSize = 12f
                                    setTextColor(AndroidColor.RED)
                                    gravity = Gravity.CENTER_HORIZONTAL
                                }
                            addView(tvTitle)
                            addView(tvSub)

                            val playerFrame =
                                FrameLayout(ctx).apply {
                                    layoutParams =
                                        LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                0,
                                                1f,
                                            )
                                            .apply { setMargins(0, dp(4), 0, 0) }
                                }

                            val player =
                                RemoteComposePlayer(ctx).apply {
                                    layoutParams =
                                        FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                        )
                                    tag = "remote_compose_player"
                                }
                            playerFrame.addView(player)
                            addView(playerFrame)
                        }
                    linearLayout.addView(playerContainer)

                    val rightCard =
                        createCardView(
                            title = "View Panel 3 (Right)",
                            subtitle = "Scroll left to return to the RemoteComposePlayer panel.",
                            bgColor = 0xFFE8F5E9.toInt(),
                        )
                    linearLayout.addView(rightCard)

                    scrollView.addView(linearLayout)
                    scrollView
                },
                update = { scrollView ->
                    val player =
                        scrollView.findViewWithTag<RemoteComposePlayer>("remote_compose_player")
                    documentState?.let { doc -> player?.setDocument(doc) }
                },
            )
        }
    }
}

@Composable
private fun HostPageCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text =
                    "Notice: In this standard Compose column, dragging vertically scrolls the list, and dragging horizontally correctly scrolls/pages the host.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32),
            )
            Spacer(modifier = Modifier.height(12.dp))
            for (i in 1..20) {
                Card(
                    colors =
                        CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Text(
                        text = "Host Item #$i (Vertical Scroll)",
                        modifier = Modifier.padding(12.dp),
                        color = Color.Black,
                    )
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
@RemoteComposable
private fun VerticalScrollDemoContent() {
    val scrollState = rememberRemoteScrollState()
    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize().verticalScroll(scrollState).padding(8.rdp)
    ) {
        RemoteText(text = "--- Remote Compose Scrollable Start ---".rs, color = Color.Blue.rc)
        for (i in 1..25) {
            val itemColor = if (i % 2 == 0) Color(0xFFE3F2FD) else Color(0xFFBBDEFB)
            RemoteBox(
                modifier =
                    RemoteModifier.fillMaxWidth()
                        .height(44.rdp)
                        .padding(vertical = 4.rdp)
                        .background(itemColor.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(text = "RC Scroll Item #$i (Vertical)".rs, color = Color.Black.rc)
            }
        }
        RemoteText(text = "--- Remote Compose Scrollable End ---".rs, color = Color.Blue.rc)
    }
}
