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

import android.os.Parcel
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BackStackStateTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(EmptyFragmentTestActivity::class.java)
    private val fragmentManager get() = activityRule.withActivity {
        supportFragmentManager
    }

    @Test
    fun testParcel() {
        val fragment = StrictFragment()
        val backStackRecord = BackStackRecord(fragmentManager).apply {
            add(fragment, "tag")
            addToBackStack("back_stack")
            setReorderingAllowed(true)
            setMaxLifecycle(fragment, Lifecycle.State.STARTED)
        }

        fragmentManager.fragmentStore.setSavedState(fragment.mWho, FragmentState(fragment))
        val backStackState = BackStackState(
            listOf(fragment.mWho),
            listOf(BackStackRecordState(backStackRecord))
        )

        val parcel = Parcel.obtain()
        backStackState.writeToParcel(parcel, 0)
        // Reset for reading
        parcel.setDataPosition(0)
        val restoredBackStackState = BackStackState(parcel)
        assertThat(restoredBackStackState.mFragments)
            .hasSize(1)
        assertThat(restoredBackStackState.mTransactions)
            .hasSize(1)

        val restoredBackStackRecords = restoredBackStackState.instantiate(fragmentManager)
        assertThat(restoredBackStackRecords)
            .hasSize(1)
        assertThat(restoredBackStackRecords[0].mOps[0].mFragment)
            .isInstanceOf(StrictFragment::class.java)
    }
}
