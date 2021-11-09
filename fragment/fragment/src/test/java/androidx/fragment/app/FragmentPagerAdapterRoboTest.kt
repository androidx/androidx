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

package androidx.fragment.app

import FragmentPagerActivity
import FragmentStatePagerActivity
import android.os.Build
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Build.VERSION_CODES.O_MR1], manifest = Config.NONE)
class FragmentPagerAdapterRoboTest {

    @Test
    fun fragmentPagerTest() {
        val activityController = Robolectric.buildActivity(FragmentPagerActivity::class.java)
            .setup()
        val activity = activityController.get()

        activity.next()

        activityController.destroy()
    }

    @Test
    fun fragmentStatePagerTest() {
        val activityController = Robolectric.buildActivity(FragmentStatePagerActivity::class.java)
            .setup()
        val activity = activityController.get()

        activity.next()

        activityController.destroy()
    }
}
