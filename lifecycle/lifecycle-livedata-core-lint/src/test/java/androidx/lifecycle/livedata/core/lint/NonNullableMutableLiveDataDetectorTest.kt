/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.lifecycle.livedata.core.lint

import androidx.lifecycle.lint.NonNullableMutableLiveDataDetector
import androidx.lifecycle.livedata.core.lint.stubs.STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NonNullableMutableLiveDataDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = NonNullableMutableLiveDataDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(NonNullableMutableLiveDataDetector.ISSUE)

    private fun check(vararg files: TestFile): TestLintResult {
        return lint().files(*files, *STUBS).testModes(TestMode.DEFAULT)
            .run()
    }

    @Test
    fun pass() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean>()
                    val x = true
                    liveData.value = x
                    liveData.postValue(bar(5))
                    val myLiveData = MyLiveData()
                    liveData.value = x
                }

                fun bar(x: Int): Boolean {
                    return x > 0
                }
            """
            ).indented(),
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyLiveData : MyLiveData2()
                open class MyLiveData2 : GenericLiveData<Boolean>()
                open class GenericLiveData<T> : MutableLiveData<T>()
            """
            ).indented()
        ).expectClean()
    }

    @Test
    fun mutableListAssignmentPass() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val lists = MutableLiveData<List<Int>>()
                    val map = HashMap<Int, Int>()

                    map[1] = 1

                    lists.value = map.values.toMutableList()
                }
            """
            ).indented()
        ).expectClean()
    }

    @Test
    fun helperMethodFails() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean>()
                    liveData.value = bar(5)
                }

                fun bar(x: Int): Boolean? {
                    if (x > 0) return true
                    return null
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/test.kt:7: Error: Expected non-nullable value [NullSafeMutableLiveData]
    liveData.value = bar(5)
                     ~~~~~~
1 errors, 0 warnings
        """
        )
    }

    @Test
    fun variableAssignmentFails() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean>()
                    val bar: Boolean? = null
                    liveData.value = bar
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/test.kt:8: Error: Expected non-nullable value [NullSafeMutableLiveData]
    liveData.value = bar
                     ~~~
1 errors, 0 warnings
        """
        ).expectFixDiffs(
            """
