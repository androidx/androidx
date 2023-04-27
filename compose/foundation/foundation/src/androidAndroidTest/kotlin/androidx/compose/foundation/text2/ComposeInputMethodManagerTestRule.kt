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

package androidx.compose.foundation.text2

import android.view.View
import androidx.compose.foundation.text2.input.internal.ComposeInputMethodManager
import androidx.compose.foundation.text2.input.internal.overrideComposeInputMethodManagerFactoryForTests
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Rule to help setting the factory used to create [ComposeInputMethodManager] instances for tests.
 * Restores the previous factory after the test finishes.
 */
internal class ComposeInputMethodManagerTestRule : TestRule {
    private var initialFactory: ((View) -> ComposeInputMethodManager)? = null

    fun setFactory(factory: (View) -> ComposeInputMethodManager) {
        val previousFactory = overrideComposeInputMethodManagerFactoryForTests(factory)
        if (initialFactory == null) {
            initialFactory = previousFactory
        }
    }

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    // Reset the factory if it was set during the test so the next test gets the
                    // default behavior.
                    initialFactory?.let(::overrideComposeInputMethodManagerFactoryForTests)
                }
            }
        }
}