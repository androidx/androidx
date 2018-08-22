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

import androidx.room.Fts3Entity
import androidx.room.ext.getAsIntList
import androidx.room.ext.getAsString
import androidx.room.ext.getAsStringList
import androidx.room.ext.hasAnnotation
import androidx.room.parser.FtsOrder
import androidx.room.parser.FtsVersion
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.Tokenizer
import androidx.room.processor.EntityProcessor.Companion.extractTableName
import androidx.room.processor.cache.Cache
import androidx.room.vo.Field
import androidx.room.vo.FtsEntity
import androidx.room.vo.FtsOptions
import androidx.room.vo.LanguageId
import androidx.room.vo.PrimaryKey
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement

class FtsTableEntityProcessor internal constructor(
    baseContext: Context,
    val element: TypeElement,
    private val referenceStack: LinkedHashSet<Name> = LinkedHashSet()
) : EntityProcessor {

    val context = baseContext.fork(element)

    override fun process(): androidx.room.vo.FtsEntity {
        return context.cache.entities.get(Cache.EntityKey(element)) {
            doProcess()
        } as androidx.room.vo.FtsEntity
    }

    private fun doProcess(): FtsEntity {
        val pojo = PojoProcessor.createFor(
                context = context,
                element = element,
                bindingScope = FieldProcessor.BindingScope.TWO_WAY,
                parent = null,
                referenceStack = referenceStack).process()

        context.checker.check(pojo.relations.isEmpty(), element, ProcessorErrors.RELATION_IN_ENTITY)

        val (ftsVersion, annotation) = if (element.hasAnnotation(Fts3Entity::class)) {
            FtsVersion.FTS3 to MoreElements.getAnnotationMirror(element,
                    androidx.room.Fts3Entity::class.java).orNull()
        } else {
            FtsVersion.FTS4 to MoreElements.getAnnotationMirror(element,
                    androidx.room.Fts4Entity::class.java).orNull()
        }
        val tableName = if (annotation != null) {
            extractTableName(element, annotation)
        } else {
            element.simpleName.toString()
        }
        val ftsOptions = getAnnotationFTSOptions(ftsVersion, annotation)

        // The %_content table contains the unadulterated data inserted by the user into the FTS
        // virtual table. See: https://www.sqlite.org/fts3.html#shadow_tables
        val shadowTableName = "${tableName}_content"

        val primaryKey = findAndValidatePrimaryKey(pojo.fields)
        val languageId = findAndValidateLanguageId(pojo.fields, ftsOptions.languageIdColumnName)

        val missingNotIndexed = ftsOptions.notIndexedColumns - pojo.fields.map { it.columnName }
        context.checker.check(missingNotIndexed.isEmpty(), element,
                ProcessorErrors.missingNotIndexedField(missingNotIndexed))

        pojo.fields.filter { it.element.hasAnnotation(androidx.room.ForeignKey::class) }.forEach {
            context.logger.e(ProcessorErrors.INVALID_FOREIGN_KEY_IN_FTS_ENTITY, it.element)
        }

        context.checker.check(ftsOptions.prefixSizes.all { it > 0 },
                element, ProcessorErrors.INVALID_FTS_ENTITY_PREFIX_SIZES)

        return FtsEntity(
                element = element,
                tableName = tableName,
                type = pojo.type,
                fields = pojo.fields,
                embeddedFields = pojo.embeddedFields,
                primaryKey = primaryKey,
                constructor = pojo.constructor,
                ftsVersion = ftsVersion,
                ftsOptions = ftsOptions,
                shadowTableName = shadowTableName)
    }

    private fun getAnnotationFTSOptions(
        version: FtsVersion,
        annotation: AnnotationMirror?
    ): FtsOptions {
        if (annotation == null) {
            return FtsOptions(
                    tokenizer = Tokenizer.SIMPLE,
                    tokenizerArgs = emptyList(),
                    languageIdColumnName = "",
                    matchInfo = FtsVersion.FTS4,
                    notIndexedColumns = emptyList(),
                    prefixSizes = emptyList(),
                    preferredOrder = FtsOrder.ASC)
        }

        val tokenizer = Tokenizer.fromAnnotation(annotation, "tokenizer")
        val tokenizerArgs = AnnotationMirrors.getAnnotationValue(annotation, "tokenizerArgs")
                .getAsStringList()

        val languageIdColumnName: String
        val matchInfo: FtsVersion
        val notIndexedColumns: List<String>
        val prefixSizes: List<Int>
        val preferredOrder: FtsOrder
        if (version == FtsVersion.FTS4) {
            languageIdColumnName = AnnotationMirrors.getAnnotationValue(annotation, "languageId")
                    .getAsString() ?: ""
            matchInfo = FtsVersion.fromAnnotation(annotation, "matchInfo")
            notIndexedColumns = AnnotationMirrors.getAnnotationValue(annotation, "notIndexed")
                    .getAsStringList()
            prefixSizes = AnnotationMirrors.getAnnotationValue(annotation, "prefix")
                    .getAsIntList()
            preferredOrder = FtsOrder.fromAnnotation(annotation, "order")
        } else {
            languageIdColumnName = ""
            matchInfo = FtsVersion.FTS4
            notIndexedColumns = emptyList()
            prefixSizes = emptyList()
            preferredOrder = FtsOrder.ASC
        }

        return FtsOptions(
                tokenizer = tokenizer,
                tokenizerArgs = tokenizerArgs,
                languageIdColumnName = languageIdColumnName,
                matchInfo = matchInfo,
                notIndexedColumns = notIndexedColumns,
                prefixSizes = prefixSizes,
                preferredOrder = preferredOrder)
    }

    private fun findAndValidatePrimaryKey(fields: List<Field>): PrimaryKey {
        val primaryKeys = fields.mapNotNull { field ->
            if (field.element.hasAnnotation(androidx.room.PrimaryKey::class)) {
                PrimaryKey(
                        declaredIn = field.element.enclosingElement,
                        fields = listOf(field),
                        autoGenerateId = true)
            } else {
                null
            }
        }
        if (primaryKeys.isEmpty()) {
            fields.firstOrNull { it.columnName == "rowid" }?.let {
                context.checker.check(it.element.hasAnnotation(androidx.room.PrimaryKey::class),
                        it.element, ProcessorErrors.MISSING_PRIMARY_KEYS_ANNOTATION_IN_ROW_ID)
            }
            return PrimaryKey.MISSING
        }
        context.checker.check(primaryKeys.size == 1, element,
                ProcessorErrors.TOO_MANY_PRIMARY_KEYS_IN_FTS_ENTITY)
        val primaryKey = primaryKeys.first()
        context.checker.check(primaryKey.columnNames.first() == "rowid",
                primaryKey.declaredIn ?: element,
                ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_NAME)
        context.checker.check(primaryKey.fields.first().affinity == SQLTypeAffinity.INTEGER,
                primaryKey.declaredIn ?: element,
                ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_AFFINITY)
        return primaryKey
    }

    private fun findAndValidateLanguageId(
        fields: List<Field>,
        languageIdColumnName: String
    ): LanguageId {
        if (languageIdColumnName.isEmpty()) {
            return LanguageId.MISSING
        }

        val languageIdField = fields.firstOrNull { it.columnName == languageIdColumnName }
        if (languageIdField == null) {
            context.logger.e(element, ProcessorErrors.missingLanguageIdField(languageIdColumnName))
            return LanguageId.MISSING
        }

        context.checker.check(languageIdField.affinity == SQLTypeAffinity.INTEGER,
                languageIdField.element, ProcessorErrors.INVALID_FTS_ENTITY_LANGUAGE_ID_AFFINITY)
        return LanguageId(languageIdField.element, languageIdField)
    }
}