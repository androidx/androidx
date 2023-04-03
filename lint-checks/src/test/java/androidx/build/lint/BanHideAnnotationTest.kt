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

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanHideAnnotationTest : AbstractLintDetectorTest(
    useDetector = BanHideAnnotation(),
    useIssues = listOf(BanHideAnnotation.ISSUE),
) {

    private val fileWithHideInJavadoc = java(
        """
/**
 * @hide
 */
public class HideClass {

    /**
     * @hide
     */
    public static final int HIDE = 0;

    /**
     * @hide
     */
    public static void hide() {}
}
        """.trimIndent()
    )

    @Test
    fun `Detection of Hide annotation in Javadoc`() {
        val input = arrayOf(fileWithHideInJavadoc)

        /* ktlint-disable max-line-length */
        val expected = """
src/HideClass.java:4: Error: @hide is not allowed in Javadoc [BanHideAnnotation]
public class HideClass {
             ~~~~~~~~~
src/HideClass.java:9: Error: @hide is not allowed in Javadoc [BanHideAnnotation]
    public static final int HIDE = 0;
                            ~~~~
src/HideClass.java:14: Error: @hide is not allowed in Javadoc [BanHideAnnotation]
    public static void hide() {}
                       ~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }
}