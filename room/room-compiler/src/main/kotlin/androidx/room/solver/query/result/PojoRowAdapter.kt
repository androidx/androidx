/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.solver.query.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.addOriginatingElement
import androidx.room.compiler.codegen.toJavaPoet
import androidx.room.compiler.processing.XType
import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.CodeGenScope
import androidx.room.verifier.QueryResultInfo
import androidx.room.vo.ColumnIndexVar
import androidx.room.vo.Field
import androidx.room.vo.FieldWithIndex
import androidx.room.vo.Pojo
import androidx.room.vo.RelationCollector
import androidx.room.writer.FieldReadWriteWriter
import androidx.room.writer.TypeWriter
import kotlin.math.abs

/**
 * Creates the entity from the given info.
 *
 * The info comes from the query processor so we know about the order of columns in the result etc.
 */
class PojoRowAdapter(
    private val context: Context,
    private val info: QueryResultInfo?,
    private val query: ParsedQuery?,
    val pojo: Pojo,
    out: XType
) : QueryMappedRowAdapter(out) {
    override val mapping: PojoMapping
    val relationCollectors: List<RelationCollector>

    private val indexAdapter: PojoIndexAdapter

    // Set when cursor is ready.
    private lateinit var fieldsWithIndices: List<FieldWithIndex>

    init {
        val remainingFields = pojo.fields.toMutableList()
        val unusedColumns = arrayListOf<String>()
        val matchedFields: List<Field>
        if (info != null) {
            matchedFields = info.columns.mapNotNull { column ->
                val field = remainingFields.firstOrNull { it.columnName == column.name }
                if (field == null) {
                    unusedColumns.add(column.name)
                    null
                } else {
                    remainingFields.remove(field)
                    field
                }
            }
            val nonNulls = remainingFields.filter { it.nonNull }
            if (nonNulls.isNotEmpty()) {
                context.logger.e(
                    ProcessorErrors.pojoMissingNonNull(
                        pojoTypeName = pojo.typeName.toJavaPoet(),
                        missingPojoFields = nonNulls.map { it.name },
                        allQueryColumns = info.columns.map { it.name }
                    )
                )
            }
            if (matchedFields.isEmpty()) {
                context.logger.e(ProcessorErrors.cannotFindQueryResultAdapter(out.typeName))
            }
        } else {
            matchedFields = remainingFields.map { it }
            remainingFields.clear()
        }
        relationCollectors = RelationCollector.createCollectors(context, pojo.relations)

        mapping = PojoMapping(
            pojo = pojo,
            matchedFields = matchedFields,
            unusedColumns = unusedColumns,
            unusedFields = remainingFields
        )

        indexAdapter = PojoIndexAdapter(mapping, info, query)
    }

    fun relationTableNames(): List<String> {
        return relationCollectors.flatMap {
            val queryTableNames = it.loadAllQuery.tables.map { it.name }
            if (it.rowAdapter is PojoRowAdapter) {
                it.rowAdapter.relationTableNames() + queryTableNames
            } else {
                queryTableNames
            }
        }.distinct()
    }

    override fun onCursorReady(
        cursorVarName: String,
        scope: CodeGenScope,
        indices: List<ColumnIndexVar>
    ) {
        fieldsWithIndices = indices.map { (column, indexVar) ->
            val field = mapping.matchedFields.first { it.columnName == column }
            FieldWithIndex(field = field, indexVar = indexVar, alwaysExists = info != null)
        }
        emitRelationCollectorsReady(cursorVarName, scope)
    }

    private fun emitRelationCollectorsReady(cursorVarName: String, scope: CodeGenScope) {
        if (relationCollectors.isNotEmpty()) {
            relationCollectors.forEach { it.writeInitCode(scope) }
            scope.builder().apply {
                beginControlFlow("while ($L.moveToNext())", cursorVarName).apply {
                    relationCollectors.forEach {
                        it.writeReadParentKeyCode(cursorVarName, fieldsWithIndices, scope)
                    }
                }
                endControlFlow()
            }
            scope.builder().addStatement("$L.moveToPosition(-1)", cursorVarName)
            relationCollectors.forEach { it.writeCollectionCode(scope) }
        }
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        fun doReadFromCursor(outVarName: String, scope: CodeGenScope) {
            FieldReadWriteWriter.readFromCursor(
                outVar = outVarName,
                outPojo = pojo,
                cursorVar = cursorVarName,
                fieldsWithIndices = fieldsWithIndices,
                relationCollectors = relationCollectors,
                scope = scope
            )
        }
        // TODO(b/127483380): Inline in code gen scope once Kotlin code gen progresses.
        if (
            context.codeLanguage == CodeLanguage.KOTLIN &&
            relationCollectors.isEmpty() &&
            fieldsWithIndices.none { it.indexVar.contains('[') } // hacky id of dupe column
        ) {
            // The name of the class is based on the query, possible to be collisions, but good,
            // enough for now.
            val nameHash = abs(query?.original?.hashCode() ?: out.asTypeName().hashCode())
            val className = XClassName.get("androidx.room.temp", "PojoRowAdapter_$nameHash")
            object : TypeWriter(CodeLanguage.KOTLIN) {
                override fun createTypeSpecBuilder(): XTypeSpec.Builder {
                    val readFunction = XFunSpec.builder(
                        CodeLanguage.KOTLIN,
                        "readFromCursor",
                        VisibilityModifier.PUBLIC
                    )
                        .returns(out.asTypeName())
                        .addParameter(AndroidTypeNames.CURSOR, cursorVarName)
                        .apply {
                            fieldsWithIndices.forEach {
                                addParameter(XTypeName.PRIMITIVE_INT, it.indexVar)
                            }
                        }
                        .addCode(
                            CodeGenScope(this).apply {
                                builder.addLocalVariable(outVarName, out.asTypeName())
                                doReadFromCursor(outVarName, this)
                                builder.addStatement("return %L", outVarName)
                            }.generate()
                        )
                        .build()
                    return XTypeSpec.classBuilder(codeLanguage, className)
                        .addOriginatingElement(pojo.element)
                        .addFunction(readFunction)
                }
            }.write(context.processingEnv)
            scope.builder().apply {
                addStatement("$L = new $T().readFromCursor($L, $L)",
                    outVarName,
                    className.toJavaPoet(),
                    cursorVarName,
                    fieldsWithIndices.joinToString { it.indexVar }
                )
            }
        } else {
            scope.builder().apply {
                doReadFromCursor(outVarName, scope)
            }
        }
    }

    override fun getDefaultIndexAdapter() = indexAdapter

    data class PojoMapping(
        val pojo: Pojo,
        val matchedFields: List<Field>,
        val unusedColumns: List<String>,
        val unusedFields: List<Field>
    ) : Mapping() {
        override val usedColumns = matchedFields.map { it.columnName }
    }
}
