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
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BackStackStateTest {

    @get:Rule
    var activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)
    private val fragmentManager get() = activityRule.activity.supportFragmentManager
            as FragmentManagerImpl

    @Test
    fun testParcel() {
        val fragment = StrictFragment()
        val backStackRecord = BackStackRecord(fragmentManager).apply {
            add(fragment, "tag")
            addToBackStack("back_stack")
            setReorderingAllowed(true)
        }
        val backStackState = BackStackState(backStackRecord)
        val parcel = Parcel.obtain()
        backStackState.writeToParcel(parcel, 0)
        // Reset for reading
        parcel.setDataPosition(0)
        val restoredBackStackState = BackStackState(parcel)
        assertThat(restoredBackStackState.mOps).asList()
            .containsExactlyElementsIn(backStackState.mOps.asList())
        assertThat(restoredBackStackState.mFragmentWhos)
            .containsExactlyElementsIn(backStackState.mFragmentWhos)
        assertThat(restoredBackStackState.mReorderingAllowed)
            .isEqualTo(backStackState.mReorderingAllowed)
    }
}
