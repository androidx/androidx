/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.scrollTransform
import androidx.wear.compose.material3.lazy.targetMorphingHeight
import androidx.wear.compose.material3.samples.AppCardSample
import androidx.wear.compose.material3.samples.AppCardWithIconSample
import androidx.wear.compose.material3.samples.AppCardWithImageSample
import androidx.wear.compose.material3.samples.ButtonExtraLargeIconSample
import androidx.wear.compose.material3.samples.ButtonLargeIconSample
import androidx.wear.compose.material3.samples.ButtonSample
import androidx.wear.compose.material3.samples.CardSample
import androidx.wear.compose.material3.samples.ChildButtonSample
import androidx.wear.compose.material3.samples.CompactButtonSample
import androidx.wear.compose.material3.samples.FilledTonalButtonSample
import androidx.wear.compose.material3.samples.FilledTonalCompactButtonSample
import androidx.wear.compose.material3.samples.FilledVariantButtonSample
import androidx.wear.compose.material3.samples.OutlinedAppCardSample
import androidx.wear.compose.material3.samples.OutlinedButtonSample
import androidx.wear.compose.material3.samples.OutlinedCardSample
import androidx.wear.compose.material3.samples.OutlinedCompactButtonSample
import androidx.wear.compose.material3.samples.OutlinedTitleCardSample
import androidx.wear.compose.material3.samples.R
import androidx.wear.compose.material3.samples.SimpleButtonSample
import androidx.wear.compose.material3.samples.SimpleChildButtonSample
import androidx.wear.compose.material3.samples.SimpleFilledTonalButtonSample
import androidx.wear.compose.material3.samples.SimpleFilledVariantButtonSample
import androidx.wear.compose.material3.samples.SimpleOutlinedButtonSample
import androidx.wear.compose.material3.samples.TitleCardSample
import androidx.wear.compose.material3.samples.TitleCardWithImageBackgroundSample
import androidx.wear.compose.material3.samples.TitleCardWithMultipleImagesSample
import androidx.wear.compose.material3.samples.TitleCardWithSubtitleAndTimeSample

