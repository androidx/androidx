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

package androidx.wear.compose.material3.demos

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnVisibleItemInfo
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

@Composable
fun TransformingLazyColumnNotificationsDemo(
    modifier: Modifier = Modifier,
    notifications: List<NotificationItem> = NotificationItem.all,
    containerCompositingStrategy: CompositingStrategy = CompositingStrategy.ModulateAlpha,
) {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                modifier = modifier,
                contentPadding = contentPadding,
            ) {
                item {
                    ListHeader(
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier =
                            Modifier.transformedHeight(this, transformationSpec).animateItem(),
                    ) {
                        Text("Notifications")
                    }
                }
                items(notifications) { notification ->
                    Box(
                        modifier =
                            Modifier.transformedHeight(scope = this@items, transformationSpec)
                                .animateItem()
                                .graphicsLayer {
                                    with(transformationSpec) {
                                        applyContainerTransformation(scrollProgress)
                                    }
                                    compositingStrategy = containerCompositingStrategy
                                    clip = false
                                }
                    ) {
                        TitleCard(
                            onClick = {},
                            title = {
                                Text(
                                    notification.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                )
                            },
                            subtitle = { Text(notification.body) },
                            modifier =
                                Modifier.graphicsLayer {
                                    with(this@items) {
                                        with(transformationSpec) {
                                            applyContentTransformation(scrollProgress)
                                        }
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransformingLazyColumnMorphingNotificationsDemo(
    modifier: Modifier = Modifier,
    notifications: List<NotificationItem> = NotificationItem.all,
) {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val cardPadding =
        with(LocalDensity.current) {
            CardDefaults.ContentPadding.calculateTopPadding().roundToPx() +
                CardDefaults.ContentPadding.calculateBottomPadding().roundToPx()
        }
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            // Since all titles for notifications are of the same height, we are measuring the Text
            // and reusing the same morphing transformation spec for all notifications.
            MeasuredHeader { headerHeight ->
                val morphingTransformationSpec =
                    rememberMorphingTransformationSpec(
                        transformationSpec,
                        minMorphingHeight = headerHeight + cardPadding,
                    )
                TransformingLazyColumn(
                    state = state,
                    contentPadding = contentPadding,
                    modifier = modifier,
                ) {
                    item { NotificationsHeader(transformationSpec) }
                    items(notifications) { notification ->
                        NotificationCard(notification = notification, morphingTransformationSpec)
                    }
                }
            }
        }
    }
}

@Composable
fun MeasuredHeader(content: @Composable (height: Int) -> Unit) {
    SubcomposeLayout { constraints ->
        val headerPlaceables =
            subcompose("Header slot") { NotificationCardTitle("Notification prototype") }
                .map { it.measure(constraints) }

        val headerHeight =
            headerPlaceables.fold(0) { currentMax, placeable ->
                maxOf(currentMax, placeable.height)
            }

        layout(constraints.maxWidth, constraints.maxHeight) {
            subcompose("List slot") { content(headerHeight) }
                .forEach { it.measure(constraints).placeRelative(0, 0) }
        }
    }
}

@Composable
internal fun NotificationCardTitle(text: String) =
    Text(
        text,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
    )

@Composable
internal fun TransformingLazyColumnItemScope.NotificationCard(
    notification: NotificationItem,
    transformationSpec: TransformationSpec,
    modifier: Modifier = Modifier,
) =
    TitleCard(
        onClick = {},
        title = { NotificationCardTitle(notification.title) },
        subtitle = { Text(notification.body) },
        transformation = SurfaceTransformation(transformationSpec),
        modifier = modifier.transformedHeight(this, transformationSpec).animateItem(),
    )

@Composable
internal fun TransformingLazyColumnItemScope.NotificationsHeader(
    transformationSpec: TransformationSpec,
    modifier: Modifier = Modifier,
) =
    ListHeader(
        transformation = SurfaceTransformation(transformationSpec),
        modifier = modifier.transformedHeight(this, transformationSpec).animateItem(),
    ) {
        Text("Notifications")
    }

data class NotificationItem(val title: String, val body: String) {
    companion object {
        val all =
            listOf(
                NotificationItem(
                    "☕ Coffee Break?",
                    "Step away from the screen and grab a pick-me-up. Step away from the screen and grab a pick-me-up.",
                ),
                NotificationItem(
                    "🌟 You're Awesome!",
                    "Just a little reminder in case you forgot 😊",
                ),
                NotificationItem(
                    "👀 Did you know?",
                    "Check out [app name]'s latest feature update.",
                ),
                NotificationItem(
                    "📅 Appointment Time",
                    "Your meeting with [name] is in 15 minutes.",
                ),
                NotificationItem(
                    "📦 Package On the Way",
                    "Your order is expected to arrive today!",
                ),
                NotificationItem(
                    "🤔 Trivia Time!",
                    "Test your knowledge with a quick quiz on [app name].",
                ),
                NotificationItem(
                    "🌤️ Weather Update",
                    "Don't forget your umbrella - rain is likely this afternoon.",
                ),
                NotificationItem(
                    "🤝 Connect with [name]",
                    "They sent you a message on [social platform].",
                ),
                NotificationItem("🧘‍♀️ Time to Breathe", "Take a 5-minute mindfulness break."),
                NotificationItem(
                    "🌟 Goal Achieved!",
                    "You completed your daily step goal. Way to go!",
                ),
                NotificationItem("💡 New Idea!", "Got a spare moment? Jot down a quick note."),
                NotificationItem("👀 Photo Memories", "Rediscover photos from this day last year."),
                NotificationItem("🚗 Parking Reminder", "Your parking meter expires in 1 hour."),
                NotificationItem("🎧 Playlist Time", "Your daily mix on [music app] is ready."),
                NotificationItem(
                    "🎬 Movie Night?",
                    "New releases are out on your favorite streaming service. New releases are out on your favorite streaming service.",
                ),
                NotificationItem(
                    "📚 Reading Time",
                    "Pick up where you left off in your current book.",
                ),
                NotificationItem(
                    "🤔 Something to Ponder",
                    "Here's a thought-provoking quote for today...",
                ),
                NotificationItem("⏰ Time for [task]", "Remember to [brief description]."),
                NotificationItem("💧 Stay Hydrated!", "Have you had a glass of water recently?"),
                NotificationItem("👀 Game Update Available", "Your favorite game has new content!"),
                NotificationItem("🌎 Learn Something New", "Fact of the day: [Insert a fun fact]."),
                NotificationItem(
                    "☀️ Step Outside",
                    "Get some fresh air and sunshine for a quick energy boost",
                ),
                NotificationItem(
                    "🎉 It's [friend's name]'s Birthday!",
                    "Don't forget to send a message.",
                ),
                NotificationItem("✈️ Travel Inspiration", "Where's your dream travel destination?"),
                NotificationItem("😋 Recipe Time", "Find a new recipe to try on [recipe website]."),
                NotificationItem(
                    "👀 Explore!",
                    "[App name] has a hidden feature - can you find it?",
                ),
                NotificationItem(
                    "💰 Savings Update",
                    "You're [percent] closer to your savings goal!",
                ),
                NotificationItem("🌟 Daily Challenge", "Try today's mini-challenge on [app name]."),
                NotificationItem(
                    "💤 Bedtime Approaching",
                    "Start winding down for a good night's sleep.",
                ),
                NotificationItem("🤝 Team Update", "[Team member] posted on your project board."),
                NotificationItem("🌿 Plant Care", "Time to water your [plant type]."),
                NotificationItem(
                    "🎮 Game Break?",
                    "Take a 10-minute break with your favorite game.",
                ),
                NotificationItem("🗣️  Your Voice Matters", "New poll available on [topic/app]."),
                NotificationItem("🎨 Get Creative", "Doodle, draw, or paint for a few minutes."),
                NotificationItem("❓Ask a Question", "What's something that's been on your mind?"),
                NotificationItem("🔍 Search Time", "Research a topic that interests you."),
                NotificationItem(
                    "🤝 Help Someone Out",
                    "Is there a small way you can assist someone today?",
                ),
                NotificationItem("🐾 Pet Appreciation", "Give your furry friend some extra love."),
                NotificationItem("📝 Journal Time", "Take 5 minutes to jot down your thoughts."),
            )
    }
}

@Composable
internal fun TransformingLazyColumnReducedMotionSample() {
    // This simulates how TransformingLazyColumn looks like when reduce motion is enabled.
    CompositionLocalProvider(LocalReduceMotion provides true) {
        val transformationSpec = rememberTransformationSpec()
        val state = rememberTransformingLazyColumnState()
        AppScaffold {
            ScreenScaffold(state) { contentPadding ->
                TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                    items(count = 10) {
                        Button(
                            onClick = {},
                            modifier =
                                Modifier.minimumVerticalContentPadding(
                                        ButtonDefaults.minimumVerticalListContentPadding
                                    )
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec)
                                    .animateItem(),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text("Item $it")
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("PrimitiveInCollection")
@Composable
fun TransformingLazyColumnAnimationSample() {
    val transformationSpec = rememberTransformationSpec()
    val state = rememberTransformingLazyColumnState()
    var elements by remember { mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)) }
    var nextElement by remember { mutableIntStateOf(10) }

    fun addCardAfter(index: Int) {
        elements =
            elements.subList(0, index + 1) +
                listOf(nextElement++) +
                elements.subList(index + 1, elements.count())
    }

    fun removeCardAt(index: Int) {
        elements = elements.subList(0, index) + elements.subList(index + 1, elements.count())
    }

    fun moveCardToStart(index: Int) {
        elements =
            elements.subList(index, index + 1) +
                elements.subList(0, index) +
                elements.subList(index + 1, elements.count())
    }

    fun moveCardToEnd(index: Int) {
        elements =
            elements.subList(0, index) +
                elements.subList(index + 1, elements.count()) +
                elements.subList(index, index + 1)
    }

    AppScaffold {
        ScreenScaffold(
            state,
            edgeButton = {
                EdgeButton(onClick = { elements = elements.shuffled() }) { Text("Shuffle") }
            },
        ) { contentPadding ->
            TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                itemsIndexed(elements, key = { _, key -> key }) { index, cardKey ->
                    var isPinned by remember { mutableStateOf(false) }
                    var pinHandle by remember {
                        mutableStateOf<PinnableContainer.PinnedHandle?>(null)
                    }
                    val pinnableContainer = LocalPinnableContainer.current
                    Card(
                        onClick = {
                            if (isPinned) {
                                pinHandle?.release()
                                pinHandle = null
                                isPinned = false
                            } else {
                                pinHandle = pinnableContainer?.pin()
                                isPinned = true
                            }
                        },
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    CardDefaults.minimumVerticalListContentPadding
                                )
                                .transformedHeight(this, transformationSpec)
                                .animateItem(),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Card $cardKey" + if (isPinned) " - Pinned" else "")
                        Row {
                            CompactButton(onClick = { moveCardToEnd(index) }) { Text(ArrowDown) }
                            CompactButton(onClick = { moveCardToStart(index) }) { Text(ArrowUp) }
                            Spacer(modifier = Modifier.weight(1f))
                            CompactButton(
                                onClick = { removeCardAt(index) },
                                enabled = elements.count() > 1,
                            ) {
                                Text("-")
                            }
                            CompactButton(onClick = { addCardAfter(index) }) { Text("+") }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("PrimitiveInCollection")
@Composable
fun TransformingLazyColumnReverseLayoutSample() {
    var reverseLayout by remember { mutableStateOf(true) }
    val transformationSpec = rememberTransformationSpec()
    val state = rememberTransformingLazyColumnState()
    var elements by remember { mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)) }
    var nextElement by remember { mutableIntStateOf(10) }

    fun addCardAfter(index: Int) {
        elements =
            elements.subList(0, index + 1) +
                listOf(nextElement++) +
                elements.subList(index + 1, elements.count())
    }

    fun removeCardAt(index: Int) {
        elements = elements.subList(0, index) + elements.subList(index + 1, elements.count())
    }

    fun moveCardToStart(index: Int) {
        elements =
            elements.subList(index, index + 1) +
                elements.subList(0, index) +
                elements.subList(index + 1, elements.count())
    }

    fun moveCardToEnd(index: Int) {
        elements =
            elements.subList(0, index) +
                elements.subList(index + 1, elements.count()) +
                elements.subList(index, index + 1)
    }

    AppScaffold {
        ScreenScaffold(
            state,
            edgeButton = {
                EdgeButton(onClick = { reverseLayout = !reverseLayout }) { Text("Reverse") }
            },
        ) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
            ) {
                itemsIndexed(elements, key = { _, key -> key }) { index, cardKey ->
                    Card(
                        onClick = {},
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    CardDefaults.minimumVerticalListContentPadding
                                )
                                .transformedHeight(this, transformationSpec)
                                .animateItem(),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Card $cardKey")
                        Row {
                            CompactButton(onClick = { moveCardToEnd(index) }) {
                                Text(if (reverseLayout) ArrowUp else ArrowDown)
                            }
                            CompactButton(onClick = { moveCardToStart(index) }) {
                                Text(if (reverseLayout) ArrowDown else ArrowUp)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            CompactButton(
                                onClick = { removeCardAt(index) },
                                enabled = elements.count() > 1,
                            ) {
                                Text("-")
                            }
                            CompactButton(onClick = { addCardAfter(index) }) { Text("+") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransformingLazyColumnExpandableCardSample() {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    var expandedIndex by remember { mutableIntStateOf(-1) }
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                items(count = 50) { cardIndex ->
                    TitleCard(
                        onClick = {
                            expandedIndex = if (expandedIndex == cardIndex) -1 else cardIndex
                        },
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    CardDefaults.minimumVerticalListContentPadding
                                )
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        title = { Text("Card $cardIndex") },
                        subtitle = {
                            AnimatedVisibility(expandedIndex == cardIndex) {
                                Text("Expanded content is available here")
                            }
                        },
                        content = { Text("Tap on Card to expand") },
                    )
                }
            }
        }
    }
}

/**
 * Demo comparing [TransformingLazyColumnState.animateScrollToItem] and
 * [TransformingLazyColumnState.scrollToItem] on a [TransformingLazyColumn] with transformed items.
 */
@Composable
fun TransformingLazyColumnAnimateScrollToItemDemo() {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val coroutineScope = rememberCoroutineScope()
    val notifications = NotificationItem.all.take(12)
    val targetIndex = notifications.size // target item index (header is at index 0)

    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                TransformingLazyColumn(
                    state = state,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        ListHeader(
                            transformation = SurfaceTransformation(transformationSpec),
                            modifier = Modifier.transformedHeight(this, transformationSpec),
                        ) {
                            Text("Animate vs Snap Scroll")
                        }
                    }

                    items(notifications.size) { index ->
                        val notification = notifications[index]
                        val isTarget = index == notifications.size - 1
                        AppCard(
                            onClick = {},
                            appName = { Text("Messages") },
                            appImage = {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.star_big_off),
                                    contentDescription = null,
                                    modifier = Modifier.size(CardDefaults.AppImageSize),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            time = { Text("${index + 1}m ago") },
                            title = {
                                Text(
                                    if (isTarget) "Target (Last #$index)"
                                    else "Message #$index: ${notification.title}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            transformation = SurfaceTransformation(transformationSpec),
                            modifier = Modifier.transformedHeight(this, transformationSpec),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    notification.body,
                                    maxLines = 3,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    CompactButton(onClick = {}) { Text("Reply") }
                                    CompactButton(onClick = {}) { Text("Dismiss") }
                                }
                            }
                        }
                    }
                }

                // Control buttons overlay comparing animated scroll vs instant snap scroll
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CompactButton(onClick = { coroutineScope.launch { state.scrollToItem(0) } }) {
                        Text("Top")
                    }

                    CompactButton(
                        onClick = {
                            coroutineScope.launch { state.animateScrollToItem(targetIndex) }
                        }
                    ) {
                        Text("Animate")
                    }

                    CompactButton(
                        onClick = { coroutineScope.launch { state.scrollToItem(targetIndex) } }
                    ) {
                        Text("Snap")
                    }
                }
            }
        }
    }
}

/**
 * Demo showing a custom [FlingBehavior] that honors each item's
 * [TransformingLazyColumnVisibleItemInfo.snapSegmentThreshold] and snaps directly to the target
 * segment center rather than scrolling smoothly.
 */
@Composable
fun TransformingLazyColumnCustomSnapFlingBehaviorDemo() {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val density = LocalDensity.current
    val isScreenRound = LocalConfiguration.current.isScreenRound
    val halfScreenHeightPx = LocalWindowInfo.current.containerSize.height / 2
    val minFlingVelocityPx = with(density) { 400.dp.toPx() }
    val customSnapFlingBehavior =
        remember(state, minFlingVelocityPx, isScreenRound) {
            DirectSegmentSnapFlingBehavior(state, minFlingVelocityPx, isScreenRound)
        }

    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                contentPadding = contentPadding,
                flingBehavior = customSnapFlingBehavior,
                // Note: Rotary uses the default smooth behavior here because the demo focuses on
                // illustrating a custom touch FlingBehavior; a production list using custom segment
                // thresholds should pair matching touch and rotary snap behaviors.
                rotaryScrollableBehavior = RotaryScrollableDefaults.behavior(state),
            ) {
                item {
                    ListHeader(
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    ListHeaderDefaults.minimumTopListContentPadding,
                                    ListHeaderDefaults.minimumBottomListContentPadding,
                                )
                                .transformedHeight(this, transformationSpec),
                    ) {
                        Text("Direct Segment Snap")
                    }
                }
                item {
                    NotificationCard(
                        notification =
                            NotificationItem(
                                "Half screen",
                                "This tall card sets Modifier.snapSegmentThreshold to half of the " +
                                    "screen height, so the custom FlingBehavior steps through it " +
                                    "directly in smaller segments rather than scrolling smoothly. " +
                                    "Flick up or down to jump immediately between segments. " +
                                    "Each segment center aligns with the center of the viewport.",
                            ),
                        transformationSpec = transformationSpec,
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    CardDefaults.minimumVerticalListContentPadding
                                )
                                .snapSegmentThreshold { _, _ -> halfScreenHeightPx },
                    )
                }
                item {
                    NotificationCard(
                        notification =
                            NotificationItem(
                                "Kept whole",
                                "This card returns measuredHeight from " +
                                    "Modifier.snapSegmentThreshold, so the custom FlingBehavior " +
                                    "treats it as a single snap point centered on the card.",
                            ),
                        transformationSpec = transformationSpec,
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    CardDefaults.minimumVerticalListContentPadding
                                )
                                .snapSegmentThreshold { _, measuredHeight -> measuredHeight },
                    )
                }
                item {
                    NotificationCard(
                        notification =
                            NotificationItem(
                                "Default threshold",
                                "This tall card does not apply Modifier.snapSegmentThreshold, so " +
                                    "TransformingLazyColumnVisibleItemInfo.snapSegmentThreshold " +
                                    "is 0 and the custom FlingBehavior uses its own default " +
                                    "threshold to divide the card into segments. Flick up or down " +
                                    "to jump directly from one segment center to the next.",
                            ),
                        transformationSpec = transformationSpec,
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                CardDefaults.minimumVerticalListContentPadding
                            ),
                    )
                }
                items(NotificationItem.all.take(6)) { notification ->
                    NotificationCard(
                        notification = notification,
                        transformationSpec = transformationSpec,
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                CardDefaults.minimumVerticalListContentPadding
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Custom [FlingBehavior] that snaps directly to the target segment center without smooth scrolling,
 * reading each item's [TransformingLazyColumnVisibleItemInfo.snapSegmentThreshold].
 */
private class DirectSegmentSnapFlingBehavior(
    private val state: TransformingLazyColumnState,
    private val minFlingVelocityPx: Float,
    private val isScreenRound: Boolean,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (initialVelocity.isNaN()) return Float.NaN
        val layoutInfo = state.layoutInfo
        val visibleItems = layoutInfo.visibleItems
        val viewportHeight = layoutInfo.viewportSize.height
        if (visibleItems.isEmpty() || viewportHeight <= 0) return initialVelocity
        if (!state.canScrollForward && !state.canScrollBackward) return initialVelocity

        val viewportCenter = viewportHeight / 2f
        val snapPoints = buildSnapPoints(visibleItems, viewportHeight)
        if (snapPoints.isEmpty()) return initialVelocity

        val direction =
            if (abs(initialVelocity) >= minFlingVelocityPx) initialVelocity.sign.toInt() else 0
        var target =
            if (direction != 0) {
                snapPoints
                    .filter { (it.position - viewportCenter) * direction > 1f }
                    .minByOrNull { abs(it.position - viewportCenter) }
            } else {
                snapPoints.minByOrNull { abs(it.position - viewportCenter) }
            }

        // If an item kept whole fills the viewport, no snap point is laid out ahead in the fling
        // direction yet. Scroll past its edge to reveal the adjacent item, then snap to it.
        if (target == null && direction != 0) {
            val edgeItem = if (direction > 0) visibleItems.last() else visibleItems.first()
            val hasMoreItems =
                if (direction > 0) {
                    edgeItem.index < layoutInfo.totalItemsCount - 1
                } else {
                    edgeItem.index > 0
                }
            if (hasMoreItems) {
                val revealDelta =
                    if (direction > 0) {
                        edgeItem.offset + edgeItem.transformedHeight - viewportCenter + 1f
                    } else {
                        edgeItem.offset - viewportCenter - 1f
                    }
                scrollBy(revealDelta)
                val updatedPoints = buildSnapPoints(state.layoutInfo.visibleItems, viewportHeight)
                target =
                    if (direction > 0) {
                        updatedPoints.firstOrNull { it.itemIndex > edgeItem.index }
                    } else {
                        updatedPoints.lastOrNull { it.itemIndex < edgeItem.index }
                    } ?: updatedPoints.minByOrNull { abs(it.position - viewportCenter) }
            } else {
                // Already past all snap points at this end of the list. If resting on the edge
                // snap point, leave velocity unconsumed for overscroll and nested scrolling.
                val edgePoint = snapPoints.minByOrNull { abs(it.position - viewportCenter) }
                if (edgePoint == null || abs(edgePoint.position - viewportCenter) <= 1f) {
                    return initialVelocity
                }
                target = edgePoint
            }
        }

        val snapTarget = target ?: return initialVelocity

        // Jump directly to the target segment center without smooth scrolling. Because
        // TransformingLazyColumn transforms item heights as items move toward the center, apply
        // a few synchronous corrections so the target segment settles on the viewport center.
        repeat(4) {
            val currentLayout = state.layoutInfo
            val currentItem =
                currentLayout.visibleItems.firstOrNull { it.index == snapTarget.itemIndex }
                    ?: return 0f
            val count = segmentCountFor(currentItem, currentLayout.viewportSize.height)
            val segmentIndex = snapTarget.segmentIndex.coerceIn(0, count - 1)
            val segmentCenter =
                currentItem.offset + (segmentIndex + 0.5f) * currentItem.transformedHeight / count
            val delta = segmentCenter - currentLayout.viewportSize.height / 2f
            if (abs(delta) <= 1f) return 0f
            val consumed = scrollBy(delta)
            if (abs(consumed) < 0.5f) {
                return if (initialVelocity * delta > 0f) initialVelocity else 0f
            }
        }
        return 0f
    }

    private fun buildSnapPoints(
        visibleItems: List<TransformingLazyColumnVisibleItemInfo>,
        viewportHeight: Int,
    ): List<SegmentSnapPoint> = buildList {
        visibleItems.forEach { item ->
            if (item.measuredHeight <= 0 || item.transformedHeight <= 0) return@forEach
            val count = segmentCountFor(item, viewportHeight)
            val segmentHeight = item.transformedHeight.toFloat() / count
            for (segmentIndex in 0 until count) {
                add(
                    SegmentSnapPoint(
                        itemIndex = item.index,
                        segmentIndex = segmentIndex,
                        position = item.offset + (segmentIndex + 0.5f) * segmentHeight,
                    )
                )
            }
        }
    }

    private fun segmentCountFor(
        item: TransformingLazyColumnVisibleItemInfo,
        viewportHeight: Int,
    ): Int {
        val requested = item.snapSegmentThreshold
        if (requested > 0 && requested >= item.measuredHeight) return 1
        val minThreshold = (viewportHeight * 0.2f).toInt().coerceAtLeast(1)
        val defaultThreshold =
            if (isScreenRound) (viewportHeight * 0.65f).toInt().coerceAtLeast(1) else viewportHeight
        val effectiveThreshold =
            (if (requested > 0) requested else defaultThreshold).coerceIn(
                minThreshold,
                viewportHeight.coerceAtLeast(minThreshold),
            )
        return maxOf(1, (item.measuredHeight + effectiveThreshold - 1) / effectiveThreshold)
    }

    private class SegmentSnapPoint(
        val itemIndex: Int,
        val segmentIndex: Int,
        val position: Float,
    )
}

private const val ArrowUp = "\u2191"
private const val ArrowDown = "\u2193"
