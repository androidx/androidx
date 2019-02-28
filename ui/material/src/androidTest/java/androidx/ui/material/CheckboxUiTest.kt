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
package androidx.ui.material.test

import androidx.test.filters.SmallTest
import androidx.ui.core.CraneWrapper
import androidx.ui.material.MaterialTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import androidx.ui.material.Checkbox
import androidx.ui.core.semantics.SemanticsProperties
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.test.assertIsChecked
import androidx.ui.test.assertIsVisible
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.findByTag
import com.google.r4a.composer

@SmallTest
@RunWith(JUnit4::class)
class CheckboxUiTest : AndroidUiTestRunner() {

    @Test
    fun checkBoxTestDemo() {
        // TODO(pavlis): AndroidUiTestRunner should wrap with CraneWrapper automatically once
        // meta-data are fixed
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <Checkbox testTag="myCheckbox"/> // TODO(pavlis): Start using tags
                </MaterialTheme>
            </CraneWrapper>
        }

        // TODO(pavlis): Improve this section
        // This is just an initial demo of testing API.

        // The API below is what developers will be using (still WIP). And what we will use
        // to verify interactions in our widgets. E.g. clickButton -> assertIsChecked. However there
        // should  be also an initial test for each widget that assert the whole semantics, see few
        // lines below.
        // FYI: This is a real working test now
        findByTag("myCheckbox")
            .assertIsVisible()
            .assertIsChecked()

        // Every widget should have test that verifies that its default semantics (when the widget
        // is initialized) is correct. E.g. button has clickable set, or editText is focusable.
        // For that we don't want to use the APIs above as it is easy to forget to verify some
        // property. So because of that we are introducing the assertion below where all fields will
        // be mandatory in the future. That will enforce each widget owner to make assumptions
        // about what their widgets need to provide.
        // TODO(pavlis): This is just a stub that needs implementation.
        findByTag("myCheckbox")
            .assertSemanticsIsEqualTo(SemanticsProperties(
                // List all properties here
            ))
    }
}