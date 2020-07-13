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

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package androidx.room.ext

import asDeclaredType
import asTypeElement
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import isAssignableFrom
import isDeclared
import isNotNone
import isSameType
import java.lang.reflect.Proxy
import java.util.Locale
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.SimpleAnnotationValueVisitor6
import javax.lang.model.util.SimpleTypeVisitor7
import javax.lang.model.util.Types
import kotlin.contracts.contract
import kotlin.reflect.KClass

private fun Element.hasModifier(modifier: Modifier): Boolean {
    return this.modifiers.contains(modifier)
}

fun Element.isPublic() = hasModifier(Modifier.PUBLIC)

fun Element.isAbstract() = hasModifier(Modifier.ABSTRACT)

fun Element.isPrivate() = hasModifier(Modifier.PRIVATE)

fun Element.isStatic() = hasModifier(Modifier.STATIC)

fun Element.isTransient() = hasModifier(Modifier.TRANSIENT)

fun Element.isFinal() = hasModifier(Modifier.FINAL)

// we handle kotlin and java defaults differently
fun ExecutableElement.isJavaDefault() = hasModifier(Modifier.DEFAULT)

// checks if this executable element can be overridden but does not check if the enclosing element
// can be overridden
fun ExecutableElement.isOverrideableIgnoringContainer(): Boolean = !isPrivate() && !isFinal()

fun Element.hasAnnotation(klass: KClass<out Annotation>): Boolean {
    return MoreElements.isAnnotationPresent(this, klass.java)
}

fun Element.hasAnnotation(clazz: Class<out Annotation>): Boolean {
    return MoreElements.isAnnotationPresent(this, clazz)
}

fun Element.hasAnyOf(vararg klass: KClass<out Annotation>): Boolean {
    return klass.any { MoreElements.isAnnotationPresent(this, it.java) }
}

fun Element.isNonNull() =
        asType().kind.isPrimitive ||
                hasAnnotation(androidx.annotation.NonNull::class) ||
                hasAnnotation(org.jetbrains.annotations.NotNull::class)

fun Element.isEntityElement() = this.hasAnnotation(androidx.room.Entity::class)

fun TypeElement.getConstructors(): List<ExecutableElement> {
    return ElementFilter.constructorsIn(enclosedElements)
}

/**
 * gets all members including super privates. does not handle duplicate field names!!!
 */
