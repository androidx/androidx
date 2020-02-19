/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.serialization.compiler.codegen

import androidx.serialization.compiler.nullability.NULLABILITY_ANNOTATIONS
import androidx.serialization.compiler.nullability.Nullability
import androidx.serialization.compiler.processing.isClassPresent
import androidx.serialization.compiler.processing.packageElement
import com.google.auto.common.GeneratedAnnotations
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement

/**
 * Details about the code generation environment, including the generating class and the
 * availability of annotations.
 */
internal data class CodeGenEnvironment(
    val generatingClassName: String? = null,
    val generated: Generated? = null,
    val nullability: Nullability? = null
) {
    constructor(
        processingEnv: ProcessingEnvironment,
        generatingClass: String? = null
    ) : this(
        generatingClass,

        generated = GeneratedAnnotations
            .generatedAnnotation(processingEnv.elementUtils, processingEnv.sourceVersion)
            .map { Generated(it) }
            .orElse(null),

        nullability = NULLABILITY_ANNOTATIONS.find { nullability ->
            processingEnv.isClassPresent(nullability.qualifiedNonNullName) &&
                    processingEnv.isClassPresent(nullability.qualifiedNullableName)
        }
    )

    data class Generated(
        val packageName: String,
        val simpleName: String = "Generated"
    ) {
        constructor(typeElement: TypeElement) : this(
            packageName = typeElement.packageElement.qualifiedName.toString(),
            simpleName = typeElement.simpleName.toString()
        )
    }
}
