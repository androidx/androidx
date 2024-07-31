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

package androidx.recyclerview.lint

import androidx.recyclerview.lint.Stubs.RECYCLER_VIEW
import androidx.recyclerview.lint.Stubs.VIEW
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class InvalidSetHasFixedSizeTest {

    @Test
    fun testCorrectUseOfHasFixedSize() {
        val resourceIds =
            kotlin(
                    "com/example/R.kt",
                    """
            package com.example

            object R {
                object id {
                    const val my_recycler_view = 0
                }
            }
        """
                )
                .indented()
                .within("src")

        val source =
            kotlin(
                    "com/example/Example.kt",
                    """
            package com.example

            import android.view.View
            import androidx.recyclerview.widget.RecyclerView

            class Example {
                fun main() {
                    val view: View = TODO()
                    val recyclerView = view.findViewById<RecyclerView>(R.id.my_recycler_view)
                    recyclerView?.setHasFixedSize(true)
                }
            }
        """
                )
                .indented()
                .within("src")

        val layoutFile =
            xml(
                    "layout/recycler_view.xml",
                    """<?xml version="1.0" encoding="utf-8"?>
                <androidx.recyclerview.widget.RecyclerView
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    android:id="@+id/my_recycler_view"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"/>
            """
                        .trimIndent()
                )
                .indented()
                .within("res")

        lint()
            .files(VIEW, RECYCLER_VIEW, layoutFile, resourceIds, source)
            .issues(InvalidSetHasFixedSizeDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testCorrectUseOfHasFixedSize2() {
        val resourceIds =
            kotlin(
                    "com/example/R.kt",
                    """
            package com.example

            object R {
                object id {
                    const val my_recycler_view = 0
                }
            }
        """
                )
                .indented()
                .within("src")

        val source =
            kotlin(
                    "com/example/Example.kt",
                    """
            package com.example

            import android.view.View
            import androidx.recyclerview.widget.RecyclerView

            class Example {
                fun main() {
                    val view: View = TODO()
                    val recyclerView = view.findViewById<RecyclerView>(R.id.my_recycler_view)
                    recyclerView?.let {
                        it.setHasFixedSize(true)
                    }
                }
            }
        """
                )
                .indented()
                .within("src")

        val layoutFile =
            xml(
                    "layout/recycler_view.xml",
                    """<?xml version="1.0" encoding="utf-8"?>
                <androidx.recyclerview.widget.RecyclerView
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    android:id="@+id/my_recycler_view"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"/>
            """
                        .trimIndent()
                )
                .indented()
                .within("res")

        lint()
            .files(VIEW, RECYCLER_VIEW, layoutFile, resourceIds, source)
            .issues(InvalidSetHasFixedSizeDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testInCorrectUsageOfFixedSize() {
        val resourceIds =
            kotlin(
                    "com/example/R.kt",
                    """
            package com.example

            object R {
                object id {
                    const val my_recycler_view = 0
                }
            }
        """
                )
                .indented()
                .within("src")

        val source =
            kotlin(
                    "com/example/Example.kt",
                    """
            package com.example

            import android.view.View
            import androidx.recyclerview.widget.RecyclerView

            class Example {
                fun main() {
                    val view: View = TODO()
                    val recyclerView = view.findViewById<RecyclerView>(R.id.my_recycler_view)
                    recyclerView?.setHasFixedSize(true)
                }
            }
        """
                )
                .indented()
                .within("src")

        val layoutFile =
            xml(
                    "layout/recycler_view.xml",
                    """<?xml version="1.0" encoding="utf-8"?>
                <androidx.recyclerview.widget.RecyclerView
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    android:id="@+id/my_recycler_view"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"/>
            """
                        .trimIndent()
                )
                .indented()
                .within("res")

        lint()
            .files(VIEW, RECYCLER_VIEW, layoutFile, resourceIds, source)
            .issues(InvalidSetHasFixedSizeDetector.ISSUE)
            .run()
            .expect(
                """
                src/com/example/Example.kt:10: Error: When using `setHasFixedSize() in an RecyclerView, wrap_content cannot be used as a value for size in the scrolling direction. [InvalidSetHasFixedSize]
                        recyclerView?.setHasFixedSize(true)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testInCorrectUsageOfFixedSize2() {
        val resourceIds =
            kotlin(
                    "com/example/R.kt",
                    """
            package com.example

            object R {
                object id {
                    const val my_recycler_view = 0
                }
            }
        """
                )
                .indented()
                .within("src")

        val source =
            kotlin(
                    "com/example/Example.kt",
                    """
            package com.example

            import android.view.View
            import androidx.recyclerview.widget.RecyclerView

            class Example {
                fun main() {
                    val view: View = TODO()
                    val recyclerView = view.findViewById<RecyclerView>(R.id.my_recycler_view)
                    recyclerView?.setHasFixedSize(true)
                }
            }
        """
                )
                .indented()
                .within("src")

        val layoutFile =
            xml(
                    "layout/recycler_view.xml",
                    """<?xml version="1.0" encoding="utf-8"?>
                <androidx.recyclerview.widget.RecyclerView
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    android:id="@+id/my_recycler_view"
                    android:layout_width="wrap_content"
                    android:layout_height="match_parent"
                    android:orientation="horizontal" />
            """
                        .trimIndent()
                )
                .indented()
                .within("res")

        lint()
            .files(VIEW, RECYCLER_VIEW, layoutFile, resourceIds, source)
            .issues(InvalidSetHasFixedSizeDetector.ISSUE)
            .run()
            .expect(
                """
                src/com/example/Example.kt:10: Error: When using `setHasFixedSize() in an RecyclerView, wrap_content cannot be used as a value for size in the scrolling direction. [InvalidSetHasFixedSize]
                        recyclerView?.setHasFixedSize(true)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testInCorrectUseOfHasFixedSize3() {
        val resourceIds =
            kotlin(
                    "com/example/R.kt",
                    """
            package com.example

            object R {
                object id {
                    const val my_recycler_view = 0
                }
            }
        """
                )
                .indented()
                .within("src")

        val source =
            kotlin(
                    "com/example/Example.kt",
                    """
            package com.example

            import android.view.View
            import androidx.recyclerview.widget.RecyclerView

            class Example {
                fun main() {
                    val view: View = TODO()
                    val recyclerView = view.findViewById<RecyclerView>(R.id.my_recycler_view)
                    setFixedSize(recyclerView)
                }

                private fun setFixedSize(recyclerView: RecyclerView?) {
                    recyclerView?.setHasFixedSize(true)
                }
            }
        """
                )
                .indented()
                .within("src")

        val layoutFile =
            xml(
                    "layout/recycler_view.xml",
                    """<?xml version="1.0" encoding="utf-8"?>
                <androidx.recyclerview.widget.RecyclerView
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    android:id="@+id/my_recycler_view"
                    android:layout_width="wrap_content"
                    android:layout_height="match_parent"/>
            """
                        .trimIndent()
                )
                .indented()
                .within("res")

        lint()
            .files(VIEW, RECYCLER_VIEW, layoutFile, resourceIds, source)
            .issues(InvalidSetHasFixedSizeDetector.ISSUE)
            .skipTestModes(TestMode.JVM_OVERLOADS)
            .run()
            .expect(
                """
                src/com/example/Example.kt:14: Error: When using `setHasFixedSize() in an RecyclerView, wrap_content cannot be used as a value for size in the scrolling direction. [InvalidSetHasFixedSize]
                        recyclerView?.setHasFixedSize(true)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }
}
