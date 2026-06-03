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

package androidx.test.shell

import androidx.test.filters.SmallTest
import androidx.test.shell.command.ScreenCommands
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

@SmallTest
class ScreenCommandsTest {

    @Test
    fun testIsKeyboardVisible() {
        assertTrue(ScreenCommands.isKeyboardVisible(DUMPSYS_INPUT_METHOD))
    }

    @Test
    fun testIsKeyboardVisibleNoImeInfo() {
        assertFalse(ScreenCommands.isKeyboardVisible(BAD_DUMPSYS_INPUT_METHOD))
    }

    companion object {
        private const val DUMPSYS_INPUT_METHOD =
            """
            Input Method Manager State:
              mCurrentUserId=0 mCurMethodId=com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME
              mSystemReady=true mInteractive=true

              mCurFocusedWindowToken=android.os.BinderProxy@a1b2c3d
              mCurrentWindowFocused=true
              mCurFocusedWindowName=Window{e4f5a6b u0 com.example.app/com.example.app.MainActivity}
              InputShown=true
        """

        private const val BAD_DUMPSYS_INPUT_METHOD =
            """
            Input Method Manager State:
        """
    }
}
