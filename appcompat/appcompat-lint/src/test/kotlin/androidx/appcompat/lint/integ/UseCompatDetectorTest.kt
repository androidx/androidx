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

package androidx.appcompat.lint.integ

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UseCompatDetectorTest {

    @Test
    fun checkCompatSubstitutionsOnActivity() {
        val input = arrayOf(
            javaSample("com.example.android.appcompat.AppCompatLintDemo"),
            javaSample("com.example.android.appcompat.AppCompatLintDemoExt"),
            javaSample("com.example.android.appcompat.CoreActivityExt")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/com/example/android/appcompat/AppCompatLintDemo.java:68: Warning: Use SwitchCompat from AppCompat or MaterialSwitch from Material library [UseSwitchCompatOrMaterialCode]
        Switch mySwitch = new Switch(this);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/android/appcompat/AppCompatLintDemo.java:63: Warning: Use TextViewCompat.setCompoundDrawableTintList() [UseCompatTextViewDrawableApis]
            noop.setCompoundDrawableTintList(csl);
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/example/android/appcompat/AppCompatLintDemo.java:64: Warning: Use TextViewCompat.setCompoundDrawableTintMode() [UseCompatTextViewDrawableApis]
            noop.setCompoundDrawableTintMode(PorterDuff.Mode.DST);
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 3 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun checkCompatSubstitutionsOnWidget() {
        val input = arrayOf(
            javaSample("com.example.android.appcompat.CustomSwitch")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/com/example/android/appcompat/CustomSwitch.java:27: Warning: Use SwitchCompat from AppCompat or MaterialSwitch from Material library [UseSwitchCompatOrMaterialCode]
public class CustomSwitch extends Switch {
                                  ~~~~~~
0 errors, 1 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }
}
