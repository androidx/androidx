/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.RoomTypeNames
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.SchemaBundle.deserialize
import androidx.room.processor.ProcessorErrors.AUTOMIGRATION_CALLBACK_MUST_BE_INTERFACE
import androidx.room.processor.ProcessorErrors.autoMigrationElementMustExtendCallback
import androidx.room.processor.ProcessorErrors.autoMigrationToVersionMustBeGreaterThanFrom
import androidx.room.util.DiffException
import androidx.room.util.SchemaDiffer
import androidx.room.vo.AutoMigrationResult
import java.io.File

// TODO: (b/183435544) Support downgrades in AutoMigrations.
class AutoMigrationProcessor(
    val element: XTypeElement,
    val context: Context,
    val from: Int,
    val to: Int,
    val callback: XType,
    val latestDbSchema: DatabaseBundle
) {
    /**
     * Retrieves two schemas of the same database provided in the @AutoMigration annotation,
     * detects the schema changes that occurred between the two versions.
     *
     * @return the AutoMigrationResult containing the schema changes detected
     */
    fun process(): AutoMigrationResult? {
        val callbackElement = callback.typeElement
        if (!callback.isTypeOf(Any::class)) {
            if (callbackElement == null) {
                context.logger.e(element, AUTOMIGRATION_CALLBACK_MUST_BE_INTERFACE)
                return null
            }

            if (!callbackElement.isInterface()) {
                context.logger.e(
                    callbackElement,
                    AUTOMIGRATION_CALLBACK_MUST_BE_INTERFACE
                )
                return null
            }

            val extendsMigrationCallback =
                context.processingEnv.requireType(RoomTypeNames.AUTO_MIGRATION_CALLBACK)
                    .isAssignableFrom(callback)
            if (!extendsMigrationCallback) {
                context.logger.e(
                    callbackElement,
                    autoMigrationElementMustExtendCallback(callbackElement.className.simpleName())
                )
                return null
            }
        }

        if (to <= from) {
            context.logger.e(
                autoMigrationToVersionMustBeGreaterThanFrom(to, from)
            )
            return null
        }

        val validatedFromSchemaFile = getValidatedSchemaFile(from) ?: return null
        val fromSchemaBundle = validatedFromSchemaFile.inputStream().use {
            deserialize(it).database
        }

        val validatedToSchemaFile = getValidatedSchemaFile(to) ?: return null
        val toSchemaBundle = if (to == latestDbSchema.version) {
            latestDbSchema
        } else {
            validatedToSchemaFile.inputStream().use {
                deserialize(it).database
            }
        }

        val callbackClassName = callbackElement?.className?.simpleName()
        val deleteColumnEntries = callbackElement?.let { element ->
            element.getAnnotations(DeleteColumn::class).map {
                AutoMigrationResult.DeletedColumn(
                    tableName = it.value.tableName,
                    columnName = it.value.deletedColumnName
                )
            }
        } ?: emptyList()

        val deleteTableEntries = callbackElement?.let { element ->
            element.getAnnotations(DeleteTable::class).map {
                AutoMigrationResult.DeletedTable(
                    deletedTableName = it.value.deletedTableName
                )
            }
        } ?: emptyList()

        val renameTableEntries = callbackElement?.let { element ->
            element.getAnnotations(RenameTable::class).map {
                AutoMigrationResult.RenamedTable(
                    originalTableName = it.value.originalTableName,
                    newTableName = it.value.newTableName
                )
            }
        } ?: emptyList()

        val renameColumnEntries = callbackElement?.let { element ->
            element.getAnnotations(RenameColumn::class).map {
                AutoMigrationResult.RenamedColumn(
                    tableName = it.value.tableName,
                    originalColumnName = it.value.originalColumnName,
                    newColumnName = it.value.newColumnName
                )
            }
        } ?: emptyList()

        val schemaDiff = try {
            SchemaDiffer(
                fromSchemaBundle = fromSchemaBundle,
                toSchemaBundle = toSchemaBundle,
                className = callbackClassName,
                deleteColumnEntries = deleteColumnEntries,
                deleteTableEntries = deleteTableEntries,
                renameTableEntries = renameTableEntries,
                renameColumnEntries = renameColumnEntries
            ).diffSchemas()
        } catch (ex: DiffException) {
            context.logger.e(ex.errorMessage)
            return null
        }

        return AutoMigrationResult(
            element = element,
            from = fromSchemaBundle.version,
            to = toSchemaBundle.version,
            schemaDiff = schemaDiff
        )
    }

    // TODO: (b/180389433) Verify automigration schemas before calling the AutoMigrationProcessor
    private fun getValidatedSchemaFile(version: Int): File? {
        val schemaFile = File(
            context.schemaOutFolder,
            "${element.className.canonicalName()}/$version.json"
        )
        if (!schemaFile.exists()) {
            context.logger.e(
                ProcessorErrors.autoMigrationSchemasNotFound(
                    context.schemaOutFolder.toString(),
                    "${element.className.canonicalName()}/$version.json"
                ),
                element
            )
            return null
        }
        return schemaFile
    }
}
