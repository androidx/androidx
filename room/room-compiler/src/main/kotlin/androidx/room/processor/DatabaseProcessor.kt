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

package androidx.room.processor

import androidx.room.AutoMigration
import androidx.room.SkipQueryVerification
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.RoomTypeNames
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.processor.ProcessorErrors.AUTO_MIGRATION_FOUND_BUT_EXPORT_SCHEMA_OFF
import androidx.room.processor.ProcessorErrors.AUTO_MIGRATION_SCHEMA_IN_FOLDER_NULL
import androidx.room.processor.ProcessorErrors.invalidAutoMigrationSchema
import androidx.room.util.SchemaFileResolver
import androidx.room.verifier.DatabaseVerificationErrors
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.Dao
import androidx.room.vo.DaoFunction
import androidx.room.vo.Database
import androidx.room.vo.DatabaseConstructor
import androidx.room.vo.DatabaseView
import androidx.room.vo.Entity
import androidx.room.vo.FtsEntity
import androidx.room.vo.Warning
import androidx.room.vo.columnNames
import androidx.room.vo.findPropertyByColumnName
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Path
import java.util.Locale

class DatabaseProcessor(baseContext: Context, val element: XTypeElement) {
    val context = baseContext.fork(element)

    private val roomDatabaseTypeElement: XTypeElement by lazy {
        context.processingEnv.requireTypeElement(RoomTypeNames.ROOM_DB)
    }

    fun process(): Database {
        try {
            return doProcess()
        } finally {
            context.databaseVerifier?.closeConnection(context)
        }
    }

    private fun doProcess(): Database {
        val dbAnnotation = element.requireAnnotation(androidx.room.Database::class)

        val entities = processEntities(dbAnnotation, element)
        val viewsMap = processDatabaseViews(dbAnnotation)
        validateForeignKeys(element, entities)
        validateExternalContentFts(element, entities)

        val extendsRoomDb = roomDatabaseTypeElement.type.isAssignableFrom(element.type)
        context.checker.check(extendsRoomDb, element, ProcessorErrors.DB_MUST_EXTEND_ROOM_DB)

        val views = resolveDatabaseViews(viewsMap.values.toList())
        val dbVerifier =
            if (element.hasAnnotation(SkipQueryVerification::class)) {
                null
            } else {
                DatabaseVerifier.create(context, element, entities, views)
            }

        if (dbVerifier != null) {
            context.attachDatabaseVerifier(dbVerifier)
            verifyDatabaseViews(viewsMap, dbVerifier)
        }
        validateUniqueTableAndViewNames(element, entities, views)

        val declaredType = element.type
        val daoFunctions =
            element
                .getAllMethods()
                .filter { it.isAbstract() }
                .filterNot {
                    // remove functions that belong to room
                    it.enclosingElement.asClassName() == RoomTypeNames.ROOM_DB
                }
                .mapNotNull { executable ->
                    // TODO when we add support for non Dao return types (e.g. database), this code
                    // needs to change
                    val daoType = executable.returnType
                    val daoElement = daoType.typeElement
                    if (daoElement == null) {
                        context.logger.e(
                            executable,
                            ProcessorErrors.DATABASE_INVALID_DAO_FUNCTION_RETURN_TYPE
                        )
                        null
                    } else {
                        if (executable.hasAnnotation(JvmName::class)) {
                            context.logger.w(
                                Warning.JVM_NAME_ON_OVERRIDDEN_FUNCTION,
                                executable,
                                ProcessorErrors.JVM_NAME_ON_OVERRIDDEN_FUNCTION
                            )
                        }
                        val dao =
                            DaoProcessor(context, daoElement, declaredType, dbVerifier).process()
                        DaoFunction(executable, dao)
                    }
                }
                .toList()

        validateUniqueDaoClasses(element, daoFunctions, entities)
        validateUniqueIndices(element, entities)

        val hasForeignKeys = entities.any { it.foreignKeys.isNotEmpty() }

        val hasClearAllTables =
            roomDatabaseTypeElement.getDeclaredMethods().any { it.name == "clearAllTables" }

        val version = dbAnnotation.getAsInt("version")
        context.checker.check(
            predicate = version > 0,
            element = element,
            errorMsg = ProcessorErrors.INVALID_DATABASE_VERSION
        )

        val constructorObject = processConstructorObject(element)
        val exportSchema = dbAnnotation["exportSchema"]?.asBoolean() == true
        val database =
            Database(
                version = version,
                element = element,
                type = element.type,
                entities = entities,
                views = views,
                daoFunctions = daoFunctions,
                exportSchema = exportSchema,
                enableForeignKeys = hasForeignKeys,
                overrideClearAllTables = hasClearAllTables,
                constructorObject = constructorObject
            )
        database.autoMigrations = processAutoMigrations(element, dbAnnotation, database.bundle)
        return database
    }

