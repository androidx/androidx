/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.activity

import android.widget.TextView
import androidx.activity.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ContentViewTest {

    @get:Rule
    val activityRule = ActivityTestRule(ContentViewActivity::class.java)

    @Test
    fun testLifecycleObserver() {
        val activity = activityRule.activity
        val inflatedTextView: TextView = activity.findViewById(R.id.inflated_text_view)
        assertThat(inflatedTextView)
            .isNotNull()
    }
}

class ContentViewActivity : ComponentActivity(R.layout.activity_inflates_res)
