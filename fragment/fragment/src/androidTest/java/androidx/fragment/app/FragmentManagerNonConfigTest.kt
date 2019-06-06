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

import android.os.Build
import androidx.fragment.app.test.NonConfigOnStopActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import androidx.testutils.recreate
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FragmentManagerNonConfigTest {

    @get:Rule
    var activityRule = ActivityTestRule(NonConfigOnStopActivity::class.java)

    /**
     * When a fragment is added during onStop(), it shouldn't show up in non-config
     * state when restored before P, because OnSaveInstanceState was already called.
     */
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O_MR1)
    fun nonConfigStop() {
        val activity = activityRule.recreate()

        // A fragment was added in onStop(), but we shouldn't see it here...
        assertThat(activity.supportFragmentManager.fragments.isEmpty()).isTrue()
    }

    /**
     * When a fragment is added during onStop(), it shouldn't show up in non-config
     * state when restored after (>=) P, because OnSaveInstanceState isn't yet called.
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun nonConfigStopSavingFragment() {
        val activity = activityRule.recreate()

        assertThat(activity.supportFragmentManager.fragments.size).isEqualTo(1)
    }
}
