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

import android.view.View
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test to prevent regressions in SupportFragmentManager fragment replace method. See b/24693644
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentReplaceTest {
    @get:Rule
    val activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    @Test
    fun testReplaceFragment() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        fm.beginTransaction()
            .add(R.id.content, StrictViewFragment(R.layout.fragment_a))
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)
        assertThat(activity.findViewById<View>(R.id.textA)).isNotNull()
        assertThat(activity.findViewById<View>(R.id.textB)).isNull()
        assertThat(activity.findViewById<View>(R.id.textC)).isNull()

        fm.beginTransaction()
            .add(R.id.content, StrictViewFragment(R.layout.fragment_b))
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)
        assertThat(activity.findViewById<View>(R.id.textA)).isNotNull()
        assertThat(activity.findViewById<View>(R.id.textB)).isNotNull()
        assertThat(activity.findViewById<View>(R.id.textC)).isNull()

        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.content, StrictViewFragment(R.layout.fragment_c))
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)
        assertThat(activity.findViewById<View>(R.id.textA)).isNull()
        assertThat(activity.findViewById<View>(R.id.textB)).isNull()
        assertThat(activity.findViewById<View>(R.id.textC)).isNotNull()
    }

    private fun executePendingTransactions(fm: FragmentManager) {
        activityRule.runOnUiThread { fm.executePendingTransactions() }
    }
}
