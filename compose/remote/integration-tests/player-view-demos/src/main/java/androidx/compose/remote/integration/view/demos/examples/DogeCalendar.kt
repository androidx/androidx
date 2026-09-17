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

@file:Suppress("RestrictedApiAndroidX") // Referring to remote-core

package androidx.compose.remote.integration.view.demos.examples

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteScrollState
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.integration.view.demos.R
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

/**
 * Small non-Doge category icon motif for each time-of-day calendar entry in the scrollable list.
 */
enum class DogeEntryCategory(val accent: Color, val badgeSurface: Color) {
    SUNRISE(Color(0xFFFFB74D), Color(0x33FFB74D)),
    BREAKFAST(Color(0xFFFFA726), Color(0x33FFA726)),
    MORNING(Color(0xFF4FC3F7), Color(0x334FC3F7)),
    NAP(Color(0xFFCE93D8), Color(0x33CE93D8)),
    LUNCH(Color(0xFF4DB6AC), Color(0x334DB6AC)),
    MID_AFTERNOON(Color(0xFFFFD54F), Color(0x33FFD54F)),
    WALK(Color(0xFF81C784), Color(0x3381C784)),
    DINNER(Color(0xFFFF8A65), Color(0x33FF8A65)),
    PLAY(Color(0xFF4DD0E1), Color(0x334DD0E1)),
    NIGHT(Color(0xFF7986CB), Color(0x337986CB)),
}

/** Preloaded calendar list entry with small category badge and expanded Doge illustration index. */
data class DogeListEntry(
    val time: String,
    val title: String,
    val task: String,
    val category: DogeEntryCategory,
    val dogeIconIndex: Int,
)