@Composable
fun TransformingLazyColumnNotificationsDemo() {
    MaterialTheme {
        Box(modifier = Modifier.aspectRatio(1f).background(Color.Black)) {
            TransformingLazyColumn(
                modifier = Modifier.padding(horizontal = 10.dp),
            ) {
                item { ListHeader { Text("Notifications") } }
                items(notificationList) { notification ->
                    Column(
                        modifier =
                            Modifier.scrollTransform(
                                    this@items,
                                    backgroundColor = Color.DarkGray,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(10.dp)
                    ) {
                        Text(
                            notification.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.targetMorphingHeight(this@items)
                        )
                        Text(notification.body)
                    }
                }
            }
        }
    }
}

@Composable
fun TransformingLazyColumnButtons() {
    val state = rememberTransformingLazyColumnState()
    TransformingLazyColumn(
        state = state,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 50.dp),
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        item(contentType = "header") {
            // No modifier is applied - no Material 3 Motion transformations.
            ListHeader { Text("Buttons", style = MaterialTheme.typography.labelLarge) }
        }

        item { SimpleButtonSample(modifier = Modifier.fillMaxWidth()) }
        item { ButtonSample() }
        item { ButtonLargeIconSample() }
        item { ButtonExtraLargeIconSample() }
        item { SimpleFilledTonalButtonSample() }
        item { FilledTonalButtonSample() }
        item { SimpleFilledVariantButtonSample() }
        item { FilledVariantButtonSample() }
        item { SimpleOutlinedButtonSample() }
        item { OutlinedButtonSample() }
        item { SimpleChildButtonSample() }
        item { ChildButtonSample() }
        item { CompactButtonSample(modifier = Modifier.fillMaxWidth()) }
        item { FilledTonalCompactButtonSample(modifier = Modifier.fillMaxWidth()) }
        item { OutlinedCompactButtonSample(modifier = Modifier.fillMaxWidth()) }
        item { ButtonBackgroundImage(painterResource(R.drawable.backgroundimage), enabled = true) }
        item { ButtonBackgroundImage(painterResource(R.drawable.backgroundimage), enabled = false) }
        item { ListHeader { Text("Complex Buttons") } }
        item {
            Row(Modifier.scrollTransform(this@item)) {
                TransformExclusion {
                    SimpleButtonSample(Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    SimpleButtonSample(Modifier.weight(1f))
                }
            }
        }
        item {
            TransformExclusion {
                val interactionSource1 = remember { MutableInteractionSource() }
                val interactionSource2 = remember { MutableInteractionSource() }
                ButtonGroup(Modifier.scrollTransform(this@item)) {
                    Button(
                        onClick = {},
                        Modifier.animateWidth(interactionSource1),
                        interactionSource = interactionSource1
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("L")
                        }
                    }
                    Button(
                        onClick = {},
                        Modifier.animateWidth(interactionSource2),
                        interactionSource = interactionSource2
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("R")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransformingLazyColumnCards() {
    val state = rememberTransformingLazyColumnState()
    TransformingLazyColumn(
        state = state,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 50.dp),
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        item { ListHeader { Text("Card") } }
        item { CardSample() }
        item { CardWithImageDemo() }
        item { CardWithMultipleImagesDemo() }
        item { OutlinedCardSample() }
        item { VerticallyCenteredBaseCard() }
        item { CardWithButtons() }

        item { ListHeader { Text("App card") } }
        item { AppCardSample() }
        item { AppCardWithIconSample() }
        item { AppCardWithImageSample() }
        item { AppCardWithMultipleImagesDemo() }
        item { OutlinedAppCardSample() }

        item { ListHeader { Text("Title card") } }
        item { TitleCardSample() }
        item { TitleCardWithSubtitleDemo() }
        item { TitleCardWithSubtitleAndTimeSample() }
        item { TitleCardWithContentSubtitleAndTimeDemo() }
        item { TitleCardWithImageDemo() }
        item { TitleCardWithMultipleImagesSample() }
        item { OutlinedTitleCardSample() }
        item { OutlinedTitleCardWithSubtitleDemo() }
        item { OutlinedTitleCardWithSubtitleAndTimeDemo() }

        item { ListHeader { Text("Image card") } }
        item { TitleCardWithImageBackgroundSample() }
    }
}

@Composable
private fun CardWithButtons() {
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        title = { Text("Card with buttons") },
        time = { Text("now") },
    ) {
        Button(onClick = { /* Do something */ }) { Text("Button 1") }
        Spacer(Modifier.height(4.dp))
        Button(onClick = { /* Do something */ }) { Text("Button 2") }
    }
}

@Composable
private fun ButtonBackgroundImage(painter: Painter, enabled: Boolean) =
    Button(
        modifier = Modifier.sizeIn(maxHeight = ButtonDefaults.Height).fillMaxWidth(),
        onClick = { /* Do something */ },
        label = { Text("Image Background", maxLines = 1) },
        enabled = enabled,
        colors = ButtonDefaults.imageBackgroundButtonColors(painter)
    )

private data class NotificationItem(val title: String, val body: String)

private val notificationList =
    listOf(
        NotificationItem(
            "☕ Coffee Break?",
            "Step away from the screen and grab a pick-me-up. Step away from the screen and grab a pick-me-up."
        ),
        NotificationItem("🌟 You're Awesome!", "Just a little reminder in case you forgot 😊"),
        NotificationItem("👀 Did you know?", "Check out [app name]'s latest feature update."),
        NotificationItem("📅 Appointment Time", "Your meeting with [name] is in 15 minutes."),
        NotificationItem("📦 Package On the Way", "Your order is expected to arrive today!"),
        NotificationItem("🤔 Trivia Time!", "Test your knowledge with a quick quiz on [app name]."),
        NotificationItem(
            "🌤️ Weather Update",
            "Don't forget your umbrella - rain is likely this afternoon."
        ),
        NotificationItem("🤝 Connect with [name]", "They sent you a message on [social platform]."),
        NotificationItem("🧘‍♀️ Time to Breathe", "Take a 5-minute mindfulness break."),
        NotificationItem("🌟 Goal Achieved!", "You completed your daily step goal. Way to go!"),
        NotificationItem("💡 New Idea!", "Got a spare moment? Jot down a quick note."),
        NotificationItem("👀 Photo Memories", "Rediscover photos from this day last year."),
        NotificationItem("🚗 Parking Reminder", "Your parking meter expires in 1 hour."),
        NotificationItem("🎧 Playlist Time", "Your daily mix on [music app] is ready."),
        NotificationItem(
            "🎬 Movie Night?",
            "New releases are out on your favorite streaming service. New releases are out on your favorite streaming service."
        ),
        NotificationItem("📚 Reading Time", "Pick up where you left off in your current book."),
        NotificationItem("🤔 Something to Ponder", "Here's a thought-provoking quote for today..."),
        NotificationItem("⏰ Time for [task]", "Remember to [brief description]."),
        NotificationItem("💧 Stay Hydrated!", "Have you had a glass of water recently?"),
        NotificationItem("👀 Game Update Available", "Your favorite game has new content!"),
        NotificationItem("🌎 Learn Something New", "Fact of the day: [Insert a fun fact]."),
        NotificationItem(
            "☀️ Step Outside",
            "Get some fresh air and sunshine for a quick energy boost"
        ),
        NotificationItem("🎉 It's [friend's name]'s Birthday!", "Don't forget to send a message."),
        NotificationItem("✈️ Travel Inspiration", "Where's your dream travel destination?"),
        NotificationItem("😋 Recipe Time", "Find a new recipe to try on [recipe website]."),
        NotificationItem("👀 Explore!", "[App name] has a hidden feature - can you find it?"),
        NotificationItem("💰 Savings Update", "You're [percent] closer to your savings goal!"),
        NotificationItem("🌟 Daily Challenge", "Try today's mini-challenge on [app name]."),
        NotificationItem("💤 Bedtime Approaching", "Start winding down for a good night's sleep."),
        NotificationItem("🤝 Team Update", "[Team member] posted on your project board."),
        NotificationItem("🌿 Plant Care", "Time to water your [plant type]."),
        NotificationItem("🎮 Game Break?", "Take a 10-minute break with your favorite game."),
        NotificationItem("🗣️  Your Voice Matters", "New poll available on [topic/app]."),
        NotificationItem("🎨 Get Creative", "Doodle, draw, or paint for a few minutes."),
        NotificationItem("❓Ask a Question", "What's something that's been on your mind?"),
        NotificationItem("🔍 Search Time", "Research a topic that interests you."),
        NotificationItem(
            "🤝 Help Someone Out",
            "Is there a small way you can assist someone today?"
        ),
        NotificationItem("🐾 Pet Appreciation", "Give your furry friend some extra love."),
        NotificationItem("📝 Journal Time", "Take 5 minutes to jot down your thoughts.")
    )
