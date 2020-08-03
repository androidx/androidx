/*
 * Copyright (C) 2020 The Android Open Source Project
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

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package androidx.contentaccess.ext

import androidx.contentaccess.IgnoreConstructor
import androidx.contentaccess.compiler.utils.JvmSignatureUtil
import asTypeElement
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.tag
import java.lang.reflect.Proxy
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.SimpleAnnotationValueVisitor6
import javax.lang.model.util.SimpleTypeVisitor7
import kotlin.reflect.KClass

fun Element.hasAnnotation(klass: KClass<out Annotation>): Boolean {
    return MoreElements.isAnnotationPresent(this, klass.java)
}

fun TypeElement.getNonPrivateNonIgnoreConstructors(): List<ExecutableElement> {
    return ElementFilter.constructorsIn(this.enclosedElements).filter {
        !it.modifiers.contains(Modifier.PRIVATE) && !it.hasAnnotation(IgnoreConstructor::class)
    }
}

fun TypeElement.hasMoreThanOneNonPrivateNonIgnoredConstructor() =
    this.getNonPrivateNonIgnoreConstructors().size > 1

fun TypeElement.isFilledThroughConstructor() = this.getNonPrivateNonIgnoreConstructors().size == 1

fun TypeElement.isNotInstantiable():
        Boolean {
    // No constructors means we can't instantiate it to fill its public fields, user might have
    // made the default constructor private or simply ignored it.
    return this.getNonPrivateNonIgnoreConstructors().isEmpty()
}

/**
 * Gets either all the parameters of the single public constructor if they exist, otherwise returns
 * a list of all public fields, even if empty.
 */
fun TypeElement.getAllConstructorParamsOrPublicFields():
        List<VariableElement> {
    val constructors = this.getNonPrivateNonIgnoreConstructors()

    if (constructors.size == 1) {
        val parameters = MoreElements.asExecutable(constructors.first()).parameters
        if (parameters.isNotEmpty()) {
            return parameters.map { it as VariableElement }
        }
    } else if (constructors.isEmpty()) {
        error("${this.qualifiedName} has no non private and non ignored constructors!")
    } else {
        error("${this.qualifiedName} has more than non private non ignored constructor")
    }
    // TODO(obenabde): explore ways to warn users if they're unknowingly doing something wrong
    //  e.g if there is a possibility they think we are filling fields instead of constructors
    //  or both etc...
    // This is a class with an empty or no public constructor, check public fields.
    return getAllNonPrivateFieldsIncludingSuperclassOnes()
}

fun TypeElement.getAllNonPrivateFieldsIncludingSuperclassOnes(): List<VariableElement> {
    var nonPrivateFields = ElementFilter.fieldsIn(this.enclosedElements)
        .filterNot { it.modifiers.contains(Modifier.PRIVATE) }
    if (superclass.kind != TypeKind.NONE) {
        nonPrivateFields = nonPrivateFields + MoreTypes.asTypeElement(superclass)
            .getAllNonPrivateFieldsIncludingSuperclassOnes()
    }
    return nonPrivateFields.map { it as VariableElement }
}

fun TypeElement.hasNonEmptyNonPrivateNonIgnoredConstructor(): Boolean {
    val constructors = ElementFilter.constructorsIn(this.enclosedElements).filter {
        !it.modifiers.contains(Modifier.PRIVATE) && !it.hasAnnotation(IgnoreConstructor::class)
    }
    if (constructors.isEmpty()) {
        return false
    }
    val parameters = MoreElements.asExecutable(constructors.first()).parameters
    return parameters.isNotEmpty()
}

fun TypeMirror.extendsBound(): TypeMirror? {
    return this.accept(object : SimpleTypeVisitor7<TypeMirror?, Void?>() {
        override fun visitWildcard(type: WildcardType, ignored: Void?): TypeMirror? {
            return type.extendsBound ?: type.superBound
        }
    }, null)
}

@KotlinPoetMetadataPreview
fun ExecutableElement.isSuspendFunction(processingEnv: ProcessingEnvironment) =
    getKotlinFunspec(processingEnv).modifiers.contains(KModifier.SUSPEND)

@KotlinPoetMetadataPreview
fun ExecutableElement.getSuspendFunctionReturnType():
        TypeMirror {
    val typeParam = MoreTypes.asDeclared(parameters.last().asType()).typeArguments.first()
    return typeParam.extendsBound() ?: typeParam
}

