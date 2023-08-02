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

package androidx.hilt.ext

import androidx.hilt.AndroidXHiltProcessor
import androidx.room.compiler.processing.XProcessingEnv
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec

const val L = "\$L"
const val T = "\$T"
const val N = "\$N"
const val S = "\$S"
const val W = "\$W"

internal fun TypeSpec.Builder.addGeneratedAnnotation(env: XProcessingEnv) = apply {
    env.findGeneratedAnnotation()?.let {
        addAnnotation(
            AnnotationSpec.builder(ClassName.bestGuess(it.asClassName().canonicalName))
                .addMember("value", S, AndroidXHiltProcessor::class.java.canonicalName)
                .build()

        )
    }
}
