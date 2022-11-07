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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.apply
import androidx.room.compiler.codegen.XMemberName.Companion.companionMember
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.codegen.asMutableClassName
import androidx.room.compiler.codegen.toJavaPoet
import androidx.room.ext.CommonTypeNames.STRING
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.concurrent.Callable
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

val L = "\$L"
val T = "\$T"
val N = "\$N"
val S = "\$S"
val W = "\$W"

val KClass<*>.typeName: ClassName
    get() = ClassName.get(this.java)
val KClass<*>.arrayTypeName: ArrayTypeName
    get() = ArrayTypeName.of(typeName)

object SupportDbTypeNames {
    val DB = XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteDatabase")
    val SQLITE_STMT: XClassName =
        XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteStatement")
    val SQLITE_OPEN_HELPER = XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper")
    val SQLITE_OPEN_HELPER_CALLBACK =
        XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper", "Callback")
    val SQLITE_OPEN_HELPER_CONFIG =
        XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper", "Configuration")
    val QUERY = XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteQuery")
}

object RoomTypeNames {
    val STRING_UTIL: XClassName = XClassName.get("$ROOM_PACKAGE.util", "StringUtil")
    val ROOM_DB: XClassName = XClassName.get(ROOM_PACKAGE, "RoomDatabase")
    val ROOM_DB_KT = XClassName.get(ROOM_PACKAGE, "RoomDatabaseKt")
    val ROOM_DB_CONFIG = XClassName.get(ROOM_PACKAGE, "DatabaseConfiguration")
    val INSERTION_ADAPTER: XClassName =
        XClassName.get(ROOM_PACKAGE, "EntityInsertionAdapter")
    val UPSERTION_ADAPTER: XClassName =
        XClassName.get(ROOM_PACKAGE, "EntityUpsertionAdapter")
    val DELETE_OR_UPDATE_ADAPTER: XClassName =
        XClassName.get(ROOM_PACKAGE, "EntityDeletionOrUpdateAdapter")
    val SHARED_SQLITE_STMT: XClassName =
        XClassName.get(ROOM_PACKAGE, "SharedSQLiteStatement")
    val INVALIDATION_TRACKER = XClassName.get(ROOM_PACKAGE, "InvalidationTracker")
    val INVALIDATION_OBSERVER: ClassName =
        ClassName.get("$ROOM_PACKAGE.InvalidationTracker", "Observer")
    val ROOM_SQL_QUERY: XClassName =
        XClassName.get(ROOM_PACKAGE, "RoomSQLiteQuery")
    val OPEN_HELPER = XClassName.get(ROOM_PACKAGE, "RoomOpenHelper")
    val OPEN_HELPER_DELEGATE: ClassName =
        ClassName.get(ROOM_PACKAGE, "RoomOpenHelper", "Delegate")
    val OPEN_HELPER_VALIDATION_RESULT: ClassName =
        ClassName.get(ROOM_PACKAGE, "RoomOpenHelper.ValidationResult")
    val TABLE_INFO: ClassName =
        ClassName.get("$ROOM_PACKAGE.util", "TableInfo")
    val TABLE_INFO_COLUMN: ClassName =
        ClassName.get("$ROOM_PACKAGE.util", "TableInfo.Column")
    val TABLE_INFO_FOREIGN_KEY: ClassName =
        ClassName.get("$ROOM_PACKAGE.util", "TableInfo.ForeignKey")
    val TABLE_INFO_INDEX: ClassName =
        ClassName.get("$ROOM_PACKAGE.util", "TableInfo.Index")
    val FTS_TABLE_INFO: ClassName =
        ClassName.get("$ROOM_PACKAGE.util", "FtsTableInfo")
    val VIEW_INFO: ClassName =
        ClassName.get("$ROOM_PACKAGE.util", "ViewInfo")
    val LIMIT_OFFSET_DATA_SOURCE: ClassName =
        ClassName.get("$ROOM_PACKAGE.paging", "LimitOffsetDataSource")
    val DB_UTIL: XClassName =
        XClassName.get("$ROOM_PACKAGE.util", "DBUtil")
    val CURSOR_UTIL: XClassName =
        XClassName.get("$ROOM_PACKAGE.util", "CursorUtil")
    val MIGRATION = XClassName.get("$ROOM_PACKAGE.migration", "Migration")
    val AUTO_MIGRATION_SPEC = XClassName.get("$ROOM_PACKAGE.migration", "AutoMigrationSpec")
    val UUID_UTIL: XClassName =
        XClassName.get("$ROOM_PACKAGE.util", "UUIDUtil")
    val AMBIGUOUS_COLUMN_RESOLVER: ClassName =
        ClassName.get(ROOM_PACKAGE, "AmbiguousColumnResolver")
    val RELATION_UTIL = XClassName.get("androidx.room.util", "RelationUtil")
}

