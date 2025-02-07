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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.applyTo
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.XTypeSpec.Builder.Companion.applyTo
import androidx.room.compiler.codegen.buildCodeBlock
import androidx.room.compiler.codegen.compat.XConverters.applyToJavaPoet
import androidx.room.compiler.codegen.compat.XConverters.applyToKotlinPoet
import androidx.room.compiler.processing.PropertySpecHelper
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomMemberNames
import androidx.room.ext.RoomTypeNames.DELETE_OR_UPDATE_ADAPTER
import androidx.room.ext.RoomTypeNames.INSERT_ADAPTER
import androidx.room.ext.RoomTypeNames.RAW_QUERY
import androidx.room.ext.RoomTypeNames.ROOM_DB
import androidx.room.ext.RoomTypeNames.ROOM_SQL_QUERY
import androidx.room.ext.RoomTypeNames.UPSERT_ADAPTER
import androidx.room.ext.capitalize
import androidx.room.processor.OnConflictProcessor
import androidx.room.solver.CodeGenScope
import androidx.room.solver.KotlinBoxedPrimitiveFunctionDelegateBinder
import androidx.room.solver.KotlinDefaultFunctionDelegateBinder
import androidx.room.solver.types.getRequiredTypeConverters
import androidx.room.vo.Dao
import androidx.room.vo.DeleteOrUpdateShortcutFunction
import androidx.room.vo.InsertFunction
import androidx.room.vo.KotlinBoxedPrimitiveFunctionDelegate
import androidx.room.vo.KotlinDefaultFunctionDelegate
import androidx.room.vo.RawQueryFunction
import androidx.room.vo.ReadQueryFunction
import androidx.room.vo.ShortcutEntity
import androidx.room.vo.TransactionFunction
import androidx.room.vo.UpdateFunction
import androidx.room.vo.UpsertFunction
import androidx.room.vo.WriteQueryFunction
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.jvm.jvmName
import java.util.Locale

