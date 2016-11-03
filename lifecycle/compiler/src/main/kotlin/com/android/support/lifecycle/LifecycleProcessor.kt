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
 *
 */

package com.android.support.lifecycle

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.*
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.NoType
import javax.tools.Diagnostic

@SupportedAnnotationTypes("com.android.support.lifecycle.OnState")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
class LifecycleProcessor : AbstractProcessor() {
    companion object ErrorMessages {
        const val TOO_MANY_ARGS_ERROR_MSG = "callback method cannot have more than 2 parameters"
        const val INVALID_SECOND_ARGUMENT = "2nd argument of a callback method" +
                " must be an int and represent the previous state"
        const val INVALID_FIRST_ARGUMENT = "1st argument of a callback method must be " +
                "a LifecycleProvider which represents the source of the event"
        const val INVALID_METHOD_MODIFIER = "method marked with OnState annotation can not be " +
                "private"
        const val INVALID_CLASS_MODIFIER = "class containing OnState methods can not be private"
        const val INVALID_STATE_OVERRIDE_METHOD = "overridden method must handle the same " +
                "onState changes as original method"
    }

    private val LIFECYCLE_PROVIDER = ClassName.get(LifecycleProvider::class.java)
    private val T = "\$T"
    private val N = "\$N"
    private val L = "\$L"

