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
class BanVisibilityDocTagsTest :
    AbstractLintDetectorTest(
        useDetector = BanVisibilityDocTags(),
        useIssues =
            listOf(
                BanVisibilityDocTags.HIDE_ISSUE,
                BanVisibilityDocTags.SUPPRESS_ISSUE,
                BanVisibilityDocTags.REMOVED_ISSUE,
            ),
    ) {

    private val fileWithHideInJavadoc =
        java(
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
        """
                .trimIndent()
        )

    @Test
    fun `Detection of Hide tag in Javadoc`() {
        val input = arrayOf(fileWithHideInJavadoc)

        val expected =
            """
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
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    private val fileWithSuppressInKdoc =
        kotlin(
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
        """
                .trimIndent()
        )

    @Test
    fun `Detection of Suppress tag in Kdoc`() {
        val input = arrayOf(fileWithSuppressInKdoc)

        val expected =
            """
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
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of removed tag`() {
        val input =
            arrayOf(
                kotlin(
                    """
                    class Foo {
                        /**
                          * A previously useful function.
                          * @removed
                          **/
                        fun foo() = Unit
                    }
                """
                        .trimIndent()
                ),
                java(
                    """
                    /**
                      * Bar class
                      * @removed don't use this
                      */
                    public class Bar {
                        /** @removed */
                        public void bar() {}
                    }
                """
                        .trimIndent()
                )
            )

        val expected =
            """
            src/Bar.java:5: Error: @removed is not allowed in documentation [BanRemovedTag]
            public class Bar {
                         ~~~
            src/Bar.java:7: Error: @removed is not allowed in documentation [BanRemovedTag]
                public void bar() {}
                            ~~~
            src/Foo.kt:6: Error: @removed is not allowed in documentation [BanRemovedTag]
                fun foo() = Unit
                    ~~~
            3 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }
}
