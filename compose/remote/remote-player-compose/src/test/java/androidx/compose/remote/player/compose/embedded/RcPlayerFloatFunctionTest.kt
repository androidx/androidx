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

package androidx.compose.remote.player.compose.embedded

import android.graphics.Color as AndroidColor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Float functions (`FloatFunctionDefine` / `FloatFunctionCall`) in the embedded player's draws. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerFloatFunctionTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun functionCall_runsBodyWithItsArguments() {
        val bitmap = rule.renderDocument {
            val args = FloatArray(1)
            val function = writer.createFloatFunction(args)
            painter.setColor(AndroidColor.RED).commit()
            writer.drawRect(0f, 0f, args[0], 100f)
            writer.endFloatFunction()
            writer.callFloatFunction(function, 40f)
        }

        assertRed(bitmap, 20, 50)
        assertNotRed(bitmap, 60, 50)
    }

    @Test
    fun functionCall_withNoArguments_runsBody() {
        val bitmap = rule.renderDocument {
            val function = writer.createFloatFunction(FloatArray(0))
            painter.setColor(AndroidColor.RED).commit()
            writer.drawRect(0f, 0f, 100f, 100f)
            writer.endFloatFunction()
            writer.callFloatFunction(function)
        }

        assertRed(bitmap, 50, 50)
    }
}
