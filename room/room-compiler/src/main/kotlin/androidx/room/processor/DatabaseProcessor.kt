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
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.RoomTypeNames
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.processor.ProcessorErrors.AUTO_MIGRATION_FOUND_BUT_EXPORT_SCHEMA_OFF
import androidx.room.processor.ProcessorErrors.AUTO_MIGRATION_SCHEMA_IN_FOLDER_NULL
import androidx.room.processor.ProcessorErrors.autoMigrationSchemasMustBeRoomGenerated
import androidx.room.processor.ProcessorErrors.invalidAutoMigrationSchema
import androidx.room.util.SchemaFileResolver
import androidx.room.verifier.DatabaseVerificationErrors
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.Dao
import androidx.room.vo.DaoMethod
import androidx.room.vo.Database
import androidx.room.vo.DatabaseView
import androidx.room.vo.Entity
import androidx.room.vo.FtsEntity
import androidx.room.vo.Warning
import androidx.room.vo.columnNames
import androidx.room.vo.findFieldByColumnName
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.util.Locale

class DatabaseProcessor(baseContext: Context, val element: XTypeElement) {
    val context = baseContext.fork(element)

    val roomDatabaseType: XType by lazy {
        context.processingEnv.requireType(RoomTypeNames.ROOM_DB)
    }

    fun process(): Database {
        try {
            return doProcess()
        } finally {
            context.databaseVerifier?.closeConnection(context)
        }
    }

