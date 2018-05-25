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

package androidx.room.writer

import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.ext.defaultValue
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.vo.CallType
import androidx.room.vo.Constructor
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Field
import androidx.room.vo.FieldWithIndex
import androidx.room.vo.Pojo
import androidx.room.vo.RelationCollector
import com.squareup.javapoet.TypeName

/**
 * Handles writing a field into statement or reading it form statement.
 */
class FieldReadWriteWriter(fieldWithIndex: FieldWithIndex) {
    val field = fieldWithIndex.field
    val indexVar = fieldWithIndex.indexVar
    val alwaysExists = fieldWithIndex.alwaysExists

    companion object {
        /*
         * Get all parents including the ones which have grand children in this list but does not
         * have any direct children in the list.
         */
        fun getAllParents(fields: List<Field>): Set<EmbeddedField> {
            val allParents = mutableSetOf<EmbeddedField>()
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
        private fun createNodeTree(
                rootVar: String,
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

        fun bindToStatement(
                ownerVar: String,
                stmtParamVar: String,
                fieldsWithIndices: List<FieldWithIndex>,
                scope: CodeGenScope
        ) {
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

        /**
         * Just constructs the given item, does NOT DECLARE. Declaration happens outside the
         * reading statement since we may never read if the cursor does not have necessary
         * columns.
         */
        private fun construct(
                outVar: String,
                constructor: Constructor?,
                typeName: TypeName,
                localVariableNames: Map<String, FieldWithIndex>,
                localEmbeddeds: List<Node>, scope: CodeGenScope
        ) {
            if (constructor == null) {
                // best hope code generation
                scope.builder().apply {
                    addStatement("$L = new $T()", outVar, typeName)
                }
                return
            }
            val variableNames = constructor.params.map { param ->
                when (param) {
                    is Constructor.FieldParam -> localVariableNames.entries.firstOrNull {
                        it.value.field === param.field
                    }?.key
                    is Constructor.EmbeddedParam -> localEmbeddeds.firstOrNull {
                        it.fieldParent === param.embedded
                    }?.varName
                    else -> null
                }
            }
            val args = variableNames.joinToString(",") { it ?: "null" }
            scope.builder().apply {
                addStatement("$L = new $T($L)", outVar, typeName, args)
            }
        }

        /**
         * Reads the row into the given variable. It does not declare it but constructs it.
         */
        fun readFromCursor(
                outVar: String,
                outPojo: Pojo,
                cursorVar: String,
                fieldsWithIndices: List<FieldWithIndex>,
                scope: CodeGenScope,
                relationCollectors: List<RelationCollector>
        ) {
            fun visitNode(node: Node) {
                val fieldParent = node.fieldParent
                fun readNode() {
                    // read constructor parameters into local fields
                    val constructorFields = node.directFields.filter {
                        it.field.setter.callType == CallType.CONSTRUCTOR
                    }.associateBy { fwi ->
                        FieldReadWriteWriter(fwi).readIntoTmpVar(cursorVar, scope)
                    }
                    // read decomposed fields
                    node.subNodes.forEach(::visitNode)
                    // construct the object
                    if (fieldParent != null) {
                        construct(outVar = node.varName,
                                constructor = fieldParent.pojo.constructor,
                                typeName = fieldParent.field.typeName,
                                localEmbeddeds = node.subNodes,
                                localVariableNames = constructorFields,
                                scope = scope)
                    } else {
                        construct(outVar = node.varName,
                                constructor = outPojo.constructor,
                                typeName = outPojo.typeName,
                                localEmbeddeds = node.subNodes,
                                localVariableNames = constructorFields,
                                scope = scope)
                    }
                    // ready any field that was not part of the constructor
                    node.directFields.filterNot {
                        it.field.setter.callType == CallType.CONSTRUCTOR
                    }.forEach { fwi ->
                        FieldReadWriteWriter(fwi).readFromCursor(
                                ownerVar = node.varName,
                                cursorVar = cursorVar,
                                scope = scope)
                    }
                    // assign relationship fields which will be read later
                    relationCollectors.filter { (relation) ->
                        relation.field.parent === fieldParent
                    }.forEach {
                        it.writeReadParentKeyCode(
                                cursorVarName = cursorVar,
                                itemVar = node.varName,
                                fieldsWithIndices = fieldsWithIndices,
                                scope = scope)
                    }
                    // assign sub modes to fields if they were not part of the constructor.
                    node.subNodes.mapNotNull {
                        val setter = it.fieldParent?.setter
                        if (setter != null && setter.callType != CallType.CONSTRUCTOR) {
                            Pair(it.varName, setter)
                        } else {
                            null
                        }
                    }.forEach { (varName, setter) ->
                        setter.writeSet(
                                ownerVar = node.varName,
                                inVar = varName,
                                builder = scope.builder())
                    }
                }
                if (fieldParent == null) {
                    // root element
                    // always declared by the caller so we don't declare this
                    readNode()
                } else {
                    // always declare, we'll set below
                    scope.builder().addStatement("final $T $L", fieldParent.pojo.typeName,
                                        node.varName)
                    if (fieldParent.nonNull) {
                        readNode()
                    } else {
                        val myDescendants = node.allFields()
                        val allNullCheck = myDescendants.joinToString(" && ") {
                            if (it.alwaysExists) {
                                "$cursorVar.isNull(${it.indexVar})"
                            } else {
                                "( ${it.indexVar} == -1 || $cursorVar.isNull(${it.indexVar}))"
                            }
                        }
                        scope.builder().apply {
                            beginControlFlow("if (! ($L))", allNullCheck).apply {
                                readNode()
                            }
                            nextControlFlow(" else ").apply {
                                addStatement("$L = null", node.varName)
                            }
                            endControlFlow()
                        }
                    }
                }
            }
            visitNode(createNodeTree(outVar, fieldsWithIndices, scope))
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
    private fun readFromCursor(ownerVar: String, cursorVar: String, scope: CodeGenScope) {
        fun toRead() {
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
                        CallType.CONSTRUCTOR -> {
                            // no-op
                        }
                    }
                }
            }
        }
        if (alwaysExists) {
            toRead()
        } else {
            scope.builder().apply {
                beginControlFlow("if ($L != -1)", indexVar).apply {
                    toRead()
                }
                endControlFlow()
            }
        }
    }

    /**
     * Reads the value into a temporary local variable.
     */
    fun readIntoTmpVar(cursorVar: String, scope: CodeGenScope): String {
        val tmpField = scope.getTmpVar("_tmp${field.name.capitalize()}")
        val typeName = field.getter.type.typeName()
        scope.builder().apply {
            addStatement("final $T $L", typeName, tmpField)
            if (alwaysExists) {
                field.cursorValueReader?.readFromCursor(tmpField, cursorVar, indexVar, scope)
            } else {
                beginControlFlow("if ($L == -1)", indexVar).apply {
                    addStatement("$L = $L", tmpField, typeName.defaultValue())
                }
                nextControlFlow("else").apply {
                    field.cursorValueReader?.readFromCursor(tmpField, cursorVar, indexVar, scope)
                }
                endControlFlow()
            }
        }
        return tmpField
    }

    /**
     * On demand node which is created based on the fields that were passed into this class.
     */
    private class Node(
            // root for me
            val varName: String,
            // set if I'm a FieldParent
            val fieldParent: EmbeddedField?
    ) {
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
