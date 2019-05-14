/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.test.filters.SmallTest
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.test.android.AndroidUiTestRunner
import com.google.common.truth.Truth
import androidx.compose.composer
import androidx.ui.test.assertDoesNotExist
import androidx.ui.test.assertIsVisible
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class TopAppBarUiTest : AndroidUiTestRunner() {

    private val bigConstraints = DpConstraints(
        minWidth = 0.dp,
        minHeight = 0.dp,
        maxHeight = 1000.dp,
        maxWidth = 1000.dp
    )

    private val defaultHeight = 56.dp

    @Test
    fun topAppBarTest_ExpandsToScreen() {
        var size: PxSize? = null
        setMaterialContent {
            Container(constraints = bigConstraints) {
                OnChildPositioned(onPositioned = { position ->
                    size = position.size
                }) {
                    TopAppBar()
                }
            }
        }
        val dm = activityTestRule.activity.resources.displayMetrics
        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(defaultHeight.toIntPx())
            Truth.assertThat(size?.width?.value?.toInt()).isEqualTo(dm.widthPixels)
        }
    }

    @Test
    fun topAppBarTest_LeadingIconPresent() {
        setMaterialContent {
            TopAppBar()
        }
        findByTag("Leading icon").assertIsVisible()
    }

    @Test
    fun topAppBarTest_TitleTextLabel_noTitle() {
        setMaterialContent {
            TopAppBar()
        }
        findByTag("Title text label").assertDoesNotExist()
    }

    @Test
    fun topAppBarTest_TitleTextLabel_withTitle() {
        val title = "Title"
        setMaterialContent {
            TopAppBar(title = title)
        }
        findByTag("Title text label").assertIsVisible()
        // TODO: I want to assert this is the same as the item above instead of visible
        findByText(title).assertIsVisible()
    }

    @Test
    fun topAppBarTest_TrailingIcons_noIcons() {
        setMaterialContent {
            TopAppBar()
        }
        findByTag("Trailing icon").assertDoesNotExist()
    }

    @Test
    fun topAppBarTest_TrailingIcons_oneIcon() {
        setMaterialContent {
            TopAppBar(icons = listOf(24.dp))
        }
        findByTag("Trailing icon").assertIsVisible()
        findByTag("Overflow icon").assertDoesNotExist()
    }

    @Test
    fun topAppBarTest_TrailingIcons_twoIcons() {
        setMaterialContent {
            TopAppBar(icons = listOf(24.dp, 24.dp))
        }
        // TODO: need API to assert I can find 2 items
        // findByTag("Trailing icon").assertIsVisible()
        findByTag("Overflow icon").assertDoesNotExist()
    }

    @Test
    fun topAppBarTest_TrailingIcons_threeIcons() {
        setMaterialContent {
            TopAppBar(icons = listOf(24.dp, 24.dp, 24.dp))
        }
        // TODO: need API to assert I can find 3 items
        // findByTag("Trailing icon").assertIsVisible()
        findByTag("Overflow icon").assertIsVisible()
    }

    // TODO: test icons are attached to the "end" side
}