    private fun doProcess(): Database {
        val dbAnnotation = element.getAnnotation(androidx.room.Database::class)!!

        val entities = processEntities(dbAnnotation, element)
        val viewsMap = processDatabaseViews(dbAnnotation)
        validateForeignKeys(element, entities)
        validateExternalContentFts(element, entities)

        val extendsRoomDb = roomDatabaseType.isAssignableFrom(element.type)
        context.checker.check(extendsRoomDb, element, ProcessorErrors.DB_MUST_EXTEND_ROOM_DB)

        val views = resolveDatabaseViews(viewsMap.values.toList())
        val dbVerifier = if (element.hasAnnotation(SkipQueryVerification::class)) {
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
        val daoMethods = element.getAllMethods().filter {
            it.isAbstract()
        }.filterNot {
            // remove methods that belong to room
            it.enclosingElement.asClassName() == RoomTypeNames.ROOM_DB
        }.mapNotNull { executable ->
            // TODO when we add support for non Dao return types (e.g. database), this code needs
            // to change
            val daoType = executable.returnType
            val daoElement = daoType.typeElement
            if (daoElement == null) {
                context.logger.e(
                    executable,
                    ProcessorErrors.DATABASE_INVALID_DAO_METHOD_RETURN_TYPE
                )
                null
            } else {
                if (executable.hasAnnotation(JvmName::class)) {
                    context.logger.w(
                        Warning.JVM_NAME_ON_OVERRIDDEN_METHOD,
                        executable,
                        ProcessorErrors.JVM_NAME_ON_OVERRIDDEN_METHOD
                    )
                }
                if (
                    context.codeLanguage == CodeLanguage.KOTLIN &&
                    executable.isKotlinPropertyMethod()
                ) {
                    context.logger.e(executable, ProcessorErrors.KOTLIN_PROPERTY_OVERRIDE)
                }
                val dao = DaoProcessor(context, daoElement, declaredType, dbVerifier)
                    .process()
                DaoMethod(executable, dao)
            }
        }.toList()

        validateUniqueDaoClasses(element, daoMethods, entities)
        validateUniqueIndices(element, entities)

        val hasForeignKeys = entities.any { it.foreignKeys.isNotEmpty() }

        val database = Database(
            version = dbAnnotation.value.version,
            element = element,
            type = element.type,
            entities = entities,
            views = views,
            daoMethods = daoMethods,
            exportSchema = dbAnnotation.value.exportSchema,
            enableForeignKeys = hasForeignKeys
        )
        database.autoMigrations = processAutoMigrations(element, database.bundle)
        return database
    }

    private fun processAutoMigrations(
        element: XTypeElement,
        latestDbSchema: DatabaseBundle
    ): List<androidx.room.vo.AutoMigration> {
        val dbAnnotation = element.getAnnotation(androidx.room.Database::class)!!

        val autoMigrationList = dbAnnotation
            .getAsAnnotationBoxArray<AutoMigration>("autoMigrations")
        if (autoMigrationList.isEmpty()) {
            return emptyList()
        }

        if (!dbAnnotation.value.exportSchema) {
            context.logger.e(
                element,
                AUTO_MIGRATION_FOUND_BUT_EXPORT_SCHEMA_OFF
            )
            return emptyList()
        }
        val schemaInFolderPath = context.schemaInFolderPath
        if (schemaInFolderPath == null) {
            context.logger.e(
                element,
                AUTO_MIGRATION_SCHEMA_IN_FOLDER_NULL
            )
            return emptyList()
        }

        return autoMigrationList.mapNotNull {
            val databaseSchemaInFolderPath = Path.of(
                schemaInFolderPath,
                element.asClassName().canonicalName
            )
            val autoMigration = it.value
            val validatedFromSchemaFile = getValidatedSchemaFile(
                autoMigration.from,
                databaseSchemaInFolderPath
            ) ?: return@mapNotNull null

            fun deserializeSchemaFile(
                fileInputStream: FileInputStream,
                versionNumber: Int
            ): DatabaseBundle? {
                return try {
                    SchemaBundle.deserialize(fileInputStream).database
                } catch (th: Throwable) {
                    invalidAutoMigrationSchema(
                        "$versionNumber.json",
                        databaseSchemaInFolderPath.toString()
                    )
                    null
                }
            }

            val fromSchemaBundle = validatedFromSchemaFile.inputStream().use {
                deserializeSchemaFile(it, autoMigration.from)
            }
            val toSchemaBundle =
                if (autoMigration.to == latestDbSchema.version) {
                    latestDbSchema
                } else {
                    val validatedToSchemaFile = getValidatedSchemaFile(
                        autoMigration.to,
                        databaseSchemaInFolderPath
                    ) ?: return@mapNotNull null
                    validatedToSchemaFile.inputStream().use {
                        deserializeSchemaFile(it, autoMigration.to)
                    }
                }
                if (fromSchemaBundle !is DatabaseBundle || toSchemaBundle !is DatabaseBundle) {
                    context.logger.e(
                        element,
                        autoMigrationSchemasMustBeRoomGenerated(
                            autoMigration.from,
                            autoMigration.to
                        )
                    )
                    return@mapNotNull null
                }

            AutoMigrationProcessor(
                context = context,
                spec = it.getAsType("spec")!!,
                fromSchemaBundle = fromSchemaBundle,
                toSchemaBundle = toSchemaBundle
            ).process()
        }
    }

    private fun getValidatedSchemaFile(version: Int, schemaFolderPath: Path): File? {
        val schemaFile = SchemaFileResolver.RESOLVER.getFile(
            schemaFolderPath.resolve("$version.json")
        )
        if (!schemaFile.exists()) {
            context.logger.e(
                ProcessorErrors.autoMigrationSchemasNotFound(
                    "$version.json",
                    schemaFolderPath.toString()
                ),
                element
            )
            return null
        }

        if (schemaFile.length() <= 0) {
            context.logger.e(
                ProcessorErrors.autoMigrationSchemaIsEmpty(
                    "$version.json",
                    schemaFolderPath.toString()
                ),
                element
            )
            return null
        }
        return schemaFile
    }

    private fun validateForeignKeys(element: XTypeElement, entities: List<Entity>) {
        val byTableName = entities.associateBy { it.tableName }
        entities.forEach { entity ->
            entity.foreignKeys.forEach foreignKeyLoop@{ foreignKey ->
                val parent = byTableName[foreignKey.parentTable]
                if (parent == null) {
                    context.logger.e(
                        element,
                        ProcessorErrors
                            .foreignKeyMissingParentEntityInDatabase(
                                foreignKey.parentTable,
                                entity.element.qualifiedName
                            )
                    )
                    return@foreignKeyLoop
                }
                val parentFields = foreignKey.parentColumns.mapNotNull { columnName ->
                    val parentField = parent.findFieldByColumnName(columnName)
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
                        ProcessorErrors
                            .foreignKeyMissingIndexInParent(
                                parentEntity = parent.element.qualifiedName,
                                childEntity = entity.element.qualifiedName,
                                parentColumns = foreignKey.parentColumns,
                                childColumns = foreignKey.childFields
                                    .map { it.columnName }
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
        daoMethods: List<DaoMethod>,
        entities: List<Entity>
    ) {
        val entityTypeNames = entities.map { it.typeName }.toSet()
        daoMethods.groupBy { it.dao.typeName }
            .forEach {
                if (it.value.size > 1) {
                    val error = ProcessorErrors.duplicateDao(
                        dao = it.key.toString(context.codeLanguage),
                        methodNames = it.value.map { it.element.name }
                    )
                    it.value.forEach { daoMethod ->
                        context.logger.e(
                            daoMethod.element,
                            ProcessorErrors.DAO_METHOD_CONFLICTS_WITH_OTHERS
                        )
                    }
                    // also report the full error for the database
                    context.logger.e(dbElement, error)
                }
            }
        val check = fun(
            element: XElement,
            dao: Dao,
            typeName: XTypeName?
        ) {
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
        daoMethods.forEach { daoMethod ->
            daoMethod.dao.deleteOrUpdateShortcutMethods.forEach { method ->
                method.entities.forEach {
                    check(method.element, daoMethod.dao, it.value.entityTypeName)
                }
            }
            daoMethod.dao.insertOrUpsertShortcutMethods.forEach { method ->
                method.entities.forEach {
                    check(method.element, daoMethod.dao, it.value.entityTypeName)
                }
            }
        }
    }

    private fun validateUniqueTableAndViewNames(
        dbElement: XTypeElement,
        entities: List<Entity>,
        views: List<DatabaseView>
    ) {
        val entitiesInfo = entities.map {
            Triple(
                it.tableName.lowercase(Locale.US),
                it.typeName.toString(context.codeLanguage),
                it.element
            )
        }
        val viewsInfo = views.map {
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
                val error = ProcessorErrors.duplicateTableNames(
                    byName.key,
                    byName.value.map { (_, typeName, _) -> typeName }
                )
                // report it for each of them and the database to make it easier
                // for the developer
                byName.value.forEach { (_, _, element) ->
                    context.logger.e(element, error)
                }
                context.logger.e(dbElement, error)
            }
    }

    private fun validateExternalContentFts(dbElement: XTypeElement, entities: List<Entity>) {
        // Validate FTS external content entities are present in the same database.
        entities.filterIsInstance(FtsEntity::class.java)
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

    private fun processEntities(
        dbAnnotation: XAnnotationBox<androidx.room.Database>,
        element: XTypeElement
    ): List<Entity> {
        val entityList = dbAnnotation.getAsTypeList("entities")
        context.checker.check(
            entityList.isNotEmpty(), element,
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

    private fun processDatabaseViews(
        dbAnnotation: XAnnotationBox<androidx.room.Database>
    ): Map<XTypeElement, DatabaseView> {
        val viewList = dbAnnotation.getAsTypeList("views")
        return viewList.mapNotNull {
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
        }.toMap()
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
                    DatabaseVerificationErrors.cannotVerifyQuery(
                        view.query.resultInfo!!.error!!
                    )
                )
            }
        }
    }

    /**
     * Resolves all the underlying tables for each of the [DatabaseView]. All the tables
     * including those that are indirectly referenced are included.
     *
     * @param views The list of all the [DatabaseView]s in this database. The order in this list is
     * important. A view always comes after all of the tables and views that it depends on.
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
}
