/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.Context
import androidx.annotation.XmlRes
import androidx.window.embedding.RuleController.Companion.parseRules

/**
 * The controller to manage [EmbeddingRule]s. It supports:
 * - [addRule]
 * - [removeRule]
 * - [setRules]
 * - [parseRules]
 * - [clearRules]
 * - [getRules]
 *
 * **Note** that this class is recommended to be configured in [androidx.startup.Initializer] or
 * [android.app.Application.onCreate], so that the rules are applied early in the application
 * startup before any activities complete initialization. The rule updates only apply to future
 * [android.app.Activity] launches and do not apply to already running activities.
 */
class RuleController internal constructor(private val embeddingBackend: EmbeddingBackend) {

    // TODO(b/258356512): Make this API a make this a coroutine API that returns
    //  Flow<Set<EmbeddingRule>>.
    /**
     * Returns a copy of the currently registered rules.
     */
    fun getRules(): Set<EmbeddingRule> {
        return embeddingBackend.getRules()
    }

    /**
     * Registers a new rule, or updates an existing rule if the [tag][EmbeddingRule.tag] has been
     * registered with [RuleController]. Will be cleared automatically when the process is stopped.
     *
     * Registering a `SplitRule` may fail if the [SplitController.splitSupportStatus]
     * returns `false`. If not supported, it could be either because
     * [androidx.window.WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED] not enabled
     * in AndroidManifest or the feature not available on the device.
     *
     * Note that registering a new rule or updating the existing rule will **not** be applied to any
     * existing split activity container, and will only be used for new split containers created
     * with future activity launches.
     *
     * @param rule new [EmbeddingRule] to register.
     */
    fun addRule(rule: EmbeddingRule) {
        embeddingBackend.addRule(rule)
    }

    /**
     * Unregisters a rule that was previously registered via [addRule] or [setRules].
     *
     * @param rule the previously registered [EmbeddingRule] to unregister.
     */
    fun removeRule(rule: EmbeddingRule) {
        embeddingBackend.removeRule(rule)
    }

    /**
     * Sets a set of [EmbeddingRule]s, which replace all rules registered by [addRule]
     * or [setRules].
     *
     * It's recommended to set the rules via an [androidx.startup.Initializer], or
     * [android.app.Application.onCreate], so that they are applied early in the application
     * startup before any activities appear.
     *
     * The [EmbeddingRule]s can be parsed from [parseRules] or built with rule Builders, which are:
     * - [SplitPairRule.Builder]
     * - [SplitPlaceholderRule.Builder]
     * - [ActivityRule.Builder]
     *
     * Registering `SplitRule`s may fail if the [SplitController.splitSupportStatus]
     * returns `false`. If not supported, it could be either because
     * [androidx.window.WindowProperties.PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED] not enabled
     * in AndroidManifest or the feature not available on the device.
     *
     * Note that updating the existing rules will **not** be applied to any existing split activity
     * container, and will only be used for new split containers created with future activity
     * launches.
     *
     * @param rules The [EmbeddingRule]s to set
     * @throws IllegalArgumentException if [rules] contains two [EmbeddingRule]s with the same
     * [EmbeddingRule.tag].
     */
    fun setRules(rules: Set<EmbeddingRule>) {
        embeddingBackend.setRules(rules)
    }

    /** Clears the rules previously registered by [addRule] or [setRules]. */
    fun clearRules() {
        embeddingBackend.setRules(emptySet())
    }

    companion object {
        /**
         * Obtains an instance of [RuleController].
         *
         * @param context the [Context] to initialize the controller with
         */
        @JvmStatic
        fun getInstance(context: Context): RuleController {
            val applicationContext = context.applicationContext
            val backend = EmbeddingBackend.getInstance(applicationContext)
            return RuleController(backend)
        }

        /**
         * Parses [EmbeddingRule]s from XML rule definitions.
         *
         * The [EmbeddingRule]s can then set by [setRules].
         *
         * @param context the context that contains the XML rule definition resources
         * @param staticRuleResourceId the resource containing the static split rules.
         * @throws IllegalArgumentException if any of the rules in the XML are malformed.
         */
        @JvmStatic
        fun parseRules(context: Context, @XmlRes staticRuleResourceId: Int): Set<EmbeddingRule> =
            RuleParser.parseRules(context.applicationContext, staticRuleResourceId) ?: emptySet()
    }
}