/** Creates the implementation for a class annotated with Dao. */
class DaoWriter(
    val dao: Dao,
    private val dbElement: XElement,
    writerContext: WriterContext,
) : TypeWriter(writerContext) {
    private val declaredDao = dao.element.type
    private val className = dao.implTypeName
    override val packageName = className.packageName

    // TODO nothing prevents this from conflicting, we should fix.
    private val dbProperty: XPropertySpec =
        XPropertySpec.builder(DB_PROPERTY_NAME, ROOM_DB, VisibilityModifier.PRIVATE).build()

    private val companionTypeBuilder = lazy { XTypeSpec.companionObjectBuilder() }

    companion object {
        const val GET_LIST_OF_TYPE_CONVERTERS_FUNCTION = "getRequiredConverters"

        const val DB_PROPERTY_NAME = "__db"

        private fun shortcutEntityFieldNamePart(shortcutEntity: ShortcutEntity): String {
            fun typeNameToFieldName(typeName: XClassName): String {
                return typeName.simpleNames.last()
            }
            return if (shortcutEntity.isPartialEntity) {
                typeNameToFieldName(shortcutEntity.dataClass.className) +
                    "As" +
                    typeNameToFieldName(shortcutEntity.entityClassName)
            } else {
                typeNameToFieldName(shortcutEntity.entityClassName)
            }
        }
    }

    override fun createTypeSpecBuilder(): XTypeSpec.Builder {
        val builder = XTypeSpec.classBuilder(className)

        val preparedQueries = dao.queryFunctions.filterIsInstance<WriteQueryFunction>()

        val shortcutFunctions = buildList {
            addAll(createInsertFunctions())
            addAll(createDeleteFunctions())
            addAll(createUpdateFunctions())
            addAll(createTransactionFunctions())
            addAll(createUpsertFunctions())
        }

        builder.apply {
            addOriginatingElement(dbElement)
            setVisibility(
                if (dao.element.isInternal()) {
                    VisibilityModifier.INTERNAL
                } else {
                    VisibilityModifier.PUBLIC
                }
            )
            if (dao.element.isInterface()) {
                addSuperinterface(dao.typeName)
            } else {
                superclass(dao.typeName)
            }
            addProperty(dbProperty)

            setPrimaryConstructor(
                createConstructor(shortcutFunctions, dao.constructorParamType != null)
            )

            shortcutFunctions.forEach { addFunction(it.functionImpl) }
            dao.queryFunctions.filterIsInstance<ReadQueryFunction>().forEach { function ->
                addFunction(createSelectFunction(function))
                if (function.isProperty) {
                    // DAO function is a getter from a Kotlin property, generate property override.
                    applyToKotlinPoet {
                        addProperty(
                            PropertySpecHelper.overriding(function.element, declaredDao)
                                .getter(
                                    FunSpec.getterBuilder()
                                        .addCode("return %L()", function.element.name)
                                        .build()
                                )
                                .build()
                        )
                    }
                }
            }
            preparedQueries.forEach { addFunction(createPreparedQueryFunction(it)) }
            dao.rawQueryFunctions.forEach { addFunction(createRawQueryFunction(it)) }
            applyTo(CodeLanguage.JAVA) {
                dao.kotlinDefaultFunctionDelegates.forEach {
                    addFunction(createDefaultImplFunctionDelegate(it))
                }
                dao.kotlinBoxedPrimitiveFunctionDelegates.forEach {
                    addFunction(createBoxedPrimitiveBridgeFunctionDelegate(it))
                }
            }
            // Keep this the last one to be generated because used custom converters will
            // register fields with a payload which we collect in dao to report used
            // Type Converters.
            addConverterListFunction(this)
            applyTo(CodeLanguage.KOTLIN) {
                if (companionTypeBuilder.isInitialized()) {
                    addType(companionTypeBuilder.value.build())
                }
            }
        }
        return builder
    }

    private fun addConverterListFunction(typeSpecBuilder: XTypeSpec.Builder) {
        // For Java a static method is created
        typeSpecBuilder.applyTo(CodeLanguage.JAVA) { addFunction(createConverterListFunction()) }
        // For Kotlin a function in the companion object is created
        companionTypeBuilder.value.applyTo(CodeLanguage.KOTLIN) {
            addFunction(createConverterListFunction())
        }
    }

    private fun createConverterListFunction(): XFunSpec {
        val body = buildCodeBlock { language ->
            val requiredTypeConverters = getRequiredTypeConverters()
            if (requiredTypeConverters.isEmpty()) {
                when (language) {
                    CodeLanguage.JAVA ->
                        addStatement("return %T.emptyList()", CommonTypeNames.COLLECTIONS)
                    CodeLanguage.KOTLIN -> addStatement("return emptyList()")
                }
            } else {
                val placeholders = requiredTypeConverters.joinToString(",") { "%L" }
                val requiredTypeConvertersLiterals =
                    requiredTypeConverters
                        .map {
                            when (language) {
                                CodeLanguage.JAVA -> XCodeBlock.ofJavaClassLiteral(it)
                                CodeLanguage.KOTLIN -> XCodeBlock.ofKotlinClassLiteral(it)
                            }
                        }
                        .toTypedArray()
                when (language) {
                    CodeLanguage.JAVA ->
                        addStatement(
                            "return %T.asList($placeholders)",
                            CommonTypeNames.ARRAYS,
                            *requiredTypeConvertersLiterals
                        )
                    CodeLanguage.KOTLIN ->
                        addStatement(
                            "return listOf($placeholders)",
                            *requiredTypeConvertersLiterals
                        )
                }
            }
        }
        return XFunSpec.builder(GET_LIST_OF_TYPE_CONVERTERS_FUNCTION, VisibilityModifier.PUBLIC)
            .applyToJavaPoet { addModifiers(javax.lang.model.element.Modifier.STATIC) }
            .applyTo { language ->
                returns(
                    CommonTypeNames.LIST.parametrizedBy(
                        when (language) {
                            CodeLanguage.JAVA -> CommonTypeNames.JAVA_CLASS
                            CodeLanguage.KOTLIN -> CommonTypeNames.KOTLIN_CLASS
                        }.parametrizedBy(XTypeName.ANY_WILDCARD)
                    )
                )
            }
            .addCode(body)
            .build()
    }

    private fun createTransactionFunctions(): List<PreparedStmtQuery> {
        return dao.transactionFunctions.map {
            PreparedStmtQuery(emptyMap(), createTransactionFunctionBody(it))
        }
    }

    private fun createTransactionFunctionBody(function: TransactionFunction): XFunSpec {
        val scope = CodeGenScope(this)
        function.functionBinder.executeAndReturn(
            parameterNames = function.parameterNames,
            daoName = dao.typeName,
            daoImplName = dao.implTypeName,
            dbProperty = dbProperty,
            scope = scope
        )
        return overrideWithoutAnnotations(function.element, declaredDao)
            .addCode(scope.generate())
            .build()
    }

    private fun createConstructor(
        shortcutFunctions: List<PreparedStmtQuery>,
        callSuper: Boolean
    ): XFunSpec {
        val body = buildCodeBlock {
            addStatement("this.%N = %L", dbProperty, dbProperty.name)
            shortcutFunctions
                .asSequence()
                .filterNot { it.fields.isEmpty() }
                .map { it.fields.values }
                .flatten()
                .groupBy { it.first.name }
                .map { it.value.first() }
                .forEach { (propertySpec, initExpression) ->
                    addStatement("this.%L = %L", propertySpec.name, initExpression)
                }
        }
        return XFunSpec.constructorBuilder(VisibilityModifier.PUBLIC)
            .apply {
                addParameter(typeName = dao.constructorParamType ?: ROOM_DB, name = dbProperty.name)
                if (callSuper) {
                    callSuperConstructor(XCodeBlock.of("%L", dbProperty.name))
                }
                addCode(body)
            }
            .build()
    }

    private fun createSelectFunction(function: ReadQueryFunction): XFunSpec {
        return overrideWithoutAnnotations(function.element, declaredDao)
            .applyToKotlinPoet {
                // TODO: Update XPoet to better handle this case.
                if (function.isProperty) {
                    // When the DAO function is from a Kotlin property, we'll still generate
                    // a DAO function, but it won't be an override and it'll be private, to be
                    // called from the overridden property's getter.
                    modifiers.remove(KModifier.OVERRIDE)
                    modifiers.removeAll(
                        listOf(KModifier.PUBLIC, KModifier.INTERNAL, KModifier.PROTECTED)
                    )
                    addModifiers(KModifier.PRIVATE)

                    // For JVM emit a @JvmName to avoid same-signature conflict with
                    // actual property.
                    if (
                        context.targetPlatforms.size == 1 &&
                            context.targetPlatforms.contains(XProcessingEnv.Platform.JVM)
                    ) {
                        jvmName("_private${function.element.name.capitalize(Locale.US)}")
                    }
                }
            }
            .addCode(createQueryFunctionBody(function))
            .build()
    }

    private fun createRawQueryFunction(function: RawQueryFunction): XFunSpec {
        return overrideWithoutAnnotations(function.element, declaredDao)
            .addCode(
                if (
                    function.runtimeQueryParam == null ||
                        function.queryResultBinder.usesCompatQueryWriter
                ) {
                    compatCreateRawQueryFunctionBody(function)
                } else {
                    createRawQueryFunctionBody(function)
                }
            )
            .build()
    }

    private fun createRawQueryFunctionBody(function: RawQueryFunction): XCodeBlock {
        val scope = CodeGenScope(this@DaoWriter)
        val sqlQueryVar = scope.getTmpVar("_sql")
        val rawQueryParamName =
            if (function.runtimeQueryParam!!.isSupportQuery()) {
                val rawQueryVar = scope.getTmpVar("_rawQuery")
                scope.builder.addLocalVariable(
                    name = rawQueryVar,
                    typeName = RAW_QUERY,
                    assignExpr =
                        XCodeBlock.of(
                            format = "%T.copyFrom(%L).toRoomRawQuery()",
                            ROOM_SQL_QUERY,
                            function.runtimeQueryParam.paramName
                        )
                )
                rawQueryVar
            } else {
                function.runtimeQueryParam.paramName
            }

        scope.builder.addLocalVal(
            sqlQueryVar,
            CommonTypeNames.STRING,
            "%L.%L",
            rawQueryParamName,
            XCodeBlock.ofString(java = "getSql()", kotlin = "sql")
        )

        if (function.returnsValue) {
            function.queryResultBinder.convertAndReturn(
                sqlQueryVar = sqlQueryVar,
                dbProperty = dbProperty,
                bindStatement = { stmtVar ->
                    this.builder.addStatement(
                        "%L.getBindingFunction().invoke(%L)",
                        rawQueryParamName,
                        stmtVar
                    )
                },
                returnTypeName = function.returnType.asTypeName(),
                inTransaction = function.inTransaction,
                scope = scope
            )
        }
        return scope.generate()
    }

    /** Used by the Non-KMP Paging3 binders and the Paging2 binders. */
    private fun compatCreateRawQueryFunctionBody(function: RawQueryFunction): XCodeBlock =
        XCodeBlock.builder()
            .apply {
                val scope = CodeGenScope(this@DaoWriter)
                val roomSQLiteQueryVar: String
                val queryParam = function.runtimeQueryParam
                if (queryParam?.isSupportQuery() == true) {
                    queryParam.paramName
                } else if (queryParam?.isString() == true) {
                    roomSQLiteQueryVar = scope.getTmpVar("_statement")
                    addLocalVariable(
                        name = roomSQLiteQueryVar,
                        typeName = ROOM_SQL_QUERY,
                        assignExpr =
                            XCodeBlock.of(
                                "%M(%L, 0)",
                                RoomMemberNames.ROOM_SQL_QUERY_ACQUIRE,
                                queryParam.paramName
                            ),
                    )
                } else {
                    // try to generate compiling code. we would've already reported this error
                    roomSQLiteQueryVar = scope.getTmpVar("_statement")
                    addLocalVariable(
                        name = roomSQLiteQueryVar,
                        typeName = ROOM_SQL_QUERY,
                        assignExpr =
                            XCodeBlock.of(
                                "%M(%S, 0)",
                                RoomMemberNames.ROOM_SQL_QUERY_ACQUIRE,
                                "missing query parameter"
                            ),
                    )
                }
                val rawQueryParamName = function.runtimeQueryParam?.paramName
                if (rawQueryParamName != null) {
                    if (function.returnsValue) {
                        function.queryResultBinder.convertAndReturn(
                            sqlQueryVar = rawQueryParamName,
                            dbProperty = dbProperty,
                            bindStatement = { stmtVar ->
                                this.builder.addStatement(
                                    "%L.getBindingFunction().invoke(%L)",
                                    rawQueryParamName,
                                    stmtVar
                                )
                            },
                            returnTypeName = function.returnType.asTypeName(),
                            inTransaction = function.inTransaction,
                            scope = scope
                        )
                    }
                }
                add(scope.generate())
            }
            .build()

    private fun createPreparedQueryFunction(function: WriteQueryFunction): XFunSpec {
        return overrideWithoutAnnotations(function.element, declaredDao)
            .addCode(createPreparedQueryFunctionBody(function))
            .build()
    }

    /**
     * Groups all insert functions based on the insert statement they will use then creates all
     * field specs, EntityInsertAdapterWriter and actual insert functions.
     */
    private fun createInsertFunctions(): List<PreparedStmtQuery> {
        return dao.insertFunctions.map { insertFunction ->
            val onConflict = OnConflictProcessor.onConflictText(insertFunction.onConflict)
            val entities = insertFunction.entities

            val fields =
                entities.mapValues {
                    val spec = getOrCreateProperty(InsertFunctionProperty(it.value, onConflict))
                    val impl =
                        EntityInsertAdapterWriter.create(it.value, onConflict)
                            .createAnonymous(this@DaoWriter)
                    spec to impl
                }
            val functionImpl =
                overrideWithoutAnnotations(insertFunction.element, declaredDao)
                    .apply { addCode(createInsertFunctionBody(insertFunction, fields)) }
                    .build()
            PreparedStmtQuery(fields, functionImpl)
        }
    }

    private fun createInsertFunctionBody(
        function: InsertFunction,
        insertAdapters: Map<String, Pair<XPropertySpec, XTypeSpec>>
    ): XCodeBlock {
        if (insertAdapters.isEmpty() || function.functionBinder == null) {
            return XCodeBlock.builder().build()
        }
        val scope = CodeGenScope(writer = this)
        ShortcutQueryParameterWriter.addNullCheckValidation(scope, function.parameters)
        function.functionBinder.convertAndReturn(
            parameters = function.parameters,
            adapters = insertAdapters,
            dbProperty = dbProperty,
            scope = scope
        )
        return scope.generate()
    }

    /** Creates EntityUpdateAdapter for each delete function. */
    private fun createDeleteFunctions(): List<PreparedStmtQuery> {
        return createShortcutFunctions(dao.deleteFunctions, "delete") { _, entity ->
            EntityDeleteAdapterWriter.create(entity).createAnonymous(this@DaoWriter)
        }
    }

    /** Creates EntityUpdateAdapter for each @Update function. */
    private fun createUpdateFunctions(): List<PreparedStmtQuery> {
        return createShortcutFunctions(dao.updateFunctions, "update") { update, entity ->
            val onConflict = OnConflictProcessor.onConflictText(update.onConflictStrategy)
            EntityUpdateAdapterWriter.create(entity, onConflict).createAnonymous(this@DaoWriter)
        }
    }

    private fun <T : DeleteOrUpdateShortcutFunction> createShortcutFunctions(
        functions: List<T>,
        functionPrefix: String,
        implCallback: (T, ShortcutEntity) -> XTypeSpec
    ): List<PreparedStmtQuery> {
        return functions.mapNotNull { function ->
            val entities = function.entities
            if (entities.isEmpty()) {
                null
            } else {
                val onConflict =
                    if (function is UpdateFunction) {
                        OnConflictProcessor.onConflictText(function.onConflictStrategy)
                    } else {
                        ""
                    }
                val fields =
                    entities.mapValues {
                        val spec =
                            getOrCreateProperty(
                                DeleteOrUpdateAdapterProperty(it.value, functionPrefix, onConflict)
                            )
                        val impl = implCallback(function, it.value)
                        spec to impl
                    }
                val functionSpec =
                    overrideWithoutAnnotations(function.element, declaredDao)
                        .apply { addCode(createDeleteOrUpdateFunctionBody(function, fields)) }
                        .build()
                PreparedStmtQuery(fields, functionSpec)
            }
        }
    }

    private fun createDeleteOrUpdateFunctionBody(
        function: DeleteOrUpdateShortcutFunction,
        adapters: Map<String, Pair<XPropertySpec, XTypeSpec>>
    ): XCodeBlock {
        if (adapters.isEmpty() || function.functionBinder == null) {
            return XCodeBlock.builder().build()
        }
        val scope = CodeGenScope(writer = this)
        ShortcutQueryParameterWriter.addNullCheckValidation(scope, function.parameters)
        function.functionBinder.convertAndReturn(
            parameters = function.parameters,
            adapters = adapters,
            dbProperty = dbProperty,
            scope = scope
        )
        return scope.generate()
    }

    /**
     * Groups all upsert functions based on the upsert statement they will use then creates all
     * field specs, EntityUpsertAdapterWriter and actual upsert functions.
     */
    private fun createUpsertFunctions(): List<PreparedStmtQuery> {
        return dao.upsertFunctions.map { upsertFunctions ->
            val entities = upsertFunctions.entities
            val fields =
                entities.mapValues {
                    val spec = getOrCreateProperty(UpsertAdapterProperty(it.value))
                    val impl =
                        EntityUpsertAdapterWriter.create(it.value)
                            .createConcrete(it.value, this@DaoWriter)
                    spec to impl
                }
            val functionImpl =
                overrideWithoutAnnotations(upsertFunctions.element, declaredDao)
                    .apply { addCode(createUpsertFunctionBody(upsertFunctions, fields)) }
                    .build()
            PreparedStmtQuery(fields, functionImpl)
        }
    }

    private fun createUpsertFunctionBody(
        function: UpsertFunction,
        upsertAdapters: Map<String, Pair<XPropertySpec, XCodeBlock>>
    ): XCodeBlock {
        if (upsertAdapters.isEmpty() || function.functionBinder == null) {
            return XCodeBlock.builder().build()
        }
        val scope = CodeGenScope(writer = this)
        ShortcutQueryParameterWriter.addNullCheckValidation(scope, function.parameters)
        function.functionBinder.convertAndReturn(
            parameters = function.parameters,
            adapters = upsertAdapters,
            dbProperty = dbProperty,
            scope = scope
        )
        return scope.generate()
    }

    private fun createPreparedQueryFunctionBody(function: WriteQueryFunction): XCodeBlock {
        val scope = CodeGenScope(this)
        val queryWriter = QueryWriter(function)
        val sqlVar = scope.getTmpVar("_sql")
        val listSizeArgs = queryWriter.prepareQuery(sqlVar, scope)
        function.preparedQueryResultBinder.executeAndReturn(
            sqlQueryVar = sqlVar,
            dbProperty = dbProperty,
            bindStatement = { stmtVar -> queryWriter.bindArgs(stmtVar, listSizeArgs, this) },
            returnTypeName = function.returnType.asTypeName(),
            scope = scope
        )
        return scope.generate()
    }

    private fun createQueryFunctionBody(function: ReadQueryFunction): XCodeBlock {
        val scope = CodeGenScope(this)
        val queryWriter = QueryWriter(function)
        val sqlStringVar = scope.getTmpVar("_sql")

        val (sqlVar, listSizeArgs) =
            if (function.queryResultBinder.usesCompatQueryWriter) {
                val roomSQLiteQueryVar = scope.getTmpVar("_statement")
                queryWriter.prepareReadAndBind(sqlStringVar, roomSQLiteQueryVar, scope)
                roomSQLiteQueryVar to emptyList()
            } else {
                sqlStringVar to queryWriter.prepareQuery(sqlStringVar, scope)
            }

        val bindStatement: (CodeGenScope.(String) -> Unit)? =
            if (queryWriter.parameters.isNotEmpty()) {
                { stmtVar -> queryWriter.bindArgs(stmtVar, listSizeArgs, this) }
            } else {
                null
            }

        function.queryResultBinder.convertAndReturn(
            sqlQueryVar = sqlVar,
            dbProperty = dbProperty,
            bindStatement = bindStatement,
            returnTypeName = function.returnType.asTypeName(),
            inTransaction = function.inTransaction,
            scope = scope
        )

        return scope.generate()
    }

    // TODO(b/251459654): Handle @JvmOverloads in delegating functions with Kotlin codegen.
    private fun createDefaultImplFunctionDelegate(
        function: KotlinDefaultFunctionDelegate
    ): XFunSpec {
        val scope = CodeGenScope(this)
        return overrideWithoutAnnotations(function.element, declaredDao)
            .apply {
                KotlinDefaultFunctionDelegateBinder.executeAndReturn(
                    daoName = dao.typeName,
                    daoImplName = dao.implTypeName,
                    functionName = function.element.jvmName,
                    returnType = function.element.returnType,
                    parameterNames = function.element.parameters.map { it.name },
                    scope = scope
                )
                addCode(scope.generate())
            }
            .build()
    }

    private fun createBoxedPrimitiveBridgeFunctionDelegate(
        function: KotlinBoxedPrimitiveFunctionDelegate
    ): XFunSpec {
        val scope = CodeGenScope(this)
        return overrideWithoutAnnotations(function.element, declaredDao)
            .apply {
                KotlinBoxedPrimitiveFunctionDelegateBinder.execute(
                    functionName = function.element.jvmName,
                    returnType = function.element.returnType,
                    parameters =
                        function.concreteFunction.parameters.map {
                            it.type.asTypeName() to it.name
                        },
                    scope = scope
                )
                addCode(scope.generate())
            }
            .build()
    }

    private fun overrideWithoutAnnotations(elm: XMethodElement, owner: XType): XFunSpec.Builder {
        return XFunSpec.overridingBuilder(elm, owner)
    }

    /**
     * Represents a query statement prepared in Dao implementation.
     *
     * @param properties This map holds all the member properties necessary for this query. The key
     *   is the corresponding parameter name in the defining query function. The value is a pair
     *   from the property declaration to definition.
     * @param functionImpl The body of the query function implementation.
     */
    data class PreparedStmtQuery(
        val fields: Map<String, Pair<XPropertySpec, Any>>,
        val functionImpl: XFunSpec
    ) {
        companion object {
            // The key to be used in `fields` where the function requires a field that is not
            // associated with any of its parameters
            const val NO_PARAM_FIELD = "-"
        }
    }

    private class InsertFunctionProperty(
        val shortcutEntity: ShortcutEntity,
        val onConflictText: String,
    ) :
        SharedPropertySpec(
            baseName = "insertAdapterOf${shortcutEntityFieldNamePart(shortcutEntity)}",
            type = INSERT_ADAPTER.parametrizedBy(shortcutEntity.dataClass.typeName)
        ) {
        override fun getUniqueKey(): String {
            return "${shortcutEntity.dataClass.typeName}-${shortcutEntity.entityTypeName}" +
                onConflictText
        }

        override fun prepare(writer: TypeWriter, builder: XPropertySpec.Builder) {}
    }

    class DeleteOrUpdateAdapterProperty(
        val shortcutEntity: ShortcutEntity,
        val functionPrefix: String,
        val onConflictText: String,
    ) :
        SharedPropertySpec(
            baseName = "${functionPrefix}AdapterOf${shortcutEntityFieldNamePart(shortcutEntity)}",
            type = DELETE_OR_UPDATE_ADAPTER.parametrizedBy(shortcutEntity.dataClass.typeName)
        ) {
        override fun prepare(writer: TypeWriter, builder: XPropertySpec.Builder) {}

        override fun getUniqueKey(): String {
            return "${shortcutEntity.dataClass.typeName}-${shortcutEntity.entityTypeName}" +
                "$functionPrefix$onConflictText"
        }
    }

    class UpsertAdapterProperty(val shortcutEntity: ShortcutEntity) :
        SharedPropertySpec(
            baseName = "upsertAdapterOf${shortcutEntityFieldNamePart(shortcutEntity)}",
            type = UPSERT_ADAPTER.parametrizedBy(shortcutEntity.dataClass.typeName)
        ) {
        override fun getUniqueKey(): String {
            return "${shortcutEntity.dataClass.typeName}-${shortcutEntity.entityTypeName}"
        }

        override fun prepare(writer: TypeWriter, builder: XPropertySpec.Builder) {}
    }
}
