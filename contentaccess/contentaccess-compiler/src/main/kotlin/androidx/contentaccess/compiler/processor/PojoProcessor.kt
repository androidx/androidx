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

import androidx.contentaccess.compiler.vo.PojoFieldVO
import androidx.contentaccess.ext.hasAnnotation
import androidx.contentaccess.ContentColumn
import androidx.contentaccess.compiler.vo.PojoVO
import androidx.contentaccess.ext.getAllFieldsIncludingPrivateSupers
import asTypeElement
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeMirror

class PojoProcessor(val typeMirror: TypeMirror, val processingEnv: ProcessingEnvironment) {
    fun process(): PojoVO {
        val returnList = mutableListOf<PojoFieldVO>()
        // TODO(obenabde): this should either be constructor or public field specific
        // eventually replace getAllFieldsIncludingPrivateSupers with a more appropriate function.
        val variables = typeMirror.asTypeElement().getAllFieldsIncludingPrivateSupers(processingEnv)
        for (v in variables) {
            val type = v.asType()
            val name = v.simpleName.toString()
            val columnName = if (v.hasAnnotation(ContentColumn::class)) {
                v.getAnnotation(ContentColumn::class.java).columnName
            } else {
                name
            }
            returnList.add(PojoFieldVO(name, columnName, type, fieldIsNullable(v)))
        }
        return PojoVO(returnList)
    }
}