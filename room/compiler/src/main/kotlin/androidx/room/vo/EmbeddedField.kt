/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.vo

import androidx.annotation.NonNull
import androidx.room.ext.hasAnnotation

/**
 * Used when a field is embedded inside an Entity or Pojo.
 */
// used in cache matching, must stay as a data class or implement equals
data class EmbeddedField(val field: Field, val prefix: String = "",
                         val parent: EmbeddedField?) {
    val getter by lazy { field.getter }
    val setter by lazy { field.setter }
    val nonNull = field.element.hasAnnotation(NonNull::class)
    lateinit var pojo: Pojo
    val mRootParent: EmbeddedField by lazy {
        parent?.mRootParent ?: this
    }

    fun isNonNullRecursively(): Boolean {
        return field.nonNull && (parent == null || parent.isNonNullRecursively())
    }
}
