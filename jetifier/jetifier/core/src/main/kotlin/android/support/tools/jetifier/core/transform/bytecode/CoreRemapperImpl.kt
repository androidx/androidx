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
import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.transform.TransformationContext
import android.support.tools.jetifier.core.transform.bytecode.asm.CustomRemapper
import android.support.tools.jetifier.core.utils.Log
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
        if (!context.isEligibleForRewrite(type)) {
            return type
        }

        val result = typesMap.mapType(type)
        if (result != null) {
            changesDone = changesDone || result != type
            Log.i(TAG, "  map: %s -> %s", type, result)
            return result
        }

        if (context.useIdentityIfTypeIsMissing) {
            Log.i(TAG, "No mapping for %s - using identity", type)
        } else {
            context.reportNoMappingFoundFailure()
            Log.e(TAG, "No mapping for: " + type)
        }
        return type
    }

    override fun rewriteString(value: String): String {
        val startsWithAndroidX = context.isInReversedMode && value.startsWith("androidx")

        val type = JavaType.fromDotVersion(value)
        if (!context.isEligibleForRewrite(type)) {
            if (startsWithAndroidX) {
                Log.i(TAG, "Found string '%s' but failed to rewrite", value)
            }
            return value
        }

        val result = typesMap.mapType(type)
        if (result != null) {
            changesDone = changesDone || result != type
            Log.i(TAG, "Map string: '%s' -> '%s'", type, result)
            return result.toDotNotation()
        }

        // We might be working with an internal type or field reference, e.g.
        // AccessibilityNodeInfoCompat.PANE_TITLE_KEY. So we try to remove last segment to help it.
        if (value.contains(".")) {
            val subTypeResult = typesMap.mapType(type.getParentType())
            if (subTypeResult != null) {
                val result = subTypeResult.toDotNotation() + '.' + value.substringAfterLast('.')
                Log.i(TAG, "Map string: '%s' -> '%s' via fallback", value, result)
                return result
            }
        }

        // We do not treat string content mismatches as errors
        Log.i(TAG, "Found string '%s' but failed to rewrite", value)
        return value
    }

    fun rewritePath(path: Path): Path {
        if (!context.rewritingSupportLib) {
            return path
        }

        val owner = path.toFile().path.replace('\\', '/').removeSuffix(".class")
        val type = JavaType(owner)
        if (!context.isEligibleForRewrite(type)) {
            return path
        }

        val result = rewriteType(type)
        if (result != type) {
            changesDone = true
            return path.fileSystem.getPath(result.fullName + ".class")
        }

        if (context.useIdentityIfTypeIsMissing) {
            Log.i(TAG, "No mapping for: %s", type)
            return path
        }

        context.reportNoMappingFoundFailure()
        Log.e(TAG, "No mapping for: %s", type)
        return path
    }
}