/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.config

import android.support.tools.jetifier.core.rules.RewriteRule
import com.google.gson.annotations.SerializedName
import java.util.ArrayList

/**
 * Data model of [Config] for JSON manipulation.
 */
class ConfigJson(
        @SerializedName("restrictToPackagePrefixes")
        val restrictToPackages: ArrayList<String>,

        @SerializedName("rules")
        val rules: ArrayList<RuleJson?>) {

    /** Creates instance of [Config] based on its internal data. */
    fun getConfig() : Config {
        val rulesResult = rules
                .filterNotNull()
                .map { it.getRule() }

        return Config(restrictToPackages.filterNotNull().toList(), rulesResult.toList())
    }

    /**
     * Data model of [RewriteRule] for JSON manipulation.
     */
    data class RuleJson(
            @SerializedName("from")
            val from: String,

            @SerializedName("to")
            val to: String,

            @SerializedName("fieldSelectors")
            val fieldSelectors: ArrayList<String>? = null) {

        /** Creates instance of [RewriteRule] based on its internal data. */
        fun getRule() : RewriteRule {
            val selectors = mutableListOf<String>()
            fieldSelectors?.filterNotNullTo(selectors)

            return RewriteRule(from, to, selectors.toList())
        }

    }
}

