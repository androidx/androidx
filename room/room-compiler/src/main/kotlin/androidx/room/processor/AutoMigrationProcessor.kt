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
import androidx.room.ProvidedAutoMigrationSpec
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.RoomTypeNames
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.processor.ProcessorErrors.AUTOMIGRATION_SPEC_MUST_BE_CLASS
import androidx.room.processor.ProcessorErrors.INNER_CLASS_AUTOMIGRATION_SPEC_MUST_BE_STATIC
import androidx.room.processor.ProcessorErrors.autoMigrationElementMustImplementSpec
import androidx.room.processor.ProcessorErrors.autoMigrationToVersionMustBeGreaterThanFrom
import androidx.room.util.DiffException
import androidx.room.util.SchemaDiffer
import androidx.room.vo.AutoMigration

// TODO: (b/183435544) Support downgrades in AutoMigrations.
class AutoMigrationProcessor(
    val element: XTypeElement,
    val context: Context,
    val spec: XType,
    val fromSchemaBundle: DatabaseBundle,
    val toSchemaBundle: DatabaseBundle
) {
    /**
     * Retrieves two schemas of the same database provided in the @AutoMigration annotation,
     * detects the schema changes that occurred between the two versions.
     *
     * @return the AutoMigrationResult containing the schema changes detected
     */
    fun process(): AutoMigration? {
        val isSpecProvided = spec.typeElement?.hasAnnotation(
            ProvidedAutoMigrationSpec::class
        ) ?: false
        val specElement = if (!spec.isTypeOf(Any::class)) {
            val typeElement = spec.typeElement

            if (typeElement == null || typeElement.isInterface() || typeElement.isAbstract()) {
                context.logger.e(element, AUTOMIGRATION_SPEC_MUST_BE_CLASS)
                return null
            }

            if (!isSpecProvided) {
                val constructors = element.getConstructors()
                context.checker.check(
                    constructors.isEmpty() || constructors.any { it.parameters.isEmpty() },
                    element,
                    ProcessorErrors.AUTOMIGRATION_SPEC_MISSING_NOARG_CONSTRUCTOR
                )
            }

            context.checker.check(
                typeElement.enclosingTypeElement == null || typeElement.isStatic(),
                typeElement,
                INNER_CLASS_AUTOMIGRATION_SPEC_MUST_BE_STATIC
            )

            val implementsMigrationSpec =
                context.processingEnv.requireType(RoomTypeNames.AUTO_MIGRATION_SPEC)
                    .isAssignableFrom(spec)
            if (!implementsMigrationSpec) {
                context.logger.e(
                    typeElement,
                    autoMigrationElementMustImplementSpec(typeElement.className.simpleName())
                )
                return null
            }
            typeElement
        } else {
            null
        }

        if (toSchemaBundle.version <= fromSchemaBundle.version) {
            context.logger.e(
                autoMigrationToVersionMustBeGreaterThanFrom(
                    toSchemaBundle.version,
                    fromSchemaBundle.version
                )
            )
            return null
        }

        val specClassName = specElement?.className?.simpleName()
        val deleteColumnEntries = specElement?.let { element ->
            element.getAnnotations(DeleteColumn::class).map {
                AutoMigration.DeletedColumn(
                    tableName = it.value.tableName,
                    columnName = it.value.columnName
                )
            }
        } ?: emptyList()

        val deleteTableEntries = specElement?.let { element ->
            element.getAnnotations(DeleteTable::class).map {
                AutoMigration.DeletedTable(
                    deletedTableName = it.value.tableName
                )
            }
        } ?: emptyList()

        val renameTableEntries = specElement?.let { element ->
            element.getAnnotations(RenameTable::class).map {
                AutoMigration.RenamedTable(
                    originalTableName = it.value.fromTableName,
                    newTableName = it.value.toTableName
                )
            }
        } ?: emptyList()

        val renameColumnEntries = specElement?.let { element ->
            element.getAnnotations(RenameColumn::class).map {
                AutoMigration.RenamedColumn(
                    tableName = it.value.tableName,
                    originalColumnName = it.value.fromColumnName,
                    newColumnName = it.value.toColumnName
                )
            }
        } ?: emptyList()

        val schemaDiff = try {
            SchemaDiffer(
                fromSchemaBundle = fromSchemaBundle,
                toSchemaBundle = toSchemaBundle,
                className = specClassName,
                deleteColumnEntries = deleteColumnEntries,
                deleteTableEntries = deleteTableEntries,
                renameTableEntries = renameTableEntries,
                renameColumnEntries = renameColumnEntries
            ).diffSchemas()
        } catch (ex: DiffException) {
            context.logger.e(ex.errorMessage)
            return null
        }

        return AutoMigration(
            element = element,
            from = fromSchemaBundle.version,
            to = toSchemaBundle.version,
            schemaDiff = schemaDiff,
            specElement = specElement,
            isSpecProvided = isSpecProvided,
        )
    }
}