    private fun processAutoMigrations(
        element: XTypeElement,
        dbAnnotation: XAnnotation,
        latestDbSchema: DatabaseBundle
    ): List<androidx.room.vo.AutoMigration> {
        val autoMigrationList = dbAnnotation["autoMigrations"]?.asAnnotationList() ?: emptyList()
        if (autoMigrationList.isEmpty()) {
            return emptyList()
        }

        if (dbAnnotation["exportSchema"]?.asBoolean() != true) {
            context.logger.e(element, AUTO_MIGRATION_FOUND_BUT_EXPORT_SCHEMA_OFF)
            return emptyList()
        }
        val schemaInFolderPath = context.schemaInFolderPath
        if (schemaInFolderPath == null) {
            context.logger.e(element, AUTO_MIGRATION_SCHEMA_IN_FOLDER_NULL)
            return emptyList()
        }

        return autoMigrationList.mapNotNull { autoMigrationAnnotation ->
            val databaseSchemaInFolderPath =
                Path.of(schemaInFolderPath, element.asClassName().canonicalName)
            val fromVersion = autoMigrationAnnotation.getAsInt("from")
            val toVersion = autoMigrationAnnotation.getAsInt("to")
            val fromSchemaBundle =
                getSchemaBundle(fromVersion, databaseSchemaInFolderPath) ?: return@mapNotNull null
            val toSchemaBundle =
                if (toVersion == latestDbSchema.version) {
                    latestDbSchema
                } else {
                    getSchemaBundle(toVersion, databaseSchemaInFolderPath) ?: return@mapNotNull null
                }
            AutoMigrationProcessor(
                    context = context,
                    spec = autoMigrationAnnotation["spec"]?.asType(),
                    fromSchemaBundle = fromSchemaBundle,
                    toSchemaBundle = toSchemaBundle
                )
                .process()
        }
    }

    private fun getSchemaBundle(version: Int, schemaFolderPath: Path): DatabaseBundle? {
        val schemaStream =
            try {
                SchemaFileResolver.RESOLVER.readPath(schemaFolderPath.resolve("$version.json"))
            } catch (e: IOException) {
                null
            }
        if (schemaStream == null) {
            context.logger.e(
                element,
                ProcessorErrors.autoMigrationSchemasNotFound(version, schemaFolderPath.toString()),
            )
            return null
        }
        val bundle =
            try {
                schemaStream.use { SchemaBundle.deserialize(schemaStream) }
            } catch (ex: FileNotFoundException) {
                context.logger.e(
                    element,
                    ProcessorErrors.autoMigrationSchemasNotFound(
                        version,
                        schemaFolderPath.toString()
                    ),
                )
                null
            } catch (th: Throwable) {
                // For debugging support include exception message in an error too.
                context.logger.e("Unable to read schema file: ${th.message ?: ""}")
                context.logger.e(
                    element,
                    invalidAutoMigrationSchema(version, schemaFolderPath.toString())
                )
                null
            }
        return bundle?.database
    }

