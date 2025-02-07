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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XNullability
import androidx.room.ext.capitalize
import androidx.room.ext.defaultValue
import androidx.room.solver.CodeGenScope
import androidx.room.vo.CallType
import androidx.room.vo.Constructor
import androidx.room.vo.DataClass
import androidx.room.vo.EmbeddedProperty
import androidx.room.vo.Property
import androidx.room.vo.PropertyWithIndex
import androidx.room.vo.RelationCollector
import java.util.Locale

/** Handles writing a property into statement or reading it from statement. */
class PropertyReadWriteWriter(propertyWithIndex: PropertyWithIndex) {
    val property = propertyWithIndex.property
    val indexVar = propertyWithIndex.indexVar
    val alwaysExists = propertyWithIndex.alwaysExists

    companion object {
        /*
         * Get all parents including the ones which have grand children in this list but does not
         * have any direct children in the list.
         */
        fun getAllParents(properties: List<Property>): Set<EmbeddedProperty> {
            val allParents = mutableSetOf<EmbeddedProperty>()
            fun addAllParents(property: Property) {
                var parent = property.parent
                while (parent != null) {
                    if (allParents.add(parent)) {
                        parent = parent.parent
                    } else {
                        break
                    }
                }
            }
            properties.forEach(::addAllParents)
            return allParents
        }

        /**
         * Convert the properties with indices into a Node tree so that we can recursively process
         * them. This work is done here instead of parsing because the result may include arbitrary
         * properties.
         */
        private fun createNodeTree(
            rootVar: String,
            propertiesWithIndices: List<PropertyWithIndex>,
            scope: CodeGenScope
        ): Node {
            val allParents = getAllParents(propertiesWithIndices.map { it.property })
            val rootNode = Node(rootVar, null)
            rootNode.directProperties = propertiesWithIndices.filter { it.property.parent == null }
            val parentNodes =
                allParents.associateWith {
                    Node(
                        varName = scope.getTmpVar("_tmp${it.property.name.capitalize(Locale.US)}"),
                        propertyParent = it
                    )
                }
            parentNodes.values.forEach { node ->
                val propertyParent = node.propertyParent!!
                val grandParent = propertyParent.parent
                val grandParentNode = grandParent?.let { parentNodes[it] } ?: rootNode
                node.directProperties =
                    propertiesWithIndices.filter { it.property.parent == propertyParent }
                node.parentNode = grandParentNode
                grandParentNode.subNodes.add(node)
            }
            return rootNode
        }

        fun bindToStatement(
            ownerVar: String,
            stmtParamVar: String,
            propertiesWithIndices: List<PropertyWithIndex>,
            scope: CodeGenScope
        ) {
            fun visitNode(node: Node) {
                fun bindWithDescendants() {
                    node.directProperties.forEach {
                        PropertyReadWriteWriter(it)
                            .bindToStatement(
                                ownerVar = node.varName,
                                stmtParamVar = stmtParamVar,
                                scope = scope
                            )
                    }
                    node.subNodes.forEach(::visitNode)
                }

                val propertyParent = node.propertyParent
                if (propertyParent != null) {
                    propertyParent.getter.writeGet(
                        ownerVar = node.parentNode!!.varName,
                        outVar = node.varName,
                        builder = scope.builder
                    )
                    scope.builder.apply {
                        if (propertyParent.nonNull) {
                            bindWithDescendants()
                        } else {
                            beginControlFlow("if (%L != null)", node.varName).apply {
                                bindWithDescendants()
                            }
                            nextControlFlow("else").apply {
                                node.allProperties().forEach {
                                    addStatement("%L.bindNull(%L)", stmtParamVar, it.indexVar)
                                }
                            }
                            endControlFlow()
                        }
                    }
                } else {
                    bindWithDescendants()
                }
            }
            visitNode(createNodeTree(ownerVar, propertiesWithIndices, scope))
        }

        /**
         * Just constructs the given item, does NOT DECLARE. Declaration happens outside the reading
         * statement since we may never read if the statement does not have necessary columns.
         */
        private fun construct(
            outVar: String,
            constructor: Constructor?,
            typeName: XTypeName,
            localVariableNames: Map<String, PropertyWithIndex>,
            localEmbeddeds: List<Node>,
            localRelations: Map<String, Property>,
            scope: CodeGenScope
        ) {
            if (constructor == null) {
                // Instantiate with default constructor, best hope for code generation
                scope.builder.apply {
                    addStatement("%L = %L", outVar, XCodeBlock.ofNewInstance(typeName))
                }
                return
            }
            val variableNames =
                constructor.params.map { param ->
                    when (param) {
                        is Constructor.Param.PropertyParam ->
                            localVariableNames.entries
                                .firstOrNull { it.value.property === param.property }
                                ?.key
                        is Constructor.Param.EmbeddedParam ->
                            localEmbeddeds
                                .firstOrNull { it.propertyParent == param.embedded }
                                ?.varName
                        is Constructor.Param.RelationParam ->
                            localRelations.entries
                                .firstOrNull { it.value === param.relation.property }
                                ?.key
                    }
                }
            val args = variableNames.joinToString(",") { it ?: "null" }
            constructor.writeConstructor(outVar, args, scope.builder)
        }

        /** Reads the row into the given variable. It does not declare it but constructs it. */
        fun readFromStatement(
            outVar: String,
            outDataClass: DataClass,
            stmtVar: String,
            propertiesWithIndices: List<PropertyWithIndex>,
            scope: CodeGenScope,
            relationCollectors: List<RelationCollector>
        ) {
            fun visitNode(node: Node) {
                val propertyParent = node.propertyParent
                fun readNode() {
                    // read constructor parameters into local properties
                    val constructorProperties =
                        node.directProperties
                            .filter { it.property.setter.callType == CallType.CONSTRUCTOR }
                            .associateBy { fwi ->
                                PropertyReadWriteWriter(fwi)
                                    .readIntoTmpVar(
                                        stmtVar,
                                        fwi.property.setter.type.asTypeName(),
                                        scope
                                    )
                            }
                    // read decomposed properties (e.g. embedded)
                    node.subNodes.forEach(::visitNode)
                    // read relationship properties
                    val relationProperties =
                        relationCollectors
                            .filter { (relation) -> relation.property.parent === propertyParent }
                            .associate {
                                it.writeReadCollectionIntoTmpVar(
                                    stmtVarName = stmtVar,
                                    propertiesWithIndices = propertiesWithIndices,
                                    scope = scope
                                )
                            }

                    // construct the object
                    if (propertyParent != null) {
                        construct(
                            outVar = node.varName,
                            constructor = propertyParent.dataClass.constructor,
                            typeName = propertyParent.property.typeName,
                            localEmbeddeds = node.subNodes,
                            localRelations = relationProperties,
                            localVariableNames = constructorProperties,
                            scope = scope
                        )
                    } else {
                        construct(
                            outVar = node.varName,
                            constructor = outDataClass.constructor,
                            typeName = outDataClass.typeName,
                            localEmbeddeds = node.subNodes,
                            localRelations = relationProperties,
                            localVariableNames = constructorProperties,
                            scope = scope
                        )
                    }
                    // ready any property that was not part of the constructor
                    node.directProperties
                        .filterNot { it.property.setter.callType == CallType.CONSTRUCTOR }
                        .forEach { fwi ->
                            PropertyReadWriteWriter(fwi)
                                .readFromStatement(
                                    ownerVar = node.varName,
                                    stmtVar = stmtVar,
                                    scope = scope
                                )
                        }
                    // assign sub nodes to properties if they were not part of the constructor.
                    node.subNodes
                        .mapNotNull {
                            val setter = it.propertyParent?.setter
                            if (setter != null && setter.callType != CallType.CONSTRUCTOR) {
                                Pair(it.varName, setter)
                            } else {
                                null
                            }
                        }
                        .forEach { (varName, setter) ->
                            setter.writeSet(
                                ownerVar = node.varName,
                                inVar = varName,
                                builder = scope.builder
                            )
                        }
                    // assign relation properties that were not part of the constructor
                    relationProperties
                        .filterNot { (_, property) ->
                            property.setter.callType == CallType.CONSTRUCTOR
                        }
                        .forEach { (varName, property) ->
                            property.setter.writeSet(
                                ownerVar = node.varName,
                                inVar = varName,
                                builder = scope.builder
                            )
                        }
                }
                if (propertyParent == null) {
                    // root element
                    // always declared by the caller so we don't declare this
                    readNode()
                } else {
                    // always declare, we'll set below
                    scope.builder.addLocalVariable(node.varName, propertyParent.property.typeName)
                    if (propertyParent.nonNull) {
                        readNode()
                    } else {
                        val myDescendants = node.allProperties()
                        val allNullCheck =
                            myDescendants.joinToString(" && ") {
                                if (it.alwaysExists) {
                                    "$stmtVar.isNull(${it.indexVar})"
                                } else {
                                    "(${it.indexVar} == -1 || $stmtVar.isNull(${it.indexVar}))"
                                }
                            }
                        scope.builder.apply {
                            beginControlFlow("if (!(%L))", allNullCheck).apply { readNode() }
                            nextControlFlow("else").apply {
                                addStatement("%L = null", node.varName)
                            }
                            endControlFlow()
                        }
                    }
                }
            }
            visitNode(createNodeTree(outVar, propertiesWithIndices, scope))
        }
    }