@KotlinPoetMetadataPreview
fun ExecutableElement.getKotlinFunspec(processingEnv: ProcessingEnvironment):
        FunSpec {
    val classInspector = ElementsClassInspector.create(processingEnv.elementUtils, processingEnv
        .typeUtils)
    val enclosingClass = this.enclosingElement as TypeElement

    val kotlinApi = enclosingClass.toTypeSpec(classInspector)
    val jvmSignature = JvmSignatureUtil.getMethodDescriptor(this)
    val funSpec = kotlinApi.funSpecs.find {
        it.tag<ImmutableKmFunction>()?.signature?.asString() == jvmSignature
    } ?: error("No matching funSpec found for $jvmSignature.")
    return funSpec
}

fun TypeElement.getAllMethodsIncludingSupers(): Set<ExecutableElement> {
    val myMethods = ElementFilter.methodsIn(this.enclosedElements).toSet()
    val interfaceMethods = interfaces.flatMap {
        it.asTypeElement().getAllMethodsIncludingSupers()
    }
    return if (superclass.kind != TypeKind.NONE) {
        myMethods + interfaceMethods + superclass.asTypeElement().getAllMethodsIncludingSupers()
    } else {
        myMethods + interfaceMethods
    }
}

interface ClassGetter {
    fun getAsTypeMirror(methodName: String): TypeMirror?
    fun getAsTypeMirrorList(methodName: String): List<TypeMirror>
    fun <T : Annotation> getAsAnnotationBox(methodName: String): AnnotationBox<T>
    fun <T : Annotation> getAsAnnotationBoxArray(methodName: String): Array<AnnotationBox<T>>
}

/**
 * Class that helps to read values from annotations. Simple types as string, int, lists can
 * be read from [value]. If you need to read classes or another annotations from annotation use
 * [getAsTypeMirror], [getAsAnnotationBox] and [getAsAnnotationBoxArray] correspondingly.
 */
class AnnotationBox<T : Annotation>(private val obj: Any) : ClassGetter by (obj as ClassGetter) {
    @Suppress("UNCHECKED_CAST")
    val value: T = obj as T
}

private fun <T : Annotation> AnnotationMirror.box(cl: Class<T>): AnnotationBox<T> {
    if (!cl.isAnnotation) {
        throw IllegalArgumentException("$cl is not annotation")
    }
    val map = cl.declaredMethods.associate { method ->
        val value = AnnotationMirrors.getAnnotationValue(this, method.name)
        val returnType = method.returnType
        val defaultValue = method.defaultValue
        val result: Any? = when {
            returnType == Boolean::class.java -> value.getAsBoolean(defaultValue as Boolean)
            returnType == String::class.java -> value.getAsString(defaultValue as String?)
            returnType == Array<String>::class.java -> value.getAsStringList().toTypedArray()
            returnType == emptyArray<Class<*>>()::class.java -> value.toListOfClassTypes()
            returnType == IntArray::class.java -> value.getAsIntList().toIntArray()
            returnType == Class::class.java -> {
                try {
                    value.toClassType()
                } catch (notPresent: TypeNotPresentException) {
                    null
                }
            }
            returnType == Int::class.java -> value.getAsInt(defaultValue as Int?)
            returnType.isAnnotation -> {
                @Suppress("UNCHECKED_CAST")
                AnnotationClassVisitor(returnType as Class<out Annotation>).visit(value)
            }
            returnType.isArray && returnType.componentType.isAnnotation -> {
                @Suppress("UNCHECKED_CAST")
                ListVisitor(returnType.componentType as Class<out Annotation>).visit(value)
            }
            returnType.isEnum -> {
                @Suppress("UNCHECKED_CAST")
                value.getAsEnum(returnType as Class<out Enum<*>>)
            }
            else -> throw UnsupportedOperationException("$returnType isn't supported")
        }
        method.name to result
    }
    return AnnotationBox(Proxy.newProxyInstance(ClassGetter::class.java.classLoader,
            arrayOf(cl, ClassGetter::class.java)) { _, method, args ->
        when (method.name) {
            ClassGetter::getAsTypeMirror.name -> map[args[0]]
            ClassGetter::getAsTypeMirrorList.name -> map[args[0]]
            "getAsAnnotationBox" -> map[args[0]]
            "getAsAnnotationBoxArray" -> map[args[0]]
            else -> map[method.name]
        }
    })
}

fun <T : Annotation> Element.toAnnotationBox(cl: KClass<T>) =
        MoreElements.getAnnotationMirror(this, cl.java).orNull()?.box(cl.java)

