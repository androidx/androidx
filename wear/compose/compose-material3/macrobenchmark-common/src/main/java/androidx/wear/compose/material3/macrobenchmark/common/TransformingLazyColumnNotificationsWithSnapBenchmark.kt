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

package androidx.wear.compose.material3.macrobenchmark.common

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.demos.NotificationItem
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

val TransformingLazyColumnNotificationsWithSnapBenchmark =
    object : MacrobenchmarkScreen {
        override val content: @Composable (BoxScope.() -> Unit)
            get() = {
                val state = rememberTransformingLazyColumnState()
                val transformationSpec = rememberTransformationSpec()
                val notifications = (0..100).flatMap { NotificationItem.all }

                TransformingLazyColumn(
                    state = state,
                    flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state),
                    rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
                    modifier = Modifier.semantics { contentDescription = CONTENT_DESCRIPTION },
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
                            transformation = SurfaceTransformation(transformationSpec),
                            modifier =
                                Modifier.transformedHeight(this, transformationSpec).animateItem(),
                        )
                    }
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                val swipeStartY = device.displayHeight * 9 / 10 // scroll up
                val swipeEndY = device.displayHeight * 1 / 10
                val midX = device.displayWidth / 2

                repeat(20) {
                    device.swipe(midX, swipeStartY, midX, swipeEndY, 2)
                    device.waitForIdle()
                }
            }
    }