object PagingTypeNames {
    val DATA_SOURCE: ClassName =
        ClassName.get(PAGING_PACKAGE, "DataSource")
    val POSITIONAL_DATA_SOURCE: ClassName =
        ClassName.get(PAGING_PACKAGE, "PositionalDataSource")
    val DATA_SOURCE_FACTORY: ClassName =
        ClassName.get(PAGING_PACKAGE, "DataSource", "Factory")
    val PAGING_SOURCE: ClassName =
        ClassName.get(PAGING_PACKAGE, "PagingSource")
    val LISTENABLE_FUTURE_PAGING_SOURCE: ClassName =
        ClassName.get(PAGING_PACKAGE, "ListenableFuturePagingSource")
    val RX2_PAGING_SOURCE: ClassName =
        ClassName.get("$PAGING_PACKAGE.rxjava2", "RxPagingSource")
    val RX3_PAGING_SOURCE: ClassName =
        ClassName.get("$PAGING_PACKAGE.rxjava3", "RxPagingSource")
}

object LifecyclesTypeNames {
    val LIVE_DATA: ClassName = ClassName.get(LIFECYCLE_PACKAGE, "LiveData")
    val COMPUTABLE_LIVE_DATA: ClassName = ClassName.get(
        LIFECYCLE_PACKAGE,
        "ComputableLiveData"
    )
}

object AndroidTypeNames {
    val CURSOR: XClassName = XClassName.get("android.database", "Cursor")
    val BUILD = XClassName.get("android.os", "Build")
    val CANCELLATION_SIGNAL: XClassName = XClassName.get("android.os", "CancellationSignal")
}

object CollectionTypeNames {
    val ARRAY_MAP = XClassName.get(COLLECTION_PACKAGE, "ArrayMap")
    val LONG_SPARSE_ARRAY = XClassName.get(COLLECTION_PACKAGE, "LongSparseArray")
    val INT_SPARSE_ARRAY: ClassName = ClassName.get(COLLECTION_PACKAGE, "SparseArrayCompat")
}

object KotlinCollectionTypeNames {
    val MUTABLE_LIST = List::class.asMutableClassName()
}

object CommonTypeNames {
    val LIST = List::class.asClassName()
    val ARRAY_LIST = XClassName.get("java.util", "ArrayList")
    val MAP = Map::class.asClassName()
    val HASH_MAP = XClassName.get("java.util", "HashMap")
    val SET = Set::class.asClassName()
    val HASH_SET = XClassName.get("java.util", "HashSet")
    val STRING = String::class.asClassName()
    val INTEGER = ClassName.get("java.lang", "Integer")
    val OPTIONAL = ClassName.get("java.util", "Optional")
    val UUID = ClassName.get("java.util", "UUID")
    val BYTE_BUFFER = XClassName.get("java.nio", "ByteBuffer")
    val JAVA_CLASS = XClassName.get("java.lang", "Class")
}

object GuavaBaseTypeNames {
    val OPTIONAL = ClassName.get("com.google.common.base", "Optional")
}

object GuavaUtilConcurrentTypeNames {
    val LISTENABLE_FUTURE = ClassName.get("com.google.common.util.concurrent", "ListenableFuture")
}

object RxJava2TypeNames {
    val FLOWABLE = ClassName.get("io.reactivex", "Flowable")
    val OBSERVABLE = ClassName.get("io.reactivex", "Observable")
    val MAYBE = ClassName.get("io.reactivex", "Maybe")
    val SINGLE = ClassName.get("io.reactivex", "Single")
    val COMPLETABLE = ClassName.get("io.reactivex", "Completable")
}

object RxJava3TypeNames {
    val FLOWABLE = ClassName.get("io.reactivex.rxjava3.core", "Flowable")
    val OBSERVABLE = ClassName.get("io.reactivex.rxjava3.core", "Observable")
    val MAYBE = ClassName.get("io.reactivex.rxjava3.core", "Maybe")
    val SINGLE = ClassName.get("io.reactivex.rxjava3.core", "Single")
    val COMPLETABLE = ClassName.get("io.reactivex.rxjava3.core", "Completable")
}

object ReactiveStreamsTypeNames {
    val PUBLISHER = ClassName.get("org.reactivestreams", "Publisher")
}

object RoomGuavaTypeNames {
    val GUAVA_ROOM = ClassName.get("$ROOM_PACKAGE.guava", "GuavaRoom")
}

