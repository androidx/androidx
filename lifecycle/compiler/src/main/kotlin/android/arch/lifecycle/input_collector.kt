/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.lifecycle

import android.arch.lifecycle.model.EventMethod
import android.arch.lifecycle.model.LifecycleObserverInfo
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic

fun collectAndVerifyInput(processingEnv: ProcessingEnvironment,
                          roundEnv: RoundEnvironment): Map<TypeElement, LifecycleObserverInfo> {
    val validator = Validator(processingEnv)

    return roundEnv.getElementsAnnotatedWith(OnLifecycleEvent::class.java).map { elem ->
        if (elem.kind != ElementKind.METHOD) {
            validator.printErrorMessage(ErrorMessages.INVALID_ANNOTATED_ELEMENT, elem)
            null
        } else {
            val enclosingElement = elem.enclosingElement
            val onState = elem.getAnnotation(OnLifecycleEvent::class.java)
            val method = MoreElements.asExecutable(elem)
            if (validator.validateClass(enclosingElement)
                    && validator.validateMethod(method, onState.value)) {
                EventMethod(method, onState, MoreElements.asType(enclosingElement))
            } else {
                null
            }
        }
    }
            .filterNotNull()
            .groupBy { MoreElements.asType(it.method.enclosingElement) }
            .mapValues { entry -> LifecycleObserverInfo(entry.key, entry.value) }

}

class Validator(val processingEnv: ProcessingEnvironment) {

    fun printErrorMessage(msg: CharSequence, elem: Element) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, msg, elem)
    }

    fun validateParam(param: VariableElement,
                      expectedType: Class<*>, errorMsg: String): Boolean {
        if (!MoreTypes.isTypeOf(expectedType, param.asType())) {
            printErrorMessage(errorMsg, param)
            return false
        }
        return true
    }

    fun validateMethod(method: ExecutableElement, event: Lifecycle.Event): Boolean {
        if (Modifier.PRIVATE in method.modifiers) {
            printErrorMessage(ErrorMessages.INVALID_METHOD_MODIFIER, method)
            return false
        }
        val params = method.parameters
        if ((params.size > 2)) {
            printErrorMessage(ErrorMessages.TOO_MANY_ARGS, method)
            return false
        }

        if (params.size == 2 && event != Lifecycle.Event.ON_ANY) {
            printErrorMessage(ErrorMessages.TOO_MANY_ARGS_NOT_ON_ANY, method)
            return false
        }

        if (params.size == 2 && !validateParam(params[1], Lifecycle.Event::class.java,
                ErrorMessages.INVALID_SECOND_ARGUMENT)) {
            return false
        }

        if (params.size > 0) {
            return validateParam(params[0], LifecycleOwner::class.java,
                    ErrorMessages.INVALID_FIRST_ARGUMENT)
        }
        return true
    }

    fun validateClass(classElement: Element): Boolean {
        if (classElement.kind != ElementKind.CLASS && classElement.kind != ElementKind.INTERFACE) {
            printErrorMessage(ErrorMessages.INVALID_ENCLOSING_ELEMENT, classElement)
            return false
        }
        if (Modifier.PRIVATE in classElement.modifiers) {
            printErrorMessage(ErrorMessages.INVALID_CLASS_MODIFIER, classElement)
            return false
        }
        return true
    }
}
