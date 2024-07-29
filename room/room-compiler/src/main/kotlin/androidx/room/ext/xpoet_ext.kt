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
import androidx.room.compiler.codegen.XMemberName
import androidx.room.compiler.codegen.XMemberName.Companion.companionMember
import androidx.room.compiler.codegen.XMemberName.Companion.packageMember
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.codegen.asMutableClassName
import androidx.room.ext.RoomGuavaTypeNames.GUAVA_ROOM
import androidx.room.solver.CodeGenScope
import com.squareup.kotlinpoet.javapoet.JTypeName
import java.util.concurrent.Callable

object SupportDbTypeNames {
    val DB = XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteDatabase")
    val SQLITE_STMT = XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteStatement")
    val SQLITE_OPEN_HELPER = XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper")
    val SQLITE_OPEN_HELPER_CALLBACK =
        XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper", "Callback")
    val SQLITE_OPEN_HELPER_CONFIG =
        XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper", "Configuration")
    val QUERY = XClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteQuery")
}

object SQLiteDriverTypeNames {
    val SQLITE = XClassName.get(SQLITE_PACKAGE, "SQLite")
    val DRIVER = XClassName.get(SQLITE_PACKAGE, "SQLiteDriver")
    val CONNECTION = XClassName.get(SQLITE_PACKAGE, "SQLiteConnection")
    val STATEMENT = XClassName.get(SQLITE_PACKAGE, "SQLiteStatement")
}

object RoomTypeNames {
    val STRING_UTIL = XClassName.get("$ROOM_PACKAGE.util", "StringUtil")
    val ROOM_DB = XClassName.get(ROOM_PACKAGE, "RoomDatabase")
    val ROOM_DB_KT = XClassName.get(ROOM_PACKAGE, "RoomDatabaseKt")
    val ROOM_DB_CALLBACK = XClassName.get(ROOM_PACKAGE, "RoomDatabase", "Callback")
    val ROOM_DB_CONFIG = XClassName.get(ROOM_PACKAGE, "DatabaseConfiguration")
    val INSERT_ADAPTER = XClassName.get(ROOM_PACKAGE, "EntityInsertAdapter")
    val UPSERT_ADAPTER = XClassName.get(ROOM_PACKAGE, "EntityUpsertAdapter")
    val DELETE_OR_UPDATE_ADAPTER = XClassName.get(ROOM_PACKAGE, "EntityDeleteOrUpdateAdapter")
    val INSERT_ADAPTER_COMPAT = XClassName.get(ROOM_PACKAGE, "EntityInsertionAdapter")
    val UPSERT_ADAPTER_COMPAT = XClassName.get(ROOM_PACKAGE, "EntityUpsertionAdapter")
    val DELETE_OR_UPDATE_ADAPTER_COMPAT =
        XClassName.get(ROOM_PACKAGE, "EntityDeletionOrUpdateAdapter")
    val SHARED_SQLITE_STMT = XClassName.get(ROOM_PACKAGE, "SharedSQLiteStatement")
    val INVALIDATION_TRACKER = XClassName.get(ROOM_PACKAGE, "InvalidationTracker")
    val ROOM_SQL_QUERY = XClassName.get(ROOM_PACKAGE, "RoomSQLiteQuery")
    val TABLE_INFO = XClassName.get("$ROOM_PACKAGE.util", "TableInfo")
    val TABLE_INFO_COLUMN = XClassName.get("$ROOM_PACKAGE.util", "TableInfo", "Column")
    val TABLE_INFO_FOREIGN_KEY = XClassName.get("$ROOM_PACKAGE.util", "TableInfo", "ForeignKey")
    val TABLE_INFO_INDEX = XClassName.get("$ROOM_PACKAGE.util", "TableInfo", "Index")
    val FTS_TABLE_INFO = XClassName.get("$ROOM_PACKAGE.util", "FtsTableInfo")
    val VIEW_INFO = XClassName.get("$ROOM_PACKAGE.util", "ViewInfo")
    val LIMIT_OFFSET_DATA_SOURCE = XClassName.get("$ROOM_PACKAGE.paging", "LimitOffsetDataSource")
    val DB_UTIL = XClassName.get("$ROOM_PACKAGE.util", "DBUtil")
    val CURSOR_UTIL = XClassName.get("$ROOM_PACKAGE.util", "CursorUtil")
    val MIGRATION = XClassName.get("$ROOM_PACKAGE.migration", "Migration")
    val AUTO_MIGRATION_SPEC = XClassName.get("$ROOM_PACKAGE.migration", "AutoMigrationSpec")
    val UUID_UTIL = XClassName.get("$ROOM_PACKAGE.util", "UUIDUtil")
    val AMBIGUOUS_COLUMN_RESOLVER = XClassName.get(ROOM_PACKAGE, "AmbiguousColumnResolver")
    val RELATION_UTIL = XClassName.get("androidx.room.util", "RelationUtil")
    val ROOM_OPEN_DELEGATE = XClassName.get(ROOM_PACKAGE, "RoomOpenDelegate")
    val ROOM_OPEN_DELEGATE_VALIDATION_RESULT =
        XClassName.get(ROOM_PACKAGE, "RoomOpenDelegate", "ValidationResult")
    val STATEMENT_UTIL = XClassName.get("$ROOM_PACKAGE.util", "SQLiteStatementUtil")
    val CONNECTION_UTIL = XClassName.get("$ROOM_PACKAGE.util", "SQLiteConnectionUtil")
    val FLOW_UTIL = XClassName.get("$ROOM_PACKAGE.coroutines", "FlowUtil")
    val RAW_QUERY = XClassName.get(ROOM_PACKAGE, "RoomRawQuery")
    val ROOM_DB_CONSTRUCTOR = XClassName.get(ROOM_PACKAGE, "RoomDatabaseConstructor")
}

