/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.contentaccess.compiler.processor

import androidx.contentaccess.ContentColumn
import androidx.contentaccess.ContentEntity
import androidx.contentaccess.ContentPrimaryKey
import androidx.contentaccess.compiler.vo.ContentColumnVO
import androidx.contentaccess.compiler.vo.ContentEntityVO
import androidx.contentaccess.ext.getAllFieldsIncludingPrivateSupers
import androidx.contentaccess.ext.hasAnnotation
import asTypeElement
import com.google.auto.common.MoreTypes
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeMirror

class ContentEntityProcessor(
    val contentAccessObjectAnnotation: TypeMirror,
    val processingEnv: ProcessingEnvironment
) {

    fun processEntity(): ContentEntityVO {
        val entity = contentAccessObjectAnnotation.asTypeElement()
        // TODO(obenabde): ensure the above exists.
        // TODO(obenabde): change this to only consider the constructor params
        val columns = entity.getAllFieldsIncludingPrivateSupers(processingEnv)
        // TODO(obenabde): Ensure a primary key column and all other columns have @ContentColumn
        val contentColumns = HashMap<String, ContentColumnVO>()
        val contentPrimaryKey = ArrayList<ContentColumnVO>()
        columns.forEach { column ->
            // TODO(obenabde): handle all the checks that need to happen here (e.g supported
            //  column types)
            if (column.hasAnnotation(ContentColumn::class)) {
                contentColumns.put(column.simpleName.toString(), ContentColumnVO(
                    column.simpleName.toString(), column.asType(),
                    column.getAnnotation(ContentColumn::class.java).columnName))
            } else if (column.hasAnnotation(ContentPrimaryKey::class)) {
                // TODO(obenabde): error if a primary key already exists.
                val vo = ContentColumnVO(column.simpleName.toString(), column.asType(), column
                    .getAnnotation(ContentPrimaryKey::class.java).columnName)
                contentColumns.put(column.simpleName.toString(), vo)
                contentPrimaryKey.add(vo)
            } else {
                // TODO(obenabde): error on ambiguous field?
            }
        }
        // TODO(obenabde): check we got some columns and a primary key
        return ContentEntityVO(entity.getAnnotation(ContentEntity::class.java).uri, MoreTypes
            .asDeclared(entity.asType()), contentColumns, contentPrimaryKey.first())
    }
}