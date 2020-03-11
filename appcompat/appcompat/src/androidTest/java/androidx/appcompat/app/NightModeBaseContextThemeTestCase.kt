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

package androidx.appcompat.app

import android.view.InflateException
import android.widget.Toast
import androidx.appcompat.testutils.NightModeActivityTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NightModeBaseContextThemeTestCase {
    @get:Rule
    val rule = NightModeActivityTestRule(NightModeActivity::class.java)

    @Test
    fun testBaseContextShowToast() {
        var result: Exception? = null

        rule.runOnUiThread {
            try {
                Toast.makeText(rule.activity.baseContext, "test", Toast.LENGTH_SHORT).show()
            } catch (e: InflateException) {
                e.printStackTrace()
                result = e
            }
        }

        assertNull("Showed Toast without exception", result)
    }

    @Test
    fun testBaseContextResolveAttribute() {
        var baseContext = rule.activity.baseContext
        var resolved = baseContext.obtainStyledAttributes(
            intArrayOf(android.R.attr.textColorLink))
        assertNotNull("Resolved @attr/textColorLink", resolved.getColorStateList(0))
    }
}
