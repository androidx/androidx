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

package android.arch.persistence.room.ext

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
    val DB: ClassName = ClassName.get("android.arch.persistence.db", "SupportSQLiteDatabase")
    val SQLITE_STMT : ClassName =
            ClassName.get("android.arch.persistence.db", "SupportSQLiteStatement")
    val SQLITE_OPEN_HELPER : ClassName =
            ClassName.get("android.arch.persistence.db", "SupportSQLiteOpenHelper")
    val SQLITE_OPEN_HELPER_CALLBACK : ClassName =
            ClassName.get("android.arch.persistence.db", "SupportSQLiteOpenHelper.Callback")
    val SQLITE_OPEN_HELPER_FACTORY : ClassName =
            ClassName.get("android.arch.persistence.db", "SupportSQLiteOpenHelper.Factory")
    val SQLITE_OPEN_HELPER_CONFIG : ClassName =
            ClassName.get("android.arch.persistence.db", "SupportSQLiteOpenHelper.Configuration")
    val SQLITE_OPEN_HELPER_CONFIG_BUILDER : ClassName =
            ClassName.get("android.arch.persistence.db",
                    "SupportSQLiteOpenHelper.Configuration.Builder")
}

object RoomTypeNames {
    val STRING_UTIL: ClassName = ClassName.get("android.arch.persistence.room.util", "StringUtil")
    val CURSOR_CONVERTER : ClassName =
            ClassName.get("android.arch.persistence.room", "CursorConverter")
    val ROOM : ClassName = ClassName.get("android.arch.persistence.room", "Room")
    val ROOM_DB : ClassName = ClassName.get("android.arch.persistence.room", "RoomDatabase")
    val ROOM_DB_CONFIG : ClassName = ClassName.get("android.arch.persistence.room",
            "DatabaseConfiguration")
    val INSERTION_ADAPTER : ClassName =
            ClassName.get("android.arch.persistence.room", "EntityInsertionAdapter")
    val DELETE_OR_UPDATE_ADAPTER : ClassName =
            ClassName.get("android.arch.persistence.room", "EntityDeletionOrUpdateAdapter")
    val SHARED_SQLITE_STMT : ClassName =
            ClassName.get("android.arch.persistence.room", "SharedSQLiteStatement")
    val INVALIDATION_TRACKER : ClassName =
            ClassName.get("android.arch.persistence.room", "InvalidationTracker")
    val INVALIDATION_OBSERVER : ClassName =
            ClassName.get("android.arch.persistence.room.InvalidationTracker", "Observer")
    val ROOM_SQL_QUERY : ClassName =
            ClassName.get("android.arch.persistence.room", "RoomSQLiteQuery")
    val OPEN_HELPER : ClassName =
            ClassName.get("android.arch.persistence.room", "RoomOpenHelper")
    val OPEN_HELPER_DELEGATE: ClassName =
            ClassName.get("android.arch.persistence.room", "RoomOpenHelper.Delegate")
    val TABLE_INFO : ClassName =
            ClassName.get("android.arch.persistence.room.util", "TableInfo")
    val TABLE_INFO_COLUMN : ClassName =
            ClassName.get("android.arch.persistence.room.util", "TableInfo.Column")
    val TABLE_INFO_FOREIGN_KEY : ClassName =
            ClassName.get("android.arch.persistence.room.util", "TableInfo.ForeignKey")
    val TABLE_INFO_INDEX : ClassName =
            ClassName.get("android.arch.persistence.room.util", "TableInfo.Index")
    val LIMIT_OFFSET_DATA_SOURCE : ClassName =
            ClassName.get("android.arch.persistence.room.paging", "LimitOffsetDataSource")
}

object ArchTypeNames {
    val APP_EXECUTOR : ClassName =
            ClassName.get("android.arch.core.executor", "ArchTaskExecutor")
}

object PagingTypeNames {
    val DATA_SOURCE: ClassName =
            ClassName.get("android.arch.paging", "DataSource")
    val TILED_DATA_SOURCE: ClassName =
            ClassName.get("android.arch.paging", "TiledDataSource")
    val LIVE_PAGED_LIST_PROVIDER: ClassName =
            ClassName.get("android.arch.paging", "LivePagedListProvider")

}

object LifecyclesTypeNames {
    val LIVE_DATA: ClassName = ClassName.get("android.arch.lifecycle", "LiveData")
    val COMPUTABLE_LIVE_DATA : ClassName = ClassName.get("android.arch.lifecycle",
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
    val INTEGER = ClassName.get("java.lang", "Integer")
}

object RxJava2TypeNames {
    val FLOWABLE = ClassName.get("io.reactivex", "Flowable")
    val MAYBE = ClassName.get("io.reactivex", "Maybe")
    val SINGLE = ClassName.get("io.reactivex", "Single")
}

object ReactiveStreamsTypeNames {
    val PUBLISHER = ClassName.get("org.reactivestreams", "Publisher")
}

object RoomRxJava2TypeNames {
    val RX_ROOM = ClassName.get("android.arch.persistence.room", "RxRoom")
    val RX_EMPTY_RESULT_SET_EXCEPTION = ClassName.get("android.arch.persistence.room",
            "EmptyResultSetException")
}

fun TypeName.defaultValue() : String {
    return if (!isPrimitive) {
        "null"
    } else if (this == TypeName.BOOLEAN) {
        "false"
    } else {
        "0"
    }
}
