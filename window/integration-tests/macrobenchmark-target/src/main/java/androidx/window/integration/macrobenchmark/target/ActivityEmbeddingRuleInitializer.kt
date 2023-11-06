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

package androidx.window.integration.macrobenchmark.target

import android.content.ComponentName
import android.content.Context
import androidx.startup.Initializer
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitPairFilter
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.SplitRule.FinishBehavior

/**
 * Initializes activity embedding rules for performance tests.
 */
class ActivityEmbeddingRuleInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        RuleController.getInstance(context).apply {
            if (USE_XML_RULES) {
                setRules(RuleController.parseRules(context, R.xml.main_split_config))
            }

            repeat(DUMMY_RULE_COUNT) { index ->
                val pairFilters: MutableSet<SplitPairFilter> = HashSet()
                pairFilters.add(
                    SplitPairFilter(
                        ComponentName(context, Activity1::class.java.name),
                        ComponentName("package $index", "cls $index"), null
                    )
                )
                val rule = SplitPairRule.Builder(pairFilters)
                    .setMinWidthDp(600)
                    .setMinHeightDp(0)
                    .setMinSmallestWidthDp(0)
                    .setFinishPrimaryWithSecondary(FinishBehavior.NEVER)
                    .setFinishSecondaryWithPrimary(FinishBehavior.NEVER)
                    .setClearTop(true)
                    .build()
                addRule(rule)
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

    companion object {
        const val DUMMY_RULE_COUNT = 0
        const val USE_XML_RULES = true
    }
}
