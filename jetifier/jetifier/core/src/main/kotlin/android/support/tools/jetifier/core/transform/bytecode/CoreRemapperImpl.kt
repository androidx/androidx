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

import android.support.tools.jetifier.core.map.TypesMap
import android.support.tools.jetifier.core.rules.JavaField
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.transform.TransformationContext
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomClassRemapper
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomRemapper
import android.support.tools.jetifier.core.utils.Log
import org.objectweb.asm.ClassVisitor

/**
 * Applies mappings defined in [TypesMap] during the remapping process.
 */
class CoreRemapperImpl(private val context: TransformationContext) : CoreRemapper {

    companion object {
        const val TAG = "CoreRemapperImpl"
    }

    private val typesMap = context.config.typesMap

    fun createClassRemapper(visitor: ClassVisitor): CustomClassRemapper {
        return CustomClassRemapper(visitor, CustomRemapper(this))
    }

    override fun rewriteType(type: JavaType): JavaType {
        val result = typesMap.types[type]

        if (!context.isEligibleForRewrite(type)) {
            return type
        }

        if (result != null) {
            Log.i(TAG, "  map: %s -> %s", type, result)
            return result
        }

        context.reportNoMappingFoundFailure()
        Log.e(TAG, "No mapping for: " + type)
        return type
    }

    override fun rewriteField(field : JavaField): JavaField {
        val result = typesMap.fields[field]

        if (!context.isEligibleForRewrite(field.owner)) {
            return field
        }

        if (result != null) {
            Log.i(TAG, "  map: %s -> %s", field, result)
            return result
        }

        context.reportNoMappingFoundFailure()
        Log.e(TAG, "No mapping for: " + field)
        return field
    }

}

