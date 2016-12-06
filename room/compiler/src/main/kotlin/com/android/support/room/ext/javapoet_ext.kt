/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room.ext

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

val L = "\$L"
val T = "\$T"
val N = "\$N"
val S = "\$S"

fun KClass<*>.typeName() = ClassName.get(this.java)
fun KClass<*>.arrayTypeName() = ArrayTypeName.of(typeName())
fun TypeMirror.typeName() = TypeName.get(this)

object SupportDbTypeNames {
    val DB: ClassName = ClassName.get("com.android.support.db", "SupportDb")
    val SQLITE_STMT : ClassName = ClassName.get("com.android.support.db", "SupportSqliteStatement")
}

object RoomTypeNames {
    val STRING_UTIL: ClassName = ClassName.get("com.android.support.room.util", "StringUtil")
    val CURSOR_CONVERTER : ClassName = ClassName.get("com.android.support.room", "CursorConverter")
    val ROOM : ClassName = ClassName.get("com.android.support.room", "Room")
    val ROOM_DB : ClassName = ClassName.get("com.android.support.room", "RoomDatabase")
    val INSERTION_ADAPTER : ClassName =
            ClassName.get("com.android.support.room", "EntityInsertionAdapter")
}

object AndroidTypeNames {
    val CURSOR : ClassName = ClassName.get("android.database", "Cursor")
}
