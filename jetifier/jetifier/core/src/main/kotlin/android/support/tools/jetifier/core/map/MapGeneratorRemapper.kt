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
import android.support.tools.jetifier.core.rules.JavaField
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.transform.bytecode.CoreRemapper
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomClassRemapper
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomRemapper
import android.support.tools.jetifier.core.utils.Log
import org.objectweb.asm.ClassVisitor
import java.util.regex.Pattern

/**
 * Hooks to asm remapping to collect data for [TypesMap] by applying all the [RewriteRule]s from the
 * given [config] on all discovered and eligible types and fields.
 */
class MapGeneratorRemapper(private val config: Config) : CoreRemapper {

    companion object {
        private const val TAG : String = "MapGeneratorRemapper"
    }

    private val typesRewritesMap = hashMapOf<JavaType, JavaType>()
    private val fieldsRewritesMap = hashMapOf<JavaField, JavaField>()

    var isMapNotComplete = false
        private set

    /**
     * Ignore mPrefix types and anything that contains $ as these are internal fields that won't be
     * ever referenced.
     */
    private val ignoredFields = Pattern.compile("(^m[A-Z]+.*$)|(.*\\$.*)")

    /**
     * Ignores types ending with '$digit' as these are private inner classes and won't be ever
     * referenced.
     */
    private val ignoredTypes = Pattern.compile("^(.*)\\$[0-9]+$")

    fun createClassRemapper(visitor: ClassVisitor): CustomClassRemapper {
        return CustomClassRemapper(visitor, CustomRemapper(this))
    }

    override fun rewriteType(type: JavaType): JavaType {
        if (!isTypeSupported(type)) {
            return type
        }

        if (typesRewritesMap.contains(type)) {
            return type
        }

        if (isTypeIgnored(type)) {
            return type
        }

        // Try to find a rule
        for (rule in config.rewriteRules) {
            val mappedTypeName = rule.apply(type) ?: continue
            typesRewritesMap.put(type, mappedTypeName)

            Log.i(TAG, "  map: %s -> %s", type, mappedTypeName)
            return mappedTypeName
        }

        isMapNotComplete = true
        Log.e(TAG, "No rule for: " + type)
        typesRewritesMap.put(type, type) // Identity
        return type
    }

    override fun rewriteField(field : JavaField): JavaField {
        if (!isTypeSupported(field.owner)) {
            return field
        }

        if (isTypeIgnored(field.owner) || isFieldIgnored(field)) {
            return field
        }

        if (fieldsRewritesMap.contains(field)) {
            return field
        }

        // Try to find a rule
        for (rule in config.rewriteRules) {
            val mappedFieldName = rule.apply(field) ?: continue
            fieldsRewritesMap.put(field, mappedFieldName)

            Log.i(TAG, "  map: %s -> %s", field, mappedFieldName)
            return mappedFieldName
        }

        isMapNotComplete = true
        Log.e(TAG, "No rule for: " + field)
        fieldsRewritesMap.put(field, field) // Identity
        return field
    }

    fun createTypesMap() : TypesMap {
        return TypesMap(typesRewritesMap, fieldsRewritesMap)
    }

    private fun isTypeSupported(type: JavaType) : Boolean {
        return config.restrictToPackagePrefixes.any{ type.fullName.startsWith(it) }
    }

    private fun isTypeIgnored(type: JavaType) : Boolean {
        return ignoredTypes.matcher(type.fullName).matches()
    }

    private fun isFieldIgnored(field: JavaField) : Boolean {
        return ignoredFields.matcher(field.name).matches()
    }
}