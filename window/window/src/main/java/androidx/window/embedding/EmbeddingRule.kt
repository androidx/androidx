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

package androidx.window.embedding

/**
 * Base abstract class for activity embedding presentation rules, such as [SplitPairRule] and
 * [ActivityRule]. Allows grouping different rule types together when updating.
 */
abstract class EmbeddingRule internal constructor(
    /**
     * A unique string to identify this [EmbeddingRule], which defaults to `null`.
     * The suggested usage is to set the tag in the corresponding rule builder to be able to
     * differentiate between different rules in the [SplitAttributesCalculatorParams.splitRuleTag].
     * For example, it can be used to compute the right [SplitAttributes] for the right split rule
     * in callback set in [SplitController.setSplitAttributesCalculator].
     *
     * @see androidx.window.embedding.RuleController.addRule
     */
    val tag: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingRule) return false

        return tag == other.tag
    }

    override fun hashCode(): Int {
        return tag.hashCode()
    }
}
