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

package androidx.camera.testing.impl

import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

public class PriorityRuleChain : TestRule {
    private val ruleEntries = mutableListOf<OrderedRule>()

    private data class OrderedRule(val priority: Int, val rule: TestRule)

    /**
     * Adds a rule to the chain with a specific priority. Lower numbers run OUTER (earlier), higher
     * numbers run INNER (later).
     */
    public fun add(priority: Int, rule: TestRule): PriorityRuleChain {
        ruleEntries.add(OrderedRule(priority, rule))
        return this
    }

    override fun apply(base: Statement, description: Description): Statement {
        // Sort by priority and build the chain
        var chain = RuleChain.emptyRuleChain()
        ruleEntries.sortedBy { it.priority }.forEach { entry -> chain = chain.around(entry.rule) }
        return chain.apply(base, description)
    }
}
