/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.health.connect.client

import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** A rule that ignores tests if a given assumption is violated. */
class AssumeRule(private val assumption: Boolean) : TestRule {
    override fun apply(statement: Statement, description: Description?): Statement {
        return object : Statement() {
            @kotlin.Throws(Throwable::class)
            override fun evaluate() {
                Assume.assumeTrue(assumption)
                statement.evaluate()
            }
        }
    }
}
