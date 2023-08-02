/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen.kotlin

import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import com.squareup.kotlinpoet.PropertySpec

internal class KotlinPropertySpec(
    override val name: String,
    internal val actual: PropertySpec
) : KotlinLang(), XPropertySpec {

    internal class Builder(
        private val name: String,
        internal val actual: PropertySpec.Builder
    ) : KotlinLang(), XPropertySpec.Builder {

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            require(annotation is KotlinAnnotationSpec)
            actual.addAnnotation(annotation.actual)
        }

        override fun initializer(initExpr: XCodeBlock) = apply {
            require(initExpr is KotlinCodeBlock)
            actual.initializer(initExpr.actual)
        }

        override fun build(): XPropertySpec {
            return KotlinPropertySpec(name, actual.build())
        }
    }
}
