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

package androidx.window.demo.embedding

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.demo.R
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EXPAND
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.SplitPlaceholderRule
import androidx.window.embedding.SplitRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class SplitAttributesToggleActivityBase : AppCompatActivity() {
    internal lateinit var splitController: SplitController
    internal lateinit var ruleController: RuleController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        splitController = SplitController.getInstance(this)
        ruleController = RuleController.getInstance(this)

        lifecycleScope.launch {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                splitController.splitInfoList(this@SplitAttributesToggleActivityBase)
                    .map { splitInfoList ->
                        if (splitInfoList.isEmpty()) {
                            EXPAND_ATTRS
                        } else {
                            splitInfoList.last().splitAttributes
                        }
                    }.collect { attrs -> updateSplitAttributesText(attrs) }
            }
        }
    }

    private suspend fun updateSplitAttributesText(splitAttributes: SplitAttributes) =
        withContext(Dispatchers.Main) {
            window.decorView.findViewById<TextView>(R.id.activity_pair_split_attributes_text_view)
                .text = resources.getString(R.string.current_split_attributes) + splitAttributes
        }

    /** Returns the [SplitRule] this activity participates in. */
    internal inline fun <reified T : SplitRule> getSplitRule(): T? =
        ruleController.getRules().find { rule ->
            if (rule !is T) {
                return@find false
            }
            when (rule) {
                is SplitPairRule -> {
                    rule.filters.any { filter ->
                        filter.primaryActivityName == componentName ||
                            filter.secondaryActivityName == componentName
                    }
                }

                is SplitPlaceholderRule -> {
                    rule.filters.any { filter -> filter.matchesActivity(this) } ||
                        rule.placeholderIntent.component == componentName
                }

                else -> false
            }
        } as? T?

    companion object {
        internal val EXPAND_ATTRS = SplitAttributes.Builder()
            .setSplitType(SPLIT_TYPE_EXPAND)
            .build()
        internal val CUSTOMIZED_SPLIT_TYPES_TEXT = arrayOf(
            "ratio(0.3)",
            "ratio(0.5)",
            "ratio(0.7)",
            "expand",
        )
        internal val CUSTOMIZED_SPLIT_TYPES_VALUE = arrayOf(
            SplitAttributes.SplitType.ratio(0.3f),
            SplitAttributes.SplitType.SPLIT_TYPE_EQUAL,
            SplitAttributes.SplitType.ratio(0.7f),
            SPLIT_TYPE_EXPAND,
        )
        internal val CUSTOMIZED_LAYOUT_DIRECTIONS_TEXT = arrayOf("locale", "bottom_to_top")
        internal val CUSTOMIZED_LAYOUT_DIRECTIONS_VALUE = arrayOf(
            SplitAttributes.LayoutDirection.LOCALE,
            SplitAttributes.LayoutDirection.BOTTOM_TO_TOP,
        )
    }
}
