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

import androidx.room.vo.Entity
import androidx.room.vo.ShortcutQueryParameter
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/**
 * Common functionality for shortcut method processors
 */
class ShortcutMethodProcessor(baseContext: Context,
                              val containing: DeclaredType,
                              val executableElement: ExecutableElement) {
    val context = baseContext.fork(executableElement)
    private val asMember = context.processingEnv.typeUtils.asMemberOf(containing, executableElement)
    private val executableType = MoreTypes.asExecutable(asMember)

    fun extractAnnotation(klass: KClass<out Annotation>,
                          errorMsg: String): AnnotationMirror? {
        val annotation = MoreElements.getAnnotationMirror(executableElement,
                klass.java).orNull()
        context.checker.check(annotation != null, executableElement, errorMsg)
        return annotation
    }

    fun extractReturnType(): TypeMirror {
        return executableType.returnType
    }

    fun extractParams(
            missingParamError: String
    ): Pair<Map<String, Entity>, List<ShortcutQueryParameter>> {
        val params = executableElement.parameters
                .map { ShortcutParameterProcessor(
                        baseContext = context,
                        containing = containing,
                        element = it).process() }
        context.checker.check(params.isNotEmpty(), executableElement, missingParamError)
        val entities = params
                .filter { it.entityType != null }
                .associateBy({ it.name }, {
                    EntityProcessor(
                            baseContext = context,
                            element = MoreTypes.asTypeElement(it.entityType)).process()
                })
        return Pair(entities, params)
    }
}
