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

package androidx.text.vertical.testapp

import android.content.Intent
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests that [DemoActivity] opens the demo named by its intent extra. */
@MediumTest
@RunWith(AndroidJUnit4::class)
class DemoActivityTest {

    @get:Rule val rule = createEmptyComposeRule()

    @Test
    fun demoActivity_demoNameExtra_opensDemoWithAncestors() {
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), DemoActivity::class.java)
                .putExtra("demo_name", "HorizontalText/CJK font")

        ActivityScenario.launch<DemoActivity>(intent).use {
            rule.onNodeWithTag(DEMO_APP_BAR_TITLE_TAG).assertTextEquals("HorizontalText > CJK font")
        }
    }

    @Test
    fun demoActivity_noExtra_opensRoot() {
        ActivityScenario.launch(DemoActivity::class.java).use {
            rule.onNodeWithTag(DEMO_APP_BAR_TITLE_TAG).assertTextEquals("Vertical Text Demos")
        }
    }
}