object RoomAnnotationTypeNames {
    val QUERY = XClassName.get(ROOM_PACKAGE, "Query")
    val DAO = XClassName.get(ROOM_PACKAGE, "Dao")
    val DATABASE = XClassName.get(ROOM_PACKAGE, "Database")
    val PRIMARY_KEY = XClassName.get(ROOM_PACKAGE, "PrimaryKey")
    val TYPE_CONVERTERS = XClassName.get(ROOM_PACKAGE, "TypeConverters")
    val TYPE_CONVERTER = XClassName.get(ROOM_PACKAGE, "TypeConverter")
    val ENTITY = XClassName.get(ROOM_PACKAGE, "Entity")
}

object PagingTypeNames {
    val DATA_SOURCE = XClassName.get(PAGING_PACKAGE, "DataSource")
    val POSITIONAL_DATA_SOURCE = XClassName.get(PAGING_PACKAGE, "PositionalDataSource")
    val DATA_SOURCE_FACTORY = XClassName.get(PAGING_PACKAGE, "DataSource", "Factory")
    val PAGING_SOURCE = XClassName.get(PAGING_PACKAGE, "PagingSource")
    val LISTENABLE_FUTURE_PAGING_SOURCE =
        XClassName.get(PAGING_PACKAGE, "ListenableFuturePagingSource")
    val RX2_PAGING_SOURCE = XClassName.get("$PAGING_PACKAGE.rxjava2", "RxPagingSource")
    val RX3_PAGING_SOURCE = XClassName.get("$PAGING_PACKAGE.rxjava3", "RxPagingSource")
}

object LifecyclesTypeNames {
    val LIVE_DATA = XClassName.get(LIFECYCLE_PACKAGE, "LiveData")
    val COMPUTABLE_LIVE_DATA = XClassName.get(LIFECYCLE_PACKAGE, "ComputableLiveData")
}

object AndroidTypeNames {
    val CURSOR = XClassName.get("android.database", "Cursor")
    val BUILD = XClassName.get("android.os", "Build")
    val CANCELLATION_SIGNAL = XClassName.get("android.os", "CancellationSignal")
}

object CollectionTypeNames {
    val ARRAY_MAP = XClassName.get(COLLECTION_PACKAGE, "ArrayMap")
    val LONG_SPARSE_ARRAY = XClassName.get(COLLECTION_PACKAGE, "LongSparseArray")
    val INT_SPARSE_ARRAY = XClassName.get(COLLECTION_PACKAGE, "SparseArrayCompat")
}