Fix for src/com/example/test.kt line 8: Change `LiveData` type to nullable:
@@ -6 +6
-     val liveData = MutableLiveData<Boolean>()
+     val liveData = MutableLiveData<Boolean?>()
Fix for src/com/example/test.kt line 8: Add non-null asserted (!!) call:
@@ -8 +8
-     liveData.value = bar
+     liveData.value = bar!!
        """
        )
    }

    @Test
    fun nullLiteralFailField() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                val liveDataField = MutableLiveData<Boolean>()

                fun foo() {
                    liveDataField.value = null
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/test.kt:8: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
    liveDataField.value = null
                          ~~~~
1 errors, 0 warnings
        """
        )
    }

    @Test
    fun nullLiteralFailMultipleFields() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                val liveDataField = MutableLiveData<Boolean>()
                val secondLiveDataField: MutableLiveData<String> = MutableLiveData()
                val thirdLiveDataField: MutableLiveData<String> = MutableLiveData<String>("Value")

                fun foo() {
                    liveDataField.value = null
                    secondLiveDataField.value = null
                    thirdLiveDataField.value = null
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/test.kt:10: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
    liveDataField.value = null
                          ~~~~
src/com/example/test.kt:11: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
    secondLiveDataField.value = null
                                ~~~~
src/com/example/test.kt:12: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
    thirdLiveDataField.value = null
                               ~~~~
3 errors, 0 warnings
        """
        )
    }

    @Test
    fun nullLiteralFailMultipleFieldsDifferentNullability() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                val liveDataField = MutableLiveData<Boolean>()
                val secondLiveDataField = MutableLiveData<String?>()

                fun foo() {
                    liveDataField.value = false
                    secondLiveDataField.value = null
                }
            """
            ).indented()
        ).expectClean()
    }

    @Test
    fun nullLiteralFailMultipleAssignment() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                val liveDataField = MutableLiveData<Boolean>()

                fun foo() {
                    liveDataField.value = false
                    liveDataField.value = null
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/test.kt:9: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
    liveDataField.value = null
                          ~~~~
1 errors, 0 warnings
        """
        )
    }

    @Test
    fun nullLiteralFailFieldAndIgnore() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                val liveDataField = MutableLiveData<Boolean>()
                val ignoreThisField = ArrayList<String>(arrayListOf("a", "b"))

                fun foo() {
                    liveDataField.value = null
                    ignoreThisField[0] = null
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/test.kt:9: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
    liveDataField.value = null
                          ~~~~
1 errors, 0 warnings
        """
        )
    }

    @Test
    fun nullLiteralFieldApply() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyClass {
                    val liveDataField = MutableLiveData<Boolean>().apply { value = null }

                    fun foo() {
                        liveDataField.value = false
                    }
                }
            """
            ).indented()
        ).expectClean()
    }

    @Test
    fun companionObjectCheck() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyClass {
                    companion object {
                        val liveDataField = MutableLiveData(true)
                    }

                    fun foo() {
                        liveDataField.value = false
                    }
                }
            """
            ).indented()
        ).expectClean()
    }

    @Test
    fun nullLiteralFailFieldAndLocalVariable() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                val liveDataField = MutableLiveData<Boolean>()

                fun foo() {
                    liveDataField.value = null
                    val liveDataVariable = MutableLiveData<Boolean>()
                    liveDataVariable.value = null
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/test.kt:8: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
    liveDataField.value = null
                          ~~~~
src/com/example/test.kt:10: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
    liveDataVariable.value = null
                             ~~~~
2 errors, 0 warnings
        """
        )
    }

    @Test
    fun nullLiteralQuickFix() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                fun foo() {
                    val liveData = MutableLiveData<Boolean>()
                    liveData.value = null
                }
            """
            ).indented()
        ).expectFixDiffs(
            """
Fix for src/com/example/test.kt line 7: Change `LiveData` type to nullable:
@@ -6 +6
-     val liveData = MutableLiveData<Boolean>()
+     val liveData = MutableLiveData<Boolean?>()
        """
        )
    }

    @Test
    fun classHierarchyTest() {
        check(
            kotlin(
                """
                package com.example

                fun foo() {
                    val liveData = MyLiveData()
                    val bar: Boolean? = true
                    liveData.value = bar
                }
            """
            ).indented(),
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyLiveData : MyLiveData2()
                open class MyLiveData2 : GenericLiveData<Boolean>()
                open class GenericLiveData<T> : MutableLiveData<T>()
            """
            ).indented()
        ).expect(
            """
src/com/example/test.kt:6: Error: Expected non-nullable value [NullSafeMutableLiveData]
    liveData.value = bar
                     ~~~
1 errors, 0 warnings
        """
        ).expectFixDiffs(
            """
Fix for src/com/example/test.kt line 6: Add non-null asserted (!!) call:
@@ -6 +6
-     liveData.value = bar
+     liveData.value = bar!!
        """
        )
    }

    @Test
    fun differentClassSameFieldTestFirstNull() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyClass1 {
                    val liveDataField = MutableLiveData<Boolean>()

                    fun foo() {
                        liveDataField.value = null
                    }
                }
            """
            ).indented(),
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyClass2 {
                    val liveDataField = MutableLiveData<Boolean>()

                    fun foo() {
                        liveDataField.value = false
                    }
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/MyClass1.kt:9: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
        liveDataField.value = null
                              ~~~~
1 errors, 0 warnings
        """
        ).expectFixDiffs(
            """
Fix for src/com/example/MyClass1.kt line 9: Change `LiveData` type to nullable:
@@ -6 +6
-     val liveDataField = MutableLiveData<Boolean>()
+     val liveDataField = MutableLiveData<Boolean?>()
        """
        )
    }

    @Test
    fun differentClassSameFieldTestSecondNull() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyClass1 {
                    val liveDataField = MutableLiveData<Boolean>()

                    fun foo() {
                        liveDataField.value = false
                    }
                }
            """
            ).indented(),
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyClass2 {
                    val liveDataField = MutableLiveData<Boolean>()

                    fun foo() {
                        liveDataField.value = null
                    }
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/MyClass2.kt:9: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
        liveDataField.value = null
                              ~~~~
1 errors, 0 warnings
        """
        ).expectFixDiffs(
            """
Fix for src/com/example/MyClass2.kt line 9: Change `LiveData` type to nullable:
@@ -6 +6
-     val liveDataField = MutableLiveData<Boolean>()
+     val liveDataField = MutableLiveData<Boolean?>()
        """
        )
    }

    @Test
    fun nestedClassSameFieldTest() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class MyClass1 {
                    val liveDataField = MutableLiveData<Boolean>()

                    fun foo() {
                        liveDataField.value = false
                    }

                    class MyClass2 {
                        val liveDataField = MutableLiveData<Boolean>()

                        fun foo() {
                            liveDataField.value = null
                        }
                    }
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/MyClass1.kt:16: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
            liveDataField.value = null
                                  ~~~~
1 errors, 0 warnings
        """
        ).expectFixDiffs(
            """
Fix for src/com/example/MyClass1.kt line 16: Change `LiveData` type to nullable:
@@ -13 +13
-         val liveDataField = MutableLiveData<Boolean>()
+         val liveDataField = MutableLiveData<Boolean?>()
        """
        )
    }

    @Test
    fun modifiersFieldTest() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.LiveData
                import androidx.lifecycle.MutableLiveData

                class MyClass1 {
                    internal val firstLiveDataField = MutableLiveData<Boolean>()
                    protected val secondLiveDataField = MutableLiveData<Boolean?>()

                    fun foo() {
                        firstLiveDataField.value = false
                        firstLiveDataField.value = null
                        secondLiveDataField.value = null
                        secondLiveDataField.value = false
                    }
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/MyClass1.kt:12: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
        firstLiveDataField.value = null
                                   ~~~~
1 errors, 0 warnings
        """
        ).expectFixDiffs(
            """
Fix for src/com/example/MyClass1.kt line 12: Change `LiveData` type to nullable:
@@ -7 +7
-     internal val firstLiveDataField = MutableLiveData<Boolean>()
+     internal val firstLiveDataField = MutableLiveData<Boolean?>()
        """
        )
    }

    @Test
    fun implementationClassTest() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.LiveData
                import androidx.lifecycle.MutableLiveData

                interface MyClass2 {
                    val firstLiveDataField : LiveData<Boolean>
                    val secondLiveDataField : LiveData<Boolean?>
                    val thirdLiveDataField : LiveData<Boolean?>
                    val fourLiveDataField : LiveData<List<Boolean>?>
                    val fiveLiveDataField : LiveData<List<Boolean>?>
                }

                class MyClass1 : MyClass2 {
                    override val firstLiveDataField = MutableLiveData<Boolean>()
                    override val secondLiveDataField = MutableLiveData<Boolean?>()
                    override val thirdLiveDataField = MutableLiveData<Boolean?>(null)
                    override val fourLiveDataField = MutableLiveData<List<Boolean>?>(null)
                    override val fiveLiveDataField : MutableLiveData<List<Boolean>?> = MutableLiveData(null)

                    fun foo() {
                        firstLiveDataField.value = false
                        firstLiveDataField.value = null
                        secondLiveDataField.value = null
                        secondLiveDataField.value = false
                        thirdLiveDataField.value = null
                        thirdLiveDataField.value = false
                        fourLiveDataField.value = null
                        fourLiveDataField.value = emptyList()
                        fiveLiveDataField.value = null
                        fiveLiveDataField.value = emptyList()
                    }
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/MyClass2.kt:23: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
        firstLiveDataField.value = null
                                   ~~~~
1 errors, 0 warnings
        """
        ).expectFixDiffs(
            """
Fix for src/com/example/MyClass2.kt line 23: Change `LiveData` type to nullable:
@@ -15 +15
-     override val firstLiveDataField = MutableLiveData<Boolean>()
+     override val firstLiveDataField = MutableLiveData<Boolean?>()
        """
        )
    }

    @Test
    fun extendClassTest() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.LiveData
                import androidx.lifecycle.MutableLiveData

                abstract class MyClass2 {
                    val firstLiveDataField : LiveData<Boolean>
                    val secondLiveDataField : LiveData<Boolean?>
                    val thirdLiveDataField : LiveData<Boolean?>
                    val fourLiveDataField : LiveData<List<Boolean>?>
                    val fiveLiveDataField : LiveData<List<Boolean>>
                }

                class MyClass1 : MyClass2() {
                    override val firstLiveDataField = MutableLiveData<Boolean>()
                    override val secondLiveDataField = MutableLiveData<Boolean?>()
                    override val thirdLiveDataField = MutableLiveData<Boolean?>(null)
                    override val fourLiveDataField = MutableLiveData<List<Boolean>?>(null)
                    override val fiveLiveDataField = MutableLiveData<List<Boolean>>()

                    fun foo() {
                        firstLiveDataField.value = false
                        firstLiveDataField.value = null
                        secondLiveDataField.value = null
                        secondLiveDataField.value = false
                        thirdLiveDataField.value = null
                        thirdLiveDataField.value = false
                        fourLiveDataField.value = null
                        fourLiveDataField.value = emptyList()
                        fiveLiveDataField.value = null
                        fiveLiveDataField.value = emptyList()
                    }
                }
            """
            ).indented()
        ).expect(
            """
