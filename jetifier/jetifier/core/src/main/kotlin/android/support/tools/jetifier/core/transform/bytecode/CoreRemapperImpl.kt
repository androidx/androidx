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
class CoreRemapperImpl(private val context: TransformationContext) : CoreRemapper {

    companion object {
        const val TAG = "CoreRemapperImpl"
    }

    private val typesMap = context.config.typesMap

    fun createClassRemapper(visitor: ClassVisitor): ClassRemapper {
        return ClassRemapper(visitor, CustomRemapper(this))
    }

    override fun rewriteType(type: JavaType): JavaType {
        if (!context.isEligibleForRewrite(type)) {
            return type
        }

        val result = typesMap.types[type]
        if (result != null) {
            Log.i(TAG, "  map: %s -> %s", type, result)
            return result
        }

        context.reportNoMappingFoundFailure()
        Log.e(TAG, "No mapping for: " + type)
        return type
    }

    override fun rewriteString(value: String): String {
        val type = JavaType.fromDotVersion(value)
        if (!context.isEligibleForRewrite(type)) {
            return value
        }

        val result = typesMap.types[type]
        if (result != null) {
            Log.i(TAG, "  map string: %s -> %s", type, result)
            return result.toDotNotation()
        }

        // We do not treat string content mismatches as errors
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
            return path.fileSystem.getPath(result.fullName + ".class")
        }

        context.reportNoMappingFoundFailure()
        Log.e(TAG, "No mapping for: " + type)
        return path
    }
}