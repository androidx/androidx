/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.core

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.utils.Log

/**
 * Wraps capabilities of [TypesMap] and [RewriteRulesMap] into one place.
 */
class TypeRewriter(private val config: Config, private val useFallback: Boolean) {

    companion object {
        private const val TAG = "TypeRewriter"
    }

    fun rewriteType(type: JavaType): JavaType? {
        val result = config.typesMap.mapType(type)
        if (result != null) {
            Log.i(TAG, "Map: %s -> %s", type, result)
            return result
        }

        if (!config.isEligibleForRewrite(type)) {
            return type
        }

        if (!useFallback) {
            Log.e(TAG, "No mapping for: " + type)
            return null
        }

        val rulesResult = config.rulesMap.rewriteType(type)
        if (rulesResult != null) {
            Log.i(TAG, "Using fallback: %s -> %s", type, rulesResult)
            return rulesResult
        }

        return null
    }
}