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

package com.android.tools.build.jetifier.processor.transform.bytecode.asm

import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.processor.transform.bytecode.CoreRemapper
import org.objectweb.asm.commons.Remapper

/**
 * Extends [Remapper] to allow further customizations.
 */
class CustomRemapper(private val remapper: CoreRemapper) : Remapper() {
    private var inKotlinMetadata = false

    fun onKotlinAnnotationVisitStart() {
        require(!inKotlinMetadata)
        inKotlinMetadata = true
    }

    fun onKotlinAnnotationVisitEnd() {
        require(inKotlinMetadata)
        inKotlinMetadata = false
    }

    override fun map(typeName: String): String {
        return remapper.rewriteType(JavaType(typeName)).fullName
    }

    override fun mapPackageName(name: String): String {
        return remapper.rewriteType(JavaType(name)).fullName
    }

    override fun mapValue(value: Any?): Any? {
        val stringVal = value as? String
        if (stringVal == null) {
            return super.mapValue(value)
        }

        fun mapPoolReferenceType(typeDeclaration: String): String {
            if (!typeDeclaration.contains(".")) {
                return remapper.rewriteType(JavaType(typeDeclaration)).fullName
            }

            if (typeDeclaration.contains("/")) {
                // Mixed "." and "/"  - not something we know how to handle
                return typeDeclaration
            }

            val toRewrite = typeDeclaration.replace(".", "/")
            return remapper.rewriteType(JavaType(toRewrite)).toDotNotation()
        }

        if (stringVal.startsWith("L") && stringVal.endsWith(";")) {
            // L denotes a type declaration. For some reason there are references in the constant
            // pool that ASM skips.
            val typeDeclaration = stringVal.substring(1, stringVal.length - 1)
            if (typeDeclaration.isEmpty()) {
                return value
            }

            if (typeDeclaration.contains(";L")) {
                // We have array of constants
                return "L" +
                    typeDeclaration
                        .split(";L")
                        .joinToString(";L") { mapPoolReferenceType(it) } +
                    ";"
            }

            return "L" + mapPoolReferenceType(typeDeclaration) + ";"
        }
        if (inKotlinMetadata) {
            return rewriteIfMethodSignature(stringVal, ::mapPoolReferenceType)
                ?: remapper.rewriteString(stringVal)
        }
        return remapper.rewriteString(stringVal)
    }
}
