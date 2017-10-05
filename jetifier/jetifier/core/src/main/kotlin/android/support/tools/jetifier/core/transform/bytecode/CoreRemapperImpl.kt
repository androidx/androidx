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

package android.support.tools.jetifier.core.transform.bytecode

import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.rules.JavaField
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.rules.RewriteRule
import android.support.tools.jetifier.core.transform.bytecode.asm.CoreRemapper
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomClassRemapper
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomRemapper
import android.support.tools.jetifier.core.utils.Log
import org.objectweb.asm.ClassVisitor
import java.util.regex.Pattern

/**
 * Applies the given collection of [RewriteRule] during the remapping process. Uses caching also.
 */
class CoreRemapperImpl(private val config: Config) : CoreRemapper {

    private val tag = "CoreRemapperImpl"

    private val typesRewritesCache = hashMapOf<JavaType, JavaType>()
    private val fieldsRewritesCache = hashMapOf<JavaField, JavaField>()

    fun createClassRemapper(visitor: ClassVisitor): CustomClassRemapper {
        return CustomClassRemapper(visitor, CustomRemapper(this))
    }

    override fun rewriteType(type: JavaType): JavaType {
        if (!isTypeSupported(type)) {
            return type
        }

        // Try cache
        val cached = typesRewritesCache[type]
        if (cached != null) {
            return cached
        }

        // Try to find a rule
        for (rule in config.rewriteRules) {
            val mappedTypeName = rule.apply(type) ?: continue
            typesRewritesCache.put(type, mappedTypeName)

            Log.i(tag, "  map: %s -> %s", type, mappedTypeName)
            return mappedTypeName
        }

        typesRewritesCache.put(type, type)

        Log.e(tag, "No rule for: " + type)
        return type
    }

    override fun rewriteField(field : JavaField): JavaField {
        if (!isTypeSupported(field.owner)) {
            return field
        }

        // Try cache
        val cached = fieldsRewritesCache[field]
        if (cached != null) {
            return cached
        }

        // Try to find a rule
        for (rule in config.rewriteRules) {
            val mappedFieldName = rule.apply(field) ?: continue
            fieldsRewritesCache.put(field, mappedFieldName)

            Log.i(tag, "  map: %s -> %s", field, mappedFieldName)
            return mappedFieldName
        }

        Log.e(tag, "No rule for: %s", field)
        return field
    }

    private fun isTypeSupported(type: JavaType) : Boolean {
        return config.restrictToPackagePrefixes.any{ type.fullName.startsWith(it) }
    }
}