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

package androidx.appcompat.lint.widget

import androidx.appcompat.widget.SwitchUsageXmlDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

@Suppress("UnstableApiUsage")
class SwitchUsageXmlDetectorTest {
    @Test
    fun testUsingAppCompatSwitch() {
        val layout = LintDetectorTest.xml(
            "layout/switch.xml",
            """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    <androidx.appcompat.widget.SwitchCompat
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
</LinearLayout>
        """
        ).indented().within("res")

        // We expect the definition of the SwitchCompat to not be flagged
        TestLintTask.lint().files(
            layout
        ).issues(SwitchUsageXmlDetector.USING_CORE_SWITCH_XML)
            .run()
            .expectClean()
    }

    @Test
    fun testUsingCoreSwitch() {
        val layout = LintDetectorTest.xml(
            "layout/switch.xml",
            """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    <Switch
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />
</LinearLayout>
        """
        ).indented().within("res")

        // We expect the definition of the core Switch to be flagged
        /* ktlint-disable max-line-length */
        TestLintTask.lint().files(
            layout
        ).issues(SwitchUsageXmlDetector.USING_CORE_SWITCH_XML)
            .run()
            .expect(
                """
res/layout/switch.xml:6: Warning: Use SwitchCompat from AppCompat or MaterialSwitch from Material library [UseSwitchCompatOrMaterialXml]
    <Switch
    ^
0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }
}