object KotlinCollectionMemberNames {
    val ARRAY_OF_NULLS = XClassName.get("kotlin", "LibraryKt").packageMember("arrayOfNulls")
    val MUTABLE_LIST_OF = KotlinTypeNames.COLLECTIONS_KT.packageMember("mutableListOf")
    val MUTABLE_SET_OF = KotlinTypeNames.SETS_KT.packageMember("mutableSetOf")
    val MUTABLE_MAP_OF = KotlinTypeNames.MAPS_KT.packageMember("mutableMapOf")
}

object CommonTypeNames {
    val VOID = Void::class.asClassName()
    val COLLECTION = Collection::class.asClassName()
    val COLLECTIONS = XClassName.get("java.util", "Collections")
    val ARRAYS = XClassName.get("java.util", "Arrays")
    val LIST = List::class.asClassName()
    val MUTABLE_LIST = List::class.asMutableClassName()
    val ARRAY_LIST = XClassName.get("java.util", "ArrayList")
    val MAP = Map::class.asClassName()
    val MUTABLE_MAP = Map::class.asMutableClassName()
    val HASH_MAP = XClassName.get("java.util", "HashMap")
    val QUEUE = XClassName.get("java.util", "Queue")
    val LINKED_HASH_MAP = LinkedHashMap::class.asClassName()
    val SET = Set::class.asClassName()
    val MUTABLE_SET = Set::class.asMutableClassName()
    val HASH_SET = XClassName.get("java.util", "HashSet")
    val STRING = String::class.asClassName()
    val STRING_BUILDER = XClassName.get("java.lang", "StringBuilder")
    val OPTIONAL = XClassName.get("java.util", "Optional")
    val UUID = XClassName.get("java.util", "UUID")
    val BYTE_BUFFER = XClassName.get("java.nio", "ByteBuffer")
    val JAVA_CLASS = XClassName.get("java.lang", "Class")
    val KOTLIN_CLASS = XClassName.get("kotlin.reflect", "KClass")
    val CALLABLE = Callable::class.asClassName()
    val DATE = XClassName.get("java.util", "Date")
}

object ExceptionTypeNames {
    val JAVA_ILLEGAL_STATE_EXCEPTION = XClassName.get("java.lang", "IllegalStateException")
    val JAVA_ILLEGAL_ARG_EXCEPTION = XClassName.get("java.lang", "IllegalArgumentException")
    val KOTLIN_ILLEGAL_STATE_EXCEPTION = XClassName.get("kotlin", "IllegalStateException")
    val KOTLIN_ILLEGAL_ARG_EXCEPTION = XClassName.get("kotlin", "IllegalArgumentException")
}

object GuavaTypeNames {
    val OPTIONAL = XClassName.get("com.google.common.base", "Optional")
    val IMMUTABLE_MULTIMAP_BUILDER =
        XClassName.get("com.google.common.collect", "ImmutableMultimap", "Builder")
    val IMMUTABLE_SET_MULTIMAP = XClassName.get("com.google.common.collect", "ImmutableSetMultimap")
    val IMMUTABLE_SET_MULTIMAP_BUILDER =
        XClassName.get("com.google.common.collect", "ImmutableSetMultimap", "Builder")
    val IMMUTABLE_LIST_MULTIMAP =
        XClassName.get("com.google.common.collect", "ImmutableListMultimap")
    val IMMUTABLE_LIST_MULTIMAP_BUILDER =
        XClassName.get("com.google.common.collect", "ImmutableListMultimap", "Builder")
    val IMMUTABLE_MAP = XClassName.get("com.google.common.collect", "ImmutableMap")
    val IMMUTABLE_LIST = XClassName.get("com.google.common.collect", "ImmutableList")
    val IMMUTABLE_LIST_BUILDER =
        XClassName.get("com.google.common.collect", "ImmutableList", "Builder")
}

