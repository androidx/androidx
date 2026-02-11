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

package androidx.compose.ui.tooling.animation.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.tooling.animation.AnimationSearch
import androidx.compose.ui.tooling.animation.Utils.addAnimations
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalAnimationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class InfiniteTransitionSearchInfoTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun searchInfoFound() {
        val search = AnimationSearch.InfiniteTransitionSearch {}
        rule.addAnimations(search) { rememberInfiniteTransition() }
        assertEquals(1, search.animations.size)

        search.animations.first().let { searchInfo ->
            val animation = searchInfo.createAnimation()
            assertNotNull(animation)
            animation!!
            val clock = searchInfo.createClock(animation)
            assertNotNull(clock)
        }
    }

    @Test
    fun attachAndDetachOverride() {
        val search = AnimationSearch.InfiniteTransitionSearch {}
        rule.addAnimations(search) { rememberInfiniteTransition() }
        assertEquals(1, search.animations.size)

        search.animations.first().let { searchInfo ->
            // Default attached values
            assertNotNull(searchInfo.toolingOverride.override.value)
            assertEquals(0L, searchInfo.toolingOverride.state.value)
            assertEquals(0L, searchInfo.toolingOverride.override.value?.value)

            // Detach
            searchInfo.detach()
            assertNull(searchInfo.toolingOverride.override.value)

            // Attach and jump to the end of the animation
            searchInfo.attach()
            searchInfo.toolingOverride.state.value = 300L
            assertNotNull(searchInfo.toolingOverride.override.value)
            assertEquals(300L, searchInfo.toolingOverride.override.value?.value)
        }
    }
}
