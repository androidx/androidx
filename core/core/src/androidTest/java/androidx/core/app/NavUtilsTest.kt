/*
 * Copyright 2019 The Android Open Source Project
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

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavUtilsTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule =
        androidx.test.rule.ActivityTestRule(NavUtilsActivity::class.java, false, false)

    @Test
    fun testGetParentActivityName() {
        val activity = activityRule.launchActivity(null)
        assertThat(NavUtils.getParentActivityName(activity))
            .isEqualTo(NavUtilsParentActivity::class.java.name)
    }

    @Test
    fun testGetParentActivityNameWithAlias() {
        val aliasComponentName =
            ComponentName(
                InstrumentationRegistry.getInstrumentation().context,
                "androidx.core.app.NavUtilsAliasActivity"
            )
        val activity =
            activityRule.launchActivity(Intent().apply { component = aliasComponentName })
        assertThat(NavUtils.getParentActivityName(activity))
            .isEqualTo(NavUtilsParentActivity::class.java.name)
    }

    @Test
    fun testGetParentActivityNameWithDisabledAlias() {
        val aliasComponentName =
            ComponentName(
                InstrumentationRegistry.getInstrumentation().context,
                "androidx.core.app.NavUtilsAliasActivity"
            )
        val activity =
            activityRule.launchActivity(Intent().apply { component = aliasComponentName })
        val packageManager = activity.packageManager
        packageManager.setComponentEnabledSetting(
            aliasComponentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        try {
            assertThat(NavUtils.getParentActivityName(activity))
                .isEqualTo(NavUtilsParentActivity::class.java.name)
        } finally {
            packageManager.setComponentEnabledSetting(
                aliasComponentName,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

class NavUtilsParentActivity : Activity()

class NavUtilsActivity : Activity()
