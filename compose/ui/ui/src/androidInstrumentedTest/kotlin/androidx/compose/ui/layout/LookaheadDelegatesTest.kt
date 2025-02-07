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

package androidx.compose.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LookaheadDelegatesTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    fun testResetLookaheadPassDelegate() {
        var placeChild by mutableStateOf(true)
        var useLookaheadScope by mutableStateOf(true)
        rule.setContent {
            val movableContent = remember {
                movableContentOf {
                    Row(Modifier.padding(5.dp).requiredSize(200.dp)) {
                        Box(Modifier.size(100.dp))
                        Box(Modifier.size(100.dp))
                        if (!useLookaheadScope) {
                            Box(Modifier.size(100.dp))
                        }
                    }
                }
            }
            Box(
                Modifier.layout { m, c ->
                    m.measure(c).run {
                        layout(width, height) {
                            if (placeChild) {
                                place(0, 0)
                            }
                        }
                    }
                }
            ) {
                // Move moveableContent from a parent in LookaheadScope to a parent that is not
                // in a LookaheadScope.
                if (useLookaheadScope) {
                    Box { LookaheadScope { movableContent() } }
                } else {
                    movableContent()
                }
            }
        }

        rule.waitForIdle()
        placeChild = false
        useLookaheadScope = !useLookaheadScope
        rule.waitForIdle()

        placeChild = true
        rule.waitForIdle()
    }

    @Test
    fun testResetLookaheadPassInvalidations() {
        var invalidateParent by mutableStateOf(false)
        var placeChild by mutableStateOf(true)
        var useLookaheadScope by mutableStateOf(true)
        var largeSize by mutableStateOf(true)
        rule.setContent {
            val movableContent = remember {
                movableContentOf {
                    Row(
                        Modifier.layout { m, c ->
                            if (isLookingAhead) {
                                @Suppress("UNUSED_EXPRESSION")
                                // Trigger a lookahead re-measurement via a state read
                                invalidateParent
                            }
                            m.measure(c).run { layout(width, height) { place(0, 0) } }
                        }
                    ) {
                        Box(
                            Modifier.layout { m, c ->
                                if (isLookingAhead) {
                                    val size = if (largeSize) 100 else 50
                                    val p = m.measure(c)
                                    layout(size, size) { p.place(0, 0) }
                                } else {
                                    m.measure(c).run { layout(width, height) { place(0, 0) } }
                                }
                            }
                        )
                    }
                }
            }
            Box(
                Modifier.layout { m, c ->
                    m.measure(c).run {
                        layout(width, height) {
                            if (placeChild) {
                                place(0, 0)
                            }
                        }
                    }
                }
            ) {
                // Move moveableContent from a parent in LookaheadScope to a parent that is not
                // in a LookaheadScope.
                if (useLookaheadScope) {
                    LookaheadScope { movableContent() }
                } else {
                    movableContent()
                }
            }
        }

        rule.waitForIdle()
        // Invalidate parent's measurement so that the child is only marked as dirty, instead of
        // being independently invalidated
        invalidateParent = true
        // Invalidate lookahead measurement via this size change
        largeSize = !largeSize
        placeChild = false
        useLookaheadScope = !useLookaheadScope

        rule.waitForIdle()

        placeChild = true
        rule.waitForIdle()
    }
}
