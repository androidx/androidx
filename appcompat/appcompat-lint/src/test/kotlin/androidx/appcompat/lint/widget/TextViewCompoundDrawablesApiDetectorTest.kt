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

import androidx.appcompat.lint.Stubs
import androidx.appcompat.widget.TextViewCompoundDrawablesApiDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class TextViewCompoundDrawablesApiDetectorTest {
    @Test
    fun testSetCompoundDrawableTintList() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.os.Bundle
            import android.widget.TextView
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    val textView = TextView(this)
                    val csl = getResources().getColorStateList(R.color.color_state_list)
                    textView.setCompoundDrawableTintList(csl)
                }
            }
            """
        ).indented().within("src")

        // We expect the call to TextView.setCompoundDrawableTintList to be flagged to use
        // TextViewCompat loading
        /* ktlint-disable max-line-length */
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            customActivity
        ).issues(TextViewCompoundDrawablesApiDetector.NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_APIS)
            .run()
            .expect("""
src/com/example/CustomActivity.kt:11: Warning: Use TextViewCompat.setCompoundDrawableTintList() [UseCompatTextViewDrawableApis]
        textView.setCompoundDrawableTintList(csl)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """.trimIndent())
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testSetCompoundDrawableTintMode() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.graphics.PorterDuff
            import android.os.Bundle
            import android.widget.TextView
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    val textView = TextView(this)
                    textView.setCompoundDrawableTintMode(PorterDuff.Mode.DST)
                }
            }
            """
        ).indented().within("src")

        // We expect the call to TextView.setCompoundDrawableTintMode to be flagged to use
        // TextViewCompat loading
        /* ktlint-disable max-line-length */
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            customActivity
        ).issues(TextViewCompoundDrawablesApiDetector.NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_APIS)
            .run()
            .expect("""
src/com/example/CustomActivity.kt:11: Warning: Use TextViewCompat.setCompoundDrawableTintMode() [UseCompatTextViewDrawableApis]
        textView.setCompoundDrawableTintMode(PorterDuff.Mode.DST)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """.trimIndent())
        /* ktlint-enable max-line-length */
    }
}
