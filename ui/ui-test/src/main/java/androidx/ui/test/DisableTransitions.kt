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

package androidx.ui.test

import androidx.ui.animation.Transition
import androidx.ui.animation.transitionsEnabled
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This rule will disable all [Transition] animations for the test.
 */
class DisableTransitions : TestRule {

    override fun apply(base: Statement, description: Description?): Statement {
        return DisableTransitionsStatement(base)
    }

    inner class DisableTransitionsStatement(
        private val base: Statement
    ) : Statement() {
        override fun evaluate() {
            transitionsEnabled = false
            try {
                base.evaluate()
            } finally {
                transitionsEnabled = true
            }
        }
    }
}