/** Preloaded static schedule of 20 unique Doge CEO & WFH calendar entries across the day. */
val DOGE_LIST_SCHEDULE =
    arrayOf(
        DogeListEntry(
            time = "7:00 AM",
            title = "Wake Up",
            task = "Wake up. Very yawn. Much executive readiness.",
            category = DogeEntryCategory.SUNRISE,
            dogeIconIndex = 0,
        ),
        DogeListEntry(
            time = "7:30 AM",
            title = "CEO Espresso",
            task = "Double oat cortado in home office. Very caffeinated.",
            category = DogeEntryCategory.BREAKFAST,
            dogeIconIndex = 11,
        ),
        DogeListEntry(
            time = "8:00 AM",
            title = "Avocado Toast",
            task = "Artisanal toast fuel before market open. Me hipster doge.",
            category = DogeEntryCategory.BREAKFAST,
            dogeIconIndex = 1,
        ),
        DogeListEntry(
            time = "8:45 AM",
            title = "Perimeter Audit",
            task = "Bark at mailman outside home office. Zero intrusions.",
            category = DogeEntryCategory.MORNING,
            dogeIconIndex = 2,
        ),
        DogeListEntry(
            time = "9:30 AM",
            title = "Much Wow Funds",
            task = "Much Wow Mutual Funds open: Comic Sans charts say SUCH PROFIT.",
            category = DogeEntryCategory.MORNING,
            dogeIconIndex = 12,
        ),
        DogeListEntry(
            time = "10:15 AM",
            title = "Executive Nap",
            task = "Strategic pillow meditation. Very alpha generation.",
            category = DogeEntryCategory.NAP,
            dogeIconIndex = 3,
        ),
        DogeListEntry(
            time = "11:00 AM",
            title = "Barkchain Sync",
            task = "Barkchain Technologies standup: Proof of Good Boy validated.",
            category = DogeEntryCategory.MORNING,
            dogeIconIndex = 13,
        ),
        DogeListEntry(
            time = "11:45 AM",
            title = "WFH Deep Work",
            task = "Suit tie on top, bunny slippers under desk. Much productivity.",
            category = DogeEntryCategory.MORNING,
            dogeIconIndex = 14,
        ),
        DogeListEntry(
            time = "12:30 PM",
            title = "Omakase Lunch",
            task = "8-seat executive salmon omakase. Expense report approved.",
            category = DogeEntryCategory.LUNCH,
            dogeIconIndex = 4,
        ),
        DogeListEntry(
            time = "1:30 PM",
            title = "Yard Recon",
            task = "Investigate suspicious squirrel activity near Wi-Fi router.",
            category = DogeEntryCategory.WALK,
            dogeIconIndex = 5,
        ),
        DogeListEntry(
            time = "2:15 PM",
            title = "Zoom Investors",
            task = "Investor call on Zoom: 'Very bull market, much synergy.'",
            category = DogeEntryCategory.MID_AFTERNOON,
            dogeIconIndex = 15,
        ),
        DogeListEntry(
            time = "3:15 PM",
            title = "Rug Recharge",
            task = "Horizontal brainstorming session on sunny home office rug.",
            category = DogeEntryCategory.NAP,
            dogeIconIndex = 6,
        ),
        DogeListEntry(
            time = "4:00 PM",
            title = "XR Glasses Demo",
            task = "Testing Barkchain spatial UI on XR glasses. Much pinch, wow.",
            category = DogeEntryCategory.MID_AFTERNOON,
            dogeIconIndex = 16,
        ),
        DogeListEntry(
            time = "5:00 PM",
            title = "Walking 1:1s",
            task = "Touch grass sync at park. Many sniff, very networking.",
            category = DogeEntryCategory.WALK,
            dogeIconIndex = 7,
        ),
        DogeListEntry(
            time = "6:00 PM",
            title = "Q4 Biscuit Audit",
            task = "Inspecting quarterly treat reserves with magnifying glass.",
            category = DogeEntryCategory.PLAY,
            dogeIconIndex = 17,
        ),
        DogeListEntry(
            time = "7:00 PM",
            title = "Founder Dinner",
            task = "3 Michelin stars with angel investors. Many good chef.",
            category = DogeEntryCategory.DINNER,
            dogeIconIndex = 8,
        ),
        DogeListEntry(
            time = "7:45 PM",
            title = "Series B Deck",
            task = "Rehearsing keynote slide: 'Barkchain To The Moon.'",
            category = DogeEntryCategory.DINNER,
            dogeIconIndex = 18,
        ),
        DogeListEntry(
            time = "8:30 PM",
            title = "Bone Decompress",
            task = "Post-market bone gnawing session. Stress level zero.",
            category = DogeEntryCategory.PLAY,
            dogeIconIndex = 9,
        ),
        DogeListEntry(
            time = "9:15 PM",
            title = "Couch Inbox Zero",
            task = "Wrapped in blanket on sofa clearing late investor emails.",
            category = DogeEntryCategory.NAP,
            dogeIconIndex = 19,
        ),
        DogeListEntry(
            time = "10:00 PM",
            title = "Sleep Mode",
            task = "Markets closed. Much sleep under stars. So quiet.",
            category = DogeEntryCategory.NIGHT,
            dogeIconIndex = 10,
        ),
    )

/**
 * Interactive scrollable Doge Calendar list-to-detail demo.
 *
 * Shows a full-screen vertically scrollable list of ~20 preloaded calendar entries with distinct
 * small category icons (never rendering the detailed Doge illustrations alongside the list).
 * Clicking any entry opens a floating Doge illustration and funny text pop-up card over the list;
 * clicking again closes the pop-up while the underlying list stays live and scrollable.
 */