object RoomRxJava2TypeNames {
    val RX_ROOM = ClassName.get(ROOM_PACKAGE, "RxRoom")
    val RX_ROOM_CREATE_FLOWABLE = "createFlowable"
    val RX_ROOM_CREATE_OBSERVABLE = "createObservable"
    val RX_EMPTY_RESULT_SET_EXCEPTION = ClassName.get(ROOM_PACKAGE, "EmptyResultSetException")
}

object RoomRxJava3TypeNames {
    val RX_ROOM = ClassName.get("$ROOM_PACKAGE.rxjava3", "RxRoom")
    val RX_ROOM_CREATE_FLOWABLE = "createFlowable"
    val RX_ROOM_CREATE_OBSERVABLE = "createObservable"
    val RX_EMPTY_RESULT_SET_EXCEPTION =
        ClassName.get("$ROOM_PACKAGE.rxjava3", "EmptyResultSetException")
}

object RoomPagingTypeNames {
    val LIMIT_OFFSET_PAGING_SOURCE: ClassName =
        ClassName.get("$ROOM_PACKAGE.paging", "LimitOffsetPagingSource")
}

object RoomPagingGuavaTypeNames {
    val LIMIT_OFFSET_LISTENABLE_FUTURE_PAGING_SOURCE: ClassName =
        ClassName.get(
            "$ROOM_PACKAGE.paging.guava",
            "LimitOffsetListenableFuturePagingSource"
        )
}

object RoomPagingRx2TypeNames {
    val LIMIT_OFFSET_RX_PAGING_SOURCE: ClassName =
        ClassName.get(
            "$ROOM_PACKAGE.paging.rxjava2",
            "LimitOffsetRxPagingSource"
        )
}

object RoomPagingRx3TypeNames {
    val LIMIT_OFFSET_RX_PAGING_SOURCE: ClassName =
        ClassName.get(
            "$ROOM_PACKAGE.paging.rxjava3",
            "LimitOffsetRxPagingSource"
        )
}

object RoomCoroutinesTypeNames {
    val COROUTINES_ROOM = XClassName.get(ROOM_PACKAGE, "CoroutinesRoom")
}

object KotlinTypeNames {
    val UNIT = ClassName.get("kotlin", "Unit")
    val CONTINUATION = XClassName.get("kotlin.coroutines", "Continuation")
    val COROUTINE_SCOPE = ClassName.get("kotlinx.coroutines", "CoroutineScope")
    val CHANNEL = ClassName.get("kotlinx.coroutines.channels", "Channel")
    val RECEIVE_CHANNEL = ClassName.get("kotlinx.coroutines.channels", "ReceiveChannel")
    val SEND_CHANNEL = ClassName.get("kotlinx.coroutines.channels", "SendChannel")
    val FLOW = ClassName.get("kotlinx.coroutines.flow", "Flow")
    val LAZY = XClassName.get("kotlin", "Lazy")
}

object RoomMemberNames {
    val DB_UTIL_QUERY = RoomTypeNames.DB_UTIL.packageMember("query")
    val CURSOR_UTIL_GET_COLUMN_INDEX =
        RoomTypeNames.CURSOR_UTIL.packageMember("getColumnIndex")
    val ROOM_SQL_QUERY_ACQUIRE =
        RoomTypeNames.ROOM_SQL_QUERY.companionMember("acquire", isJvmStatic = true)
    val ROOM_DATABASE_WITH_TRANSACTION =
        RoomTypeNames.ROOM_DB_KT.packageMember("withTransaction")
}

val DEFERRED_TYPES = listOf(
    LifecyclesTypeNames.LIVE_DATA,
    LifecyclesTypeNames.COMPUTABLE_LIVE_DATA,
    RxJava2TypeNames.FLOWABLE,
    RxJava2TypeNames.OBSERVABLE,
    RxJava2TypeNames.MAYBE,
    RxJava2TypeNames.SINGLE,
    RxJava2TypeNames.COMPLETABLE,
    RxJava3TypeNames.FLOWABLE,
    RxJava3TypeNames.OBSERVABLE,
    RxJava3TypeNames.MAYBE,
    RxJava3TypeNames.SINGLE,
    RxJava3TypeNames.COMPLETABLE,
    GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE,
    KotlinTypeNames.FLOW,
    ReactiveStreamsTypeNames.PUBLISHER
)

fun TypeName.defaultValue(): String {
    return if (!isPrimitive) {
        "null"
    } else if (this == TypeName.BOOLEAN) {
        "false"
    } else {
        "0"
    }
}