src/com/example/MyClass2.kt:23: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
        firstLiveDataField.value = null
                                   ~~~~
src/com/example/MyClass2.kt:30: Error: Cannot set non-nullable LiveData value to null [NullSafeMutableLiveData]
        fiveLiveDataField.value = null
                                  ~~~~
2 errors, 0 warnings
        """
        ).expectFixDiffs(
            """
Fix for src/com/example/MyClass2.kt line 23: Change `LiveData` type to nullable:
@@ -15 +15
-     override val firstLiveDataField = MutableLiveData<Boolean>()
+     override val firstLiveDataField = MutableLiveData<Boolean?>()
Fix for src/com/example/MyClass2.kt line 30: Change `LiveData` type to nullable:
@@ -19 +19
-     override val fiveLiveDataField = MutableLiveData<List<Boolean>>()
+     override val fiveLiveDataField = MutableLiveData<List<Boolean>?>()
        """
        )
    }

    @Test
    fun objectLiveData() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.LiveData

                val foo = object : LiveData<Int>() {
                    private fun bar() {
                        value = 0
                    }
                }
            """
            ).indented()
        ).expectClean()
    }

    @Test
    fun justKotlinObject() {
        check(
            kotlin(
                """
                package com.example

                object Foo
            """
            ).indented()
        ).expectClean()
    }

    @Test
    fun genericParameterDefinition() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData

                class Foo<T>(
                    var target: MutableLiveData<T>
                ) {

                    fun foo(value: T) {
                        target.value = null
                    }
                }
            """
            ).indented()
        ).expectClean()
    }

    @Test
    fun suspendFunction() {
        check(kotlin("""
            package com.example

            import androidx.lifecycle.MutableLiveData

            class Foo(
                var target: MutableLiveData<Boolean>
            ) {

                suspend fun foo() {
                    target.value = nonNullable()
                }

                suspend fun nonNullable() = true
            }
        """).indented()).expectClean()
    }

    @Test
    fun nullableSuspendFunction() {
        check(kotlin("""
            package com.example

            import androidx.lifecycle.MutableLiveData

            class Foo(
                var target: MutableLiveData<String>
            ) {

                suspend fun foo() {
                    target.value = nullable()
                }

                suspend fun nullable(): String? = null
            }
        """).indented()).expect("""
