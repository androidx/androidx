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

package androidx.compose.ui.scrollcapture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests real Foundation scrollable components' integration with scroll capture.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class ScrollCaptureIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    private val captureTester = ScrollCaptureTester(rule)

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        ComposeFeatureFlag_LongScreenshotsEnabled = true
    }

    @After
    fun tearDown() {
        @Suppress("DEPRECATION")
        ComposeFeatureFlag_LongScreenshotsEnabled = false
    }

    @Test
    fun search_findsVerticalScrollModifier() {
        captureTester.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .size(10.toDp())
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(Modifier.size(100.toDp()))
                }
            }
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).hasSize(1)
        val target = targets.single()
        assertThat(target.localVisibleRect.width()).isEqualTo(10)
        assertThat(target.localVisibleRect.height()).isEqualTo(10)
    }

    @Test
    fun search_findsLazyColumn() {
        captureTester.setContent {
            with(LocalDensity.current) {
                LazyColumn(Modifier.size(10.toDp())) {
                    item {
                        Box(Modifier.size(100.toDp()))
                    }
                }
            }
        }

        val targets = captureTester.findCaptureTargets()
        assertThat(targets).hasSize(1)
        val target = targets.single()
        assertThat(target.localVisibleRect.width()).isEqualTo(10)
        assertThat(target.localVisibleRect.height()).isEqualTo(10)
    }
}