object GuavaUtilConcurrentTypeNames {
    val LISTENABLE_FUTURE = XClassName.get("com.google.common.util.concurrent", "ListenableFuture")
}

object RxJava2TypeNames {
    val FLOWABLE = XClassName.get("io.reactivex", "Flowable")
    val OBSERVABLE = XClassName.get("io.reactivex", "Observable")
    val MAYBE = XClassName.get("io.reactivex", "Maybe")
    val SINGLE = XClassName.get("io.reactivex", "Single")
    val COMPLETABLE = XClassName.get("io.reactivex", "Completable")
}

object RxJava3TypeNames {
    val FLOWABLE = XClassName.get("io.reactivex.rxjava3.core", "Flowable")
    val OBSERVABLE = XClassName.get("io.reactivex.rxjava3.core", "Observable")
    val MAYBE = XClassName.get("io.reactivex.rxjava3.core", "Maybe")
    val SINGLE = XClassName.get("io.reactivex.rxjava3.core", "Single")
    val COMPLETABLE = XClassName.get("io.reactivex.rxjava3.core", "Completable")
}

object ReactiveStreamsTypeNames {
    val PUBLISHER = XClassName.get("org.reactivestreams", "Publisher")
}

object RoomGuavaTypeNames {
    val GUAVA_ROOM = XClassName.get("$ROOM_PACKAGE.guava", "GuavaRoom")
    val GUAVA_ROOM_MARKER = XClassName.get("$ROOM_PACKAGE.guava", "GuavaRoomArtifactMarker")
}

object RoomGuavaMemberNames {
    val GUAVA_ROOM_CREATE_LISTENABLE_FUTURE = GUAVA_ROOM.packageMember("createListenableFuture")
}

object RoomRxJava2TypeNames {
    val RX_ROOM = XClassName.get(ROOM_PACKAGE, "RxRoom")
    val RX_ROOM_CREATE_FLOWABLE = "createFlowable"
    val RX_ROOM_CREATE_OBSERVABLE = "createObservable"
    val RX_EMPTY_RESULT_SET_EXCEPTION = XClassName.get(ROOM_PACKAGE, "EmptyResultSetException")
}

object RoomRxJava3TypeNames {
    val RX_ROOM = XClassName.get("$ROOM_PACKAGE.rxjava3", "RxRoom")
    val RX_ROOM_CREATE_FLOWABLE = "createFlowable"
    val RX_ROOM_CREATE_OBSERVABLE = "createObservable"
    val RX_EMPTY_RESULT_SET_EXCEPTION =
        XClassName.get("$ROOM_PACKAGE.rxjava3", "EmptyResultSetException")
}

object RoomPagingTypeNames {
    val LIMIT_OFFSET_PAGING_SOURCE =
        XClassName.get("$ROOM_PACKAGE.paging", "LimitOffsetPagingSource")
}

object RoomPagingGuavaTypeNames {
    val LIMIT_OFFSET_LISTENABLE_FUTURE_PAGING_SOURCE =
        XClassName.get("$ROOM_PACKAGE.paging.guava", "LimitOffsetListenableFuturePagingSource")
}

object RoomPagingRx2TypeNames {
    val LIMIT_OFFSET_RX_PAGING_SOURCE =
        XClassName.get("$ROOM_PACKAGE.paging.rxjava2", "LimitOffsetRxPagingSource")
}

object RoomPagingRx3TypeNames {
    val LIMIT_OFFSET_RX_PAGING_SOURCE =
        XClassName.get("$ROOM_PACKAGE.paging.rxjava3", "LimitOffsetRxPagingSource")
}

object RoomCoroutinesTypeNames {
    val COROUTINES_ROOM = XClassName.get(ROOM_PACKAGE, "CoroutinesRoom")
}

