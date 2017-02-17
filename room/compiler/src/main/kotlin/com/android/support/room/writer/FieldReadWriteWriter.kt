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

package com.android.support.room.writer

import com.android.support.room.ext.L
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.vo.CallType
import com.android.support.room.vo.Field
import com.android.support.room.vo.DecomposedField
import com.android.support.room.vo.FieldWithIndex

/**
 * Handles writing a field into statement or reading it form statement.
 */
class FieldReadWriteWriter(fieldWithIndex: FieldWithIndex) {
    val field = fieldWithIndex.field
    val indexVar = fieldWithIndex.indexVar

    companion object {
        /*
         * Get all parents including the ones which have grand children in this list but does not
         * have any direct children in the list.
         */
        fun getAllParents(fields: List<Field>): Set<DecomposedField> {
            val allParents = mutableSetOf<DecomposedField>()
            fun addAllParents(field: Field) {
                var parent = field.parent
                while (parent != null) {
                    if (allParents.add(parent)) {
                        parent = parent.parent
                    } else {
                        break
                    }
                }
            }
            fields.forEach(::addAllParents)
            return allParents
        }

        /**
         * Convert the fields with indices into a Node tree so that we can recursively process
         * them. This work is done here instead of parsing because the result may include arbitrary
         * fields.
         */
        private fun createNodeTree(rootVar: String,
                           fieldsWithIndices: List<FieldWithIndex>,
                           scope: CodeGenScope): Node {
            val allParents = getAllParents(fieldsWithIndices.map { it.field })
            val rootNode = Node(rootVar, null)
            rootNode.directFields = fieldsWithIndices.filter { it.field.parent == null }
            val parentNodes = allParents.associate {
                Pair(it, Node(
                        varName = scope.getTmpVar("_tmp${it.field.name.capitalize()}"),
                        fieldParent = it))
            }
            parentNodes.values.forEach { node ->
                val fieldParent = node.fieldParent!!
                val grandParent = fieldParent.parent
                val grandParentNode = grandParent?.let {
                    parentNodes[it]
                } ?: rootNode
                node.directFields = fieldsWithIndices.filter { it.field.parent == fieldParent }
                node.parentNode = grandParentNode
                grandParentNode.subNodes.add(node)
            }
            return rootNode
        }

        fun bindToStatement(ownerVar: String, stmtParamVar: String,
                            fieldsWithIndices: List<FieldWithIndex>,
                            scope: CodeGenScope) {
            fun visitNode(node: Node) {
                fun bindWithDescendants() {
                    node.directFields.forEach {
                        FieldReadWriteWriter(it).bindToStatement(
                                ownerVar = node.varName,
                                stmtParamVar = stmtParamVar,
                                scope = scope
                        )
                    }
                    node.subNodes.forEach(::visitNode)
                }

                val fieldParent = node.fieldParent
                if (fieldParent != null) {
                    fieldParent.getter.writeGet(
                            ownerVar = node.parentNode!!.varName,
                            outVar = node.varName,
                            builder = scope.builder()
                    )
                    scope.builder().apply {
                        beginControlFlow("if($L != null)", node.varName).apply {
                            bindWithDescendants()
                        }
                        nextControlFlow("else").apply {
                            node.allFields().forEach {
                                addStatement("$L.bindNull($L)", stmtParamVar, it.indexVar)
                            }
                        }
                        endControlFlow()
                    }
                } else {
                    bindWithDescendants()
                }
            }
            visitNode(createNodeTree(ownerVar, fieldsWithIndices, scope))
        }

        fun readFromCursor(ownerVar: String, cursorVar: String,
                           fieldsWithIndices: List<FieldWithIndex>,
                           scope: CodeGenScope) {
            fun visitNode(node: Node) {
                val fieldParent = node.fieldParent
                fun readNode() {
                    if (fieldParent != null) {
                        scope.builder()
                                .addStatement("final $T $L = new $T()", fieldParent.pojo.typeName,
                                        node.varName, fieldParent.pojo.typeName)
                    }
                    node.directFields.forEach {
                        FieldReadWriteWriter(it).readFromCursor(node.varName, cursorVar, scope)
                    }
                    node.subNodes.forEach(::visitNode)
                    if (fieldParent != null) {
                        fieldParent.setter.writeSet(
                                ownerVar = node.parentNode!!.varName,
                                inVar = node.varName,
                                builder = scope.builder()
                        )
                    }
                }
                if (fieldParent == null) {
                    // root element
                    readNode()
                } else {
                    if (fieldParent.nonNull) {
                        readNode()
                    } else {
                        val myDescendants = node.allFields()
                        val allNullCheck = myDescendants.joinToString(" && ") {
                            "$cursorVar.isNull(${it.indexVar})"
                        }
                        scope.builder().apply {
                            beginControlFlow("if (! ($L))", allNullCheck).apply {
                                readNode()
                            }
                            endControlFlow()
                        }
                    }
                }
            }
            visitNode(createNodeTree(ownerVar, fieldsWithIndices, scope))
        }
    }

    /**
     * @param ownerVar The entity / pojo that owns this field. It must own this field! (not the
     * container pojo)
     * @param stmtParamVar The statement variable
     * @param scope The code generation scope
     */
    private fun bindToStatement(ownerVar: String, stmtParamVar: String, scope: CodeGenScope) {
        field.statementBinder?.let { binder ->
            val varName = if (field.getter.callType == CallType.FIELD) {
                "$ownerVar.${field.name}"
            } else {
                "$ownerVar.${field.getter.name}()"
            }
            binder.bindToStmt(stmtParamVar, indexVar, varName, scope)
        }
    }

    /**
     * @param ownerVar The entity / pojo that owns this field. It must own this field (not the
     * container pojo)
     * @param cursorVar The cursor variable
     * @param scope The code generation scope
     */
    fun readFromCursor(ownerVar: String, cursorVar: String, scope: CodeGenScope) {
        field.cursorValueReader?.let { reader ->
            scope.builder().apply {
                when (field.setter.callType) {
                    CallType.FIELD -> {
                        reader.readFromCursor("$ownerVar.${field.getter.name}", cursorVar,
                                indexVar, scope)
                    }
                    CallType.METHOD -> {
                        val tmpField = scope.getTmpVar("_tmp${field.name.capitalize()}")
                        addStatement("final $T $L", field.getter.type.typeName(), tmpField)
                        reader.readFromCursor(tmpField, cursorVar, indexVar, scope)
                        addStatement("$L.$L($L)", ownerVar, field.setter.name, tmpField)
                    }
                }
            }
        }
    }

    /**
     * On demand node which is created based on the fields that were passed into this class.
     */
    private class Node(
            // root for me
            val varName: String,
            // set if I'm a FieldParent
            val fieldParent: DecomposedField?) {
        // whom do i belong
        var parentNode: Node? = null
        // these fields are my direct fields
        lateinit var directFields: List<FieldWithIndex>
        // these nodes are under me
        val subNodes = mutableListOf<Node>()

        fun allFields(): List<FieldWithIndex> {
            return directFields + subNodes.flatMap { it.allFields() }
        }
    }
}
