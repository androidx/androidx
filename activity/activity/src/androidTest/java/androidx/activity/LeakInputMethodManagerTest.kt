/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LeakInputMethodManagerTest {

    @Suppress("DEPRECATION")
    val activityRule = androidx.test.rule.ActivityTestRule(LeakingActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess())
        .around(activityRule)

    @Test
    fun leakThroughRemovedEditText() {
        activityRule.runOnUiThread {
            activityRule.activity.removeEditText()
        }
        activityRule.activity.blockingFinish()
    }
}

class LeakingActivity : ComponentActivity() {
    private val timeout = 10L // sec
    private val latch = CountDownLatch(1)
    private lateinit var editText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        editText = EditText(this)
        layout.addView(editText, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        layout.addView(TextView(this), ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(layout)
    }

    fun blockingFinish() {
        finish()
        latch.await(timeout, TimeUnit.SECONDS)
    }

    fun removeEditText() = (editText.parent as ViewGroup).removeView(editText)

    override fun onDestroy() {
        super.onDestroy()
        latch.countDown()
    }
}
