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

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.LinkedList
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.type.NoType
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

fun Element.getPackage(): PackageElement = MoreElements.getPackage(this)
fun Element.getPackageQName() = getPackage().qualifiedName.toString()
fun ExecutableElement.name() = simpleName.toString()
fun ExecutableElement.isPackagePrivate() = !modifiers.any {
    it == PUBLIC || it == PROTECTED || it == PRIVATE
}

fun ExecutableElement.isProtected() = modifiers.contains(PROTECTED)

@SupportedAnnotationTypes("android.arch.lifecycle.OnLifecycleEvent")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
class LifecycleProcessor : AbstractProcessor() {
    companion object ErrorMessages {
        const val TOO_MANY_ARGS = "callback method cannot have more than 2 parameters"
        const val TOO_MANY_ARGS_NOT_ON_ANY = "only callback annotated with ON_ANY " +
                "can have 2 parameters"
        const val INVALID_SECOND_ARGUMENT = "2nd argument of a callback method" +
                " must be Lifecycle.Event and represent the current event"
        const val INVALID_FIRST_ARGUMENT = "1st argument of a callback method must be " +
                "a LifecycleOwner which represents the source of the event"
        const val INVALID_METHOD_MODIFIER = "method marked with OnLifecycleEvent annotation can " +
                "not be private"
        const val INVALID_CLASS_MODIFIER = "class containing OnLifecycleEvent methods can not be " +
                "private"
        const val INVALID_STATE_OVERRIDE_METHOD = "overridden method must handle the same " +
                "onState changes as original method"
    }

    private val LIFECYCLE_OWNER = ClassName.get(LifecycleOwner::class.java)
    private val JAVA_LIFECYCLE_EVENT = Lifecycle.Event::class.java
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

    private fun validateMethod(method: ExecutableElement, event: Lifecycle.Event): Boolean {
        if (PRIVATE in method.modifiers) {
            printErrorMessage(INVALID_METHOD_MODIFIER, method)
            return false
        }
        val params = method.parameters
        if ((params.size > 2)) {
            printErrorMessage(TOO_MANY_ARGS, method)
            return false
        }

        if (params.size == 2 && event != Lifecycle.Event.ON_ANY) {
            printErrorMessage(TOO_MANY_ARGS_NOT_ON_ANY, method)
            return false
        }

        if (params.size == 2 && !validateParam(params[1], JAVA_LIFECYCLE_EVENT,
                INVALID_SECOND_ARGUMENT)) {
            return false
        }

        if (params.size > 0) {
            return validateParam(params[0], LifecycleOwner::class.java,
                    INVALID_FIRST_ARGUMENT)
        }
        return true
    }

    private fun validateClass(classElement: Element): Boolean {
        if (classElement.kind != ElementKind.CLASS && classElement.kind != ElementKind.INTERFACE) {
            printErrorMessage("Parent of OnLifecycleEvent should be a class or interface",
                    classElement)
            return false
        }
        if (PRIVATE in classElement.modifiers) {
            printErrorMessage(INVALID_CLASS_MODIFIER, classElement)
            return false
        }
        return true
    }

