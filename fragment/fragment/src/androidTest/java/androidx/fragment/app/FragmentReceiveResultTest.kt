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
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import androidx.core.app.ActivityCompat
import androidx.fragment.app.test.FragmentResultActivity
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.same
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for Fragment startActivityForResult and startIntentSenderForResult.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentReceiveResultTest {
    @get:Rule
    val activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private lateinit var activity: FragmentTestActivity
    private lateinit var fragment: TestFragment

    @Before
    fun setup() {
        activity = activityRule.activity
        fragment = attachTestFragment()
    }

    @Test
    fun testStartActivityForResultOk() {
        startActivityForResult(10, Activity.RESULT_OK, "content 10")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult).isTrue()
        assertThat(fragment.requestCode).isEqualTo(10)
        assertThat(fragment.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(fragment.resultContent).isEqualTo("content 10")
    }

    @Test
    fun testStartActivityForResultCanceled() {
        startActivityForResult(20, Activity.RESULT_CANCELED, "content 20")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult).isTrue()
        assertThat(fragment.requestCode).isEqualTo(20)
        assertThat(fragment.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(fragment.resultContent).isEqualTo("content 20")
    }

    @Test
    fun testStartIntentSenderForResultOk() {
        startIntentSenderForResult(30, Activity.RESULT_OK, "content 30")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult).isTrue()
        assertThat(fragment.requestCode).isEqualTo(30)
        assertThat(fragment.resultCode).isEqualTo(Activity.RESULT_OK)
        assertThat(fragment.resultContent).isEqualTo("content 30")
    }

    @Test
    fun testStartIntentSenderForResultCanceled() {
        startIntentSenderForResult(40, Activity.RESULT_CANCELED, "content 40")

        assertWithMessage("Fragment should receive result").that(fragment.hasResult).isTrue()
        assertThat(fragment.requestCode).isEqualTo(40)
        assertThat(fragment.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(fragment.resultContent).isEqualTo("content 40")
    }

    @Test
    fun testActivityResult_withDelegate() {
        val delegate = mock(ActivityCompat.PermissionCompatDelegate::class.java)

        val data = Intent()
        ActivityCompat.setPermissionCompatDelegate(delegate)

        activityRule.activity.onActivityResult(42, 43, data)

        verify(delegate).onActivityResult(
            same(activityRule.activity), eq(42), eq(43),
            same(data)
        )

        ActivityCompat.setPermissionCompatDelegate(null)
        activityRule.activity.onActivityResult(42, 43, data)
        verifyNoMoreInteractions(delegate)
    }

    private fun attachTestFragment(): TestFragment {
        val fragment = TestFragment()
        activityRule.runOnUiThread {
            activity.supportFragmentManager.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
            activity.supportFragmentManager.executePendingTransactions()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return fragment
    }

    private fun startActivityForResult(
        requestCode: Int,
        resultCode: Int,
        content: String
    ) {
        activityRule.runOnUiThread {
            val intent = Intent(activity, FragmentResultActivity::class.java)
            intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT, content)

            fragment.startActivityForResult(intent, requestCode)
        }
        assertThat(fragment.resultReceiveLatch.await(1, TimeUnit.SECONDS)).isTrue()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun startIntentSenderForResult(
        requestCode: Int,
        resultCode: Int,
        content: String
    ) {
        activityRule.runOnUiThread {
            val intent = Intent(activity, FragmentResultActivity::class.java)
            intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT, content)

            val pendingIntent = PendingIntent.getActivity(activity, requestCode, intent, 0)

            try {
                fragment.startIntentSenderForResult(
                    pendingIntent.intentSender,
                    requestCode, null, 0, 0, 0, null
                )
            } catch (e: IntentSender.SendIntentException) {
                fail("IntentSender failed")
            }
        }
        assertThat(fragment.resultReceiveLatch.await(1, TimeUnit.SECONDS)).isTrue()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    class TestFragment : Fragment() {
        internal var hasResult = false
        internal var requestCode = -1
        internal var resultCode = 100
        internal lateinit var resultContent: String
        internal val resultReceiveLatch = CountDownLatch(1)

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            hasResult = true
            this.requestCode = requestCode
            this.resultCode = resultCode
            resultContent = data!!.getStringExtra(FragmentResultActivity.EXTRA_RESULT_CONTENT)!!
            resultReceiveLatch.countDown()
        }
    }
}
