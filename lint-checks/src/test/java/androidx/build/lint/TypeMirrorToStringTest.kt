/*
 * Copyright 2024 The Android Open Source Project
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
class TypeMirrorToStringTest :
    AbstractLintDetectorTest(
        useDetector = TypeMirrorToString(),
        useIssues = listOf(TypeMirrorToString.ISSUE),
    ) {
    @Test
    fun `Test usage TypeMirror#toString on simple receiver`() {
        val input =
            arrayOf(
                java(
                    """
                        package androidx.test;
                        import javax.lang.model.type.TypeMirror;
                        public class Foo {
                            public String getStringForType(TypeMirror tm) {
                                return tm.toString();
                            }
                        }
                    """
                        .trimIndent()
                )
            )
        val expected =
            """
                src/androidx/test/Foo.java:5: Error: TypeMirror.toString includes annotations [TypeMirrorToString]
                        return tm.toString();
                               ~~~~~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
                Autofix for src/androidx/test/Foo.java line 5: Use TypeName.toString:
                @@ -2 +2
                + import com.squareup.javapoet.TypeName;
                @@ -5 +6
                -         return tm.toString();
                +         return TypeName.get(tm).toString();
            """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of TypeMirror#toString on method call receiver`() {
        val input =
            arrayOf(
                java(
                    """
                        package androidx.test;
                        import javax.lang.model.type.TypeMirror;
                        public class Foo {
                            public TypeMirror getMirror() {
                                return null;
                            }
                            public String getStringForType() {
                                return getMirror().toString();
                            }
                        }
                    """
                        .trimIndent()
                )
            )
        val expected =
            """
                src/androidx/test/Foo.java:8: Error: TypeMirror.toString includes annotations [TypeMirrorToString]
                        return getMirror().toString();
                               ~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
                Autofix for src/androidx/test/Foo.java line 8: Use TypeName.toString:
                @@ -2 +2
                + import com.squareup.javapoet.TypeName;
                @@ -8 +9
                -         return getMirror().toString();
                +         return TypeName.get(getMirror()).toString();
            """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
