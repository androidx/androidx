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
package androidx.room.processor

import androidx.room.ext.toAnnotationBox
import androidx.room.vo.Entity
import androidx.room.vo.ShortcutQueryParameter
import asTypeElement
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/**
 * Common functionality for shortcut method processors
 */
class ShortcutMethodProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement
) {
    val context = baseContext.fork(executableElement)
    private val delegate = MethodProcessorDelegate.createFor(context, containing, executableElement)

    fun <T : Annotation> extractAnnotation(klass: KClass<T>, errorMsg: String): T? {
        val annotation = executableElement.toAnnotationBox(klass)
        context.checker.check(annotation != null, executableElement, errorMsg)
        return annotation?.value
    }

    fun extractReturnType() = delegate.extractReturnType()

    fun extractParams(
        missingParamError: String
    ): Pair<Map<String, Entity>, List<ShortcutQueryParameter>> {
        val params = delegate.extractParams()
                .map { ShortcutParameterProcessor(
                        baseContext = context,
                        containing = containing,
                        element = it).process() }
        context.checker.check(params.isNotEmpty(), executableElement, missingParamError)
        val entities = params
                .filter { it.entityType != null }
                .associateBy({ it.name }, {
                    EntityProcessor(
                            context = context,
                            element = it.entityType!!.asTypeElement()).process()
                })
        return Pair(entities, params)
    }

    fun findInsertMethodBinder(
        returnType: TypeMirror,
        params: List<ShortcutQueryParameter>
    ) = delegate.findInsertMethodBinder(returnType, params)

    fun findDeleteOrUpdateMethodBinder(returnType: TypeMirror) =
        delegate.findDeleteOrUpdateMethodBinder(returnType)
}