    private fun validateForeignKeys(element: XTypeElement, entities: List<Entity>) {
        val byTableName = entities.associateBy { it.tableName }
        entities.forEach { entity ->
            entity.foreignKeys.forEach foreignKeyLoop@{ foreignKey ->
                val parent = byTableName[foreignKey.parentTable]
                if (parent == null) {
                    context.logger.e(
                        element,
                        ProcessorErrors.foreignKeyMissingParentEntityInDatabase(
                            foreignKey.parentTable,
                            entity.element.qualifiedName
                        )
                    )
                    return@foreignKeyLoop
                }
                val parentFields =
                    foreignKey.parentColumns.mapNotNull { columnName ->
                        val parentField = parent.findPropertyByColumnName(columnName)
                        if (parentField == null) {
                            context.logger.e(
                                entity.element,
                                ProcessorErrors.foreignKeyParentColumnDoesNotExist(
                                    parentEntity = parent.element.qualifiedName,
                                    missingColumn = columnName,
                                    allColumns = parent.columnNames
                                )
                            )
                        }
                        parentField
                    }
                if (parentFields.size != foreignKey.parentColumns.size) {
                    return@foreignKeyLoop
                }
                // ensure that it is indexed in the parent
                if (!parent.isUnique(foreignKey.parentColumns)) {
                    context.logger.e(
                        parent.element,
                        ProcessorErrors.foreignKeyMissingIndexInParent(
                            parentEntity = parent.element.qualifiedName,
                            childEntity = entity.element.qualifiedName,
                            parentColumns = foreignKey.parentColumns,
                            childColumns = foreignKey.childProperties.map { it.columnName }
                        )
                    )
                    return@foreignKeyLoop
                }
            }
        }
    }

    private fun validateUniqueIndices(element: XTypeElement, entities: List<Entity>) {
        entities
            .flatMap { entity ->
                // associate each index with its entity
                entity.indices.map { Pair(it.name, entity) }
            }
            .groupBy { it.first } // group by index name
            .filter { it.value.size > 1 } // get the ones with duplicate names
            .forEach {
                // do not report duplicates from the same entity
                if (it.value.distinctBy { it.second.typeName }.size > 1) {
                    context.logger.e(
                        element,
                        ProcessorErrors.duplicateIndexInDatabase(
                            it.key,
                            it.value.map {
                                "${it.second.typeName.toString(context.codeLanguage)} > ${it.first}"
                            }
                        )
                    )
                }
            }
    }

    private fun validateUniqueDaoClasses(
        dbElement: XTypeElement,
        daoFunctions: List<DaoFunction>,
        entities: List<Entity>
    ) {
        val entityTypeNames = entities.map { it.typeName }.toSet()
        daoFunctions
            .groupBy { it.dao.typeName }
            .forEach {
                if (it.value.size > 1) {
                    val error =
                        ProcessorErrors.duplicateDao(
                            dao = it.key.toString(context.codeLanguage),
                            functionNames = it.value.map { it.element.name }
                        )
                    it.value.forEach { daoFunction ->
                        context.logger.e(
                            daoFunction.element,
                            ProcessorErrors.DAO_FUNCTION_CONFLICTS_WITH_OTHERS
                        )
                    }
                    // also report the full error for the database
                    context.logger.e(dbElement, error)
                }
            }
        val check =
            fun(element: XElement, dao: Dao, typeName: XTypeName?) {
                typeName?.let {
                    if (!entityTypeNames.contains(typeName)) {
                        context.logger.e(
                            element,
                            ProcessorErrors.shortcutEntityIsNotInDatabase(
                                database = dbElement.qualifiedName,
                                dao = dao.typeName.toString(context.codeLanguage),
                                entity = typeName.toString(context.codeLanguage)
                            )
                        )
                    }
                }
            }
        daoFunctions.forEach { daoFunction ->
            daoFunction.dao.mDeleteOrUpdateShortcutFunctions.forEach { function ->
                function.entities.forEach {
                    check(function.element, daoFunction.dao, it.value.entityTypeName)
                }
            }
            daoFunction.dao.mInsertOrUpsertShortcutFunctions.forEach { function ->
                function.entities.forEach {
                    check(function.element, daoFunction.dao, it.value.entityTypeName)
                }
            }
        }
    }

