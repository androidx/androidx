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

package androidx.room.compiler.processing.util

import com.squareup.javapoet.ClassName

internal object KotlinTypeNames {
    val ANY_CLASS_NAME = ClassName.get("kotlin", "Any")
    val UNIT_CLASS_NAME = ClassName.get("kotlin", "Unit")
    val INT_CLASS_NAME = ClassName.get("kotlin", "Int")
    val LONG_CLASS_NAME = ClassName.get("kotlin", "Long")
    val STRING_CLASS_NAME = ClassName.get("kotlin", "String")
    val LIST_CLASS_NAME = ClassName.get("kotlin.collections", "List")
    val MUTABLELIST_CLASS_NAME = ClassName.get("kotlin.collections", "MutableList")
    val MAP_CLASS_NAME = ClassName.get("kotlin.collections", "Map")
    val PAIR_CLASS_NAME = ClassName.get(Pair::class.java)
    val CONTINUATION_CLASS_NAME = ClassName.get("kotlin.coroutines", "Continuation")
}