object KotlinTypeNames {
    val ANY = Any::class.asClassName()
    val UNIT = XClassName.get("kotlin", "Unit")
    val CONTINUATION = XClassName.get("kotlin.coroutines", "Continuation")
    val CHANNEL = XClassName.get("kotlinx.coroutines.channels", "Channel")
    val RECEIVE_CHANNEL = XClassName.get("kotlinx.coroutines.channels", "ReceiveChannel")
    val SEND_CHANNEL = XClassName.get("kotlinx.coroutines.channels", "SendChannel")
    val FLOW = XClassName.get("kotlinx.coroutines.flow", "Flow")
    val LAZY = XClassName.get("kotlin", "Lazy")
    val COLLECTIONS_KT = XClassName.get("kotlin.collections", "CollectionsKt")
    val SETS_KT = XClassName.get("kotlin.collections", "SetsKt")
    val MAPS_KT = XClassName.get("kotlin.collections", "MapsKt")
    val STRING_BUILDER = XClassName.get("kotlin.text", "StringBuilder")
    val LINKED_HASH_MAP = XClassName.get("kotlin.collections", "LinkedHashMap")
}

object RoomMemberNames {
    val DB_UTIL_QUERY = RoomTypeNames.DB_UTIL.packageMember("query")
    val DB_UTIL_FOREIGN_KEY_CHECK = RoomTypeNames.DB_UTIL.packageMember("foreignKeyCheck")
    val DB_UTIL_DROP_FTS_SYNC_TRIGGERS = RoomTypeNames.DB_UTIL.packageMember("dropFtsSyncTriggers")
    val DB_UTIL_PERFORM_SUSPENDING = RoomTypeNames.DB_UTIL.packageMember("performSuspending")
    val DB_UTIL_PERFORM_BLOCKING = RoomTypeNames.DB_UTIL.packageMember("performBlocking")
    val DB_UTIL_PERFORM_IN_TRANSACTION_SUSPENDING =
        RoomTypeNames.DB_UTIL.packageMember("performInTransactionSuspending")
    val CURSOR_UTIL_GET_COLUMN_INDEX = RoomTypeNames.CURSOR_UTIL.packageMember("getColumnIndex")
    val CURSOR_UTIL_GET_COLUMN_INDEX_OR_THROW =
        RoomTypeNames.CURSOR_UTIL.packageMember("getColumnIndexOrThrow")
    val CURSOR_UTIL_WRAP_MAPPED_COLUMNS =
        RoomTypeNames.CURSOR_UTIL.packageMember("wrapMappedColumns")
    val ROOM_SQL_QUERY_ACQUIRE =
        RoomTypeNames.ROOM_SQL_QUERY.companionMember("acquire", isJvmStatic = true)
    val ROOM_DATABASE_WITH_TRANSACTION = RoomTypeNames.ROOM_DB_KT.packageMember("withTransaction")
    val TABLE_INFO_READ = RoomTypeNames.TABLE_INFO.companionMember("read", isJvmStatic = true)
    val FTS_TABLE_INFO_READ =
        RoomTypeNames.FTS_TABLE_INFO.companionMember("read", isJvmStatic = true)
    val VIEW_INFO_READ = RoomTypeNames.VIEW_INFO.companionMember("read", isJvmStatic = true)
}

object SQLiteDriverMemberNames {
    val CONNECTION_EXEC_SQL = SQLiteDriverTypeNames.SQLITE.packageMember("execSQL")
}

