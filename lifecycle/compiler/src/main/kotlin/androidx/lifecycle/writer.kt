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

package androidx.lifecycle

import androidx.lifecycle.model.AdapterClass
import androidx.lifecycle.model.EventMethodCall
import androidx.lifecycle.model.getAdapterName
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.StandardLocation

fun writeModels(infos: List<AdapterClass>, processingEnv: ProcessingEnvironment) {
    infos.forEach({ writeAdapter(it, processingEnv) })
}

private val GENERATED_PACKAGE = "javax.annotation"
private val GENERATED_NAME = "Generated"
private val LIFECYCLE_EVENT = Lifecycle.Event::class.java

private val T = "\$T"
private val N = "\$N"
private val L = "\$L"
private val S = "\$S"

private val OWNER_PARAM: ParameterSpec = ParameterSpec.builder(
        ClassName.get(LifecycleOwner::class.java), "owner").build()
private val EVENT_PARAM: ParameterSpec = ParameterSpec.builder(
        ClassName.get(LIFECYCLE_EVENT), "event").build()
private val ON_ANY_PARAM: ParameterSpec = ParameterSpec.builder(TypeName.BOOLEAN, "onAny").build()

private val METHODS_LOGGER: ParameterSpec = ParameterSpec.builder(
        ClassName.get(MethodCallsLogger::class.java), "logger").build()

private const val HAS_LOGGER_VAR = "hasLogger"

private fun writeAdapter(adapter: AdapterClass, processingEnv: ProcessingEnvironment) {
    val receiverField: FieldSpec = FieldSpec.builder(ClassName.get(adapter.type), "mReceiver",
            Modifier.FINAL).build()
    val dispatchMethodBuilder = MethodSpec.methodBuilder("callMethods")
            .returns(TypeName.VOID)
            .addParameter(OWNER_PARAM)
            .addParameter(EVENT_PARAM)
            .addParameter(ON_ANY_PARAM)
            .addParameter(METHODS_LOGGER)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
    val dispatchMethod = dispatchMethodBuilder.apply {

        addStatement("boolean $L = $N != null", HAS_LOGGER_VAR, METHODS_LOGGER)
        val callsByEventType = adapter.calls.groupBy { it.method.onLifecycleEvent.value }
        beginControlFlow("if ($N)", ON_ANY_PARAM).apply {
            writeMethodCalls(callsByEventType[Lifecycle.Event.ON_ANY] ?: emptyList(), receiverField)
        }.endControlFlow()

        callsByEventType
                .filterKeys { key -> key != Lifecycle.Event.ON_ANY }
                .forEach { (event, calls) ->
                    beginControlFlow("if ($N == $T.$L)", EVENT_PARAM, LIFECYCLE_EVENT, event)
                    writeMethodCalls(calls, receiverField)
                    endControlFlow()
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
            method.addParameter(OWNER_PARAM)
        }
        if (it.parameters.size == 2) {
            method.addParameter(EVENT_PARAM)
        }

        val count = it.parameters.size
        val paramString = generateParamString(count)
        method.addStatement("$N.$L($paramString)", receiverParam, it.name(),
                *takeParams(count, OWNER_PARAM, EVENT_PARAM))
        method.build()
    }

    val constructor = MethodSpec.constructorBuilder()
            .addParameter(receiverParam)
            .addStatement("this.$N = $N", receiverField, receiverParam)
            .build()

    val adapterName = getAdapterName(adapter.type)
    val adapterTypeSpecBuilder = TypeSpec.classBuilder(adapterName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get(GeneratedAdapter::class.java))
            .addField(receiverField)
            .addMethod(constructor)
            .addMethod(dispatchMethod)
            .addMethods(syntheticMethods)

    addGeneratedAnnotationIfAvailable(adapterTypeSpecBuilder, processingEnv)

    JavaFile.builder(adapter.type.getPackageQName(), adapterTypeSpecBuilder.build())
            .build().writeTo(processingEnv.filer)

    generateKeepRule(adapter.type, processingEnv)
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

private fun generateKeepRule(type: TypeElement, processingEnv: ProcessingEnvironment) {
    val adapterClass = type.getPackageQName() + "." + getAdapterName(type)
    val observerClass = type.toString()
    val keepRule = """# Generated keep rule for Lifecycle observer adapter.
        |-if class $observerClass {
        |    <init>(...);
        |}
        |-keep class $adapterClass {
        |    <init>(...);
        |}
        |""".trimMargin()

    // Write the keep rule to the META-INF/proguard directory of the Jar file. The file name
    // contains the fully qualified observer name so that file names are unique. This will allow any
    // jar file merging to not overwrite keep rule files.
    val path = "META-INF/proguard/$observerClass.pro"
    val out = processingEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", path)
    out.openWriter().use { it.write(keepRule) }
}

private fun MethodSpec.Builder.writeMethodCalls(calls: List<EventMethodCall>,
                                                receiverField: FieldSpec) {
    calls.forEach { (method, syntheticAccess) ->
        val count = method.method.parameters.size
        val callType = 1 shl count
        val methodName = method.method.name()
        beginControlFlow("if (!$L || $N.approveCall($S, $callType))",
                HAS_LOGGER_VAR, METHODS_LOGGER, methodName).apply {

            if (syntheticAccess == null) {
                val paramString = generateParamString(count)
                addStatement("$N.$L($paramString)", receiverField,
                        methodName,
                        *takeParams(count, OWNER_PARAM, EVENT_PARAM))
            } else {
                val originalType = syntheticAccess
                val paramString = generateParamString(count + 1)
                val className = ClassName.get(originalType.getPackageQName(),
                        getAdapterName(originalType))
                addStatement("$T.$L($paramString)", className,
                        syntheticName(method.method),
                        *takeParams(count + 1, receiverField, OWNER_PARAM, EVENT_PARAM))
            }
        }.endControlFlow()
    }
    addStatement("return")
}

private fun takeParams(count: Int, vararg params: Any) = params.take(count).toTypedArray()

private fun generateParamString(count: Int) = (0 until count).joinToString(",") { N }
