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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.LazyColumn
import androidx.wear.compose.material3.lazy.minMorphingHeightConsumer
import androidx.wear.compose.material3.lazy.targetMorphingHeight
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class LazyColumnMorphingHeightTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun targetMorphingHeightReportedCorrectly() {
        val expectedMorphingHeight = 10

        var actualMorphingHeight: Int? = null

        rule.setContent {
            LazyColumn {
                item {
                    Box(
                        modifier =
                            Modifier.size(100.dp).minMorphingHeightConsumer {
                                actualMorphingHeight = it
                            }
                    ) {
                        Spacer(
                            modifier =
                                Modifier.height(
                                        with(LocalDensity.current) { expectedMorphingHeight.toDp() }
                                    )
                                    .targetMorphingHeight(this@item)
                        )
                    }
                }
            }
        }

        rule.waitForIdle()

        assertThat(actualMorphingHeight).isEqualTo(expectedMorphingHeight)
    }

    @Test
    fun noValueIsReportedWhenNoComposableAnnotatedWithTargetMorphingHeight() {
        var initialReportedMorphingHeight: Int? = 11

        rule.setContent {
            LazyColumn {
                item {
                    Box(
                        modifier =
                            Modifier.size(100.dp).minMorphingHeightConsumer {
                                initialReportedMorphingHeight = null
                            }
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        rule.waitForIdle()

        // New morphing height should not be reported.
        assertThat(initialReportedMorphingHeight).isNotNull()
    }

    @Test
    fun morphingHeightIsReportedToAncestors() {
        var reportedMorphingHeight: Int? = null

        rule.setContent {
            LazyColumn {
                item {
                    Box(
                        modifier =
                            Modifier.size(100.dp)
                                .minMorphingHeightConsumer { reportedMorphingHeight = it }
                                .targetMorphingHeight(scope = this@item)
                    )
                }
            }
        }

        rule.waitForIdle()

        assertThat(reportedMorphingHeight).isNotNull()
    }

    @Test
    fun morphingHeightIsNotReportedToDescendants() {
        var reportedMorphingHeight: Int? = null

        rule.setContent {
            LazyColumn {
                item {
                    Box(
                        modifier =
                            Modifier.size(100.dp)
                                // Misuse of API - targetMorphingHeight must be applied after
                                // consumer.
                                .targetMorphingHeight(scope = this@item)
                                .minMorphingHeightConsumer { reportedMorphingHeight = it }
                    )
                }
            }
        }

        rule.waitForIdle()

        assertThat(reportedMorphingHeight).isNull()
    }

    @Test
    fun morphingHeightIsNotReportedToMultipleAncestors() {
        var reportedMorphingHeight: Int? = null
        val expectedMorphingHeight = 10

        rule.setContent {
            LazyColumn {
                item {
                    Box(
                        modifier =
                            Modifier.size(
                                    with(LocalDensity.current) { expectedMorphingHeight.toDp() }
                                )
                                // This consumer's callback is ignored as traversal stops.
                                .minMorphingHeightConsumer { reportedMorphingHeight = null }
                                // This consumer receives the callback as it is the first one.
                                .minMorphingHeightConsumer { reportedMorphingHeight = it }
                                .targetMorphingHeight(scope = this@item)
                    )
                }
            }
        }

        rule.waitForIdle()

        assertThat(reportedMorphingHeight).isEqualTo(expectedMorphingHeight)
    }
}
