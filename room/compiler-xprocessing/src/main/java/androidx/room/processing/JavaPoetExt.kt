/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.room.processing

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Javapoet does not model NonType, unlike javac, which makes it hard to rely on TypeName for
 * common functionality (e.g. ability to implement XType.isLong as typename() == TypeName.LONG
 * instead of in the base class)
 *
 * For those cases, we have this hacky type so that we can always query TypeName on an XType.
 *
 * We should still strive to avoid these cases, maybe turn it to an error in tests.
 */
private val NONE_TYPE_NAME = ClassName.get("androidx.room.processing.error", "NotAType")

internal fun TypeMirror.safeTypeName(): TypeName = if (kind == TypeKind.NONE) {
    NONE_TYPE_NAME
} else {
    TypeName.get(this)
}