    override fun process(annotations: MutableSet<out TypeElement>,
                         roundEnv: RoundEnvironment): Boolean {
        val world = roundEnv.getElementsAnnotatedWith(OnLifecycleEvent::class.java).map { elem ->
            if (elem.kind != ElementKind.METHOD) {
                printErrorMessage("OnLifecycleEvent can only be added to methods", elem)
                null
            } else {
                val enclosingElement = elem.enclosingElement
                val onState = elem.getAnnotation(OnLifecycleEvent::class.java)
                val method = MoreElements.asExecutable(elem)
                if (validateClass(enclosingElement) && validateMethod(method, onState.value)) {
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

    private fun superObservers(world: Map<TypeElement, LifecycleObserverInfo>,
                               observer: LifecycleObserverInfo): List<LifecycleObserverInfo> {
        val stack = LinkedList<TypeMirror>()
        stack += observer.type.interfaces.reversed()
        stack += observer.type.superclass
        val result = mutableListOf<LifecycleObserverInfo>()
        while (stack.isNotEmpty()) {
            val typeMirror = stack.removeLast()
            if (typeMirror is NoType) {
                continue
            }
            val type = MoreTypes.asTypeElement(typeMirror)
            val currentObserver = world[type]
            if (currentObserver != null) {
                result.add(currentObserver)
            } else {
                stack += type.interfaces.reversed()
                stack += type.superclass
            }
        }
        return result
    }

    private fun mergeAndVerifyMethods(classMethods: List<StateMethod>,
                                      parentMethods: List<StateMethod>): List<StateMethod> {
        return parentMethods + classMethods.filter { currentMethod ->
            val baseMethod = parentMethods.find { m ->
                currentMethod.method.simpleName == m.method.simpleName
                        && currentMethod.method.parameters.size == m.method.parameters.size
            }
            if (baseMethod != null
                    && baseMethod.onLifecycleEvent != currentMethod.onLifecycleEvent) {
                printErrorMessage(INVALID_STATE_OVERRIDE_METHOD, currentMethod.method)
            }
            baseMethod == null
        }

    }

    private fun flattenObserverInfos(
            world: Map<TypeElement, LifecycleObserverInfo>): List<LifecycleObserverInfo> {
        val superObservers = world.mapValues { superObservers(world, it.value) }
        val packagePrivateMethods = world.mapValues { observer ->
            if (observer.value.type.kind.isInterface) {
                emptyList()
            } else {
                observer.value.methods.filter {
                    it.method.isPackagePrivate() || it.method.isProtected()
                }.map { it.method }
            }
        }

        val ppMethodsToType = packagePrivateMethods.entries.fold(
                mapOf<ExecutableElement, TypeElement>(), { map, entry ->
            map + entry.value.associate { it to entry.key }
        })

        world.values.forEach {
            val observers = superObservers[it.type]!!
            val currentPackage = it.type.getPackageQName()
            observers.filter { superObserver ->
                superObserver.type.getPackageQName() != currentPackage
                        && packagePrivateMethods[superObserver.type]!!.isNotEmpty()
            }.forEach { it.syntheticMethods.addAll(packagePrivateMethods[it.type]!!) }
        }


        val flattened: MutableMap<LifecycleObserverInfo, LifecycleObserverInfo> = mutableMapOf()
        fun traverse(observer: LifecycleObserverInfo) {
            if (observer in flattened) {
                return
            }
            val observers = superObservers[observer.type]!!
            if (observers.isEmpty()) {
                flattened[observer] = observer
                return
            }
            observers.filter { it !in flattened }.forEach(::traverse)
            val currentPackage = observer.type.getPackageQName()
            val methods = observers.fold(emptyList<StateMethod>(),
                    { list, observer -> mergeAndVerifyMethods(observer.methods, list) }).map {
                val packageName = ppMethodsToType[it.method]?.getPackageQName()
                if (packageName == null || packageName == currentPackage) {
                    it
                } else {
                    StateMethod(it.method, it.onLifecycleEvent, ppMethodsToType[it.method])
                }
            }

            flattened[observer] = LifecycleObserverInfo(observer.type,
                    mergeAndVerifyMethods(observer.methods, methods), observer.syntheticMethods)
        }

        world.values.forEach(::traverse)
        return flattened.values.toList()
    }

    private fun writeAdapter(observer: LifecycleObserverInfo) {
        val ownerParam = ParameterSpec.builder(LIFECYCLE_OWNER, "owner").build()
        val eventParam = ParameterSpec.builder(ClassName.get(JAVA_LIFECYCLE_EVENT), "event").build()
        val receiverName = "mReceiver"
        val receiverField = FieldSpec.builder(ClassName.get(observer.type), receiverName,
                Modifier.FINAL).build()

        val dispatchMethodBuilder = MethodSpec.methodBuilder("onStateChanged")
                .returns(TypeName.VOID)
                .addParameter(ownerParam)
                .addParameter(eventParam)
                .addModifiers(PUBLIC)
                .addAnnotation(Override::class.java)
        val dispatchMethod = dispatchMethodBuilder.apply {
            observer.methods
                    .groupBy { stateMethod -> stateMethod.onLifecycleEvent.value }
                    .forEach { entry ->
                        val event = entry.key
                        val methods = entry.value
                        if (event == Lifecycle.Event.ON_ANY) {
                            writeMethodCalls(eventParam, methods, ownerParam, receiverField)
                        } else {
                            beginControlFlow("if ($N == $T.$L)", eventParam, JAVA_LIFECYCLE_EVENT, event)
                                    .writeMethodCalls(eventParam, methods, ownerParam, receiverField)
                            endControlFlow()
                        }
                    }
        }.build()

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        val getWrappedMethod = MethodSpec.methodBuilder("getReceiver")
                .returns(ClassName.get(Object::class.java))
                .addModifiers(PUBLIC)
                .addStatement("return $N", receiverField)
                .build()

        val receiverParam = ParameterSpec.builder(ClassName.get(observer.type), "receiver").build()

        val syntheticMethods = observer.syntheticMethods.map {
            val method = MethodSpec.methodBuilder(syntheticName(it))
                    .returns(TypeName.VOID)
                    .addModifiers(PUBLIC)
                    .addModifiers(Modifier.STATIC)
                    .addParameter(receiverParam)
            if (it.parameters.size >= 1) {
                method.addParameter(ownerParam)
            }
            if (it.parameters.size == 2) {
                method.addParameter(eventParam)
            }

            val count = it.parameters.size
            val paramString = generateParamString(count)
            method.addStatement("$N.$L($paramString)", receiverParam, it.name(),
                    *takeParams(count, ownerParam, eventParam))
            method.build()
        }

        val constructor = MethodSpec.constructorBuilder()
                .addParameter(receiverParam)
                .addStatement("this.$N = $N", receiverField, receiverParam)
                .build()

        val adapterName = getAdapterName(observer.type)
        val adapter = TypeSpec.classBuilder(adapterName)
                .addModifiers(PUBLIC)
                .addSuperinterface(ClassName.get(GenericLifecycleObserver::class.java))
                .addField(receiverField)
                .addMethod(constructor)
                .addMethod(dispatchMethod)
                .addMethod(getWrappedMethod)
                .addMethods(syntheticMethods)
                .build()
        JavaFile.builder(observer.type.getPackageQName(), adapter)
                .build().writeTo(processingEnv.filer)
    }

    private fun MethodSpec.Builder.writeMethodCalls(eventParam: ParameterSpec,
                                                    methods: List<StateMethod>,
                                                    ownerParam: ParameterSpec,
                                                    receiverField: FieldSpec) {
        methods.forEach { method ->
            val count = method.method.parameters.size
            if (method.syntheticAccess == null) {
                val paramString = generateParamString(count)
                addStatement("$N.$L($paramString)", receiverField,
                        method.method.name(),
                        *takeParams(count, ownerParam, eventParam))

            } else {
                val originalType = method.syntheticAccess
                val paramString = generateParamString(count + 1)
                val className = ClassName.get(originalType.getPackageQName(),
                        getAdapterName(originalType))
                addStatement("$T.$L($paramString)", className,
                        syntheticName(method.method),
                        *takeParams(count + 1, receiverField, ownerParam,
                                eventParam))
            }
        }
    }

    private fun syntheticName(method: ExecutableElement) = "__synthetic_" + method.simpleName

    private fun takeParams(count: Int, vararg params: Any) = params.take(count).toTypedArray()

    private fun generateParamString(count: Int) = (0..(count - 1)).joinToString(",") { N }

    private fun getAdapterName(type: TypeElement): String {
        val packageElement = type.getPackage()
        val qName = type.qualifiedName.toString()
        val partialName = if (packageElement.isUnnamed) qName else qName.substring(
                packageElement.qualifiedName.toString().length + 1)
        return Lifecycling.getAdapterName(partialName)
    }

    data class StateMethod(val method: ExecutableElement, val onLifecycleEvent: OnLifecycleEvent,
                           val syntheticAccess: TypeElement? = null)

    data class LifecycleObserverInfo(val type: TypeElement, val methods: List<StateMethod>,
                                     var syntheticMethods:
                                     MutableSet<ExecutableElement> = mutableSetOf())
}