src/com/example/Foo.kt:10: Error: Expected non-nullable value [NullSafeMutableLiveData]
        target.value = nullable()
                       ~~~~~~~~~~
1 errors, 0 warnings
        """)
    }

    @Test
    fun dataClassFromBinary_nonNull() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData
                import some.other.pkg.SomeData

                fun foo() {
                    val liveData = MutableLiveData<SomeData>()
                    val x = SomeData()
                    liveData.value = x
                    liveData.postValue(bar(6))
                }

                fun bar(x: Int): SomeData {
                    return SomeData(extras = x)
                }
            """
            ).indented(),
            DATA_LIB,
        ).expectClean()
    }

    @Test
    fun dataClassFromBinary_nullable() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.MutableLiveData
                import some.other.pkg.SomeData

                fun foo() {
                    val liveData = MutableLiveData<SomeData>()
                    val bar: SomeData? = SomeData()
                    liveData.value = bar
                }
            """
            ).indented(),
            DATA_LIB,
        ).expect(
            """
src/com/example/test.kt:9: Error: Expected non-nullable value [NullSafeMutableLiveData]
    liveData.value = bar
                     ~~~
1 errors, 0 warnings
        """
        ).expectFixDiffs(
            """
Fix for src/com/example/test.kt line 9: Change `LiveData` type to nullable:
@@ -7 +7
-     val liveData = MutableLiveData<SomeData>()
+     val liveData = MutableLiveData<SomeData?>()
Fix for src/com/example/test.kt line 9: Add non-null asserted (!!) call:
@@ -9 +9
-     liveData.value = bar
+     liveData.value = bar!!
        """
        )
    }

    @Test
    fun typeArgumentFromJava() {
        check(
            kotlin(
                """
                package com.example

                import androidx.lifecycle.LiveData
                import some.other.pkg.SomeData

                abstract class Test : LiveData<SomeData>() {
                  abstract val remoteRefreshCounter: RemoteRefreshCounter

                  fun foo() {
                    val counterValue = remoteRefreshCounter.value
                    // This will trigger the detector, but the receiver type is from Java.
                    remoteRefreshCounter.value = (counterValue ?: 1) - 1
                  }
                }
                """
            ).indented(),
            DATA_LIB,
            java(
                """
                package com.example;

                import androidx.lifecycle.MutableLiveData;

                public final class RemoteRefreshCounter extends MutableLiveData<Integer> {
                  public RemoteRefreshCounter() {}
                }
                """
            ).indented(),
        ).expectClean()
    }

    private companion object {
        val DATA_LIB: TestFile =
            bytecode(
                "libs/data.jar",
                kotlin(
                    """
                    package some.other.pkg

                    data class SomeData
                    @JvmOverloads
                    constructor(val items: List<Boolean> = listOf(), val extras: Int = 42)
                """
                ).indented(),
                0x9ae81803,
                """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgEuXiTs7P1UutSMwtyEkVYgtJLS7x
                LlFi0GIAAJY6UNwvAAAA
                """,
                """
                some/other/pkg/SomeData.class:
                H4sIAAAAAAAA/41X3VMTVxT/3c3XZgmwiSDIh6JoDQkaRFrbgl+g1mBQC4pa
                bOsSVlhIdnF3Q/Wl45N/gjPtS2f60Cce6kwFp850qL71L+pDp9NzdpcACVJm
                wr3n3j3nd8/H79w7/PXv738AGMIzgTbHKus5y13Q7dzy0nxuipZXNFeLQQio
                i9qKlitp5nzu1uyiXnRjCAlERwzTcC8ItKQLnkLFNUq5guG4w/m+aYG+2u2R
                whbOqGWVdM0cvuDrHiosWW7JMHOLK+Xc+Er51opulyxtzhkW6C1Y9nxuUXdn
                bc0wnZxmmparuYZF8k3LvVkplUgrYrh62ZGhCBzehmWYrm6bWimXN12brI2i
                E0NCoLW4oBeXAvPbmq2VdVIUOJku1AY7vG1nikHmh/umE2hCs4JGqAKhNK8j
                SCkI4wClqzYdCShojUPCQcqa/tS1NUdA5BNoxyHe7hAIuwuGw3n4QB0owrH6
                NOcLu4V6RX+sVUruGCXItStF17InNHtJt4c50z2BRdEqlSg4L4tjW/INqu0R
                gbheXnaf8SECqXRffUBHcUxBD3o57qhCMZwQkOd1N89V4DTW2uxefDqJjK4G
                KaFE5gWUolVetkzddM9sXwxSkorWMnG1fxe+7ZG3y/vl4R4YCT75+JyfWIH7
                6Q/q7lKjOkJ9+KQETuE0p5OilV3Lpxt3WF89Calpa/dGK0ZpTrdj+ETBOaZi
                8yZy2muP8zI+Iwpqy8u6OSdwKr0Lteu2AlDybRgjjHu+xrIa1V6WF9nyEtW7
                v8fvAPJlVKArnd/b7grbXWXFsb0Vv2DF6wm6zj5maZwSuKA5C2PWnJ5AARlu
                zgmB5BYGXQn6PGfrFlGL/Miz3pcKbmOSkmTrjlfrqP6kopWIna27xfyVwPG9
                7ie6XbTZkk7FkjVbv8pQ/3vJVLETuI8HfMnQKRGPLdSN9YoCB+o6gls9vT/i
                +5fXYWbdI4ET++zb5ObVM6G72pzfJVJ5JUQPiuAhzgPomlui/acGrwZImqOe
                /nvjeY8itUuKpG48V+gnqbRWG3xRViQ5QXOT/O6F3L7xfFAaEKNH5ViqSZZU
                qUNOhVPSQHgglJJpHemQBqLvfo5Kamy8WU1sfb3+/kVoPK428ndP7lWbSCa4
                GpjBKgypifEDajPJ4UFZVTvC7WJAXH//koGSvsZLQXKK5AMsTyarDsjkfEdY
                ltX4ZNt2+K1PitrA8VNjU1YaNvvy9BJRLD5lzJuaW7F1gc7JiukaZT1vrhiO
                QdS5vEUnoinTmdq6YJj6zUp5VrfvML2YFlZRK01rtsHrYPN4LVb1odsB2jjl
                asWlCW05MFOmrIpd1K8ZvDgUYEzXeYMzxJgw1VVCih84mpe91Sye0BylMOM0
                p/iVo7mR1vRWkIVNq3ukx5xozaYa3iCZWUdLJvsabZmu1+h85cE4NDIpo6So
                kKlL6x7fCF3oZnqRxAcLT+JjNl2IVV2Qaa4wH6XAH36xie60Yj8uEWCE5o7u
                8Pc/IPaGurk7QlJUzZzPZLvW8ZHvzAqNYUiKXHULZMduJXESxwiF0XJMeP6S
                +Q1tv1ajiPqbXgSJQPYj8L1N70hYEn3IBO5tB+xc3QdgEtmqN7213oh9e9Bf
                9aC31oPdQJL8bAWVHaWZi6T6lX0L6YGXxy0H/CKq1SKq1SKqO4qY2+FUTRHj
                GCAC+j6eDYrY7RVR5nD9KrLYyXVcw1n//KCODQrJ9FLQKQxwDZJ3fvotzj1Y
                x6epz9dwgWHWcDk15suda7im9q0hv4YbtclMB7Fs91fwoxM4eJHwmTFtPubN
                kU71yAJjvsHUo5FtaT1IajKacYfSwMBtAbDAXUwHzpaD1hnKZH9BJLya/RPS
                j4iEVrMbkCb4hH76e4MZb5NPoXXnT7wIr1bJ3IBwTP4HLTFIierR/M/AZlWG
                8BBf08Gc9xhz4J7nRwTfBIU+RzKbNActnA1FxDq0VzuCoc9VxOagzn51v61r
                Ue+UeHBKKoj2JMmciXhGhKI78f3cx2tyL3vo33mjhac0z9NukWDmZhDKQ8/j
                cZ72FkiEkccilmYgHJRQnkGLg24HdLcp3ki/dm+MOuhzkHFw0sExB1kH/Q5O
                Objr4KGDgoMhUvsPdw4FK1ANAAA=
                """
            )
    }
}