val DEFERRED_TYPES =
    listOf(
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

fun XTypeName.defaultValue(): String {
    return if (!isPrimitive) {
        "null"
    } else if (this == XTypeName.PRIMITIVE_BOOLEAN) {
        "false"
    } else if (this == XTypeName.PRIMITIVE_DOUBLE) {
        "0.0"
    } else if (this == XTypeName.PRIMITIVE_FLOAT) {
        "0f"
    } else {
        "0"
    }
}

fun CallableTypeSpecBuilder(
    language: CodeLanguage,
    parameterTypeName: XTypeName,
    callBody: XFunSpec.Builder.() -> Unit
) =
    XTypeSpec.anonymousClassBuilder(language, "").apply {
        addSuperinterface(CommonTypeNames.CALLABLE.parametrizedBy(parameterTypeName))
        addFunction(
            XFunSpec.builder(
                    language = language,
                    name = "call",
                    visibility = VisibilityModifier.PUBLIC,
                    isOverride = true
                )
                .apply {
                    returns(parameterTypeName)
                    callBody()
                }
                .apply(
                    javaMethodBuilder = { addException(JTypeName.get(Exception::class.java)) },
                    kotlinFunctionBuilder = {}
                )
                .build()
        )
    }

fun Function1TypeSpec(
    language: CodeLanguage,
    parameterTypeName: XTypeName,
    parameterName: String,
    returnTypeName: XTypeName,
    callBody: XFunSpec.Builder.() -> Unit
) =
    XTypeSpec.anonymousClassBuilder(language, "")
        .apply {
            superclass(
                Function1::class.asClassName().parametrizedBy(parameterTypeName, returnTypeName)
            )
            addFunction(
                XFunSpec.builder(
                        language = language,
                        name = "invoke",
                        visibility = VisibilityModifier.PUBLIC,
                        isOverride = true
                    )
                    .apply {
                        addParameter(parameterTypeName, parameterName)
                        returns(returnTypeName)
                        callBody()
                    }
                    .build()
            )
        }
        .build()

/**
 * Generates a code block that invokes a function with a functional type as last parameter.
 *
 * For Java (jvmTarget >= 8) it will generate:
 * ```
 * <functionName>(<args>, (<lambdaSpec.paramName>) -> <lambdaSpec.body>);
 * ```
 *
 * For Java (jvmTarget < 8) it will generate:
 * ```
 * <functionName>(<args>, new Function1<>() { <lambdaSpec.body> });
 * ```
 *
 * For Kotlin it will generate:
 * ```
 * <functionName>(<args>) { <lambdaSpec.body> }
 * ```
 *
 * The ideal usage of this utility function is to generate code that invokes the various
 * `DBUtil.perform*()` APIs for interacting with the database connection in DAOs.
 */
fun InvokeWithLambdaParameter(
    scope: CodeGenScope,
    functionName: XMemberName,
    argFormat: List<String>,
    args: List<Any>,
    continuationParamName: String? = null,
    lambdaSpec: LambdaSpec
): XCodeBlock =
    XCodeBlock.builder(scope.language)
        .apply {
            check(argFormat.size == args.size)
            when (language) {
                CodeLanguage.JAVA -> {
                    if (lambdaSpec.javaLambdaSyntaxAvailable) {
                        val argsFormatString = argFormat.joinToString(separator = ", ")
                        add(
                            "%M($argsFormatString, (%L) -> {\n",
                            functionName,
                            *args.toTypedArray(),
                            lambdaSpec.parameterName
                        )
                        indent()
                        val bodyScope = scope.fork()
                        with(lambdaSpec) { bodyScope.builder.body(bodyScope) }
                        add(bodyScope.generate())
                        unindent()
                        add("}")
                        if (continuationParamName != null) {
                            add(", %L", continuationParamName)
                        }
                        add(");\n")
                    } else {
                        val adjustedArgsFormatString =
                            buildList {
                                    addAll(argFormat)
                                    add("%L") // the anonymous function
                                    if (continuationParamName != null) {
                                        add("%L")
                                    }
                                }
                                .joinToString(separator = ", ")
                        val adjustedArgs = buildList {
                            addAll(args)
                            add(
                                Function1TypeSpec(
                                    language = language,
                                    parameterTypeName = lambdaSpec.parameterTypeName,
                                    parameterName = lambdaSpec.parameterName,
                                    returnTypeName = lambdaSpec.returnTypeName,
                                    callBody = {
                                        val bodyScope = scope.fork()
                                        with(lambdaSpec) { bodyScope.builder.body(bodyScope) }
                                        addCode(bodyScope.generate())
                                    }
                                )
                            )
                            if (continuationParamName != null) {
                                add(continuationParamName)
                            }
                        }
                        add(
                            "%M($adjustedArgsFormatString);\n",
                            functionName,
                            *adjustedArgs.toTypedArray(),
                        )
                    }
                }
                CodeLanguage.KOTLIN -> {
                    val argsFormatString = argFormat.joinToString(separator = ", ")
                    if (lambdaSpec.parameterTypeName.rawTypeName != KotlinTypeNames.CONTINUATION) {
                        add(
                            "%M($argsFormatString) { %L ->\n",
                            functionName,
                            *args.toTypedArray(),
                            lambdaSpec.parameterName
                        )
                    } else {
                        add(
                            "%M($argsFormatString) {\n",
                            functionName,
                            *args.toTypedArray(),
                        )
                    }
                    indent()
                    val bodyScope = scope.fork()
                    with(lambdaSpec) { bodyScope.builder.body(bodyScope) }
                    add(bodyScope.generate())
                    unindent()
                    add("}\n")
                }
            }
        }
        .build()

/** Describes the lambda to be generated with [InvokeWithLambdaParameter]. */
abstract class LambdaSpec(
    val parameterTypeName: XTypeName,
    val parameterName: String,
    val returnTypeName: XTypeName,
    val javaLambdaSyntaxAvailable: Boolean
) {
    abstract fun XCodeBlock.Builder.body(scope: CodeGenScope)
}

/**
 * Generates an array literal with the given [values]
 *
 * Example: `ArrayLiteral(XTypeName.PRIMITIVE_INT, 1, 2, 3)`
 *
 * For Java will produce: `new int[] {1, 2, 3}`
 *
 * For Kotlin will produce: `intArrayOf(1, 2, 3)`,
 */
fun ArrayLiteral(language: CodeLanguage, type: XTypeName, vararg values: Any): XCodeBlock {
    val space =
        when (language) {
            CodeLanguage.JAVA -> "%W"
            CodeLanguage.KOTLIN -> " "
        }
    val initExpr =
        when (language) {
            CodeLanguage.JAVA -> XCodeBlock.of(language, "new %T[] ", type)
            CodeLanguage.KOTLIN -> XCodeBlock.of(language, getArrayOfFunction(type))
        }
    val openingChar =
        when (language) {
            CodeLanguage.JAVA -> "{"
            CodeLanguage.KOTLIN -> "("
        }
    val closingChar =
        when (language) {
            CodeLanguage.JAVA -> "}"
            CodeLanguage.KOTLIN -> ")"
        }
    return XCodeBlock.of(
        language,
        "%L$openingChar%L$closingChar",
        initExpr,
        XCodeBlock.builder(language)
            .apply {
                val joining =
                    Array(values.size) { i ->
                        XCodeBlock.of(
                            language,
                            if (type == CommonTypeNames.STRING) "%S" else "%L",
                            values[i]
                        )
                    }
                val placeholders = joining.joinToString(separator = ",$space") { "%L" }
                add(placeholders, *joining)
            }
            .build()
    )
}

/**
 * Generates a 2D array literal where the value at `i`,`j` will be produced by `valueProducer. For
 * example:
 * ```
 * DoubleArrayLiteral(XTypeName.PRIMITIVE_INT, 2, { _ -> 3 }, { i, j -> i + j })
 * ```
 *
 * For Java will produce:
 * ```
 * new int[][] {
 *   {0, 1, 2},
 *   {1, 2, 3}
 * }
 * ```
 *
 * For Kotlin will produce:
 * ```
 * arrayOf(
 *   intArrayOf(0, 1, 2),
 *   intArrayOf(1, 2, 3)
 * )
 * ```
 */
fun DoubleArrayLiteral(
    language: CodeLanguage,
    type: XTypeName,
    rowSize: Int,
    columnSizeProducer: (Int) -> Int,
    valueProducer: (Int, Int) -> Any
): XCodeBlock {
    val space =
        when (language) {
            CodeLanguage.JAVA -> "%W"
            CodeLanguage.KOTLIN -> " "
        }
    val outerInit =
        when (language) {
            CodeLanguage.JAVA -> XCodeBlock.of(language, "new %T[][] ", type)
            CodeLanguage.KOTLIN -> XCodeBlock.of(language, "arrayOf")
        }
    val innerInit =
        when (language) {
            CodeLanguage.JAVA -> XCodeBlock.of(language, "", type)
            CodeLanguage.KOTLIN -> XCodeBlock.of(language, getArrayOfFunction(type))
        }
    val openingChar =
        when (language) {
            CodeLanguage.JAVA -> "{"
            CodeLanguage.KOTLIN -> "("
        }
    val closingChar =
        when (language) {
            CodeLanguage.JAVA -> "}"
            CodeLanguage.KOTLIN -> ")"
        }
    return XCodeBlock.of(
        language,
        "%L$openingChar%L$closingChar",
        outerInit,
        XCodeBlock.builder(language)
            .apply {
                val joining =
                    Array(rowSize) { i ->
                        XCodeBlock.of(
                            language,
                            "%L$openingChar%L$closingChar",
                            innerInit,
                            XCodeBlock.builder(language)
                                .apply {
                                    val joining =
                                        Array(columnSizeProducer(i)) { j ->
                                            XCodeBlock.of(
                                                language,
                                                if (type == CommonTypeNames.STRING) "%S" else "%L",
                                                valueProducer(i, j)
                                            )
                                        }
                                    val placeholders =
                                        joining.joinToString(separator = ",$space") { "%L" }
                                    add(placeholders, *joining)
                                }
                                .build()
                        )
                    }
                val placeholders = joining.joinToString(separator = ",$space") { "%L" }
                add(placeholders, *joining)
            }
            .build()
    )
}

private fun getArrayOfFunction(type: XTypeName) =
    when (type) {
        XTypeName.PRIMITIVE_BOOLEAN -> "booleanArrayOf"
        XTypeName.PRIMITIVE_BYTE -> "byteArrayOf"
        XTypeName.PRIMITIVE_SHORT -> "shortArrayOf"
        XTypeName.PRIMITIVE_INT -> "intArrayOf"
        XTypeName.PRIMITIVE_LONG -> "longArrayOf"
        XTypeName.PRIMITIVE_CHAR -> "charArrayOf"
        XTypeName.PRIMITIVE_FLOAT -> "floatArrayOf"
        XTypeName.PRIMITIVE_DOUBLE -> "doubleArrayOf"
        else -> "arrayOf"
    }

fun getToArrayFunction(type: XTypeName) =
    when (type) {
        XTypeName.PRIMITIVE_BOOLEAN -> "toBooleanArray()"
        XTypeName.PRIMITIVE_BYTE -> "toByteArray()"
        XTypeName.PRIMITIVE_SHORT -> "toShortArray()"
        XTypeName.PRIMITIVE_INT -> "toIntArray()"
        XTypeName.PRIMITIVE_LONG -> "toLongArray()"
        XTypeName.PRIMITIVE_CHAR -> "toCharArray()"
        XTypeName.PRIMITIVE_FLOAT -> "toFloatArray()"
        XTypeName.PRIMITIVE_DOUBLE -> "toDoubleArray()"
        else -> error("Provided type expected to be primitive. Found: $type")
    }

/** Code of expression for [Collection.size] in Kotlin, and [java.util.Collection.size] for Java. */
fun CollectionsSizeExprCode(language: CodeLanguage, varName: String) =
    XCodeBlock.of(
        language,
        when (language) {
            CodeLanguage.JAVA -> "%L.size()" // java.util.Collections.size()
            CodeLanguage.KOTLIN -> "%L.size" // kotlin.collections.Collection.size
        },
        varName
    )

/** Code of expression for [Array.size] in Kotlin, and `arr.length` for Java. */
fun ArraySizeExprCode(language: CodeLanguage, varName: String) =
    XCodeBlock.of(
        language,
        when (language) {
            CodeLanguage.JAVA -> "%L.length" // Just `arr.length`
            CodeLanguage.KOTLIN -> "%L.size" // kotlin.Array.size and primitives (e.g. IntArray)
        },
        varName
    )

/** Code of expression for [Map.keys] in Kotlin, and [java.util.Map.keySet] for Java. */
fun MapKeySetExprCode(language: CodeLanguage, varName: String) =
    XCodeBlock.of(
        language,
        when (language) {
            CodeLanguage.JAVA -> "%L.keySet()" // java.util.Map.keySet()
            CodeLanguage.KOTLIN -> "%L.keys" // kotlin.collections.Map.keys
        },
        varName
    )