@Suppress("DEPRECATION")
private class ListVisitor<T : Annotation>(private val annotationClass: Class<T>) :
    SimpleAnnotationValueVisitor6<Array<AnnotationBox<T>>, Void?>() {
    override fun visitArray(
        values: MutableList<out AnnotationValue>?,
        void: Void?
    ): Array<AnnotationBox<T>> {
        val visitor = AnnotationClassVisitor(annotationClass)
        return values?.mapNotNull { visitor.visit(it) }?.toTypedArray() ?: emptyArray()
    }
}

@Suppress("DEPRECATION")
private class AnnotationClassVisitor<T : Annotation>(private val annotationClass: Class<T>) :
    SimpleAnnotationValueVisitor6<AnnotationBox<T>?, Void?>() {
    override fun visitAnnotation(a: AnnotationMirror?, v: Void?) = a?.box(annotationClass)
}

// code below taken from dagger2
// compiler/src/main/java/dagger/internal/codegen/ConfigurationAnnotations.java
@Suppress("DEPRECATION")
private val TO_LIST_OF_TYPES = object :
    SimpleAnnotationValueVisitor6<List<TypeMirror>, Void?>() {
    override fun visitArray(values: MutableList<out AnnotationValue>?, p: Void?): List<TypeMirror> {
        return values?.mapNotNull {
            val tmp = TO_TYPE.visit(it)
            tmp
        } ?: emptyList()
    }

    override fun defaultAction(o: Any?, p: Void?): List<TypeMirror>? {
        return emptyList()
    }
}

@Suppress("DEPRECATION")
private val TO_TYPE = object : SimpleAnnotationValueVisitor6<TypeMirror, Void>() {

    override fun visitType(t: TypeMirror, p: Void?): TypeMirror {
        return t
    }

    override fun defaultAction(o: Any?, p: Void?): TypeMirror {
        throw TypeNotPresentException(o!!.toString(), null)
    }
}

private fun AnnotationValue.toListOfClassTypes(): List<TypeMirror> {
    return TO_LIST_OF_TYPES.visit(this)
}

private fun AnnotationValue.toClassType(): TypeMirror? {
    return TO_TYPE.visit(this)
}

@Suppress("DEPRECATION")
private val ANNOTATION_VALUE_TO_INT_VISITOR = object : SimpleAnnotationValueVisitor6<Int?, Void>() {
    override fun visitInt(i: Int, p: Void?): Int? {
        return i
    }
}

@Suppress("DEPRECATION")
private val ANNOTATION_VALUE_TO_BOOLEAN_VISITOR = object :
    SimpleAnnotationValueVisitor6<Boolean?, Void>() {
    override fun visitBoolean(b: Boolean, p: Void?): Boolean? {
        return b
    }
}

@Suppress("DEPRECATION")
private val ANNOTATION_VALUE_TO_STRING_VISITOR = object :
    SimpleAnnotationValueVisitor6<String?, Void>() {
    override fun visitString(s: String?, p: Void?): String? {
        return s
    }
}

@Suppress("DEPRECATION")
private val ANNOTATION_VALUE_STRING_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor6<List<String>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<String> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_STRING_VISITOR.visit(it)
        } ?: emptyList()
    }
}

@Suppress("DEPRECATION")
private val ANNOTATION_VALUE_INT_ARR_VISITOR = object :
    SimpleAnnotationValueVisitor6<List<Int>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<Int> {
        return vals?.mapNotNull {
            ANNOTATION_VALUE_TO_INT_VISITOR.visit(it)
        } ?: emptyList()
    }
}

private fun AnnotationValue.getAsInt(def: Int? = null): Int? {
    return ANNOTATION_VALUE_TO_INT_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsIntList(): List<Int> {
    return ANNOTATION_VALUE_INT_ARR_VISITOR.visit(this)
}

private fun AnnotationValue.getAsString(def: String? = null): String? {
    return ANNOTATION_VALUE_TO_STRING_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsBoolean(def: Boolean): Boolean {
    return ANNOTATION_VALUE_TO_BOOLEAN_VISITOR.visit(this) ?: def
}

private fun AnnotationValue.getAsStringList(): List<String> {
    return ANNOTATION_VALUE_STRING_ARR_VISITOR.visit(this)
}

@Suppress("UNCHECKED_CAST", "DEPRECATION")
private fun <T : Enum<*>> AnnotationValue.getAsEnum(enumClass: Class<T>): T {
    return object : SimpleAnnotationValueVisitor6<T, Void>() {
        override fun visitEnumConstant(value: VariableElement?, p: Void?): T {
            return enumClass.getDeclaredMethod("valueOf", String::class.java)
                    .invoke(null, value!!.simpleName.toString()) as T
        }
    }.visit(this)
}
