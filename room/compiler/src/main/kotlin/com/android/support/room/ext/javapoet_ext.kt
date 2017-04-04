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
    val DB: ClassName = ClassName.get("com.android.support.db", "SupportSQLiteDatabase")
    val SQLITE_STMT : ClassName = ClassName.get("com.android.support.db", "SupportSQLiteStatement")
    val SQLITE_OPEN_HELPER : ClassName =
            ClassName.get("com.android.support.db", "SupportSQLiteOpenHelper")
    val SQLITE_OPEN_HELPER_CALLBACK : ClassName =
            ClassName.get("com.android.support.db", "SupportSQLiteOpenHelper.Callback")
    val SQLITE_OPEN_HELPER_FACTORY : ClassName =
            ClassName.get("com.android.support.db", "SupportSQLiteOpenHelper.Factory")
    val SQLITE_OPEN_HELPER_CONFIG : ClassName =
            ClassName.get("com.android.support.db", "SupportSQLiteOpenHelper.Configuration")
    val SQLITE_OPEN_HELPER_CONFIG_BUILDER : ClassName =
            ClassName.get("com.android.support.db", "SupportSQLiteOpenHelper.Configuration.Builder")
}

object RoomTypeNames {
    val STRING_UTIL: ClassName = ClassName.get("com.android.support.room.util", "StringUtil")
    val CURSOR_CONVERTER : ClassName = ClassName.get("com.android.support.room", "CursorConverter")
    val ROOM : ClassName = ClassName.get("com.android.support.room", "Room")
    val ROOM_DB : ClassName = ClassName.get("com.android.support.room", "RoomDatabase")
    val ROOM_DB_CONFIG : ClassName = ClassName.get("com.android.support.room",
            "DatabaseConfiguration")
    val INSERTION_ADAPTER : ClassName =
            ClassName.get("com.android.support.room", "EntityInsertionAdapter")
    val DELETE_OR_UPDATE_ADAPTER : ClassName =
            ClassName.get("com.android.support.room", "EntityDeletionOrUpdateAdapter")
    val SHARED_SQLITE_STMT : ClassName =
            ClassName.get("com.android.support.room", "SharedSQLiteStatement")
    val INVALIDATION_TRACKER : ClassName =
            ClassName.get("com.android.support.room", "InvalidationTracker")
    val INVALIDATION_OBSERVER : ClassName =
            ClassName.get("com.android.support.room.InvalidationTracker", "Observer")
    val ROOM_SQL_QUERY : ClassName =
            ClassName.get("com.android.support.room", "RoomSQLiteQuery")
    val OPEN_HELPER : ClassName =
            ClassName.get("com.android.support.room", "RoomOpenHelper")
    val OPEN_HELPER_DELEGATE: ClassName =
            ClassName.get("com.android.support.room", "RoomOpenHelper.Delegate")
    val TABLE_INFO : ClassName =
            ClassName.get("com.android.support.room.util", "TableInfo")
    val TABLE_INFO_COLUMN : ClassName =
            ClassName.get("com.android.support.room.util", "TableInfo.Column")
    val TABLE_INFO_FOREIGN_KEY : ClassName =
            ClassName.get("com.android.support.room.util", "TableInfo.ForeignKey")
}

object LifecyclesTypeNames {
    val LIVE_DATA: ClassName = ClassName.get("com.android.support.lifecycle", "LiveData")
    val COMPUTABLE_LIVE_DATA : ClassName = ClassName.get("com.android.support.lifecycle",
            "ComputableLiveData")
}

object AndroidTypeNames {
    val CURSOR : ClassName = ClassName.get("android.database", "Cursor")
    val ARRAY_MAP : ClassName = ClassName.get("android.support.v4.util", "ArrayMap")
}

object CommonTypeNames {
    val LIST = ClassName.get("java.util", "List")
    val SET = ClassName.get("java.util", "Set")
    val STRING = ClassName.get("java.lang", "String")
}

object RxJava2TypeNames {
    val FLOWABLE = ClassName.get("io.reactivex", "Flowable")
}

object ReactiveStreamsTypeNames {
    val PUBLISHER = ClassName.get("org.reactivestreams", "Publisher")
}

object RoomRxJava2TypeNames {
    val RX_ROOM = ClassName.get("com.android.support.room", "RxRoom")
}