// TODO handle conflicts with super: b/35568142
fun TypeElement.getAllFieldsIncludingPrivateSupers(processingEnvironment: ProcessingEnvironment):
        Set<VariableElement> {
    val myMembers = processingEnvironment.elementUtils.getAllMembers(this)
            .filter { it.kind == ElementKind.FIELD }
            .filter { it is VariableElement }
            .map { it as VariableElement }
            .toSet()
    if (superclass.isNotNone()) {
        return myMembers + superclass.asTypeElement()
                .getAllFieldsIncludingPrivateSupers(processingEnvironment)
    } else {
        return myMembers
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

fun TypeMirror.isCollection(): Boolean {
    return MoreTypes.isType(this) &&
            (MoreTypes.isTypeOf(java.util.List::class.java, this) ||
                    MoreTypes.isTypeOf(java.util.Set::class.java, this))
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

// a variant of Types.isAssignable that ignores variance.
fun TypeMirror.isAssignableFromWithoutVariance(
    typeUtils: Types,
    from: TypeMirror
) = typeUtils.isAssignableWithoutVariance(from = from, to = this)

private fun Types.isAssignableWithoutVariance(from: TypeMirror, to: TypeMirror): Boolean {
    val assignable = to.isAssignableFrom(this, from)
    if (assignable) {
        return true
    }
    if (!from.isDeclared() || !to.isDeclared()) {
        return false
    }
    val declaredFrom = MoreTypes.asDeclared(from)
    val declaredTo = MoreTypes.asDeclared(to)
    val fromTypeArgs = declaredFrom.typeArguments
    val toTypeArgs = declaredTo.typeArguments
    // no type arguments, we don't need extra checks
    if (fromTypeArgs.isEmpty() || fromTypeArgs.size != toTypeArgs.size) {
        return false
    }
    // check erasure version first, if it does not match, no reason to proceed
    if (!erasure(to).isAssignableFrom(this, erasure(from))) {
        return false
    }
    // convert from args to their upper bounds if it exists
    val fromExtendsBounds = fromTypeArgs.map {
        it.extendsBound()
    }
    // if there are no upper bound conversions, return.
    if (fromExtendsBounds.all { it == null }) {
        return false
    }
    // try to move the types of the from to their upper bounds. It does not matter for the "to"
    // because Types.isAssignable handles it as it is valid java
    return (0 until fromTypeArgs.size).all { index ->
        isAssignableWithoutVariance(
                from = fromExtendsBounds[index] ?: fromTypeArgs[index],
                to = toTypeArgs[index])
    }
}

// converts ? in Set< ? extends Foo> to Foo
fun TypeMirror.extendsBound(): TypeMirror? {
    return this.accept(object : SimpleTypeVisitor7<TypeMirror?, Void?>() {
        override fun visitWildcard(type: WildcardType, ignored: Void?): TypeMirror? {
            return type.extendsBound ?: type.superBound
        }
    }, null)
}

/**
 * If the type mirror is in form of ? extends Foo, it returns Foo; otherwise, returns the TypeMirror
 * itself.
 */
fun TypeMirror.extendsBoundOrSelf(): TypeMirror {
    return extendsBound() ?: this
}

/**
 * Suffix of the Kotlin synthetic class created interface method implementations.
 */
const val DEFAULT_IMPLS_CLASS_NAME = "DefaultImpls"

/**
 * Finds the default implementation method corresponding to this Kotlin interface method.
 */
fun ExecutableElement.findKotlinDefaultImpl(typeUtils: Types): ExecutableElement? {
    fun paramsMatch(ourParams: List<VariableElement>, theirParams: List<VariableElement>): Boolean {
        if (ourParams.size != theirParams.size - 1) {
            return false
        }
        ourParams.forEachIndexed { i, variableElement ->
            // Plus 1 to their index because their first param is a self object.
            if (!theirParams[i + 1].type.isSameType(typeUtils, variableElement.type)) {
                return false
            }
        }
        return true
    }

    val parent = this.enclosingElement as TypeElement
    val innerClass: TypeElement = parent.enclosedElements.find {
        it.isType() && it.simpleName.contentEquals(DEFAULT_IMPLS_CLASS_NAME)
    } as? TypeElement ?: return null
    return innerClass.getDeclaredMethods().find {
        it.name == this.name && paramsMatch(this.parameters, it.parameters)
    }
}

/**
 * Finds the Kotlin's suspend function return type by inspecting the type param of the Continuation
 * parameter of the function. This method assumes the executable type is a suspend function.
 * @see KotlinMetadataElement.isSuspendFunction
 */
fun ExecutableType.getSuspendFunctionReturnType(): TypeMirror {
    // the continuation parameter is always the last parameter of a suspend function and it only has
    // one type parameter, e.g Continuation<? super T>
    val typeParam = MoreTypes.asDeclared(parameterTypes.last()).typeArguments.first()
    return typeParam.extendsBoundOrSelf() // reduce the type param
}

fun Element.getPackage() = MoreElements.getPackage(this).qualifiedName.toString()

fun Element.asTypeElement() = MoreElements.asType(this)

fun Element.asVariableElement() = MoreElements.asVariable(this)

fun Element.asExecutableElement() = MoreElements.asExecutable(this)

fun Element.isType(): Boolean {
    contract {
        returns(true) implies (this@isType is TypeElement)
    }
    return MoreElements.isType(this)
}

fun Element.isMethod(): Boolean {
    contract {
        returns(true) implies (this@isMethod is ExecutableElement)
    }
    return kind == ElementKind.METHOD
}

fun Element.isConstructor(): Boolean {
    contract {
        returns(true) implies (this@isConstructor is ExecutableElement)
    }
    return kind == ElementKind.CONSTRUCTOR
}

fun Element.isField(): Boolean {
    contract {
        returns(true) implies (this@isField is VariableElement)
    }
    return kind == ElementKind.FIELD
}

fun Element.isInterface(): Boolean {
    contract {
        returns(true) implies (this@isInterface is TypeElement)
    }
    return kind == ElementKind.INTERFACE
}

fun Element.asDeclaredType() = asType().asDeclaredType()

/**
 * methods declared in this type
 *  includes all instance/static methods in this
 */
fun TypeElement.getDeclaredMethods() = ElementFilter.methodsIn(
    enclosedElements
)

/**
 * Methods declared in this type and its parents
 *  includes all instance/static methods in this
 *  includes all instance/static methods in parent CLASS if they are accessible from this (e.g. not
 *  private).
 *  does not include static methods in parent interfaces
 */
fun TypeElement.getAllMethods(
    processingEnv: ProcessingEnvironment
) = ElementFilter.methodsIn(
    processingEnv.elementUtils.getAllMembers(this)
)

/**
 * Instance methods declared in this and supers
 *  include non private instance methods
 *  also includes non-private instance methods from supers
 */
fun TypeElement.getAllNonPrivateInstanceMethods(
    processingEnvironment: ProcessingEnvironment
) = MoreElements.getLocalAndInheritedMethods(
    this,
    processingEnvironment.typeUtils,
    processingEnvironment.elementUtils)

fun VariableElement.asMemberOf(
    types: Types,
    container: DeclaredType?
) = MoreTypes.asMemberOf(types, container, this)

fun ExecutableElement.asMemberOf(
    types: Types,
    container: DeclaredType?
) = MoreTypes.asExecutable(types.asMemberOf(container, this))

/**
 * Returns the string representation of the element kind.
 */
fun Element.kindName() = kind.name.toLowerCase(Locale.US)

// instead of using asType on Element, we should use a specific Element subclass's  .type
// it is not enforce-able until migrating to the abstraction but still a step in the right direction
// of safety
val VariableElement.type: TypeMirror
    get() = asType()

val TypeElement.type: DeclaredType
    get() = MoreTypes.asDeclared(asType())

val ExecutableElement.executableType: ExecutableType
    get() = MoreTypes.asExecutable(asType())

val Element.name: String
    get() = simpleName.toString()