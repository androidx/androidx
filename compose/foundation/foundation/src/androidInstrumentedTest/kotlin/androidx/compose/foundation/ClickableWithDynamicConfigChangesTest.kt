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

package androidx.compose.foundation

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClickableWithDynamicConfigChangesTest {

    @get:Rule
    val rule: ComposeContentTestRule =
        createAndroidComposeRule<TestActivityWithScreenLayoutConfigChanges>()

    @Test
    fun click_viewAddedAndRemovedWithRecomposerCancelledAndRecreated_clickStillWorks() {
        lateinit var grandParent: ViewGroup
        lateinit var parentComposeView: ComposeView

        var counter = 0

        rule.setContent {
            val view = LocalView.current
            parentComposeView = view.parent as ComposeView
            grandParent = parentComposeView.parent as ViewGroup

            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable {
                            ++counter
                        }
                )
            }
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        rule.runOnUiThread {
            grandParent.removeView(parentComposeView)
        }

        rule.runOnUiThread {
            (rule as? AndroidComposeTestRule<*, *>)?.cancelAndRecreateRecomposer()
        }

        rule.runOnUiThread {
            // THIS is the right one to cancel!
            parentComposeView.setParentCompositionContext(null)
        }

        rule.runOnUiThread {
            grandParent.addView(parentComposeView)
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }
}
