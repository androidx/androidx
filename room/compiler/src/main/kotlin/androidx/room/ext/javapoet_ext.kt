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

package androidx.room.ext

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
    val DB: ClassName = ClassName.get("androidx.sqlite.db", "SupportSQLiteDatabase")
    val SQLITE_STMT: ClassName =
            ClassName.get("androidx.sqlite.db", "SupportSQLiteStatement")
    val SQLITE_OPEN_HELPER: ClassName =
            ClassName.get("androidx.sqlite.db", "SupportSQLiteOpenHelper")
    val SQLITE_OPEN_HELPER_CALLBACK: ClassName =
            ClassName.get("androidx.sqlite.db", "SupportSQLiteOpenHelper.Callback")
    val SQLITE_OPEN_HELPER_CONFIG: ClassName =
            ClassName.get("androidx.sqlite.db", "SupportSQLiteOpenHelper.Configuration")
    val QUERY: ClassName =
            ClassName.get("androidx.sqlite.db", "SupportSQLiteQuery")
}

object RoomTypeNames {
    val STRING_UTIL: ClassName = ClassName.get("androidx.room.util", "StringUtil")
    val ROOM_DB: ClassName = ClassName.get("androidx.room", "RoomDatabase")
    val ROOM_DB_CONFIG: ClassName = ClassName.get("androidx.room",
            "DatabaseConfiguration")
    val INSERTION_ADAPTER: ClassName =
            ClassName.get("androidx.room", "EntityInsertionAdapter")
    val DELETE_OR_UPDATE_ADAPTER: ClassName =
            ClassName.get("androidx.room", "EntityDeletionOrUpdateAdapter")
    val SHARED_SQLITE_STMT: ClassName =
            ClassName.get("androidx.room", "SharedSQLiteStatement")
    val INVALIDATION_TRACKER: ClassName =
            ClassName.get("androidx.room", "InvalidationTracker")
    val INVALIDATION_OBSERVER: ClassName =
            ClassName.get("androidx.room.InvalidationTracker", "Observer")
    val ROOM_SQL_QUERY: ClassName =
            ClassName.get("androidx.room", "RoomSQLiteQuery")
    val OPEN_HELPER: ClassName =
            ClassName.get("androidx.room", "RoomOpenHelper")
    val OPEN_HELPER_DELEGATE: ClassName =
            ClassName.get("androidx.room", "RoomOpenHelper.Delegate")
    val TABLE_INFO: ClassName =
            ClassName.get("androidx.room.util", "TableInfo")
    val TABLE_INFO_COLUMN: ClassName =
            ClassName.get("androidx.room.util", "TableInfo.Column")
    val TABLE_INFO_FOREIGN_KEY: ClassName =
            ClassName.get("androidx.room.util", "TableInfo.ForeignKey")
    val TABLE_INFO_INDEX: ClassName =
            ClassName.get("androidx.room.util", "TableInfo.Index")
    val LIMIT_OFFSET_DATA_SOURCE: ClassName =
            ClassName.get("androidx.room.paging", "LimitOffsetDataSource")
}

object PagingTypeNames {
    val DATA_SOURCE: ClassName =
            ClassName.get("androidx.paging", "DataSource")
    val POSITIONAL_DATA_SOURCE: ClassName =
            ClassName.get("androidx.paging", "PositionalDataSource")
    val DATA_SOURCE_FACTORY: ClassName =
            ClassName.get("androidx.paging", "DataSource.Factory")
}

object LifecyclesTypeNames {
    val LIVE_DATA: ClassName = ClassName.get("androidx.lifecycle", "LiveData")
    val COMPUTABLE_LIVE_DATA: ClassName = ClassName.get("androidx.lifecycle",
            "ComputableLiveData")
}

object AndroidTypeNames {
    val CURSOR: ClassName = ClassName.get("android.database", "Cursor")
    val ARRAY_MAP: ClassName = ClassName.get("androidx.collection", "ArrayMap")
    val BUILD: ClassName = ClassName.get("android.os", "Build")
}

object CommonTypeNames {
    val LIST = ClassName.get("java.util", "List")
    val SET = ClassName.get("java.util", "Set")
    val STRING = ClassName.get("java.lang", "String")
    val INTEGER = ClassName.get("java.lang", "Integer")
    val OPTIONAL = ClassName.get("java.util", "Optional")
}

object GuavaBaseTypeNames {
    val OPTIONAL = ClassName.get("com.google.common.base", "Optional")
}

object GuavaUtilConcurrentTypeNames {
    val LISTENABLE_FUTURE = ClassName.get("com.google.common.util.concurrent", "ListenableFuture")
}

object RxJava2TypeNames {
    val FLOWABLE = ClassName.get("io.reactivex", "Flowable")
    val MAYBE = ClassName.get("io.reactivex", "Maybe")
    val SINGLE = ClassName.get("io.reactivex", "Single")
}

object ReactiveStreamsTypeNames {
    val PUBLISHER = ClassName.get("org.reactivestreams", "Publisher")
}

object RoomGuavaTypeNames {
    val GUAVA_ROOM = ClassName.get("androidx.room.guava", "GuavaRoom")
}

object RoomRxJava2TypeNames {
    val RX_ROOM = ClassName.get("androidx.room", "RxRoom")
    val RX_EMPTY_RESULT_SET_EXCEPTION = ClassName.get("androidx.room",
            "EmptyResultSetException")
}

fun TypeName.defaultValue(): String {
    return if (!isPrimitive) {
        "null"
    } else if (this == TypeName.BOOLEAN) {
        "false"
    } else {
        "0"
    }
}
