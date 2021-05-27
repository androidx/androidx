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
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
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

        val AMBIGUOUS_STRINGS = setOf(
            JavaType.fromDotVersion("android.support.v4"),
            JavaType.fromDotVersion("android.support.v4.content"),
            JavaType.fromDotVersion("android.support.v4.widget"),
            JavaType.fromDotVersion("android.support.v4.view"),
            JavaType.fromDotVersion("android.support.v4.media"),
            JavaType.fromDotVersion("android.support.v13"),
            JavaType.fromDotVersion("android.support.v13.view"),
            JavaType.fromDotVersion("android.support.v13.app"),
            JavaType.fromDotVersion("android.support.design.widget")
        )
    }

    var changesDone = false
        private set

    val remapper = CustomRemapper(this)
    val classRemapper = object : ClassRemapper(visitor, remapper) {
        override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
            val annotationVisitor = super.visitAnnotation(descriptor, visible)
            return if (descriptor == "Lkotlin/Metadata;")
                KotlinMetadataVisitor(annotationVisitor) else annotationVisitor
        }
    }

    inner class KotlinMetadataVisitor(
        visitor: AnnotationVisitor
    ) : AnnotationVisitor(Opcodes.ASM8, visitor) {
        init {
            remapper.onKotlinAnnotationVisitStart()
        }

        override fun visitEnd() {
            remapper.onKotlinAnnotationVisitEnd()
            super.visitEnd()
        }
    }

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
        val hasDotSeparators = value.contains(".")
        val hasSlashSeparators = value.contains("/")

        if (hasDotSeparators && hasSlashSeparators) {
            // We do not support mix of both separators
            return value
        }

        val type = if (hasDotSeparators) {
            JavaType.fromDotVersion(value)
        } else {
            JavaType(value)
        }

        if (!context.config.isEligibleForRewrite(type)) {
            return value
        }

        // Verify that we did not make an ambiguous mapping, see b/116745353
        if (!context.allowAmbiguousPackages && AMBIGUOUS_STRINGS.contains(type)) {
            throw AmbiguousStringJetifierException(
                "The given artifact contains a string literal " +
                    "with a package reference '$value' that cannot be safely rewritten. " +
                    "Libraries using reflection such as annotation processors need to be " +
                    "updated manually to add support for androidx."
            )
        }

        // Strings map has a priority over types map
        val mappedString = context.config.stringsMap.mapType(type)
        if (mappedString != null) {
            changesDone = changesDone || mappedString != type
            Log.i(TAG, "Map string: '%s' -> '%s'", type, mappedString)
            return if (hasDotSeparators) mappedString.toDotNotation() else mappedString.fullName
        }

        val mappedType = context.config.typesMap.mapType(type)
        if (mappedType != null) {
            changesDone = changesDone || mappedType != type
            Log.i(TAG, "Map string: '%s' -> '%s'", type, mappedType)
            return if (hasDotSeparators) mappedType.toDotNotation() else mappedType.fullName
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
                return if (hasDotSeparators) {
                    rewrittenType.toDotNotation()
                } else {
                    rewrittenType.fullName
                }
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

/**
 * Thrown when jetifier finds a string reference to a package that has ambiguous mapping.
 */
class AmbiguousStringJetifierException(message: String) : Exception(message)
