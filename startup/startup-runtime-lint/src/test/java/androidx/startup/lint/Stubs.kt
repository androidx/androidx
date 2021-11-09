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

package androidx.startup.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin

object Stubs {
    val TEST_INITIALIZER: TestFile = kotlin(
        "com/example/TestInitializer.kt",
        """
            package com.example

            import androidx.startup.Initializer

            class TestInitializer: Initializer<Unit> {
                override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
            }
        """
    ).indented().within("src")

    val TEST_INITIALIZER_WITH_DEPENDENCIES: TestFile = kotlin(
        "com/example/TestInitializer.kt",
        """
            package com.example

            import androidx.startup.Initializer

            class TestInitializer: Initializer<Unit> {
                override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
                    SecondInitializer::class.java
                )
            }
        """
    ).indented().within("src")

    val TEST_INITIALIZER_2: TestFile = kotlin(
        "com/example/SecondInitializer.kt",
        """
            package com.example

            import androidx.startup.Initializer

            class SecondInitializer: Initializer<Unit> {
                override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
            }
        """
    ).indented().within("src")

    /**
     * The Test Initializer in Java.
     */
    val TEST_INITIALIZER_JAVA: TestFile = java(
        "com/example/TestInitializer.java",
        """
            package com.example;

            import androidx.startup.Initializer;

            class TestInitializer extends Initializer<Void> {

            }
        """
    ).indented().within("src")

    /**
     * The Initializer.
     */
    val INITIALIZER: TestFile = kotlin(
        "androidx/startup/Initializer.kt",
        """
            package androidx.startup
            interface Initializer<out T : Any> {
                fun dependencies(): List<Class<out Initializer<*>>>
            }
        """
    ).indented().within("src")
}
