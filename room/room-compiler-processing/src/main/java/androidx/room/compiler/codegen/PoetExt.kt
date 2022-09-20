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

package androidx.room.compiler.codegen

import androidx.room.compiler.processing.XNullability
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName
import com.squareup.kotlinpoet.javapoet.toKTypeName

// TODO(b/127483380): Recycle to room-compiler?
val L = "\$L"
val T = "\$T"
val N = "\$N"
val S = "\$S"
val W = "\$W"

internal fun JTypeName.toKTypeName(nullability: XNullability): KTypeName = this.toKTypeName().let {
    when (nullability) {
        XNullability.NULLABLE, XNullability.UNKNOWN -> it.copy(nullable = true)
        XNullability.NONNULL -> it.copy(nullable = false)
    }
}