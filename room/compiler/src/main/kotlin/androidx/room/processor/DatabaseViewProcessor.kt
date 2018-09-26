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

import androidx.room.parser.ParsedQuery
import androidx.room.parser.QueryType
import androidx.room.parser.SqlParser
import androidx.room.vo.DatabaseView
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement

class DatabaseViewProcessor(
    baseContext: Context,
    val element: TypeElement
) {

    val context = baseContext.fork(element)

    fun process(): DatabaseView {
        context.checker.hasAnnotation(element, androidx.room.DatabaseView::class,
                ProcessorErrors.VIEW_MUST_BE_ANNOTATED_WITH_DATABASE_VIEW)
        val annotation = MoreElements.getAnnotationMirror(element,
                androidx.room.DatabaseView::class.java).orNull()

        val viewName: String = if (annotation != null) {
            extractViewName(element, annotation)
        } else {
            element.simpleName.toString()
        }

        val query: ParsedQuery = if (annotation != null) {
            val query = SqlParser.parse(
                    AnnotationMirrors.getAnnotationValue(annotation, "value").value.toString())
            context.checker.check(query.errors.isEmpty(), element,
                    query.errors.joinToString("\n"))
            context.checker.check(query.type == QueryType.SELECT, element,
                    ProcessorErrors.VIEW_QUERY_MUST_BE_SELECT)
            context.checker.check(query.bindSections.isEmpty(), element,
                    ProcessorErrors.VIEW_QUERY_CANNOT_TAKE_ARGUMENTS)
            query
        } else {
            ParsedQuery.MISSING
        }

        context.checker.notBlank(viewName, element,
                ProcessorErrors.VIEW_NAME_CANNOT_BE_EMPTY)
        context.checker.check(!viewName.startsWith("sqlite_", true), element,
                ProcessorErrors.VIEW_NAME_CANNOT_START_WITH_SQLITE)

        val pojo = PojoProcessor.createFor(
                context = context,
                element = element,
                bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                parent = null).process()

        return DatabaseView(
                element = element,
                viewName = viewName,
                query = query,
                type = pojo.type,
                fields = pojo.fields,
                embeddedFields = pojo.embeddedFields,
                constructor = pojo.constructor)
    }

    companion object {
        private fun extractViewName(element: TypeElement, annotation: AnnotationMirror): String {
            val annotationValue = AnnotationMirrors
                    .getAnnotationValue(annotation, "viewName").value.toString()
            return if (annotationValue == "") {
                element.simpleName.toString()
            } else {
                annotationValue
            }
        }
    }
}
