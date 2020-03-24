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

private const val RUNTIME_PACKAGE = "androidx.serialization.runtime.internal"

internal val ENUM_SERIALIZER: ClassName = ClassName.get(RUNTIME_PACKAGE, "EnumSerializerV1")

internal val NULLABLE: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("androidx.annotation", "Nullable")).build()

internal val NON_NULL: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("androidx.annotation", "NonNull")).build()

internal val OVERRIDE: AnnotationSpec =
    AnnotationSpec.builder(ClassName.get("java.lang", "Override")).build()
