/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.bytecode

import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.android.tools.build.jetifier.processor.transform.bytecode.asm.CustomRemapper
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.commons.ClassRemapper
import java.nio.file.Path

/**
 * Applies mappings defined in [TypesMap] during the remapping process.
 */
class CoreRemapperImpl(
    private val context: TransformationContext,
    visitor: ClassVisitor
) : CoreRemapper {

    companion object {
        const val TAG = "CoreRemapperImpl"
    }

    private val typesMap = context.config.typesMap

    var changesDone = false
        private set

    val classRemapper = ClassRemapper(visitor, CustomRemapper(this))

    override fun rewriteType(type: JavaType): JavaType {
        val result = context.typeRewriter.rewriteType(type)
        if (result != null) {
            changesDone = changesDone || result != type
            return result
        }

        context.reportNoMappingFoundFailure(TAG, type)
        return type
    }

    override fun rewriteString(value: String): String {
        val type = JavaType.fromDotVersion(value)
        if (!context.config.isEligibleForRewrite(type)) {
            return value
        }

        val mappedType = context.config.typesMap.mapType(type)
        if (mappedType != null) {
            changesDone = changesDone || mappedType != type
            Log.i(TAG, "Map string: '%s' -> '%s'", type, mappedType)
            return mappedType.toDotNotation()
        }

        // We might be working with an internal type or field reference, e.g.
        // AccessibilityNodeInfoCompat.PANE_TITLE_KEY. So we try to remove last segment to help it.
        if (value.contains(".")) {
            val subTypeResult = context.config.typesMap.mapType(type.getParentType())
            if (subTypeResult != null) {
                val result = subTypeResult.toDotNotation() + '.' + value.substringAfterLast('.')
                Log.i(TAG, "Map string: '%s' -> '%s' via type fallback", value, result)
                return result
            }
        }

        // Try rewrite rules
        if (context.useFallbackIfTypeIsMissing) {
            val rewrittenType = context.config.rulesMap.rewriteType(type)
            if (rewrittenType != null) {
                Log.i(TAG, "Map string: '%s' -> '%s' via fallback", value, rewrittenType)
                return rewrittenType.toDotNotation()
            }
        }

        // We do not treat string content mismatches as errors
        Log.i(TAG, "Found string '%s' but failed to rewrite", value)
        return value
    }

    fun rewritePath(path: Path): Path {
        val owner = path.toFile().path.replace('\\', '/').removeSuffix(".class")
        val type = JavaType(owner)

        val result = context.typeRewriter.rewriteType(type)
        if (result == null) {
            context.reportNoMappingFoundFailure("PathRewrite", type)
            return path
        }

        if (result != type) {
            changesDone = true
            return path.fileSystem.getPath(result.fullName + ".class")
        }

        return path
    }
}
