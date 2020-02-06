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

    /**
     * The Test component.
     */
    val TEST_COMPONENT: TestFile = kotlin(
        "com/example/TestComponentInitializer.kt",
        """
            package com.example

            import androidx.startup.ComponentInitializer

            class TestComponentInitializer: ComponentInitializer<Unit> {

            }
        """
    ).indented().within("src")

    /**
     * The Test component in Java.
     */
    val TEST_COMPONENT_JAVA: TestFile = java(
        "com/example/TestComponentInitializer.java",
        """
            package com.example;

            import androidx.startup.ComponentInitializer;

            class TestComponentInitializer extends ComponentInitializer<Void> {

            }
        """
    ).indented().within("src")

    /**
     * The ComponentInitializer.
     */
    val COMPONENT_INITIALIZER: TestFile = kotlin(
        "androidx/startup/ComponentInitializer.kt",
        """
            package androidx.startup
            interface ComponentInitializer<out T : Any> {
            }
        """
    ).indented().within("src")
}
