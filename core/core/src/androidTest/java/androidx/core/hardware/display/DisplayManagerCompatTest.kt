/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.hardware.display

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DisplayManagerCompatTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun testGetInstance() {
        val displayManagerA = DisplayManagerCompat.getInstance(context)
        assertNotNull(displayManagerA)

        val displayManagerB = DisplayManagerCompat.getInstance(context)
        assertSame(displayManagerA, displayManagerB)
    }

    @Test
    fun testGetDisplay() {
        val displayManager = DisplayManagerCompat.getInstance(context)
        assertNotNull(displayManager)

        val displays = displayManager.displays
        assertNotNull(displays)

        // If this device has displays, make sure we can obtain them. This is objectively an
        // integration test, but it's the best we can do given the platform's testability.
        displays.forEach { display ->
            val actualDisplay = displayManager.getDisplay(display.displayId)
            assertNotNull(actualDisplay)
        }
    }
}
