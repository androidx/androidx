/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.app

import android.app.ActivityOptions
import android.view.Display
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ActivityOptionsCompatTest {

    @Config(minSdk = 33)
    @Suppress("deprecation")
    @Test
    fun testSetPendingIntentBackgroundActivityStartMode() {
        val activityOptionsCompat = ActivityOptionsCompat.makeBasic()

        activityOptionsCompat.setPendingIntentBackgroundActivityStartMode(
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        )
        activityOptionsCompat.setPendingIntentBackgroundActivityStartMode(
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_DENIED
        )
    }

    @Config(minSdk = 26)
    @Test
    fun testSetLaunchDisplayId() {
        val activityOptionsCompat = ActivityOptionsCompat.makeBasic()
        assertEquals(activityOptionsCompat.launchDisplayId, Display.INVALID_DISPLAY)

        activityOptionsCompat.setLaunchDisplayId(0)
        assertEquals(activityOptionsCompat.launchDisplayId, 0)
    }

    @Config(maxSdk = 25)
    @Test
    fun testGetLaunchDisplayId_pre26() {
        val activityOptionsCompat = ActivityOptionsCompat.makeBasic()
        assertEquals(activityOptionsCompat.launchDisplayId, Display.INVALID_DISPLAY)

        activityOptionsCompat.launchDisplayId = 0
        assertEquals(activityOptionsCompat.launchDisplayId, Display.INVALID_DISPLAY)
    }
}