fun CallableTypeSpec(
    language: CodeLanguage,
    parameterTypeName: XTypeName,
    callBody: XFunSpec.Builder.() -> Unit
) = XTypeSpec.anonymousClassBuilder(language, "").apply {
    addSuperinterface(Callable::class.asClassName().parametrizedBy(parameterTypeName))
    addFunction(
        XFunSpec.builder(
            language = language,
            name = "call",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true
        ).apply {
            returns(parameterTypeName)
            callBody()
        }.apply(
            javaMethodBuilder = {
                addException(Exception::class.typeName)
            },
            kotlinFunctionBuilder = { }
        ).build()
    )
}.build()

// TODO(b/127483380): Remove once XPoet is more widely adopted.
// @Deprecated("Use CallableTypeSpec, will be removed as part of XPoet migration.")
fun CallableTypeSpecBuilder(
    parameterTypeName: TypeName,
    callBody: MethodSpec.Builder.() -> Unit
) = TypeSpec.anonymousClassBuilder("").apply {
    superclass(ParameterizedTypeName.get(Callable::class.typeName, parameterTypeName))
    addMethod(
        MethodSpec.methodBuilder("call").apply {
            returns(parameterTypeName)
            addException(Exception::class.typeName)
            addModifiers(Modifier.PUBLIC)
            addAnnotation(Override::class.java)
            callBody()
        }.build()
    )
}

fun Function1TypeSpec(
    language: CodeLanguage,
    parameterTypeName: XTypeName,
    parameterName: String,
    returnTypeName: XTypeName,
    callBody: XFunSpec.Builder.() -> Unit
) = XTypeSpec.anonymousClassBuilder(language, "").apply {
    superclass(
        Function1::class.asClassName().parametrizedBy(parameterTypeName, returnTypeName)
    )
    addFunction(
        XFunSpec.builder(
            language = language,
            name = "invoke",
            visibility = VisibilityModifier.PUBLIC,
            isOverride = true
        ).apply {
            addParameter(parameterTypeName, parameterName)
            returns(returnTypeName)
            callBody()
        }.build()
    )
}.build()

/**
 * Generates a 2D array literal where the value at `i`,`j` will be produced by `valueProducer.
 * For example:
 * ```
 * DoubleArrayLiteral(TypeName.INT, 2, { _ -> 3 }, { i, j -> i + j })
 * ```
 * will produce:
 * ```
 * new int[][] {
 *   { 0, 1, 2 },
 *   { 1, 2, 3 }
 * }
 * ```
 */
fun DoubleArrayLiteral(
    type: TypeName,
    rowSize: Int,
    columnSizeProducer: (Int) -> Int,
    valueProducer: (Int, Int) -> Any
): CodeBlock = CodeBlock.of(
    "new $T[][] {$W$L$W}", type,
    CodeBlock.join(
        List(rowSize) { i ->
            CodeBlock.of(
                "{$W$L$W}",
                CodeBlock.join(
                    List(columnSizeProducer(i)) { j ->
                        CodeBlock.of(
                            if (type == STRING.toJavaPoet()) S else L,
                            valueProducer(i, j)
                        )
                    },
                    ",$W"
                ),
            )
        },
        ",$W"
    )
)

/**
 * Code of expression for [Collection.size] in Kotlin, and [java.util.Collection.size] for Java.
 */
fun CollectionsSizeExprCode(language: CodeLanguage, varName: String) = XCodeBlock.of(
    language,
    when (language) {
        CodeLanguage.JAVA -> "%L.size()" // java.util.Collections.size()
        CodeLanguage.KOTLIN -> "%L.size" // kotlin.collections.Collection.size
    },
    varName
)

/**
 * Code of expression for [Array.size] in Kotlin, and `arr.length` for Java.
 */
fun ArraySizeExprCode(language: CodeLanguage, varName: String) = XCodeBlock.of(
    language,
    when (language) {
        CodeLanguage.JAVA -> "%L.length" // Just `arr.length`
        CodeLanguage.KOTLIN -> "%L.size" // kotlin.Array.size and primitives (e.g. IntArray)
    },
    varName
)

/**
 * Code of expression for [Map.keys] in Kotlin, and [java.util.Map.keySet] for Java.
 */
fun MapKeySetExprCode(language: CodeLanguage, varName: String) = XCodeBlock.of(
    language,
    when (language) {
        CodeLanguage.JAVA -> "%L.keySet()" // java.util.Map.keySet()
        CodeLanguage.KOTLIN -> "%L.keys" // kotlin.collections.Map.keys
    },
    varName
)