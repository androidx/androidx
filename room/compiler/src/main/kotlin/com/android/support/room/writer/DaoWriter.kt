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

package com.android.support.room.writer

import com.android.support.room.ext.L
import com.android.support.room.ext.N
import com.android.support.room.ext.RoomTypeNames
import com.android.support.room.ext.SupportDbTypeNames
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.parser.QueryType
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.vo.Dao
import com.android.support.room.vo.DeletionMethod
import com.android.support.room.vo.Entity
import com.android.support.room.vo.InsertionMethod
import com.android.support.room.vo.QueryMethod
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import stripNonJava
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC

/**
 * Creates the implementation for a class annotated with Dao.
 */
class DaoWriter(val dao: Dao) : ClassWriter(ClassName.get(dao.type) as ClassName) {
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
                createDeletionMethods() +
                createPreparedDeleteOrUpdateQueries(preparedDeleteOrUpdateQueries)

        builder.apply {
            addModifiers(PUBLIC)
            if (dao.element.kind == ElementKind.INTERFACE) {
                addSuperinterface(dao.typeName)
            } else {
                superclass(dao.typeName)
            }
            addField(dbField)
            val dbParam = ParameterSpec.builder(dbField.type, dbField.name).build()

            addMethod(createConstructor(dbParam, shortcutMethods))

            shortcutMethods.forEach {
                addMethod(it.methodImpl)
            }

            dao.queryMethods.filter { it.query.type == QueryType.SELECT }.forEach { method ->
                addMethod(createSelectMethod(method))
            }
            oneOffDeleteOrUpdateQueries.forEach {
                addMethod(createDeleteOrUpdateQueryMethod(it))
            }
        }
        return builder
    }

    private fun createPreparedDeleteOrUpdateQueries(preparedDeleteQueries: List<QueryMethod>)
            : List<PreparedStmtQuery> {
        return preparedDeleteQueries.map { method ->
            val fieldSpec = addSharedField(PreparedStatementField(method))
            val queryWriter = QueryWriter(method)
            val fieldImpl = PreparedStatementWriter(queryWriter)
                    .createAnonymous(this@DaoWriter, dbField)
            val methodBody = createPreparedDeleteQueryMethodBody(method, fieldSpec, queryWriter)
            PreparedStmtQuery(fieldSpec, fieldImpl, methodBody)
        }
    }

    private fun createPreparedDeleteQueryMethodBody(method: QueryMethod,
                                                    preparedStmtField: FieldSpec,
                                                    queryWriter: QueryWriter): MethodSpec {
        val scope = CodeGenScope(this)
        val methodBuilder = overrideWithoutAnnotations(method.element).apply {
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

    private fun createConstructor(dbParam: ParameterSpec,
                                  shortcutMethods: List<PreparedStmtQuery>): MethodSpec {
        return MethodSpec.constructorBuilder().apply {
            addParameter(dbParam)
            addModifiers(PUBLIC)
            addStatement("this.$N = $N", dbField, dbParam)
            shortcutMethods.filterNot {
                it.field == null || it.fieldImpl == null
            }.groupBy {
                it.field?.name
            }.map {
                it.value.first()
            }.forEach {
                addStatement("this.$N = $L", it.field, it.fieldImpl)
            }
        }.build()
    }

    private fun createSelectMethod(method: QueryMethod): MethodSpec {
        return overrideWithoutAnnotations(method.element).apply {
            addCode(createQueryMethodBody(method))
        }.build()
    }

    private fun createDeleteOrUpdateQueryMethod(method: QueryMethod): MethodSpec {
        return overrideWithoutAnnotations(method.element).apply {
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
                    val onConflict = insertionMethod.onConflictText
                    val entity = insertionMethod.entity

                    if (entity == null) {
                        null
                    } else {
                        val fieldSpec = addSharedField(
                                InsertionMethodField(entity, onConflict))
                        val implSpec = EntityInsertionAdapterWriter(entity, onConflict)
                                .createAnonymous(this@DaoWriter, dbField.name)
                        val methodImpl = overrideWithoutAnnotations(insertionMethod.element).apply {
                            addCode(createInsertionMethodBody(insertionMethod, fieldSpec))
                        }.build()
                        PreparedStmtQuery(fieldSpec, implSpec, methodImpl)
                    }
                }.filterNotNull()
    }

    private fun createInsertionMethodBody(method: InsertionMethod,
                                          insertionAdapter: FieldSpec?): CodeBlock {
        val insertionType = method.insertionType
        if (insertionAdapter == null || insertionType == null) {
            return CodeBlock.builder().build()
        }
        val scope = CodeGenScope(this)

        return scope.builder().apply {
            // TODO assert thread
            // TODO collect results
            addStatement("$N.beginTransaction()", dbField)
            beginControlFlow("try").apply {
                method.parameters.forEach { param ->
                    addStatement("$N.$L($L)", insertionAdapter, insertionType.methodName,
                            param.name)
                }
                addStatement("$N.setTransactionSuccessful()", dbField)
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", dbField)
            }
            endControlFlow()
        }.build()
    }

    /**
     * Groups all deletion methods based on the delete statement they will use then creates all
     * field specs, EntityDeletionAdapterWriter and actual deletion methods.
     */
    private fun createDeletionMethods(): List<PreparedStmtQuery> {
        return dao.deletionMethods
                .map { method ->
                    val entity = method.entity

                    if (entity == null) {
                        null
                    } else {
                        val fieldSpec = addSharedField(DeleteOrUpdateAdapterField(entity))
                        val implSpec = EntityDeletionAdapterWriter(entity)
                                .createAnonymous(this@DaoWriter, dbField.name)
                        val methodSpec = overrideWithoutAnnotations(method.element).apply {
                            addCode(createDeletionMethodBody(method, fieldSpec))
                        }.build()
                        PreparedStmtQuery(fieldSpec, implSpec, methodSpec)
                    }

                }.filterNotNull()
    }

    private fun createDeletionMethodBody(method: DeletionMethod,
                                         deletionAdapter: FieldSpec?): CodeBlock {
        if (deletionAdapter == null) {
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
                    addStatement("$L$N.$L($L)",
                            if (resultVar == null) "" else "$resultVar +=",
                            deletionAdapter, method.deletionMethodFor(param), param.name)
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
        queryWriter.prepareQuery(sqlVar, scope)
        scope.builder().apply {
            addStatement("$T $L = $N.compileStatement($L)",
                    SupportDbTypeNames.SQLITE_STMT, stmtVar, dbField, sqlVar)
            queryWriter.bindArgs(stmtVar, emptyList(), scope)
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
        method.queryResultBinder.convertAndReturn(roomSQLiteQueryVar, dbField, scope)
        return scope.builder().build()
    }

    private fun overrideWithoutAnnotations(elm: ExecutableElement): MethodSpec.Builder {
        val baseSpec = MethodSpec.overriding(elm).build()
        return MethodSpec.methodBuilder(baseSpec.name).apply {
            addAnnotation(Override::class.java)
            addModifiers(baseSpec.modifiers)
            addParameters(baseSpec.parameters)
            varargs(baseSpec.varargs)
            returns(baseSpec.returnType)
        }
    }

    data class PreparedStmtQuery(val field: FieldSpec?,
                                 val fieldImpl: TypeSpec?,
                                 val methodImpl: MethodSpec)

    private class InsertionMethodField(val entity: Entity, val onConflictText: String)
        : SharedFieldSpec(
            "insertionAdapterOf${Companion.typeNameToFieldName(entity.typeName)}",
            RoomTypeNames.INSERTION_ADAPTER) {

        override fun getUniqueKey(): String {
            return "${entity.typeName} $onConflictText"
        }

        override fun prepare(builder: FieldSpec.Builder) {
            builder.addModifiers(FINAL, PRIVATE)
        }
    }

    class DeleteOrUpdateAdapterField(val entity: Entity) : SharedFieldSpec(
            "deletionAdapterOf${Companion.typeNameToFieldName(entity.typeName)}",
            RoomTypeNames.DELETE_OR_UPDATE_ADAPTER) {
        override fun prepare(builder: FieldSpec.Builder) {
            builder.addModifiers(PRIVATE, FINAL)
        }

        override fun getUniqueKey(): String {
            return entity.typeName.toString()
        }
    }

    class PreparedStatementField(val method: QueryMethod) : SharedFieldSpec(
            "preparedStmtOf${method.name.capitalize()}", RoomTypeNames.SHARED_SQLITE_STMT) {
        override fun prepare(builder: FieldSpec.Builder) {
            builder.addModifiers(PRIVATE, FINAL)
        }

        override fun getUniqueKey(): String {
            return method.query.original
        }
    }
}
