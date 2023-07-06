/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Intent
import android.os.Bundle
import androidx.window.embedding.ActivityFilter
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitPlaceholderRule
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ADJACENT

/**
 * Example trampoline activity that launches a split and finishes itself.
 */
class SplitActivityTrampoline : SplitActivityBase() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityFilters = setOf(ActivityFilter(componentName(
            "androidx.window.demo.embedding.SplitActivityTrampolineTarget"), null))
        val placeholderIntent = Intent()
        placeholderIntent.component =
            componentName("androidx.window.demo.embedding.SplitActivityPlaceholder")
        val defaultSplitAttributes = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(SPLIT_RATIO))
            .build()
        val placeholderRule = SplitPlaceholderRule.Builder(activityFilters, placeholderIntent)
            .setMinWidthDp(MIN_SPLIT_WIDTH_DP)
            .setMinHeightDp(0)
            .setMinSmallestWidthDp(0)
            .setFinishPrimaryWithPlaceholder(ADJACENT)
            .setDefaultSplitAttributes(defaultSplitAttributes)
            .build()
        RuleController.getInstance(this).addRule(placeholderRule)
        val activityIntent = Intent()
        activityIntent.component = componentName(
            "androidx.window.demo.embedding.SplitActivityTrampolineTarget")
        startActivity(activityIntent)

        finish()
    }
}
