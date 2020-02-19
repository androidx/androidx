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

package androidx.appcompat.lint.res

import androidx.appcompat.lint.Stubs
import androidx.appcompat.res.ColorStateListAlphaDetector
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class ColorStateListAlphaDetectorTest {
    @Test
    fun testIncompleteColorStateList() {
        // We expect the definition of the color state list to be flagged since it has
        // app:alpha but no android:alpha on one of the entries. We also expect a matching
        // fix to add android:alpha attribute with the same value as the existing app:alpha one.
        /* ktlint-disable max-line-length */
        lint().files(
            Stubs.COLOR_STATE_LIST
        ).issues(ColorStateListAlphaDetector.NOT_USING_ANDROID_ALPHA)
            .run()
            .expect("""
res/color/color_state_list.xml:4: Error: Must use android:alpha if app:alpha is used [UseAndroidAlpha]
    <item app:alpha="?android:disabledAlpha"
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
         """.trimIndent())
            .expectFixDiffs("""
Fix for res/color/color_state_list.xml line 4: Set alpha="?android:disabledAlpha":
@@ -6 +6
+         android:alpha="?android:disabledAlpha"
            """.trimIndent())
        /* ktlint-enable max-line-length */
    }
}
