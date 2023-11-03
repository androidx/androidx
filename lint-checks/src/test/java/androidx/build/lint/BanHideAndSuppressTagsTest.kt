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
class BanHideAndSuppressTagsTest : AbstractLintDetectorTest(
    useDetector = BanHideAndSuppressTags(),
    useIssues = listOf(BanHideAndSuppressTags.HIDE_ISSUE, BanHideAndSuppressTags.SUPPRESS_ISSUE),
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
    fun `Detection of Hide tag in Javadoc`() {
        val input = arrayOf(fileWithHideInJavadoc)

        /* ktlint-disable max-line-length */
        val expected = """
src/HideClass.java:4: Error: @hide is not allowed in documentation [BanHideTag]
public class HideClass {
             ~~~~~~~~~
src/HideClass.java:9: Error: @hide is not allowed in documentation [BanHideTag]
    public static final int HIDE = 0;
                            ~~~~
src/HideClass.java:14: Error: @hide is not allowed in documentation [BanHideTag]
    public static void hide() {}
                       ~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    private val fileWithSuppressInKdoc = kotlin(
        """
/**
 * @suppress
 */
public class SuppressClass {

    /**
     * @suppress
     */
    public fun suppress() {}
    /**
    * @suppress
    */
    public val suppressedProperty = 1
}
        """.trimIndent()
    )

    @Test
    fun `Detection of Suppress tag in Kdoc`() {
        val input = arrayOf(fileWithSuppressInKdoc)

        /* ktlint-disable max-line-length */
        val expected = """
src/SuppressClass.kt:4: Error: @suppress is not allowed in documentation [BanSuppressTag]
public class SuppressClass {
             ~~~~~~~~~~~~~
src/SuppressClass.kt:9: Error: @suppress is not allowed in documentation [BanSuppressTag]
    public fun suppress() {}
               ~~~~~~~~
src/SuppressClass.kt:13: Error: @suppress is not allowed in documentation [BanSuppressTag]
    public val suppressedProperty = 1
               ~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }
}
