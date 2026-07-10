/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Owner
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidComposeViewTest {

    @get:Rule val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = 35)
    @Test
    fun testSetRequestedFrameRate_calledWhenVoted() {
        var view: View? = null
        rule.setContent {
            Box(Modifier.size(10.dp))
            view = LocalView.current
        }

        rule.runOnIdle {
            // Initial state should be NaN
            assertTrue(
                "Expected initial requestedFrameRate to be NaN",
                view!!.requestedFrameRate.isNaN(),
            )

            // Vote for 60fps
            (view as Owner).voteFrameRate(60f)
            // Trigger draw
            view!!.invalidate()
        }

        rule.runOnIdle {
            // After draw, it should be 60f
            assertEquals(60f, view!!.requestedFrameRate)
        }

        rule.runOnIdle {
            // Trigger another draw without voting. It should settle back to NaN.
            view!!.invalidate()
        }

        rule.runOnIdle {
            assertTrue(
                "Expected requestedFrameRate to settle back to NaN",
                view!!.requestedFrameRate.isNaN(),
            )
        }
    }

    @SdkSuppress(minSdkVersion = 35)
    @Test
    fun testSetRequestedFrameRate_notCalledUnnecessarily() {
        var view: View? = null
        rule.setContent {
            Box(Modifier.size(10.dp))
            view = LocalView.current
        }

        rule.runOnIdle {
            // Initial state should be NaN
            assertTrue(view!!.requestedFrameRate.isNaN())

            // MANUALLY set a different value on the view.
            // If AndroidComposeView calls setRequestedFrameRate(NaN) on every frame,
            // it will overwrite this value.
            view!!.requestedFrameRate = 120f
        }

        rule.runOnIdle {
            // Trigger a redraw
            view!!.invalidate()
        }

        rule.runOnIdle {
            // It should NOT have been overwritten because currentFrameRate is NaN
            // and lastSetFrameRate was also NaN (from initial draw).
            assertEquals(
                "Requested frame rate was unnecessarily overwritten!",
                120f,
                view!!.requestedFrameRate,
            )
        }
    }
}
