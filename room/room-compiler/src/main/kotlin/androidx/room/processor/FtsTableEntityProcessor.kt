/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.room.Fts3
import androidx.room.Fts4
import androidx.room.FtsOptions.MatchInfo
import androidx.room.FtsOptions.Order
import androidx.room.FtsOptions.TOKENIZER_SIMPLE
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.parser.FtsVersion
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.EntityProcessor.Companion.extractForeignKeys
import androidx.room.processor.EntityProcessor.Companion.extractIndices
import androidx.room.processor.EntityProcessor.Companion.extractTableName
import androidx.room.processor.cache.Cache
import androidx.room.vo.Entity
import androidx.room.vo.FtsEntity
import androidx.room.vo.FtsOptions
import androidx.room.vo.LanguageId
import androidx.room.vo.PrimaryKey
import androidx.room.vo.Properties
import androidx.room.vo.Property
import androidx.room.vo.columnNames

class FtsTableEntityProcessor
internal constructor(
    baseContext: Context,
    val element: XTypeElement,
    private val referenceStack: LinkedHashSet<String> = LinkedHashSet()
) : EntityProcessor {

    val context = baseContext.fork(element)

    override fun process(): FtsEntity {
        return context.cache.entities.get(Cache.EntityKey(element)) { doProcess() } as FtsEntity
    }

    private fun doProcess(): FtsEntity {
        if (!element.validate()) {
            context.reportMissingTypeReference(element.qualifiedName)
            return FtsEntity(
                element = element,
                tableName = element.name,
                type = element.type,
                properties = emptyList(),
                embeddedProperties = emptyList(),
                primaryKey = PrimaryKey.MISSING,
                constructor = null,
                shadowTableName = null,
                ftsVersion = FtsVersion.FTS3,
                ftsOptions =
                    FtsOptions(
                        tokenizer = TOKENIZER_SIMPLE,
                        tokenizerArgs = emptyList(),
                        contentEntity = null,
                        languageIdColumnName = "",
                        matchInfo = MatchInfo.FTS3,
                        notIndexedColumns = emptyList(),
                        prefixSizes = emptyList(),
                        preferredOrder = Order.ASC
                    )
            )
        }
        context.checker.hasAnnotation(
            element,
            androidx.room.Entity::class,
            ProcessorErrors.ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY
        )
        val entityAnnotation = element.getAnnotation(androidx.room.Entity::class)
        val tableName: String
        if (entityAnnotation != null) {
            tableName = extractTableName(element, entityAnnotation)
            context.checker.check(
                extractIndices(entityAnnotation, tableName).isEmpty(),
                element,
                ProcessorErrors.INDICES_IN_FTS_ENTITY
            )
            context.checker.check(
                extractForeignKeys(entityAnnotation).isEmpty(),
                element,
                ProcessorErrors.FOREIGN_KEYS_IN_FTS_ENTITY
            )
        } else {
            tableName = element.name
        }

        val pojo =
            DataClassProcessor.createFor(
                    context = context,
                    element = element,
                    bindingScope = PropertyProcessor.BindingScope.TWO_WAY,
                    parent = null,
                    referenceStack = referenceStack
                )
                .process()

        context.checker.check(pojo.relations.isEmpty(), element, ProcessorErrors.RELATION_IN_ENTITY)

        val (ftsVersion, ftsOptions) =
            if (element.hasAnnotation(androidx.room.Fts3::class)) {
                FtsVersion.FTS3 to getFts3Options(element.requireAnnotation(Fts3::class))
            } else {
                FtsVersion.FTS4 to getFts4Options(element.requireAnnotation(Fts4::class))
            }

        val shadowTableName =
            if (ftsOptions.contentEntity != null) {
                // In 'external content' mode the FTS table content is in another table.
                // See: https://www.sqlite.org/fts3.html#_external_content_fts4_tables_
                ftsOptions.contentEntity.tableName
            } else {
                // The %_content table contains the unadulterated data inserted by the user into the
                // FTS
                // virtual table. See: https://www.sqlite.org/fts3.html#shadow_tables
                "${tableName}_content"
            }

        val primaryKey = findAndValidatePrimaryKey(entityAnnotation, pojo.properties)
        findAndValidateLanguageId(pojo.properties, ftsOptions.languageIdColumnName)

        val missingNotIndexed = ftsOptions.notIndexedColumns - pojo.columnNames
        context.checker.check(
            missingNotIndexed.isEmpty(),
            element,
            ProcessorErrors.missingNotIndexedProperty(missingNotIndexed)
        )

        context.checker.check(
            ftsOptions.prefixSizes.all { it > 0 },
            element,
            ProcessorErrors.INVALID_FTS_ENTITY_PREFIX_SIZES
        )

        val entity =
            FtsEntity(
                element = element,
                tableName = tableName,
                type = pojo.type,
                properties = pojo.properties,
                embeddedProperties = pojo.embeddedProperties,
                primaryKey = primaryKey,
                constructor = pojo.constructor,
                ftsVersion = ftsVersion,
                ftsOptions = ftsOptions,
                shadowTableName = shadowTableName
            )

        validateExternalContentEntity(entity)

        return entity
    }

    private fun getFts3Options(annotation: XAnnotation) =
        FtsOptions(
            tokenizer = annotation["tokenizer"]?.asString() ?: TOKENIZER_SIMPLE,
            tokenizerArgs = annotation["tokenizerArgs"]?.asStringList() ?: emptyList(),
            contentEntity = null,
            languageIdColumnName = "",
            matchInfo = MatchInfo.FTS4,
            notIndexedColumns = emptyList(),
            prefixSizes = emptyList(),
            preferredOrder = Order.ASC
        )

    private fun getFts4Options(annotation: XAnnotation): FtsOptions {
        val contentEntity: Entity? = getContentEntity(annotation["contentEntity"]?.asType())
        return FtsOptions(
            tokenizer = annotation["tokenizer"]?.asString() ?: TOKENIZER_SIMPLE,
            tokenizerArgs = annotation["tokenizerArgs"]?.asStringList() ?: emptyList(),
            contentEntity = contentEntity,
            languageIdColumnName = annotation["languageId"]?.asString() ?: "",
            matchInfo =
                annotation["matchInfo"]?.asEnum()?.let { MatchInfo.valueOf(it.name) }
                    ?: MatchInfo.FTS4,
            notIndexedColumns = annotation["notIndexed"]?.asStringList() ?: emptyList(),
            prefixSizes = annotation["prefix"]?.asIntList() ?: emptyList(),
            preferredOrder =
                annotation["order"]?.asEnum()?.let { Order.valueOf(it.name) } ?: Order.ASC,
        )
    }

    private fun getContentEntity(entityType: XType?): Entity? {
        if (entityType == null) {
            context.logger.e(element, ProcessorErrors.FTS_EXTERNAL_CONTENT_CANNOT_FIND_ENTITY)
            return null
        }

        val defaultType = context.processingEnv.requireType(Object::class)
        if (entityType.isSameType(defaultType)) {
            return null
        }
        val contentEntityElement = entityType.typeElement
        if (contentEntityElement == null) {
            context.logger.e(element, ProcessorErrors.FTS_EXTERNAL_CONTENT_CANNOT_FIND_ENTITY)
            return null
        }
        if (!contentEntityElement.hasAnnotation(androidx.room.Entity::class)) {
            context.logger.e(
                contentEntityElement,
                ProcessorErrors.externalContentNotAnEntity(
                    contentEntityElement.asClassName().canonicalName
                )
            )
            return null
        }
        return EntityProcessor(context, contentEntityElement, referenceStack).process()
    }

    private fun findAndValidatePrimaryKey(
        entityAnnotation: XAnnotation?,
        properties: List<Property>
    ): PrimaryKey {
        val keysFromEntityAnnotation =
            entityAnnotation?.get("primaryKeys")?.asStringList()?.mapNotNull { pkColumnName ->
                val property = properties.firstOrNull { it.columnName == pkColumnName }
                context.checker.check(
                    property != null,
                    element,
                    ProcessorErrors.primaryKeyColumnDoesNotExist(
                        pkColumnName,
                        properties.map { it.columnName }
                    )
                )
                property?.let { pkProperty ->
                    PrimaryKey(
                        declaredIn = pkProperty.element.enclosingElement,
                        properties = Properties(pkProperty),
                        autoGenerateId = true
                    )
                }
            } ?: emptyList()

        val keysFromPrimaryKeyAnnotations =
            properties.mapNotNull { property ->
                if (property.element.hasAnnotation(androidx.room.PrimaryKey::class)) {
                    PrimaryKey(
                        declaredIn = property.element.enclosingElement,
                        properties = Properties(property),
                        autoGenerateId = true
                    )
                } else {
                    null
                }
            }
        val primaryKeys = keysFromEntityAnnotation + keysFromPrimaryKeyAnnotations
        if (primaryKeys.isEmpty()) {
            properties
                .firstOrNull { it.columnName == "rowid" }
                ?.let {
                    context.checker.check(
                        it.element.hasAnnotation(androidx.room.PrimaryKey::class),
                        it.element,
                        ProcessorErrors.MISSING_PRIMARY_KEYS_ANNOTATION_IN_ROW_ID
                    )
                }
            return PrimaryKey.MISSING
        }
        context.checker.check(
            primaryKeys.size == 1,
            element,
            ProcessorErrors.TOO_MANY_PRIMARY_KEYS_IN_FTS_ENTITY
        )
        val primaryKey = primaryKeys.first()
        context.checker.check(
            primaryKey.columnNames.first() == "rowid",
            primaryKey.declaredIn ?: element,
            ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_NAME
        )
        context.checker.check(
            primaryKey.properties.first().affinity == SQLTypeAffinity.INTEGER,
            primaryKey.declaredIn ?: element,
            ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_AFFINITY
        )
        return primaryKey
    }

    private fun validateExternalContentEntity(ftsEntity: FtsEntity) {
        val contentEntity = ftsEntity.ftsOptions.contentEntity
        if (contentEntity == null) {
            return
        }

        // Verify external content columns are a superset of those defined in the FtsEntity
        ftsEntity.nonHiddenProperties
            .filterNot {
                contentEntity.properties.any { contentProperty ->
                    contentProperty.columnName == it.columnName
                }
            }
            .forEach {
                context.logger.e(
                    it.element,
                    ProcessorErrors.missingFtsContentProperty(
                        element.qualifiedName,
                        it.columnName,
                        contentEntity.element.qualifiedName
                    )
                )
            }
    }

    private fun findAndValidateLanguageId(
        properties: List<Property>,
        languageIdColumnName: String
    ): LanguageId {
        if (languageIdColumnName.isEmpty()) {
            return LanguageId.MISSING
        }

        val languageIdProperty = properties.firstOrNull { it.columnName == languageIdColumnName }
        if (languageIdProperty == null) {
            context.logger.e(
                element,
                ProcessorErrors.missingLanguageIdProperty(languageIdColumnName)
            )
            return LanguageId.MISSING
        }

        context.checker.check(
            languageIdProperty.affinity == SQLTypeAffinity.INTEGER,
            languageIdProperty.element,
            ProcessorErrors.INVALID_FTS_ENTITY_LANGUAGE_ID_AFFINITY
        )
        return LanguageId(languageIdProperty.element, languageIdProperty)
    }
}
