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

import androidx.room.AutoMigration
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.RoomTypeNames
import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.SchemaBundle.deserialize
import androidx.room.util.DiffException
import androidx.room.util.SchemaDiffer
import androidx.room.vo.AutoMigrationResult
import java.io.File

class AutoMigrationProcessor(
    val context: Context,
    val element: XTypeElement,
    val latestDbSchema: DatabaseBundle
) {
    /**
     * Retrieves two schemas of the same database provided in the @AutoMigration annotation,
     * detects the schema changes that occurred between the two versions.
     *
     * @return the AutoMigrationResult containing the schema changes detected
     */
    fun process(): AutoMigrationResult? {
        if (!element.isInterface()) {
            context.logger.e(
                ProcessorErrors.AUTOMIGRATION_ANNOTATED_TYPE_ELEMENT_MUST_BE_INTERFACE,
                element
            )
            return null
        }

        if (!context.processingEnv
            .requireType(RoomTypeNames.AUTO_MIGRATION_CALLBACK)
            .isAssignableFrom(element.type)
        ) {
            context.logger.e(
                ProcessorErrors.AUTOMIGRATION_ELEMENT_MUST_IMPLEMENT_AUTOMIGRATION_CALLBACK,
                element
            )
            return null
        }

        val annotationBox = element.getAnnotation(AutoMigration::class)
        if (annotationBox == null) {
            context.logger.e(
                element,
                ProcessorErrors.AUTOMIGRATION_ANNOTATION_MISSING
            )
            return null
        }

        val from = annotationBox.value.from
        val to = annotationBox.value.to

        if (to <= from) {
            context.logger.e(
                ProcessorErrors.autoMigrationToVersionMustBeGreaterThanFrom(to, from),
                element
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

        val schemaDiff = try {
            SchemaDiffer(
                fromSchemaBundle = fromSchemaBundle,
                toSchemaBundle = toSchemaBundle
            ).diffSchemas()
        } catch (ex: DiffException) {
            context.logger.e(ex.errorMessage)
            return null
        }

        return AutoMigrationResult(
            element = element,
            from = fromSchemaBundle.version,
            to = toSchemaBundle.version,
            addedColumns = schemaDiff.addedColumn,
            addedTables = schemaDiff.addedTable
        )
    }

    // TODO: File bug for not supporting downgrades.
    // TODO: (b/180389433) If the files don't exist the getSchemaFile() method should return
    //  null and before calling process
    private fun getValidatedSchemaFile(version: Int): File? {
        val schemaFile = File(
            context.schemaOutFolder,
            "${element.className.enclosingClassName()}/$version.json"
        )
        if (!schemaFile.exists()) {
            context.logger.e(
                ProcessorErrors.autoMigrationSchemasNotFound(
                    context.schemaOutFolder.toString(),
                    "${element.className.enclosingClassName()}/$version.json"
                ),
                element
            )
            return null
        }
        return schemaFile
    }
}
