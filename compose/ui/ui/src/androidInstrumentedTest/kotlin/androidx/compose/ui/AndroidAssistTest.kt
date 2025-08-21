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

package androidx.compose.ui

import android.view.ViewStructure
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.autofill.FakeViewStructure
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAssistTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()
    private lateinit var androidComposeView: AndroidComposeView

    private val contentTag = "content_tag"
    private val accessibilityClassName = "android.view.ViewGroup"

    // Test that the assistStructure only has its classname set on API levels 23 to 27. See
    // b/251152083 and b/320768586 for more information.
    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 27)
    fun verifyAssistStructureSet() {
        val viewStructure: ViewStructure = FakeViewStructure()

        rule.setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            Box(Modifier.testTag(contentTag))
        }

        rule.runOnIdle { androidComposeView.dispatchProvideStructure(viewStructure) }

        // Use FakeViewStructure here in order to test the accessibility class name is set properly.
        Truth.assertThat(viewStructure)
            .isEqualTo(FakeViewStructure().apply { setClassName(accessibilityClassName) })
    }
}
