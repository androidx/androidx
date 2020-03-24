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

package androidx.serialization.compiler.codegen.java

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import javax.lang.model.SourceVersion
import javax.lang.model.SourceVersion.RELEASE_8
import kotlin.reflect.KClass

internal fun testJavaGenerator(
    generatingClass: KClass<*>,
    sourceVersion: SourceVersion = SourceVersion.latest()
): JavaGenerator {

    val packageName = if (sourceVersion <= RELEASE_8) {
        "javax.annotation.processing"
    } else {
        "javax.annotation"
    }

    val generatedAnnotation = AnnotationSpec.builder(ClassName.get(packageName, "Generated"))
        .addMember("value", "\$S", generatingClass.java.canonicalName)
        .build()

    return JavaGenerator(sourceVersion, generatedAnnotation)
}
