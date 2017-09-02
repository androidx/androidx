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

import android.arch.lifecycle.model.AdapterClass
import android.arch.lifecycle.model.EventMethodCall
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

fun writeModels(infos: List<AdapterClass>, processingEnv: ProcessingEnvironment) {
    infos.forEach({ writeAdapter(it, processingEnv) })
}


private val GENERATED_PACKAGE = "javax.annotation"
private val GENERATED_NAME = "Generated"
private val LIFECYCLE_OWNER = ClassName.get(LifecycleOwner::class.java)
private val LIFECYCLE_EVENT = Lifecycle.Event::class.java

private val T = "\$T"
private val N = "\$N"
private val L = "\$L"
private val S = "\$S"

private fun writeAdapter(adapter: AdapterClass, processingEnv: ProcessingEnvironment) {
    val ownerParam = ParameterSpec.builder(LIFECYCLE_OWNER, "owner").build()
    val eventParam = ParameterSpec.builder(ClassName.get(LIFECYCLE_EVENT), "event").build()
    val receiverName = "mReceiver"
    val receiverField = FieldSpec.builder(ClassName.get(adapter.type), receiverName,
            Modifier.FINAL).build()

    val dispatchMethodBuilder = MethodSpec.methodBuilder("onStateChanged")
            .returns(TypeName.VOID)
            .addParameter(ownerParam)
            .addParameter(eventParam)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
    val dispatchMethod = dispatchMethodBuilder.apply {
        adapter.calls
                .groupBy { (eventMethod) -> eventMethod.onLifecycleEvent.value }
                .forEach { entry ->
                    val event = entry.key
                    val calls = entry.value
                    if (event == Lifecycle.Event.ON_ANY) {
                        writeMethodCalls(eventParam, calls, ownerParam, receiverField)
                    } else {
                        beginControlFlow("if ($N == $T.$L)", eventParam, LIFECYCLE_EVENT, event)
                                .writeMethodCalls(eventParam, calls, ownerParam, receiverField)
                        endControlFlow()
                    }
                }
    }.build()

    val receiverParam = ParameterSpec.builder(
            ClassName.get(adapter.type), "receiver").build()

    val syntheticMethods = adapter.syntheticMethods.map {
        val method = MethodSpec.methodBuilder(syntheticName(it))
                .returns(TypeName.VOID)
                .addModifiers(Modifier.PUBLIC)
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

    val adapterName = getAdapterName(adapter.type)
    val adapterTypeSpecBuilder = TypeSpec.classBuilder(adapterName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get(GenericLifecycleObserver::class.java))
            .addField(receiverField)
            .addMethod(constructor)
            .addMethod(dispatchMethod)
            .addMethods(syntheticMethods)

    addGeneratedAnnotationIfAvailable(adapterTypeSpecBuilder, processingEnv)

    JavaFile.builder(adapter.type.getPackageQName(), adapterTypeSpecBuilder.build())
            .build().writeTo(processingEnv.filer)
}

private fun addGeneratedAnnotationIfAvailable(adapterTypeSpecBuilder: TypeSpec.Builder,
                                   processingEnv: ProcessingEnvironment) {
    val generatedAnnotationAvailable = processingEnv
            .elementUtils
            .getTypeElement(GENERATED_PACKAGE + "." + GENERATED_NAME) != null
    if (generatedAnnotationAvailable) {
        val generatedAnnotationSpec =
                AnnotationSpec.builder(ClassName.get(GENERATED_PACKAGE, GENERATED_NAME)).addMember(
                "value",
                S,
                LifecycleProcessor::class.java.canonicalName).build()
        adapterTypeSpecBuilder.addAnnotation(generatedAnnotationSpec)
    }
}

private fun MethodSpec.Builder.writeMethodCalls(eventParam: ParameterSpec,
                                                calls: List<EventMethodCall>,
                                                ownerParam: ParameterSpec,
                                                receiverField: FieldSpec) {
    calls.forEach { (method, syntheticAccess) ->
        val count = method.method.parameters.size
        if (syntheticAccess == null) {
            val paramString = generateParamString(count)
            addStatement("$N.$L($paramString)", receiverField,
                    method.method.name(),
                    *takeParams(count, ownerParam, eventParam))

        } else {
            val originalType = syntheticAccess
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
