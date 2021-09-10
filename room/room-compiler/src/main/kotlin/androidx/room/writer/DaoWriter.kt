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

package androidx.room.writer

import androidx.room.compiler.processing.MethodSpecHelper
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.addOriginatingElement
import androidx.room.compiler.processing.isVoid
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.T
import androidx.room.ext.W
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.processor.OnConflictProcessor
import androidx.room.solver.CodeGenScope
import androidx.room.solver.KotlinDefaultMethodDelegateBinder
import androidx.room.solver.types.getRequiredTypeConverters
import androidx.room.vo.Dao
import androidx.room.vo.InsertionMethod
import androidx.room.vo.KotlinBoxedPrimitiveMethodDelegate
import androidx.room.vo.KotlinDefaultMethodDelegate
import androidx.room.vo.QueryMethod
import androidx.room.vo.RawQueryMethod
import androidx.room.vo.ReadQueryMethod
import androidx.room.vo.ShortcutEntity
import androidx.room.vo.ShortcutMethod
import androidx.room.vo.TransactionMethod
import androidx.room.vo.UpdateMethod
import androidx.room.vo.WriteQueryMethod
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

/**
 * Creates the implementation for a class annotated with Dao.
 */
class DaoWriter(
    val dao: Dao,
    private val dbElement: XElement,
    val processingEnv: XProcessingEnv
) :
    ClassWriter(dao.typeName) {
    private val declaredDao = dao.element.type

    companion object {
        const val GET_LIST_OF_TYPE_CONVERTERS_METHOD = "getRequiredConverters"
        // TODO nothing prevents this from conflicting, we should fix.
        val dbField: FieldSpec = FieldSpec
            .builder(RoomTypeNames.ROOM_DB, "__db", PRIVATE, FINAL)
            .build()

        private fun shortcutEntityFieldNamePart(shortcutEntity: ShortcutEntity): String {
            return if (shortcutEntity.isPartialEntity) {
                typeNameToFieldName(shortcutEntity.pojo.typeName) + "As" +
                    typeNameToFieldName(shortcutEntity.entityTypeName)
            } else {
                typeNameToFieldName(shortcutEntity.entityTypeName)
            }
        }

        private fun typeNameToFieldName(typeName: TypeName?): String {
            return if (typeName is ClassName) {
                typeName.simpleName()
            } else {
                typeName.toString().replace('.', '_').stripNonJava()
            }
        }
    }

    override fun createTypeSpecBuilder(): TypeSpec.Builder {
        val builder = TypeSpec.classBuilder(dao.implTypeName)
        /**
         * For prepared statements that perform insert/update/delete, we check if there are any
         * arguments of variable length (e.g. "IN (:var)"). If not, we should re-use the statement.
         * This requires more work but creates good performance.
         */
        val groupedPreparedQueries = dao.queryMethods
            .filterIsInstance<WriteQueryMethod>()
            .groupBy { it.parameters.any { it.queryParamAdapter?.isMultiple ?: true } }
        // queries that can be prepared ahead of time
        val preparedQueries = groupedPreparedQueries[false] ?: emptyList()
        // queries that must be rebuilt every single time
        val oneOffPreparedQueries = groupedPreparedQueries[true] ?: emptyList()
        val shortcutMethods = createInsertionMethods() +
            createDeletionMethods() + createUpdateMethods() + createTransactionMethods() +
            createPreparedQueries(preparedQueries)

        builder.apply {
            addOriginatingElement(dbElement)
            addModifiers(PUBLIC)
            addModifiers(FINAL)
            if (dao.element.isInterface()) {
                addSuperinterface(dao.typeName)
            } else {
                superclass(dao.typeName)
            }
            addField(dbField)
            val dbParam = ParameterSpec
                .builder(dao.constructorParamType ?: dbField.type, dbField.name).build()

            addMethod(createConstructor(dbParam, shortcutMethods, dao.constructorParamType != null))

            shortcutMethods.forEach {
                addMethod(it.methodImpl)
            }

            dao.queryMethods.filterIsInstance<ReadQueryMethod>().forEach { method ->
                addMethod(createSelectMethod(method))
            }
            oneOffPreparedQueries.forEach {
                addMethod(createPreparedQueryMethod(it))
            }
            dao.rawQueryMethods.forEach {
                addMethod(createRawQueryMethod(it))
            }
            dao.kotlinDefaultMethodDelegates.forEach {
                addMethod(createDefaultMethodDelegate(it))
            }

            dao.delegatingMethods.forEach {
                addMethod(createDelegatingMethod(it))
            }
            // keep this the last one to be generated because used custom converters will register
            // fields with a payload which we collect in dao to report used Type Converters.
            addMethod(createConverterListMethod())
        }
        return builder
    }

    private fun createConverterListMethod(): MethodSpec {
        return MethodSpec.methodBuilder(GET_LIST_OF_TYPE_CONVERTERS_METHOD).apply {
            addModifiers(STATIC, PUBLIC)
            returns(
                ParameterizedTypeName.get(
                    CommonTypeNames.LIST,
                    ParameterizedTypeName.get(
                        ClassName.get(Class::class.java),
                        WildcardTypeName.subtypeOf(Object::class.java)
                    )
                )
            )
            val requiredTypeConverters = getRequiredTypeConverters()
            if (requiredTypeConverters.isEmpty()) {
                addStatement("return $T.emptyList()", ClassName.get(Collections::class.java))
            } else {
                val placeholders = requiredTypeConverters.joinToString(",") {
                    "$T.class"
                }
                val args = arrayOf(ClassName.get(Arrays::class.java)) + requiredTypeConverters
                addStatement("return $T.asList($placeholders)", *args)
            }
        }.build()
    }

    private fun createPreparedQueries(
        preparedQueries: List<WriteQueryMethod>
    ): List<PreparedStmtQuery> {
        return preparedQueries.map { method ->
            val fieldSpec = getOrCreateField(PreparedStatementField(method))
            val queryWriter = QueryWriter(method)
            val fieldImpl = PreparedStatementWriter(queryWriter)
                .createAnonymous(this@DaoWriter, dbField)
            val methodBody =
                createPreparedQueryMethodBody(method, fieldSpec, queryWriter)
            PreparedStmtQuery(
                mapOf(
                    PreparedStmtQuery.NO_PARAM_FIELD
                        to (fieldSpec to fieldImpl)
                ),
                methodBody
            )
        }
    }

    private fun createPreparedQueryMethodBody(
        method: WriteQueryMethod,
        preparedStmtField: FieldSpec,
        queryWriter: QueryWriter
    ): MethodSpec {
        val scope = CodeGenScope(this)
        method.preparedQueryResultBinder.executeAndReturn(
            prepareQueryStmtBlock = {
                val stmtName = getTmpVar("_stmt")
                builder().apply {
                    addStatement(
                        "final $T $L = $N.acquire()",
                        SupportDbTypeNames.SQLITE_STMT, stmtName, preparedStmtField
                    )
                }
                queryWriter.bindArgs(stmtName, emptyList(), this)
                stmtName
            },
            preparedStmtField = preparedStmtField.name,
            dbField = dbField,
            scope = scope
        )
        return overrideWithoutAnnotations(method.element, declaredDao)
            .addCode(scope.generate())
            .build()
    }

    private fun createTransactionMethods(): List<PreparedStmtQuery> {
        return dao.transactionMethods.map {
            PreparedStmtQuery(emptyMap(), createTransactionMethodBody(it))
        }
    }

    private fun createTransactionMethodBody(method: TransactionMethod): MethodSpec {
        val scope = CodeGenScope(this)
        method.methodBinder.executeAndReturn(
            returnType = method.returnType,
            parameterNames = method.parameterNames,
            daoName = dao.typeName,
            daoImplName = dao.implTypeName,
            dbField = dbField,
            scope = scope
        )
        return overrideWithoutAnnotations(method.element, declaredDao)
            .addCode(scope.generate())
            .build()
    }

    private fun createConstructor(
        dbParam: ParameterSpec,
        shortcutMethods: List<PreparedStmtQuery>,
        callSuper: Boolean
    ): MethodSpec {
        return MethodSpec.constructorBuilder().apply {
            addParameter(dbParam)
            addModifiers(PUBLIC)
            if (callSuper) {
                addStatement("super($N)", dbParam)
            }
            addStatement("this.$N = $N", dbField, dbParam)
            shortcutMethods.asSequence().filterNot {
                it.fields.isEmpty()
            }.map {
                it.fields.values
            }.flatten().groupBy {
                it.first.name
            }.map {
                it.value.first()
            }.forEach {
                addStatement("this.$N = $L", it.first, it.second)
            }
        }.build()
    }

    private fun createSelectMethod(method: ReadQueryMethod): MethodSpec {
        return overrideWithoutAnnotations(method.element, declaredDao).apply {
            addCode(createQueryMethodBody(method))
        }.build()
    }

    private fun createRawQueryMethod(method: RawQueryMethod): MethodSpec {
        return overrideWithoutAnnotations(method.element, declaredDao).apply {
            val scope = CodeGenScope(this@DaoWriter)
            val roomSQLiteQueryVar: String
            val queryParam = method.runtimeQueryParam
            val shouldReleaseQuery: Boolean

            when {
                queryParam?.isString() == true -> {
                    roomSQLiteQueryVar = scope.getTmpVar("_statement")
                    shouldReleaseQuery = true
                    addStatement(
                        "$T $L = $T.acquire($L, 0)",
                        RoomTypeNames.ROOM_SQL_QUERY,
                        roomSQLiteQueryVar,
                        RoomTypeNames.ROOM_SQL_QUERY,
                        queryParam.paramName
                    )
                }
                queryParam?.isSupportQuery() == true -> {
                    shouldReleaseQuery = false
                    roomSQLiteQueryVar = scope.getTmpVar("_internalQuery")
                    // move it to a final variable so that the generated code can use it inside
                    // callback blocks in java 7
                    addStatement(
                        "final $T $L = $N",
                        queryParam.type,
                        roomSQLiteQueryVar,
                        queryParam.paramName
                    )
                }
                else -> {
                    // try to generate compiling code. we would've already reported this error
                    roomSQLiteQueryVar = scope.getTmpVar("_statement")
                    shouldReleaseQuery = false
                    addStatement(
                        "$T $L = $T.acquire($L, 0)",
                        RoomTypeNames.ROOM_SQL_QUERY,
                        roomSQLiteQueryVar,
                        RoomTypeNames.ROOM_SQL_QUERY,
                        "missing query parameter"
                    )
                }
            }
            if (method.returnsValue) {
                // don't generate code because it will create 1 more error. The original error is
                // already reported by the processor.
                method.queryResultBinder.convertAndReturn(
                    roomSQLiteQueryVar = roomSQLiteQueryVar,
                    canReleaseQuery = shouldReleaseQuery,
                    dbField = dbField,
                    inTransaction = method.inTransaction,
                    scope = scope
                )
            }
            addCode(scope.builder().build())
        }.build()
    }

    private fun createPreparedQueryMethod(method: WriteQueryMethod): MethodSpec {
        return overrideWithoutAnnotations(method.element, declaredDao).apply {
            addCode(createPreparedQueryMethodBody(method))
        }.build()
    }

    /**
     * Groups all insertion methods based on the insert statement they will use then creates all
     * field specs, EntityInsertionAdapterWriter and actual insert methods.
     */
    private fun createInsertionMethods(): List<PreparedStmtQuery> {
        return dao.insertionMethods
            .map { insertionMethod ->
                val onConflict = OnConflictProcessor.onConflictText(insertionMethod.onConflict)
                val entities = insertionMethod.entities

                val fields = entities.mapValues {
                    val spec = getOrCreateField(InsertionMethodField(it.value, onConflict))
                    val impl = EntityInsertionAdapterWriter.create(it.value, onConflict)
                        .createAnonymous(this@DaoWriter, dbField.name)
                    spec to impl
                }
                val methodImpl = overrideWithoutAnnotations(
                    insertionMethod.element,
                    declaredDao
                ).apply {
                    addCode(createInsertionMethodBody(insertionMethod, fields))
                }.build()
                PreparedStmtQuery(fields, methodImpl)
            }
    }

    private fun createInsertionMethodBody(
        method: InsertionMethod,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>
    ): CodeBlock {
        if (insertionAdapters.isEmpty()) {
            return CodeBlock.builder().build()
        }

        val scope = CodeGenScope(this)

        method.methodBinder.convertAndReturn(
            parameters = method.parameters,
            insertionAdapters = insertionAdapters,
            dbField = dbField,
            scope = scope
        )
        return scope.builder().build()
    }

    /**
     * Creates EntityUpdateAdapter for each deletion method.
     */
    private fun createDeletionMethods(): List<PreparedStmtQuery> {
        return createShortcutMethods(dao.deletionMethods, "deletion") { _, entity ->
            EntityDeletionAdapterWriter.create(entity)
                .createAnonymous(this@DaoWriter, dbField.name)
        }
    }

    /**
     * Creates EntityUpdateAdapter for each @Update method.
     */
    private fun createUpdateMethods(): List<PreparedStmtQuery> {
        return createShortcutMethods(dao.updateMethods, "update") { update, entity ->
            val onConflict = OnConflictProcessor.onConflictText(update.onConflictStrategy)
            EntityUpdateAdapterWriter.create(entity, onConflict)
                .createAnonymous(this@DaoWriter, dbField.name)
        }
    }

    private fun <T : ShortcutMethod> createShortcutMethods(
        methods: List<T>,
        methodPrefix: String,
        implCallback: (T, ShortcutEntity) -> TypeSpec
    ): List<PreparedStmtQuery> {
        return methods.mapNotNull { method ->
            val entities = method.entities
            if (entities.isEmpty()) {
                null
            } else {
                val onConflict = if (method is UpdateMethod) {
                    OnConflictProcessor.onConflictText(method.onConflictStrategy)
                } else {
                    ""
                }
                val fields = entities.mapValues {
                    val spec = getOrCreateField(
                        DeleteOrUpdateAdapterField(it.value, methodPrefix, onConflict)
                    )
                    val impl = implCallback(method, it.value)
                    spec to impl
                }
                val methodSpec = overrideWithoutAnnotations(method.element, declaredDao).apply {
                    addCode(createDeleteOrUpdateMethodBody(method, fields))
                }.build()
                PreparedStmtQuery(fields, methodSpec)
            }
        }
    }

    private fun createDeleteOrUpdateMethodBody(
        method: ShortcutMethod,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>
    ): CodeBlock {
        if (adapters.isEmpty() || method.methodBinder == null) {
            return CodeBlock.builder().build()
        }
        val scope = CodeGenScope(this)

        method.methodBinder.convertAndReturn(
            parameters = method.parameters,
            adapters = adapters,
            dbField = dbField,
            scope = scope
        )
        return scope.builder().build()
    }

    private fun createPreparedQueryMethodBody(method: WriteQueryMethod): CodeBlock {
        val scope = CodeGenScope(this)
        method.preparedQueryResultBinder.executeAndReturn(
            prepareQueryStmtBlock = {
                val queryWriter = QueryWriter(method)
                val sqlVar = getTmpVar("_sql")
                val stmtVar = getTmpVar("_stmt")
                val listSizeArgs = queryWriter.prepareQuery(sqlVar, this)
                builder().apply {
                    addStatement(
                        "final $T $L = $N.compileStatement($L)",
                        SupportDbTypeNames.SQLITE_STMT, stmtVar, dbField, sqlVar
                    )
                }
                queryWriter.bindArgs(stmtVar, listSizeArgs, this)
                stmtVar
            },
            preparedStmtField = null,
            dbField = dbField,
            scope = scope
        )
        return scope.generate()
    }

    private fun createQueryMethodBody(method: ReadQueryMethod): CodeBlock {
        val queryWriter = QueryWriter(method)
        val scope = CodeGenScope(this)
        val sqlVar = scope.getTmpVar("_sql")
        val roomSQLiteQueryVar = scope.getTmpVar("_statement")
        queryWriter.prepareReadAndBind(sqlVar, roomSQLiteQueryVar, scope)
        method.queryResultBinder.convertAndReturn(
            roomSQLiteQueryVar = roomSQLiteQueryVar,
            canReleaseQuery = true,
            dbField = dbField,
            inTransaction = method.inTransaction,
            scope = scope
        )
        return scope.builder().build()
    }

    private fun createDefaultMethodDelegate(method: KotlinDefaultMethodDelegate): MethodSpec {
        val scope = CodeGenScope(this)
        return overrideWithoutAnnotations(method.element, declaredDao).apply {
            KotlinDefaultMethodDelegateBinder.executeAndReturn(
                daoName = dao.typeName,
                daoImplName = dao.implTypeName,
                methodName = method.element.name,
                returnType = method.element.returnType,
                parameterNames = method.element.parameters.map { it.name },
                scope = scope
            )
            addCode(scope.builder().build())
        }.build()
    }

    private fun createDelegatingMethod(method: KotlinBoxedPrimitiveMethodDelegate): MethodSpec {
        return overrideWithoutAnnotations(method.element, declaredDao).apply {

            val args = method.concreteMethod.parameters.map {
                val paramTypename = it.type.typeName
                if (paramTypename.isBoxedPrimitive()) {
                    CodeBlock.of("$L", paramTypename, it.name.toString())
                } else {
                    CodeBlock.of("($T) $L", paramTypename.unbox(), it.name.toString())
                }
            }
            if (method.element.returnType.isVoid()) {
                addStatement("$L($L)", method.element.name, CodeBlock.join(args, ",$W"))
            } else {
                addStatement("return $L($L)", method.element.name, CodeBlock.join(args, ",$W"))
            }
        }.build()
    }

    private fun overrideWithoutAnnotations(
        elm: XMethodElement,
        owner: XType
    ): MethodSpec.Builder {
        return MethodSpecHelper.overridingWithFinalParams(elm, owner)
    }

    /**
     * Represents a query statement prepared in Dao implementation.
     *
     * @param fields This map holds all the member fields necessary for this query. The key is the
     * corresponding parameter name in the defining query method. The value is a pair from the field
     * declaration to definition.
     * @param methodImpl The body of the query method implementation.
     */
    data class PreparedStmtQuery(
        val fields: Map<String, Pair<FieldSpec, TypeSpec>>,
        val methodImpl: MethodSpec
    ) {
        companion object {
            // The key to be used in `fields` where the method requires a field that is not
            // associated with any of its parameters
            const val NO_PARAM_FIELD = "-"
        }
    }

    private class InsertionMethodField(
        val shortcutEntity: ShortcutEntity,
        val onConflictText: String
    ) : SharedFieldSpec(
        baseName = "insertionAdapterOf${shortcutEntityFieldNamePart(shortcutEntity)}",
        type = ParameterizedTypeName.get(
            RoomTypeNames.INSERTION_ADAPTER, shortcutEntity.pojo.typeName
        )
    ) {
        override fun getUniqueKey(): String {
            return "${shortcutEntity.pojo.typeName}-${shortcutEntity.entityTypeName}$onConflictText"
        }

        override fun prepare(writer: ClassWriter, builder: FieldSpec.Builder) {
            builder.addModifiers(FINAL, PRIVATE)
        }
    }

    class DeleteOrUpdateAdapterField(
        val shortcutEntity: ShortcutEntity,
        val methodPrefix: String,
        val onConflictText: String
    ) : SharedFieldSpec(
        baseName = "${methodPrefix}AdapterOf${shortcutEntityFieldNamePart(shortcutEntity)}",
        type = ParameterizedTypeName.get(
            RoomTypeNames.DELETE_OR_UPDATE_ADAPTER, shortcutEntity.pojo.typeName
        )
    ) {
        override fun prepare(writer: ClassWriter, builder: FieldSpec.Builder) {
            builder.addModifiers(PRIVATE, FINAL)
        }

        override fun getUniqueKey(): String {
            return "${shortcutEntity.pojo.typeName}-${shortcutEntity.entityTypeName}" +
                "$methodPrefix$onConflictText"
        }
    }

    class PreparedStatementField(val method: QueryMethod) : SharedFieldSpec(
        "preparedStmtOf${method.name.capitalize(Locale.US)}", RoomTypeNames.SHARED_SQLITE_STMT
    ) {
        override fun prepare(writer: ClassWriter, builder: FieldSpec.Builder) {
            builder.addModifiers(PRIVATE, FINAL)
        }

        override fun getUniqueKey(): String {
            return method.query.original
        }
    }
}
