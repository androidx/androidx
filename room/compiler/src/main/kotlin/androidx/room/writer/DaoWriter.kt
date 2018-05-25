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

import androidx.room.ext.L
import androidx.room.ext.N
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.parser.QueryType
import androidx.room.processor.OnConflictProcessor
import androidx.room.solver.CodeGenScope
import androidx.room.vo.Dao
import androidx.room.vo.Entity
import androidx.room.vo.InsertionMethod
import androidx.room.vo.QueryMethod
import androidx.room.vo.RawQueryMethod
import androidx.room.vo.ShortcutMethod
import androidx.room.vo.TransactionMethod
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import me.eugeniomarletti.kotlin.metadata.shadow.load.java.JvmAbi
import stripNonJava
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

/**
 * Creates the implementation for a class annotated with Dao.
 */
class DaoWriter(val dao: Dao, val processingEnv: ProcessingEnvironment)
    : ClassWriter(dao.typeName) {
    private val declaredDao = MoreTypes.asDeclared(dao.element.asType())

    companion object {
        // TODO nothing prevents this from conflicting, we should fix.
        val dbField: FieldSpec = FieldSpec
                .builder(RoomTypeNames.ROOM_DB, "__db", PRIVATE, FINAL)
                .build()

        private fun typeNameToFieldName(typeName: TypeName?): String {
            if (typeName is ClassName) {
                return typeName.simpleName()
            } else {
                return typeName.toString().replace('.', '_').stripNonJava()
            }
        }
    }

    override fun createTypeSpecBuilder(): TypeSpec.Builder {
        val builder = TypeSpec.classBuilder(dao.implTypeName)
        /**
         * if delete / update query method wants to return modified rows, we need prepared query.
         * in that case, if args are dynamic, we cannot re-use the query, if not, we should re-use
         * it. this requires more work but creates good performance.
         */
        val groupedDeleteUpdate = dao.queryMethods
                .filter { it.query.type == QueryType.DELETE || it.query.type == QueryType.UPDATE }
                .groupBy { it.parameters.any { it.queryParamAdapter?.isMultiple ?: true } }
        // delete queries that can be prepared ahead of time
        val preparedDeleteOrUpdateQueries = groupedDeleteUpdate[false] ?: emptyList()
        // delete queries that must be rebuild every single time
        val oneOffDeleteOrUpdateQueries = groupedDeleteUpdate[true] ?: emptyList()
        val shortcutMethods = createInsertionMethods() +
                createDeletionMethods() + createUpdateMethods() + createTransactionMethods() +
                createPreparedDeleteOrUpdateQueries(preparedDeleteOrUpdateQueries)

        builder.apply {
            addModifiers(PUBLIC)
            if (dao.element.kind == ElementKind.INTERFACE) {
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

            dao.queryMethods.filter { it.query.type == QueryType.SELECT }.forEach { method ->
                addMethod(createSelectMethod(method))
            }
            oneOffDeleteOrUpdateQueries.forEach {
                addMethod(createDeleteOrUpdateQueryMethod(it))
            }
            dao.rawQueryMethods.forEach {
                addMethod(createRawQueryMethod(it))
            }
        }
        return builder
    }

    private fun createPreparedDeleteOrUpdateQueries(
            preparedDeleteQueries: List<QueryMethod>): List<PreparedStmtQuery> {
        return preparedDeleteQueries.map { method ->
            val fieldSpec = getOrCreateField(PreparedStatementField(method))
            val queryWriter = QueryWriter(method)
            val fieldImpl = PreparedStatementWriter(queryWriter)
                    .createAnonymous(this@DaoWriter, dbField)
            val methodBody = createPreparedDeleteQueryMethodBody(method, fieldSpec, queryWriter)
            PreparedStmtQuery(mapOf(PreparedStmtQuery.NO_PARAM_FIELD
                    to (fieldSpec to fieldImpl)), methodBody)
        }
    }

    private fun createPreparedDeleteQueryMethodBody(
            method: QueryMethod,
            preparedStmtField: FieldSpec,
            queryWriter: QueryWriter
    ): MethodSpec {
        val scope = CodeGenScope(this)
        val methodBuilder = overrideWithoutAnnotations(method.element, declaredDao).apply {
            val stmtName = scope.getTmpVar("_stmt")
            addStatement("final $T $L = $N.acquire()",
                    SupportDbTypeNames.SQLITE_STMT, stmtName, preparedStmtField)
            addStatement("$N.beginTransaction()", dbField)
            beginControlFlow("try").apply {
                val bindScope = scope.fork()
                queryWriter.bindArgs(stmtName, emptyList(), bindScope)
                addCode(bindScope.builder().build())
                if (method.returnsValue) {
                    val resultVar = scope.getTmpVar("_result")
                    addStatement("final $L $L = $L.executeUpdateDelete()",
                            method.returnType.typeName(), resultVar, stmtName)
                    addStatement("$N.setTransactionSuccessful()", dbField)
                    addStatement("return $L", resultVar)
                } else {
                    addStatement("$L.executeUpdateDelete()", stmtName)
                    addStatement("$N.setTransactionSuccessful()", dbField)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", dbField)
                addStatement("$N.release($L)", preparedStmtField, stmtName)
            }
            endControlFlow()
        }
        return methodBuilder.build()
    }

    private fun createTransactionMethods(): List<PreparedStmtQuery> {
        return dao.transactionMethods.map {
            PreparedStmtQuery(emptyMap(), createTransactionMethodBody(it))
        }
    }

    private fun createTransactionMethodBody(method: TransactionMethod): MethodSpec {
        val scope = CodeGenScope(this)
        val methodBuilder = overrideWithoutAnnotations(method.element, declaredDao).apply {
            addStatement("$N.beginTransaction()", dbField)
            beginControlFlow("try").apply {
                val returnsValue = method.element.returnType.kind != TypeKind.VOID
                val resultVar = if (returnsValue) {
                    scope.getTmpVar("_result")
                } else {
                    null
                }
                addDelegateToSuperStatement(method.element, method.callType, resultVar)
                addStatement("$N.setTransactionSuccessful()", dbField)
                if (returnsValue) {
                    addStatement("return $N", resultVar)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", dbField)
            }
            endControlFlow()
        }
        return methodBuilder.build()
    }

    private fun MethodSpec.Builder.addDelegateToSuperStatement(
            element: ExecutableElement,
            callType: TransactionMethod.CallType,
            result: String?) {
        val params: MutableList<Any> = mutableListOf()
        val format = buildString {
            if (result != null) {
                append("$T $L = ")
                params.add(element.returnType)
                params.add(result)
            }
            when (callType) {
                TransactionMethod.CallType.CONCRETE -> {
                    append("super.$N(")
                    params.add(element.simpleName)
                }
                TransactionMethod.CallType.DEFAULT_JAVA8 -> {
                    append("$N.super.$N(")
                    params.add(element.enclosingElement.simpleName)
                    params.add(element.simpleName)
                }
                TransactionMethod.CallType.DEFAULT_KOTLIN -> {
                    append("$N.$N.$N(this, ")
                    params.add(element.enclosingElement.simpleName)
                    params.add(JvmAbi.DEFAULT_IMPLS_CLASS_NAME)
                    params.add(element.simpleName)
                }
            }
            var first = true
            element.parameters.forEach {
                if (first) {
                    first = false
                } else {
                    append(", ")
                }
                append(L)
                params.add(it.simpleName)
            }
            append(")")
        }
        addStatement(format, *params.toTypedArray())
    }

    private fun createConstructor(
            dbParam: ParameterSpec,
            shortcutMethods: List<PreparedStmtQuery>,
            callSuper: Boolean): MethodSpec {
        return MethodSpec.constructorBuilder().apply {
            addParameter(dbParam)
            addModifiers(PUBLIC)
            if (callSuper) {
                addStatement("super($N)", dbParam)
            }
            addStatement("this.$N = $N", dbField, dbParam)
            shortcutMethods.filterNot {
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

    private fun createSelectMethod(method: QueryMethod): MethodSpec {
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
                    addStatement("$T $L = $T.acquire($L, 0)",
                            RoomTypeNames.ROOM_SQL_QUERY,
                            roomSQLiteQueryVar,
                            RoomTypeNames.ROOM_SQL_QUERY,
                            queryParam.paramName)
                }
                queryParam?.isSupportQuery() == true -> {
                    shouldReleaseQuery = false
                    roomSQLiteQueryVar = scope.getTmpVar("_internalQuery")
                    // move it to a final variable so that the generated code can use it inside
                    // callback blocks in java 7
                    addStatement("final $T $L = $N",
                            queryParam.type,
                            roomSQLiteQueryVar,
                            queryParam.paramName)
                }
                else -> {
                    // try to generate compiling code. we would've already reported this error
                    roomSQLiteQueryVar = scope.getTmpVar("_statement")
                    shouldReleaseQuery = false
                    addStatement("$T $L = $T.acquire($L, 0)",
                            RoomTypeNames.ROOM_SQL_QUERY,
                            roomSQLiteQueryVar,
                            RoomTypeNames.ROOM_SQL_QUERY,
                            "missing query parameter")
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
                        scope = scope)
            }
            addCode(scope.builder().build())
        }.build()
    }

    private fun createDeleteOrUpdateQueryMethod(method: QueryMethod): MethodSpec {
        return overrideWithoutAnnotations(method.element, declaredDao).apply {
            addCode(createDeleteOrUpdateQueryMethodBody(method))
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
                        val impl = EntityInsertionAdapterWriter(it.value, onConflict)
                                .createAnonymous(this@DaoWriter, dbField.name)
                        spec to impl
                    }
                    val methodImpl = overrideWithoutAnnotations(insertionMethod.element,
                            declaredDao).apply {
                        addCode(createInsertionMethodBody(insertionMethod, fields))
                    }.build()
                    PreparedStmtQuery(fields, methodImpl)
                }
    }

    private fun createInsertionMethodBody(
            method: InsertionMethod,
            insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>
    ): CodeBlock {
        val insertionType = method.insertionType
        if (insertionAdapters.isEmpty() || insertionType == null) {
            return CodeBlock.builder().build()
        }
        val scope = CodeGenScope(this)

        return scope.builder().apply {
            // TODO assert thread
            // TODO collect results
            addStatement("$N.beginTransaction()", dbField)
            val needsReturnType = insertionType != InsertionMethod.Type.INSERT_VOID
            val resultVar = if (needsReturnType) {
                scope.getTmpVar("_result")
            } else {
                null
            }

            beginControlFlow("try").apply {
                method.parameters.forEach { param ->
                    val insertionAdapter = insertionAdapters[param.name]?.first
                    if (needsReturnType) {
                        // if it has more than 1 parameter, we would've already printed the error
                        // so we don't care about re-declaring the variable here
                        addStatement("$T $L = $N.$L($L)",
                                insertionType.returnTypeName, resultVar,
                                insertionAdapter, insertionType.methodName,
                                param.name)
                    } else {
                        addStatement("$N.$L($L)", insertionAdapter, insertionType.methodName,
                                param.name)
                    }
                }
                addStatement("$N.setTransactionSuccessful()", dbField)
                if (needsReturnType) {
                    addStatement("return $L", resultVar)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", dbField)
            }
            endControlFlow()
        }.build()
    }

    /**
     * Creates EntityUpdateAdapter for each deletion method.
     */
    private fun createDeletionMethods(): List<PreparedStmtQuery> {
        return createShortcutMethods(dao.deletionMethods, "deletion", { _, entity ->
            EntityDeletionAdapterWriter(entity)
                    .createAnonymous(this@DaoWriter, dbField.name)
        })
    }

    /**
     * Creates EntityUpdateAdapter for each @Update method.
     */
    private fun createUpdateMethods(): List<PreparedStmtQuery> {
        return createShortcutMethods(dao.updateMethods, "update", { update, entity ->
            val onConflict = OnConflictProcessor.onConflictText(update.onConflictStrategy)
            EntityUpdateAdapterWriter(entity, onConflict)
                    .createAnonymous(this@DaoWriter, dbField.name)
        })
    }

    private fun <T : ShortcutMethod> createShortcutMethods(
            methods: List<T>, methodPrefix: String,
            implCallback: (T, Entity) -> TypeSpec
    ): List<PreparedStmtQuery> {
        return methods.mapNotNull { method ->
            val entities = method.entities

            if (entities.isEmpty()) {
                null
            } else {
                val fields = entities.mapValues {
                    val spec = getOrCreateField(DeleteOrUpdateAdapterField(it.value, methodPrefix))
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
        if (adapters.isEmpty()) {
            return CodeBlock.builder().build()
        }
        val scope = CodeGenScope(this)
        val resultVar = if (method.returnCount) {
            scope.getTmpVar("_total")
        } else {
            null
        }
        return scope.builder().apply {
            if (resultVar != null) {
                addStatement("$T $L = 0", TypeName.INT, resultVar)
            }
            addStatement("$N.beginTransaction()", dbField)
            beginControlFlow("try").apply {
                method.parameters.forEach { param ->
                    val adapter = adapters[param.name]?.first
                    addStatement("$L$N.$L($L)",
                            if (resultVar == null) "" else "$resultVar +=",
                            adapter, param.handleMethodName(), param.name)
                }
                addStatement("$N.setTransactionSuccessful()", dbField)
                if (resultVar != null) {
                    addStatement("return $L", resultVar)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", dbField)
            }
            endControlFlow()
        }.build()
    }

    /**
     * @Query with delete action
     */
    private fun createDeleteOrUpdateQueryMethodBody(method: QueryMethod): CodeBlock {
        val queryWriter = QueryWriter(method)
        val scope = CodeGenScope(this)
        val sqlVar = scope.getTmpVar("_sql")
        val stmtVar = scope.getTmpVar("_stmt")
        val listSizeArgs = queryWriter.prepareQuery(sqlVar, scope)
        scope.builder().apply {
            addStatement("$T $L = $N.compileStatement($L)",
                    SupportDbTypeNames.SQLITE_STMT, stmtVar, dbField, sqlVar)
            queryWriter.bindArgs(stmtVar, listSizeArgs, scope)
            addStatement("$N.beginTransaction()", dbField)
            beginControlFlow("try").apply {
                if (method.returnsValue) {
                    val resultVar = scope.getTmpVar("_result")
                    addStatement("final $L $L = $L.executeUpdateDelete()",
                            method.returnType.typeName(), resultVar, stmtVar)
                    addStatement("$N.setTransactionSuccessful()", dbField)
                    addStatement("return $L", resultVar)
                } else {
                    addStatement("$L.executeUpdateDelete()", stmtVar)
                    addStatement("$N.setTransactionSuccessful()", dbField)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", dbField)
            }
            endControlFlow()
        }
        return scope.builder().build()
    }

    private fun createQueryMethodBody(method: QueryMethod): CodeBlock {
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
                scope = scope)
        return scope.builder().build()
    }

    private fun overrideWithoutAnnotations(
            elm: ExecutableElement,
            owner: DeclaredType): MethodSpec.Builder {
        val baseSpec = MethodSpec.overriding(elm, owner, processingEnv.typeUtils).build()
        return MethodSpec.methodBuilder(baseSpec.name).apply {
            addAnnotation(Override::class.java)
            addModifiers(baseSpec.modifiers)
            addParameters(baseSpec.parameters)
            varargs(baseSpec.varargs)
            returns(baseSpec.returnType)
        }
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
            val methodImpl: MethodSpec) {
        companion object {
            // The key to be used in `fields` where the method requires a field that is not
            // associated with any of its parameters
            const val NO_PARAM_FIELD = "-"
        }
    }

    private class InsertionMethodField(val entity: Entity, val onConflictText: String)
        : SharedFieldSpec(
            "insertionAdapterOf${Companion.typeNameToFieldName(entity.typeName)}",
            RoomTypeNames.INSERTION_ADAPTER) {

        override fun getUniqueKey(): String {
            return "${entity.typeName} $onConflictText"
        }

        override fun prepare(writer: ClassWriter, builder: FieldSpec.Builder) {
            builder.addModifiers(FINAL, PRIVATE)
        }
    }

    class DeleteOrUpdateAdapterField(val entity: Entity, val methodPrefix: String)
        : SharedFieldSpec(
            "${methodPrefix}AdapterOf${Companion.typeNameToFieldName(entity.typeName)}",
            RoomTypeNames.DELETE_OR_UPDATE_ADAPTER) {
        override fun prepare(writer: ClassWriter, builder: FieldSpec.Builder) {
            builder.addModifiers(PRIVATE, FINAL)
        }

        override fun getUniqueKey(): String {
            return entity.typeName.toString() + methodPrefix
        }
    }

    class PreparedStatementField(val method: QueryMethod) : SharedFieldSpec(
            "preparedStmtOf${method.name.capitalize()}", RoomTypeNames.SHARED_SQLITE_STMT) {
        override fun prepare(writer: ClassWriter, builder: FieldSpec.Builder) {
            builder.addModifiers(PRIVATE, FINAL)
        }

        override fun getUniqueKey(): String {
            return method.query.original
        }
    }
}
