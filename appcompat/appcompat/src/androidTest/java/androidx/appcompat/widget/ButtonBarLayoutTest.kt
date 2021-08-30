/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appcompat.widget

import android.widget.LinearLayout
import androidx.appcompat.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ButtonBarLayoutTest {

    @Test
    fun testStacked() {
        ActivityScenario.launch(ButtonBarLayoutActivity::class.java).use { scenario ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            scenario.onActivity { activity ->
                val buttonBar = activity.findViewById<ButtonBarLayout>(R.id.button_bar)
                buttonBar.layoutParams.width = 20
                buttonBar.requestLayout()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val buttonBar = activity.findViewById<ButtonBarLayout>(R.id.button_bar)
                assertEquals(
                    "Layout must be vertical",
                    LinearLayout.VERTICAL,
                    buttonBar.orientation
                )
                assertEquals(
                    "Height should be 30px",
                    30,
                    buttonBar.height
                )
            }
        }
    }

    @Test
    fun testNotStacked() {
        ActivityScenario.launch(ButtonBarLayoutActivity::class.java).use { scenario ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            scenario.onActivity { activity ->
                val buttonBar = activity.findViewById<ButtonBarLayout>(R.id.button_bar)
                buttonBar.layoutParams.width = 50
                buttonBar.requestLayout()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val buttonBar = activity.findViewById<ButtonBarLayout>(R.id.button_bar)
                assertEquals(
                    "Layout must be horizontal",
                    LinearLayout.HORIZONTAL,
                    buttonBar.orientation
                )
                assertEquals(
                    "Height should be 10px",
                    10,
                    buttonBar.height
                )
            }
        }
    }
}
