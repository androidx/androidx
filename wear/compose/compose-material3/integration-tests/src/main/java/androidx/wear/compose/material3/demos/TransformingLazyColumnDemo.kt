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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

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
) =
    TitleCard(
        onClick = {},
        title = { NotificationCardTitle(notification.title) },
        subtitle = { Text(notification.body) },
        transformation = SurfaceTransformation(transformationSpec),
        modifier = Modifier.transformedHeight(this, transformationSpec).animateItem(),
    )

@Composable
internal fun TransformingLazyColumnItemScope.NotificationsHeader(
    transformationSpec: TransformationSpec
) =
    ListHeader(
        transformation = SurfaceTransformation(transformationSpec),
        modifier = Modifier.transformedHeight(this, transformationSpec).animateItem(),
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

    AppScaffold {
        ScreenScaffold(
            state,
            edgeButton = {
                EdgeButton(onClick = { elements = elements.shuffled() }) { Text("Shuffle") }
            },
        ) { contentPadding ->
            TransformingLazyColumn(state = state, contentPadding = contentPadding) {
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