    private fun validateUniqueTableAndViewNames(
        dbElement: XTypeElement,
        entities: List<Entity>,
        views: List<DatabaseView>
    ) {
        val entitiesInfo =
            entities.map {
                Triple(
                    it.tableName.lowercase(Locale.US),
                    it.typeName.toString(context.codeLanguage),
                    it.element
                )
            }
        val viewsInfo =
            views.map {
                Triple(
                    it.viewName.lowercase(Locale.US),
                    it.typeName.toString(context.codeLanguage),
                    it.element
                )
            }
        (entitiesInfo + viewsInfo)
            .groupBy { (name, _, _) -> name }
            .filter { it.value.size > 1 }
            .forEach { byName ->
                val error =
                    ProcessorErrors.duplicateTableNames(
                        byName.key,
                        byName.value.map { (_, typeName, _) -> typeName }
                    )
                // report it for each of them and the database to make it easier
                // for the developer
                byName.value.forEach { (_, _, element) -> context.logger.e(element, error) }
                context.logger.e(dbElement, error)
            }
    }

    private fun validateExternalContentFts(dbElement: XTypeElement, entities: List<Entity>) {
        // Validate FTS external content entities are present in the same database.
        entities
            .filterIsInstance(FtsEntity::class.java)
            .filterNot {
                it.ftsOptions.contentEntity == null ||
                    entities.contains(it.ftsOptions.contentEntity)
            }
            .forEach {
                context.logger.e(
                    dbElement,
                    ProcessorErrors.missingExternalContentEntity(
                        it.element.qualifiedName,
                        it.ftsOptions.contentEntity!!.element.qualifiedName
                    )
                )
            }
    }

    private fun processEntities(dbAnnotation: XAnnotation, element: XTypeElement): List<Entity> {
        val entityList = dbAnnotation["entities"]?.asTypeList() ?: emptyList()
        context.checker.check(
            entityList.isNotEmpty(),
            element,
            ProcessorErrors.DATABASE_ANNOTATION_MUST_HAVE_LIST_OF_ENTITIES
        )
        return entityList.mapNotNull {
            val typeElement = it.typeElement
            if (typeElement == null) {
                context.logger.e(
                    element,
                    ProcessorErrors.invalidEntityTypeInDatabaseAnnotation(
                        it.asTypeName().toString(context.codeLanguage)
                    )
                )
                null
            } else {
                EntityProcessor(context, typeElement).process()
            }
        }
    }

    private fun processDatabaseViews(dbAnnotation: XAnnotation): Map<XTypeElement, DatabaseView> {
        val viewList = dbAnnotation["views"]?.asTypeList() ?: emptyList()
        return viewList
            .mapNotNull {
                val viewElement = it.typeElement
                if (viewElement == null) {
                    context.logger.e(
                        element,
                        ProcessorErrors.invalidViewTypeInDatabaseAnnotation(
                            it.asTypeName().toString(context.codeLanguage)
                        )
                    )
                    null
                } else {
                    viewElement to DatabaseViewProcessor(context, viewElement).process()
                }
            }
            .toMap()
    }

    private fun verifyDatabaseViews(
        map: Map<XTypeElement, DatabaseView>,
        dbVerifier: DatabaseVerifier
    ) {
        for ((viewElement, view) in map) {
            if (viewElement.hasAnnotation(SkipQueryVerification::class)) {
                continue
            }
            view.query.resultInfo = dbVerifier.analyze(view.query.original)
            if (view.query.resultInfo?.error != null) {
                context.logger.e(
                    viewElement,
                    DatabaseVerificationErrors.cannotVerifyQuery(view.query.resultInfo!!.error!!)
                )
            }
        }
    }

