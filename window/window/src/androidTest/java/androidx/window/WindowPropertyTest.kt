/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class WindowPropertyTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(TestActivity::class.java)

    @Test
    fun test_property_activity_embedding_allow_system_override() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // No-op, but to suppress lint
            return
        }
        activityRule.scenario.onActivity { activity ->
            // Should be true as defined in AndroidManifest.xml
            assertTrue(
                getProperty(
                    activity,
                    WindowProperty.PROPERTY_ACTIVITY_EMBEDDING_ALLOW_SYSTEM_OVERRIDE
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Throws(PackageManager.NameNotFoundException::class)
    private fun getProperty(context: Context, propertyName: String): Boolean {
        val packageManager = context.packageManager
        val property = packageManager.getProperty(
            propertyName, context.applicationContext.packageName)
        if (!property.isBoolean) {
            throw IllegalStateException("Property=$propertyName must have a boolean value")
        }
        return property.boolean
    }
}