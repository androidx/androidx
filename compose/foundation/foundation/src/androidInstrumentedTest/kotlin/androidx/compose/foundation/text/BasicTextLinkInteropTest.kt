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

package androidx.compose.foundation.text

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.compose.foundation.TestActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextLinkInteropTest {
    @get:Rule val activityRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun interop_multiMeasure_doesNotCauseInfiniteRecomposition_inLinks() {
        activityRule.activityRule.scenario.onActivity {
            val root = LinearLayout(it)
            root.orientation = 1
            root.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            it.setContentView(root)

            val scrollView = ScrollView(it)
            scrollView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            scrollView.clipChildren = false
            scrollView.clipToPadding = false
            scrollView.isFillViewport = true // "true" results in double measurement
            root.addView(scrollView)

            val view = ComposeView(it)
            view.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            scrollView.addView(view)

            view.setContent {
                val annotatedString = buildAnnotatedString {
                    append("Android")
                    withLink(LinkAnnotation.Url("url")) { append(" www.google.com") }
                }
                BasicText(
                    modifier = Modifier.padding(24.dp).testTag("text"),
                    text = annotatedString,
                )
            }
        }

        activityRule.onNodeWithTag("text").assertIsDisplayed()
    }
}
