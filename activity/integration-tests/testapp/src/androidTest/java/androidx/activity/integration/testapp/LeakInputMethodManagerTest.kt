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

package androidx.activity.integration.testapp

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class LeakInputMethodManagerTest {

    @get:Rule
    val activityRule = ActivityTestRule(LeakingActivity::class.java)

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