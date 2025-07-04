/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.testing

import android.app.Activity
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FakeActivityPanelEntityTest {
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()

    private lateinit var underTest: FakeActivityPanelEntity

    @Before
    fun setUp() {
        underTest = FakeActivityPanelEntity()
    }

    @Test
    fun activityPanelEntityLaunchActivity_callsImplLaunchActivity() {
        val launchIntent = Intent(activity.applicationContext, Activity::class.java)
        underTest.launchActivity(launchIntent, null)

        assertThat(underTest.launchIntent).isEqualTo(launchIntent)
        assertThat(underTest.launchBundle).isNull()
    }

    @Test
    fun activityPanelEntityMoveActivity_callsImplMoveActivity() {
        underTest.moveActivity(activity)

        assertThat(underTest.movedActivity).isEqualTo(activity)
    }
}
