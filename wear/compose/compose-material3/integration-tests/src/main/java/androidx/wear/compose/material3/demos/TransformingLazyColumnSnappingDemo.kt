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

package androidx.wear.compose.material3.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.lazy.rememberTransformationSpec

/**
 * Snapping with items of different sizes. Items that fit on the screen at their width snap to their
 * center; taller items are stepped through in equal sections by both the crown / bezel and touch
 * flings.
 */
@Composable
fun TransformingLazyColumnSnappingDemo() {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                contentPadding = contentPadding,
                flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
                rotaryScrollableBehavior =
                    RotaryScrollableDefaults.snapBehavior(scrollableState = state),
            ) {
                item {
                    NotificationsHeader(
                        transformationSpec,
                        Modifier.minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        ),
                    )
                }
                items(snappingDemoNotifications) { notification ->
                    NotificationCard(
                        notification,
                        transformationSpec,
                        Modifier.minimumVerticalContentPadding(
                            CardDefaults.minimumVerticalListContentPadding
                        ),
                    )
                }
            }
        }
    }
}

private val TallNotification =
    NotificationItem(
        "📰 Morning Briefing",
        "Rain is expected after 3 PM, so take an umbrella. Your first meeting moved to 10:30 and " +
            "the train to the office is running about five minutes late this morning.",
    )

private val TallerNotification =
    NotificationItem(
        "🐝 Pollinator Season",
        "Late-blooming asters and goldenrod give bees a final nectar source before winter. " +
            "Leaving seed heads standing also shelters overwintering insects, and a shallow " +
            "dish of water with a few pebbles gives tired bees a safe place to drink. Skip " +
            "the leaf blower this autumn: fallen leaves are where many butterflies spend the " +
            "cold months.",
    )

private val TallestNotification =
    NotificationItem(
        "📚 Fun Facts",
        "Honey never spoils: archaeologists have found pots of honey in ancient Egyptian tombs " +
            "that are over 3,000 years old and still edible. The Eiffel Tower gets about 15 cm " +
            "taller in the summer due to thermal expansion of the iron. The human brain " +
            "generates about 12-25 watts of power, enough to power a low-energy LED light " +
            "bulb. Octopuses have three hearts and blue blood. Bananas are berries, but " +
            "strawberries are not. A day on Venus is longer than its year.",
    )

/** The notifications demo list with a few tall cards, including two next to each other. */
private val snappingDemoNotifications =
    NotificationItem.all.toMutableList().apply {
        add(3, TallNotification)
        add(8, TallerNotification)
        add(9, TallNotification)
        add(15, TallestNotification)
    }