    /**
     * Resolves all the underlying tables for each of the [DatabaseView]. All the tables including
     * those that are indirectly referenced are included.
     *
     * @param views The list of all the [DatabaseView]s in this database. The order in this list is
     *   important. A view always comes after all of the tables and views that it depends on.
     */
    fun resolveDatabaseViews(views: List<DatabaseView>): List<DatabaseView> {
        if (views.isEmpty()) {
            return emptyList()
        }
        val viewNames = views.map { it.viewName }
        fun isTable(name: String) = viewNames.none { it.equals(name, ignoreCase = true) }
        for (view in views) {
            // Some of these "tables" might actually be views.
            view.tables.addAll(view.query.tables.map { (name, _) -> name })
        }
        val unresolvedViews = views.toMutableList()
        // We will resolve nested views step by step, and store the results here.
        val resolvedViews = mutableMapOf<String, Set<String>>()
        val result = mutableListOf<DatabaseView>()
        do {
            for ((viewName, tables) in resolvedViews) {
                for (view in unresolvedViews) {
                    // If we find a nested view, replace it with the list of concrete tables.
                    if (view.tables.removeIf { it.equals(viewName, ignoreCase = true) }) {
                        view.tables.addAll(tables)
                    }
                }
            }
            var countNewlyResolved = 0
            // Separate out views that have all of their underlying tables resolved.
            unresolvedViews
                .filter { view -> view.tables.all { isTable(it) } }
                .forEach { view ->
                    resolvedViews[view.viewName] = view.tables
                    unresolvedViews.remove(view)
                    result.add(view)
                    countNewlyResolved++
                }
            // We couldn't resolve a single view in this step. It indicates circular reference.
            if (countNewlyResolved == 0) {
                context.logger.e(
                    element,
                    ProcessorErrors.viewCircularReferenceDetected(
                        unresolvedViews.map { it.viewName }
                    )
                )
                break
            }
            // We are done if we have resolved tables for all the views.
        } while (unresolvedViews.isNotEmpty())
        return result
    }

    private fun processConstructorObject(element: XTypeElement): DatabaseConstructor? {
        val annotation = element.getAnnotation(androidx.room.ConstructedBy::class)
        if (annotation == null) {
            // If no @ConstructedBy is present then validate target is JVM (including Android)
            // since reflection is available in those platforms and a database constructor is not
            // needed.
            context.checker.check(
                predicate = context.isJvmOnlyTarget(),
                element = element,
                errorMsg = ProcessorErrors.MISSING_CONSTRUCTED_BY_ANNOTATION
            )
            return null
        }
        val type = annotation.getAsType("value")
        val typeElement = type.typeElement
        if (typeElement == null) {
            context.logger.e(element, ProcessorErrors.INVALID_CONSTRUCTED_BY_CLASS)
            return null
        }

        context.checker.check(
            predicate = typeElement.isKotlinObject(),
            element = typeElement,
            errorMsg = ProcessorErrors.INVALID_CONSTRUCTED_BY_NOT_OBJECT
        )

        context.checker.check(
            predicate = typeElement.isExpect(),
            element = typeElement,
            errorMsg = ProcessorErrors.INVALID_CONSTRUCTED_BY_NOT_EXPECT
        )

        val expectedSuperInterfaceTypeName =
            RoomTypeNames.ROOM_DB_CONSTRUCTOR.parametrizedBy(element.asClassName())
        val superInterface = typeElement.superInterfaces.singleOrNull()
        if (
            superInterface == null ||
                superInterface.asTypeName().rawTypeName != RoomTypeNames.ROOM_DB_CONSTRUCTOR
        ) {
            context.logger.e(
                element = typeElement,
                msg =
                    ProcessorErrors.invalidConstructedBySuperInterface(
                        expectedSuperInterfaceTypeName.toString(context.codeLanguage)
                    )
            )
            return null
        }

        val typeArg = superInterface.typeArguments.singleOrNull()
        if (typeArg == null || typeArg.asTypeName().rawTypeName != element.asClassName()) {
            context.logger.e(
                element = typeElement,
                msg =
                    ProcessorErrors.invalidConstructedBySuperInterface(
                        expectedSuperInterfaceTypeName.toString(context.codeLanguage)
                    )
            )
            return null
        }

        val initializeExecutableElement =
            context.processingEnv
                .requireTypeElement(RoomTypeNames.ROOM_DB_CONSTRUCTOR)
                .getDeclaredMethods()
                .single()
        val isInitOverridden =
            typeElement.getDeclaredMethods().any {
                it.overrides(initializeExecutableElement, typeElement)
            }

        return DatabaseConstructor(typeElement, isInitOverridden)
    }
}
