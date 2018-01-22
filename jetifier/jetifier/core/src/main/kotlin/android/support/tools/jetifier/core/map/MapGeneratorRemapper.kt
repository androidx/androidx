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

package android.support.tools.jetifier.core.map

import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.transform.bytecode.CoreRemapper
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomRemapper
import android.support.tools.jetifier.core.utils.Log
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.commons.ClassRemapper

/**
 * Hooks to asm remapping to collect data for [TypesMap] by applying all the [RewriteRule]s from the
 * given [config] on all discovered and eligible types.
 */
class MapGeneratorRemapper(private val config: Config) : CoreRemapper {

    companion object {
        private const val TAG: String = "MapGeneratorRemapper"
    }

    private val typesRewritesMap = hashMapOf<JavaType, JavaType>()

    var isMapNotComplete = false
        private set

    fun createClassRemapper(visitor: ClassVisitor): ClassRemapper {
        return ClassRemapper(visitor, CustomRemapper(this))
    }

    override fun rewriteType(type: JavaType): JavaType {
        if (!isTypeSupported(type)) {
            return type
        }

        if (typesRewritesMap.contains(type)) {
            return type
        }

        // Try to find a rule
        for (rule in config.rewriteRules) {
            val typeRewriteResult = rule.apply(type)
            if (typeRewriteResult.isIgnored) {
                Log.i(TAG, "Ignoring: " + type)
                return type
            }
            if (typeRewriteResult.result == null) {
                continue
            }
            typesRewritesMap.put(type, typeRewriteResult.result)
            Log.i(TAG, "  map: %s -> %s", type, typeRewriteResult.result)
            return typeRewriteResult.result
        }

        isMapNotComplete = true
        Log.e(TAG, "No rule for: " + type)
        typesRewritesMap.put(type, type) // Identity
        return type
    }

    override fun rewriteString(value: String): String {
        // We don't build map from strings
        return value
    }

    fun createTypesMap(): TypesMap {
        return TypesMap(typesRewritesMap)
    }

    private fun isTypeSupported(type: JavaType): Boolean {
        return config.restrictToPackagePrefixes.any { type.fullName.startsWith(it) }
    }
}