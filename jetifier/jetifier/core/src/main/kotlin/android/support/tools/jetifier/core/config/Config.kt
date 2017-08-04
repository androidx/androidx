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

/**
 * The main and only one configuration that is used by the tool and all its transformers.
 *
 * [restrictToPackagePrefix] Package prefix that limits the scope of the rewriting.
 * [rewriteRules] List of rules that are applied on the byte code to rewrite it.
 */
data class Config(
        val restrictToPackagePrefix: String,
        val rewriteRules: List<RewriteRule>) {

    companion object {
        /** Path to the default config file located within the jar file. */
        const val DEFAULT_CONFIG_RES_PATH = "/default.config"
    }
}