@Composable
@RemoteComposable
fun DogeCalendar(initialSelectedIndex: Int = -1) {
    val dogeBitmaps = rememberDogeBitmaps()
    // 20 entries * 74.rdp + 4 * 74.rdp bottom spacer = 24 * 74.rdp total content height,
    // so notches = 24 snaps every scroll gesture onto an exact 74.rdp pill slot (0, 74, ..., 1406).
    val scrollState = rememberRemoteScrollState(notches = 24)
    val isExpanded = remember { MutableRemoteInt(if (initialSelectedIndex >= 0) 1 else 0) }
    val nextExpanded = isExpanded.isEqualTo(0.ri).select(1.ri, 0.ri)
    val toggleDetailAction = valueChange(isExpanded, nextExpanded)

    val scrimOffsetX = isExpanded.isEqualTo(1.ri).select(0f.rf, 2000f.rf).asRemoteDp()

    // Scrollable list stays visible underneath at all times; tapping toggles a floating gold-rimmed
    // pop-up card centered over the list, and scrolling updates both the list and open pop-up live.
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
        contentAlignment = RemoteAlignment.Center,
    ) {
        // Emit scrollable list first so scrollState.positionState is updated before detail slots
        RemoteRow(modifier = RemoteModifier.fillMaxSize()) {
            DogeFullScreenList(
                entries = DOGE_LIST_SCHEDULE,
                scrollState = scrollState,
                toggleDetailAction = toggleDetailAction,
                modifier = RemoteModifier.weight(1f).fillMaxHeight().padding(14.rdp),
            )
        }

        // Subtle translucent scrim so underlying scroll list & header remain clearly visible around
        // the floating pop-up card
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxSize()
                    .offset(x = scrimOffsetX, y = 0.rdp)
                    .background(Color(0x55000000).rc)
        )

        // Floating pop-up cards share the exact same topSlotDelta formula as DogeListItemRow
        DOGE_LIST_SCHEDULE.forEachIndexed { index, entry ->
            val topSlotDelta = (index * 74f).rf - scrollState.positionState
            val isHighlighted = (topSlotDelta * topSlotDelta).isLessThan((37f * 37f).rf)
            val detailOffsetX =
                isExpanded
                    .isEqualTo(1.ri)
                    .select(isHighlighted.select(0f.rf, 2000f.rf), 2000f.rf)
                    .asRemoteDp()

            DogeExpandedSlot(
                entry = entry,
                icon = dogeBitmaps[entry.dogeIconIndex],
                modifier =
                    RemoteModifier.size(width = 342.rdp, height = 376.rdp)
                        .offset(x = detailOffsetX, y = 26.rdp),
            )
        }
    }
}

@Composable
private fun rememberDogeBitmaps(): Array<ImageBitmap> {
    val res = LocalResources.current
    return remember {
        arrayOf(
                R.drawable.doge_yawning_on_bed,
                R.drawable.doge_eating_avocado_toast,
                R.drawable.doge_barking_at_mailman,
                R.drawable.doge_sleeping_on_pillow,
                R.drawable.doge_eating_sushi,
                R.drawable.doge_hunting_squirrel,
                R.drawable.doge_sleeping_on_rug,
                R.drawable.doge_walking_in_park,
                R.drawable.doge_eating_fancy_dinner,
                R.drawable.doge_chewing_bone,
                R.drawable.doge_sleeping_under_stars,
                R.drawable.doge_morning_espresso,
                R.drawable.doge_much_wow_mutual_funds,
                R.drawable.doge_barkchain_ceo,
                R.drawable.doge_home_office_laptop,
                R.drawable.doge_zoom_investor_call,
                R.drawable.doge_trying_xr_glasses,
                R.drawable.doge_quarterly_treat_audit,
                R.drawable.doge_keynote_pitch,
                R.drawable.doge_wfh_couch_emails,
            )
            .map { resId ->
                val decoded = BitmapFactory.decodeResource(res, resId, BitmapFactory.Options())
                val normalized =
                    if (decoded.width > 512) {
                        Bitmap.createScaledBitmap(decoded, 512, 512, true)
                    } else {
                        decoded
                    }
                normalized.asImageBitmap()
            }
            .toTypedArray()
    }
}

