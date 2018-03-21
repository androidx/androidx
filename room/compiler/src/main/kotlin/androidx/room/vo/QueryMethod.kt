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

package androidx.room.vo

import androidx.room.ext.typeName
import androidx.room.parser.ParsedQuery
import androidx.room.solver.query.result.QueryResultBinder
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

/**
 * A class that holds information about a QueryMethod.
 * It is self sufficient and must have all generics etc resolved once created.
 */
data class QueryMethod(val element: ExecutableElement, val query: ParsedQuery, val name: String,
                       val returnType: TypeMirror, val parameters: List<QueryParameter>,
                       val inTransaction: Boolean,
                       val queryResultBinder: QueryResultBinder) {
    val sectionToParamMapping by lazy {
        query.bindSections.map {
            if (it.text.trim() == "?") {
                Pair(it, parameters.firstOrNull())
            } else if (it.text.startsWith(":")) {
                val subName = it.text.substring(1)
                Pair(it, parameters.firstOrNull {
                    it.sqlName == subName
                })
            } else {
                Pair(it, null)
            }
        }
    }

    val returnsValue by lazy {
        returnType.typeName() != TypeName.VOID
    }
}