    /**
     * @param ownerVar The entity / pojo variable that owns this property. It must own this
     *   property! (not the container pojo)
     * @param stmtParamVar The statement variable
     * @param scope The code generation scope
     */
    private fun bindToStatement(ownerVar: String, stmtParamVar: String, scope: CodeGenScope) {
        val binder = property.statementBinder ?: return
        property.getter.writeGetToStatement(ownerVar, stmtParamVar, indexVar, binder, scope)
    }

    /**
     * @param ownerVar The entity / pojo variable that owns this property. It must own this property
     *   (not the container pojo)
     * @param stmtVar The statement variable
     * @param scope The code generation scope
     */
    private fun readFromStatement(ownerVar: String, stmtVar: String, scope: CodeGenScope) {
        fun doRead() {
            val reader = property.statementValueReader ?: return
            property.setter.writeSetFromStatement(ownerVar, stmtVar, indexVar, reader, scope)
        }
        if (alwaysExists) {
            doRead()
        } else {
            scope.builder.apply {
                beginControlFlow("if (%L != -1)", indexVar).apply { doRead() }
                endControlFlow()
            }
        }
    }

    /** Reads the value into a temporary local variable. */
    fun readIntoTmpVar(stmtVar: String, typeName: XTypeName, scope: CodeGenScope): String {
        val tmpProperty = scope.getTmpVar("_tmp${property.name.capitalize(Locale.US)}")
        scope.builder.apply {
            addLocalVariable(tmpProperty, typeName)
            if (alwaysExists) {
                property.statementValueReader?.readFromStatement(
                    tmpProperty,
                    stmtVar,
                    indexVar,
                    scope
                )
            } else {
                beginControlFlow("if (%L == -1)", indexVar).applyTo { language ->
                    val defaultValue = typeName.defaultValue()
                    if (
                        language == CodeLanguage.KOTLIN &&
                            typeName.nullability == XNullability.NONNULL &&
                            defaultValue == "null"
                    ) {
                        addStatement(
                            "error(%S)",
                            "Missing value for a NON-NULL column '${property.columnName}', " +
                                "found NULL value instead."
                        )
                    } else {
                        addStatement("%L = %L", tmpProperty, defaultValue)
                    }
                }
                nextControlFlow("else").apply {
                    property.statementValueReader?.readFromStatement(
                        tmpProperty,
                        stmtVar,
                        indexVar,
                        scope
                    )
                }
                endControlFlow()
            }
        }
        return tmpProperty
    }

    /** On demand node which is created based on the properties that were passed into this class. */
    private class Node(
        // root for me
        val varName: String,
        // set if I'm a PropertyParent
        val propertyParent: EmbeddedProperty?
    ) {
        // whom do i belong
        var parentNode: Node? = null
        // these properties are my direct properties
        lateinit var directProperties: List<PropertyWithIndex>
        // these nodes are under me
        val subNodes = mutableListOf<Node>()

        fun allProperties(): List<PropertyWithIndex> {
            return directProperties + subNodes.flatMap { it.allProperties() }
        }
    }
}
