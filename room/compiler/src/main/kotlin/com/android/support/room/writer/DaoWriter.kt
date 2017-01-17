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
import javax.lang.model.element.Modifier.*

/**
 * Creates the implementation for a class annotated with Dao.
 */
class DaoWriter(val dao: Dao) : ClassWriter(ClassName.get(dao.type) as ClassName) {
    companion object {
        val dbField: FieldSpec = FieldSpec
                .builder(RoomTypeNames.ROOM_DB, "__db", PRIVATE, FINAL)
                .build()
    }

    override fun createTypeSpec(): TypeSpec {
        val builder = TypeSpec.classBuilder(dao.implTypeName)
        val scope = CodeGenScope()

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
        val shortcutMethods = groupAndCreateInsertionMethods(scope) +
                groupAndCreateDeletionMethods(scope) +
                createPreparedDeleteOrUpdateQueries(preparedDeleteOrUpdateQueries, scope)

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
                addMethods(it.methodImpls)
                if (it.field != null) {
                    addField(it.field)
                }
            }

            dao.queryMethods.filter { it.query.type == QueryType.SELECT }.forEach { method ->
                addMethod(createSelectMethod(method))
            }
            oneOffDeleteOrUpdateQueries.forEach {
                addMethod(createDeleteOrUpdateQueryMethod(it))
            }
        }
        return builder.build()
    }

    private fun createPreparedDeleteOrUpdateQueries(preparedDeleteQueries: List<QueryMethod>,
                                                    scope: CodeGenScope): List<PreparedStmtQuery> {
        return preparedDeleteQueries.map { method ->
            val fieldName = scope.getTmpVar("_preparedStmtOf${method.name.capitalize()}")
            val fieldSpec =  FieldSpec.builder(RoomTypeNames.SHARED_SQLITE_STMT, fieldName,
                    PRIVATE, FINAL).build()
            val queryWriter = QueryWriter(method)
            val fieldImpl = PreparedStatementWriter(queryWriter).createAnonymous(dbField)
            val methodBody = createPreparedDeleteQueryMethodBody(method, fieldSpec, queryWriter)
            PreparedStmtQuery(fieldSpec, fieldImpl, listOf(methodBody))
        }
    }

    private fun createPreparedDeleteQueryMethodBody(method: QueryMethod,
                                                    preparedStmtField : FieldSpec,
                                                    queryWriter: QueryWriter): MethodSpec {
        val scope = CodeGenScope()
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
            }.forEach {
                addStatement("this.$N = $L", it.field, it.fieldImpl)
            }
        }.build()
    }

    private fun createSelectMethod(method : QueryMethod) : MethodSpec {
        return overrideWithoutAnnotations(method.element).apply {
            addCode(createQueryMethodBody(method))
        }.build()
    }

    private fun createDeleteOrUpdateQueryMethod(method : QueryMethod) : MethodSpec {
        return overrideWithoutAnnotations(method.element).apply {
            addCode(createDeleteOrUpdateQueryMethodBody(method))
        }.build()
    }

    /**
     * Groups all insertion methods based on the insert statement they will use then creates all
     * field specs, EntityInsertionAdapterWriter and actual insert methods.
     */
    private fun groupAndCreateInsertionMethods(scope : CodeGenScope): List<PreparedStmtQuery> {
        return dao.insertionMethods
                .groupBy {
                    Pair(it.entity?.typeName, it.onConflictText)
                }.map { entry ->
            val onConflict = entry.key.second
            val methods = entry.value
            val entity = methods.first().entity

            val fieldSpec : FieldSpec?
            val implSpec : TypeSpec?
            if (entity == null) {
                fieldSpec = null
                implSpec = null
            } else {
                val fieldName = scope
                        .getTmpVar("__insertionAdapterOf${typeNameToFieldName(entity.typeName)}")
                fieldSpec = FieldSpec.builder(RoomTypeNames.INSERTION_ADAPTER, fieldName,
                        FINAL, PRIVATE).build()
                implSpec = EntityInsertionAdapterWriter(entity, onConflict)
                        .createAnonymous(dbField.name)
            }
            val insertionMethodImpls = methods.map { method ->
                overrideWithoutAnnotations(method.element).apply {
                    addCode(createInsertionMethodBody(method, fieldSpec))
                }.build()
            }
            PreparedStmtQuery(fieldSpec, implSpec, insertionMethodImpls)
        }
    }

    private fun createInsertionMethodBody(method: InsertionMethod,
                                          insertionAdapter: FieldSpec?): CodeBlock {
        val insertionType = method.insertionType
        if (insertionAdapter == null || insertionType == null) {
            return CodeBlock.builder().build()
        }
        val scope = CodeGenScope()

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
    private fun groupAndCreateDeletionMethods(scope : CodeGenScope): List<PreparedStmtQuery> {
        return dao.deletionMethods
                .groupBy {
                    it.entity?.typeName
                }.map { entry ->
            val methods = entry.value
            val entity = methods.first().entity

            val fieldSpec : FieldSpec?
            val implSpec : TypeSpec?
            if (entity == null) {
                fieldSpec = null
                implSpec = null
            } else {
                val fieldName = scope
                        .getTmpVar("__deletionAdapterOf${typeNameToFieldName(entity.typeName)}")
                fieldSpec = FieldSpec.builder(RoomTypeNames.DELETE_OR_UPDATE_ADAPTER, fieldName,
                        FINAL, PRIVATE).build()
                implSpec = EntityDeletionAdapterWriter(entity)
                        .createAnonymous(dbField.name)
            }
            val deletionMethodImpls = methods.map { method ->
                overrideWithoutAnnotations(method.element).apply {
                    addCode(createDeletionMethodBody(method, fieldSpec))
                }.build()
            }
            PreparedStmtQuery(fieldSpec, implSpec, deletionMethodImpls)
        }
    }

    private fun createDeletionMethodBody(method: DeletionMethod,
                                          deletionAdapter: FieldSpec?): CodeBlock {
        if (deletionAdapter == null) {
            return CodeBlock.builder().build()
        }
        val scope = CodeGenScope()
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
        val scope = CodeGenScope()
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
        val scope = CodeGenScope()
        val sqlVar = scope.getTmpVar("_sql")
        val argsVar = scope.getTmpVar("_args")
        queryWriter.prepareReadAndBind(sqlVar, argsVar, scope)
        method.queryResultBinder.convertAndReturn(sqlVar, argsVar, dbField, scope)
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

    private fun typeNameToFieldName(typeName: TypeName): String {
        if (typeName is ClassName) {
            return typeName.simpleName()
        } else {
            return typeName.toString().replace('.', '_').stripNonJava()
        }
    }

    data class PreparedStmtQuery(val field: FieldSpec?,
                                 val fieldImpl: TypeSpec?,
                                 val methodImpls: List<MethodSpec>)
}
