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
class UsingOnClickInXmlDetectorTest {

    @Test
    fun checkActivityWithClick() {
        val input = arrayOf(
            javaSample("com.example.android.appcompat.ActivityWithClick"),
            xmlSample("layout.view_with_click")
        )

        /* ktlint-disable max-line-length */
        val expected = """
res/layout/view_with_click.xml:26: Warning: Use databinding or explicit wiring of click listener in code [UsingOnClickInXml]
        android:onClick="myButtonClick"
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }
}