    private fun printErrorMessage(msg: CharSequence, elem: Element) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, msg, elem)
    }

    private fun validateParam(param: VariableElement,
                              expectedType: Class<*>, errorMsg: String): Boolean {
        if (!MoreTypes.isTypeOf(expectedType, param.asType())) {
            printErrorMessage(errorMsg, param)
            return false
        }
        return true
    }

    private fun validateMethod(method: ExecutableElement): Boolean {
        if (Modifier.PRIVATE in method.modifiers) {
            printErrorMessage(INVALID_METHOD_MODIFIER, method)
            return false
        }
        if (method.parameters.size > 2) {
            printErrorMessage(TOO_MANY_ARGS_ERROR_MSG, method)
            return false
        }
        if (method.parameters.size > 1) {
            // 2nd parameter must be an int
            return validateParam(method.parameters[1], Integer.TYPE, INVALID_SECOND_ARGUMENT)
        }
        if (method.parameters.size > 0) {
            return validateParam(method.parameters[0], LifecycleProvider::class.java,
                    INVALID_FIRST_ARGUMENT)
        }
        return true
    }

    private fun validateClass(classElement: Element): Boolean {
        if (classElement.kind != ElementKind.CLASS) {
            printErrorMessage("Parent of OnState should be a class", classElement)
            return false
        }
        if (Modifier.PRIVATE in classElement.modifiers) {
            printErrorMessage(INVALID_CLASS_MODIFIER, classElement)
            return false
        }
        return true
    }

    override fun process(annotations: MutableSet<out TypeElement>,
                         roundEnv: RoundEnvironment): Boolean {
        val world = roundEnv.getElementsAnnotatedWith(OnState::class.java).map { elem ->
            if (elem.kind != ElementKind.METHOD) {
                printErrorMessage("OnState can only be added to methods", elem)
                null
            } else {
                val enclosingElement = elem.enclosingElement
                val onState = elem.getAnnotation(OnState::class.java)
                val method = MoreElements.asExecutable(elem)
                if (validateClass(enclosingElement) && validateMethod(method)) {
                    StateMethod(method, onState)
                } else {
                    null
                }
            }
        }
                .filterNotNull()
                .groupBy { MoreElements.asType(it.method.enclosingElement) }
                .mapValues { entry -> LifecycleObserverInfo(entry.key, entry.value) }

        flattenObserverInfos(world).forEach {
            writeAdapter(it)
        }
        return true
    }

    private fun superObserver(world: Map<TypeElement, LifecycleObserverInfo>,
                              observer: LifecycleObserverInfo): LifecycleObserverInfo? {
        // TODO: do something about interfaces.
        var currentSuper = observer.type.superclass
        while (currentSuper !is NoType) {
            val currentType = MoreTypes.asTypeElement(currentSuper)
            if (currentType in world) {
                return world[currentType]
            }
            currentSuper = currentType.superclass
        }
        return null
    }

    private fun mergeAndVerifyMethods(classMethods: List<StateMethod>,
                                      parentMethods: List<StateMethod>): List<StateMethod> {
        return parentMethods + classMethods.filter { currentMethod ->
            val baseMethod = parentMethods.find { m ->
                currentMethod.method.simpleName == m.method.simpleName
                        && currentMethod.method.parameters.size == m.method.parameters.size
            }
            if (baseMethod != null && baseMethod.onState != currentMethod.onState) {
                printErrorMessage(INVALID_STATE_OVERRIDE_METHOD, currentMethod.method)
            }
            baseMethod == null
        }

    }

    private fun flattenObserverInfos(
            world: Map<TypeElement, LifecycleObserverInfo>): List<LifecycleObserverInfo> {
        val superObservers = world.mapValues { superObserver(world, it.value) }
        var flattened: MutableMap<LifecycleObserverInfo, LifecycleObserverInfo> = HashMap()
        fun traverse(observer: LifecycleObserverInfo) {
            if (observer in flattened) {
                return
            }
            val sObserver = superObservers[observer.type]
            if (sObserver == null) {
                flattened[observer] = observer
                return
            }
            if (sObserver !in flattened) {
                traverse(sObserver)
            }

            val flat = flattened[sObserver]
            flattened[observer] = LifecycleObserverInfo(observer.type,
                    mergeAndVerifyMethods(observer.methods, flat!!.methods))
        }

        world.values.forEach(::traverse)
        return flattened.values.toList()
    }

    private fun writeAdapter(observer: LifecycleObserverInfo) {
        val packageElement = MoreElements.getPackage(observer.type)
        val qName = observer.type.qualifiedName.toString()
        // TODO what if no package
        val partialName = qName.substring(packageElement.toString().length + 1)
        val adapterName = Lifecycling.getAdapterName(partialName)

        val providerParam = ParameterSpec.builder(LIFECYCLE_PROVIDER, "provider").build()
        val prevStateParam = ParameterSpec.builder(TypeName.INT, "previousState").build()
        val curStateName = "curState"
        val receiverName = "mReceiver"
        val receiverField = FieldSpec.builder(ClassName.get(observer.type), receiverName,
                Modifier.FINAL).build()

        val dispatchMethod = MethodSpec.methodBuilder("onStateChanged")
                .returns(TypeName.VOID)
                .addParameter(providerParam)
                .addParameter(prevStateParam)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addStatement("final $T $N = $N.getLifecycle().getCurrentState()",
                        TypeName.INT, curStateName, providerParam)
                .apply {
                    observer.methods.groupBy { it.onState.value }
                            .forEach { entry ->
                                val onStateValue = entry.key
                                val methods = entry.value
                                beginControlFlow("if (($N & $L) != 0)", curStateName, onStateValue)
                                        .apply {
                                            methods.forEach { method ->
                                                writeMethodCall(method.method, prevStateParam,
                                                        providerParam, receiverField)
                                            }
                                        }
                                endControlFlow()
                            }
                }.build()

        val getWrappedMethod = MethodSpec.methodBuilder("getReceiver")
                .returns(ClassName.get(Object::class.java))
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return $N", receiverField)
                .build()


        val receiverParam = ParameterSpec.builder(ClassName.get(observer.type), "receiver").build()
        val constructor = MethodSpec.constructorBuilder()
                .addParameter(receiverParam)
                .addStatement("this.$N = $N", receiverField, receiverParam)
                .build()

        val adapter = TypeSpec.classBuilder(adapterName)
                .addSuperinterface(ClassName.get(GenericLifecycleObserver::class.java))
                .addField(receiverField)
                .addMethod(constructor)
                .addMethod(dispatchMethod)
                .addMethod(getWrappedMethod)
                .build()
        JavaFile.builder(packageElement.qualifiedName.toString(), adapter)
                .build().writeTo(processingEnv.filer)
    }

    private fun MethodSpec.Builder.writeMethodCall(method: ExecutableElement,
                                                   prevStateParam: ParameterSpec?,
                                                   providerParam: ParameterSpec?,
                                                   receiverField: FieldSpec?) {
        val methodName = method.simpleName.toString()
        when (method.parameters.size) {
            0 -> {
                addStatement("$N.$L()", receiverField, methodName)
            }
            1 -> {
                addStatement("$N.$L($N)", receiverField, methodName,
                        providerParam)
            }
            2 -> {
                addStatement("$N.$L($N, $N)", receiverField, methodName,
                        providerParam, prevStateParam)
            }
            else -> {
                printErrorMessage("Inconsistency. Method $methodName should have 0, 1 or 2 params",
                        method)
            }
        }
    }

    data class StateMethod(val method: ExecutableElement, val onState: OnState)

    data class LifecycleObserverInfo(val type: TypeElement, val methods: List<StateMethod>)
}