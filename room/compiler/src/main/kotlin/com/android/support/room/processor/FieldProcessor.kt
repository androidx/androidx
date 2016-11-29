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

import com.android.support.room.ColumnName
import com.android.support.room.PrimaryKey
import com.android.support.room.preconditions.Checks
import com.android.support.room.vo.Field
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element
import javax.lang.model.type.DeclaredType

class FieldProcessor(val context: Context) {
    fun parse(containing : DeclaredType, element : Element) : Field {
        val member = context.processingEnv.typeUtils.asMemberOf(containing, element)
        val type = TypeName.get(member)
        val columnNameAnnotation = MoreElements.getAnnotationMirror(element,
                ColumnName::class.java)
        val name = element.simpleName.toString()
        val columnName : String
        if (columnNameAnnotation.isPresent) {
            columnName = AnnotationMirrors
                    .getAnnotationValue(columnNameAnnotation.get(), "value").value.toString()
        } else {
            columnName = name
        }
        Checks.notBlank(columnName, element, ProcessorErrors.COLUMN_NAME_CANNOT_BE_EMPTY)
        Checks.notUnbound(type, element,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS)
        return Field(name = name,
                type = type,
                primaryKey = MoreElements.isAnnotationPresent(element, PrimaryKey::class.java),
                element = element,
                columnName = columnName)
    }
}
