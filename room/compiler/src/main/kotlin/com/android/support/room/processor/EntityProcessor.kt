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

package com.android.support.room.processor

import com.android.support.room.vo.Entity
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import javax.lang.model.element.TypeElement

class EntityProcessor(baseContext: Context, val element: TypeElement) {
    val context = baseContext.fork(element)

    fun process(): Entity {
        context.checker.hasAnnotation(element, com.android.support.room.Entity::class,
                ProcessorErrors.ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY)
        val pojo = PojoProcessor(
                baseContext = context,
                element = element,
                bindingScope = FieldProcessor.BindingScope.TWO_WAY,
                parent = null).process()
        val annotation = MoreElements.getAnnotationMirror(element,
                com.android.support.room.Entity::class.java).orNull()
        val tableName : String
        if (annotation != null) {
            val annotationValue = AnnotationMirrors
                    .getAnnotationValue(annotation, "tableName").value.toString()
            if (annotationValue == "") {
                tableName = element.simpleName.toString()
            } else {
                tableName = annotationValue
            }
        } else {
            tableName = element.simpleName.toString()
        }
        context.checker.notBlank(tableName, element,
                ProcessorErrors.ENTITY_TABLE_NAME_CANNOT_BE_EMPTY)
        val entity = Entity(element = element,
                tableName = tableName,
                type = pojo.type,
                fields = pojo.fields,
                decomposedFields = pojo.decomposedFields)
        context.checker.check(entity.primaryKeys.isNotEmpty(), element,
                ProcessorErrors.MISSING_PRIMARY_KEY)
        return entity
    }
}
