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

package androidx.fragment.app

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class SafeTransactionInOnResumeTest {

    @get:Rule
    var mActivityRule = ActivityTestRule<OnResumeTestActivity>(OnResumeTestActivity::class.java)

    @Test()
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    fun onResumeTest() {
        assertTrue(mActivityRule.activity.testResult())
    }
}

class DialogActivity : Activity() {
    companion object {
        private var dialogActivity: Activity? = null
        fun finish() {
            dialogActivity!!.finish()
            dialogActivity = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialogActivity = this
    }
}

class OnResumeTestActivity : FragmentActivity() {
    private var firstResume = true
    private val testFinishedLatch = CountDownLatch(1)
    private var testSuccess = false
    override fun onResume() {
        super.onResume()
        if (firstResume) {
            firstResume = false
            startActivity(Intent(this, DialogActivity::class.java))
        } else {
            testSuccess = !supportFragmentManager.isStateSaved
            testFinishedLatch.countDown()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        DialogActivity.finish()
    }

    fun testResult() = testFinishedLatch.await(1, TimeUnit.MINUTES) && testSuccess
}