@Composable
@RemoteComposable
private fun DogeFullScreenList(
    entries: Array<DogeListEntry>,
    scrollState: RemoteScrollState,
    toggleDetailAction: androidx.compose.remote.creation.compose.action.Action,
    modifier: RemoteModifier = RemoteModifier,
) {
    RemoteColumn(
        modifier = modifier,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        // List header pill matching DogeCalendar aesthetic (tapping anywhere opens highlighted
        // item)
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxWidth()
                    .height(52.rdp)
                    .clickable(toggleDetailAction)
                    .padding(bottom = 8.rdp)
                    .clip(RemoteRoundedCornerShape(26.rdp))
                    .background(Color(0x1AFFFFFF).rc)
                    .padding(3.rdp)
                    .clip(RemoteRoundedCornerShape(23.rdp))
                    .background(Color(0xCC161616).rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteFitBox(modifier = RemoteModifier.padding(horizontal = 20.rdp)) {
                arrayOf(16.rsp, 14.rsp, 12.rsp, 10.rsp).forEach { size ->
                    RemoteText(
                        text = "Doge Calendar".rs,
                        fontSize = size,
                        color = Color.White.rc,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }

        // Scrollable list + XR scroll progress indicator side-by-side
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth().weight(1f),
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            RemoteColumn(
                modifier = RemoteModifier.weight(1f).fillMaxHeight().verticalScroll(scrollState),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                entries.forEachIndexed { index, entry ->
                    DogeListItemRow(
                        index = index,
                        entry = entry,
                        scrollState = scrollState,
                        toggleDetailAction = toggleDetailAction,
                    )
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth().height(296.rdp).clickable(toggleDetailAction)
                )
            }

            DogeScrollProgressBar(
                scrollState = scrollState,
                maxScrollPx = (entries.size - 1) * 74f,
                toggleDetailAction = toggleDetailAction,
            )
        }
    }
}

@Composable
@RemoteComposable
private fun DogeScrollProgressBar(
    scrollState: RemoteScrollState,
    maxScrollPx: Float,
    toggleDetailAction: androidx.compose.remote.creation.compose.action.Action,
) {
    // Track inner height inside 370.rdp list area with 6.rdp top/bottom inset = 358.rdp;
    // 46.rdp gold thumb travels smoothly across 312.rdp [0..312] as scroll moves [0..maxScrollPx].
    val progress = scrollState.positionState / maxScrollPx.rf
    val thumbOffsetY = (progress * 312f.rf).asRemoteDp()

    RemoteBox(
        modifier =
            RemoteModifier.fillMaxHeight()
                .padding(start = 8.rdp, top = 6.rdp, bottom = 6.rdp)
                .clickable(toggleDetailAction),
        contentAlignment = RemoteAlignment.TopCenter,
    ) {
        RemoteBox(
            modifier =
                RemoteModifier.size(width = 6.rdp, height = 358.rdp)
                    .clip(RemoteRoundedCornerShape(3.rdp))
                    .background(Color(0x28FFFFFF).rc),
            contentAlignment = RemoteAlignment.TopCenter,
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(width = 6.rdp, height = 46.rdp)
                        .offset(x = 0.rdp, y = thumbOffsetY)
                        .clip(RemoteRoundedCornerShape(3.rdp))
                        .background(Color(0xFFF3D27A).rc)
            )
        }
    }
}

@Composable
@RemoteComposable
private fun DogeListItemRow(
    index: Int,
    entry: DogeListEntry,
    scrollState: RemoteScrollState,
    toggleDetailAction: androidx.compose.remote.creation.compose.action.Action,
) {
    // Highlight whichever pill's vertical center is closest to the top slot (rowTop in [-37, +37])
    val topSlotDelta = (index * 74f).rf - scrollState.positionState
    val isHighlighted = (topSlotDelta * topSlotDelta).isLessThan((37f * 37f).rf)
    val highlightOffsetX = isHighlighted.select(0f.rf, 2000f.rf).asRemoteDp()

    RemoteBox(
        modifier =
            RemoteModifier.fillMaxWidth()
                .height(74.rdp)
                .clickable(toggleDetailAction)
                .padding(vertical = 5.rdp)
                .clip(RemoteRoundedCornerShape(32.rdp))
                .background(Color(0x18FFFFFF).rc),
        contentAlignment = RemoteAlignment.CenterStart,
    ) {
        // Default unselected dark pill interior
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxSize()
                    .padding(3.rdp)
                    .clip(RemoteRoundedCornerShape(29.rdp))
                    .background(Color(0xCC181818).rc)
        )

        // Active scroll highlight rim & warm surface fill (pre-measured at full bounds, translated
        // via offset)
        RemoteBox(
            modifier =
                RemoteModifier.fillMaxSize()
                    .offset(x = highlightOffsetX, y = 0.rdp)
                    .background(Color(0xFFF3D27A).rc)
                    .padding(3.rdp)
                    .clip(RemoteRoundedCornerShape(29.rdp))
                    .background(Color(0xFF332814).rc)
        )

        RemoteRow(
            modifier = RemoteModifier.fillMaxSize().padding(horizontal = 17.rdp),
            verticalAlignment = RemoteAlignment.CenterVertically,
        ) {
            DogeCategoryBadge(category = entry.category, badgeSize = 42)

            RemoteFitBox(
                modifier = RemoteModifier.weight(1f).padding(start = 14.rdp, end = 8.rdp),
                horizontalAlignment = RemoteAlignment.Start,
            ) {
                arrayOf(15.rsp, 13.rsp, 11.rsp, 9.rsp).forEach { size ->
                    RemoteText(
                        text = "${entry.time}  •  ${entry.title}".rs,
                        fontSize = size,
                        color = Color.White.rc,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Floating pop-up Doge detail card rendered directly over the scrollable list view. Tapping
 * anywhere toggles the pop-up open/closed while keeping the list underneath live and scrollable.
 */
@Composable
@RemoteComposable
private fun DogeExpandedSlot(
    entry: DogeListEntry,
    icon: ImageBitmap,
    modifier: RemoteModifier = RemoteModifier,
) {
    RemoteColumn(
        modifier =
            modifier
                .clip(RemoteRoundedCornerShape(28.rdp))
                .background(Color(0xFFF3D27A).rc)
                .padding(2.rdp)
                .clip(RemoteRoundedCornerShape(26.rdp))
                .background(Color(0xF0161616).rc)
                .padding(horizontal = 16.rdp, vertical = 12.rdp),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.SpaceBetween,
    ) {
        RemoteFitBox(modifier = RemoteModifier.fillMaxWidth().height(22.rdp)) {
            arrayOf(13.rsp, 11.rsp, 9.rsp).forEach { size ->
                RemoteText(
                    text = "${entry.time}  •  ${entry.title}".rs,
                    fontSize = size,
                    color = Color(0xFFF3D27A).rc,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }

        RemoteImage(
            remoteBitmap = icon.rb,
            contentDescription = RemoteString("Doge Icon"),
            modifier = RemoteModifier.size(176.rdp),
        )

        RemoteBox(
            modifier =
                RemoteModifier.fillMaxWidth()
                    .height(122.rdp)
                    .clip(RemoteRoundedCornerShape(26.rdp))
                    .background(Color(0x1AFFFFFF).rc)
                    .padding(2.rdp)
                    .clip(RemoteRoundedCornerShape(24.rdp))
                    .background(Color(0xCC202020).rc),
            contentAlignment = RemoteAlignment.Center,
        ) {
            RemoteFitBox(modifier = RemoteModifier.padding(horizontal = 16.rdp, vertical = 8.rdp)) {
                arrayOf(14.rsp, 12.rsp, 10.rsp, 8.rsp, 7.rsp).forEach { size ->
                    RemoteText(
                        text = entry.task.rs,
                        fontSize = size,
                        color = Color.White.rc,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                    )
                }
            }
        }
    }
}

/**
 * Small category icon badge rendered via crisp vector primitives on [RemoteCanvas]. Distinct from
 * the full-size Doge illustrations shown in the expanded view.
 */
@Composable
@RemoteComposable
private fun DogeCategoryBadge(category: DogeEntryCategory, badgeSize: Int = 42) {
    RemoteBox(
        modifier =
            RemoteModifier.size(badgeSize.rdp)
                .clip(RemoteRoundedCornerShape((badgeSize / 2).rdp))
                .background(category.badgeSurface.rc),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteCanvas(modifier = RemoteModifier.size((badgeSize - 14).rdp)) {
            drawCategoryGlyph(category)
        }
    }
}

private fun RemoteDrawScope.drawCategoryGlyph(category: DogeEntryCategory) {
    val strokePaint =
        RemotePaint().apply {
            color = category.accent.rc
            style = PaintingStyle.Stroke
            strokeWidth = 2.4f.rf
        }
    val fillPaint =
        RemotePaint().apply {
            color = category.accent.rc
            style = PaintingStyle.Fill
        }

    when (category) {
        DogeEntryCategory.SUNRISE -> {
            strokeLine(0.12f, 0.72f, 0.88f, 0.72f, strokePaint)
            strokeArc(0.22f, 0.36f, 0.78f, 0.92f, 180f, 180f, false, strokePaint)
            strokeLine(0.50f, 0.12f, 0.50f, 0.24f, strokePaint)
        }
        DogeEntryCategory.BREAKFAST -> {
            remoteCanvas.drawRoundRect(
                width * 0.18f.rf,
                height * 0.42f.rf,
                width * 0.72f.rf,
                height * 0.86f.rf,
                4f.rf,
                4f.rf,
                strokePaint,
            )
            strokeArc(0.64f, 0.48f, 0.88f, 0.78f, -90f, 180f, false, strokePaint)
            strokeLine(0.34f, 0.16f, 0.34f, 0.30f, strokePaint)
            strokeLine(0.54f, 0.14f, 0.54f, 0.30f, strokePaint)
        }
        DogeEntryCategory.MORNING -> {
            strokeArc(0.22f, 0.20f, 0.78f, 0.76f, 180f, 180f, false, strokePaint)
            strokeLine(0.16f, 0.70f, 0.84f, 0.70f, strokePaint)
            fillDot(0.50f, 0.83f, 0.08f, fillPaint)
        }
        DogeEntryCategory.NAP -> {
            strokeArc(0.18f, 0.18f, 0.82f, 0.82f, 35f, 250f, false, strokePaint)
            fillDot(0.72f, 0.28f, 0.07f, fillPaint)
        }
        DogeEntryCategory.LUNCH -> {
            strokeArc(0.16f, 0.28f, 0.84f, 0.86f, 0f, 180f, true, strokePaint)
            strokeLine(0.22f, 0.34f, 0.82f, 0.16f, strokePaint)
            strokeLine(0.22f, 0.44f, 0.84f, 0.30f, strokePaint)
        }
        DogeEntryCategory.MID_AFTERNOON -> {
            fillDot(0.50f, 0.50f, 0.20f, fillPaint)
            strokeLine(0.50f, 0.08f, 0.50f, 0.20f, strokePaint)
            strokeLine(0.50f, 0.80f, 0.50f, 0.92f, strokePaint)
            strokeLine(0.08f, 0.50f, 0.20f, 0.50f, strokePaint)
            strokeLine(0.80f, 0.50f, 0.92f, 0.50f, strokePaint)
        }
        DogeEntryCategory.WALK -> {
            fillDot(0.50f, 0.62f, 0.18f, fillPaint)
            fillDot(0.28f, 0.34f, 0.09f, fillPaint)
            fillDot(0.50f, 0.26f, 0.09f, fillPaint)
            fillDot(0.72f, 0.34f, 0.09f, fillPaint)
        }
        DogeEntryCategory.DINNER -> {
            strokeLine(0.12f, 0.76f, 0.88f, 0.76f, strokePaint)
            strokeArc(0.18f, 0.34f, 0.82f, 0.98f, 180f, 180f, false, strokePaint)
            fillDot(0.50f, 0.24f, 0.07f, fillPaint)
        }
        DogeEntryCategory.PLAY -> {
            fillDot(0.30f, 0.50f, 0.16f, strokePaint)
            fillDot(0.70f, 0.50f, 0.16f, strokePaint)
            strokeLine(0.44f, 0.50f, 0.56f, 0.50f, strokePaint)
        }
        DogeEntryCategory.NIGHT -> {
            fillDot(0.50f, 0.50f, 0.15f, fillPaint)
            strokeLine(0.50f, 0.12f, 0.50f, 0.88f, strokePaint)
            strokeLine(0.12f, 0.50f, 0.88f, 0.50f, strokePaint)
        }
    }
}

private fun RemoteDrawScope.strokeLine(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    paint: RemotePaint,
) {
    remoteCanvas.drawLine(width * x1.rf, height * y1.rf, width * x2.rf, height * y2.rf, paint)
}

private fun RemoteDrawScope.strokeArc(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    startAngle: Float,
    sweepAngle: Float,
    useCenter: Boolean,
    paint: RemotePaint,
) {
    remoteCanvas.drawArc(
        width * left.rf,
        height * top.rf,
        width * right.rf,
        height * bottom.rf,
        startAngle.rf,
        sweepAngle.rf,
        useCenter,
        paint,
    )
}

private fun RemoteDrawScope.fillDot(cx: Float, cy: Float, r: Float, paint: RemotePaint) {
    remoteCanvas.drawCircle(width * cx.rf, height * cy.rf, width * r.rf, paint)
}

@Preview(widthDp = 480, heightDp = 480)
@Composable
private fun DogeCalendarListPreview() = RemoteContentPreview {
    DogeCalendar(initialSelectedIndex = -1)
}

@Preview(widthDp = 480, heightDp = 480)
@Composable
private fun DogeCalendarExpandedPreview() = RemoteContentPreview {
    DogeCalendar(initialSelectedIndex = 3)
}
