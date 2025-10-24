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

package androidx.xr.compose.subspace.node

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceMeasureAndLayoutDelegate]. */
@RunWith(AndroidJUnit4::class)
class SubspaceMeasureAndLayoutDelegateTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun requestMeasure_alwaysReturnsTrue() {
        val node = SubspaceLayoutNode()
        val delegate = SubspaceMeasureAndLayoutDelegate(node)
        assertTrue(delegate.requestMeasure(node))
    }

    @Test
    fun requestLayout_alwaysReturnsTrue() {
        val node = SubspaceLayoutNode()
        val delegate = SubspaceMeasureAndLayoutDelegate(node)
        assertTrue(delegate.requestLayout(node))
    }
}
