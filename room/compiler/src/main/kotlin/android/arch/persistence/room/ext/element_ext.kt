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

package android.arch.persistence.room.ext

import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor6
import kotlin.reflect.KClass

fun Element.hasAnyOf(vararg modifiers: Modifier): Boolean {
    return this.modifiers.any { modifiers.contains(it) }
}

fun Element.hasAnnotation(klass: KClass<out Annotation>): Boolean {
    return MoreElements.isAnnotationPresent(this, klass.java)
}

fun Element.isNonNull() =
        asType().kind.isPrimitive
                || hasAnnotation(android.support.annotation.NonNull::class)
                || hasAnnotation(org.jetbrains.annotations.NotNull::class)

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
    if (superclass.kind != TypeKind.NONE) {
        return myMembers + MoreTypes.asTypeElement(superclass)
                .getAllFieldsIncludingPrivateSupers(processingEnvironment)
    } else {
        return myMembers
    }
}

// code below taken from dagger2
// compiler/src/main/java/dagger/internal/codegen/ConfigurationAnnotations.java
private val TO_LIST_OF_TYPES = object
    : SimpleAnnotationValueVisitor6<List<TypeMirror>, Void?>() {
    override fun visitArray(values: MutableList<out AnnotationValue>?, p: Void?): List<TypeMirror> {
        return values?.map {
            val tmp = TO_TYPE.visit(it)
            tmp
        }?.filterNotNull() ?: emptyList()
    }

    override fun defaultAction(o: Any?, p: Void?): List<TypeMirror>? {
        return emptyList()
    }
}

private val TO_TYPE = object : SimpleAnnotationValueVisitor6<TypeMirror, Void>() {

    override fun visitType(t: TypeMirror, p: Void?): TypeMirror {
        return t
    }

    override fun defaultAction(o: Any?, p: Void?): TypeMirror {
        throw TypeNotPresentException(o!!.toString(), null)
    }
}

fun AnnotationValue.toListOfClassTypes(): List<TypeMirror> {
    return TO_LIST_OF_TYPES.visit(this)
}

fun AnnotationValue.toType(): TypeMirror {
    return TO_TYPE.visit(this)
}

fun AnnotationValue.toClassType(): TypeMirror? {
    return TO_TYPE.visit(this)
}

fun TypeMirror.isCollection(): Boolean {
    return MoreTypes.isType(this)
            && (MoreTypes.isTypeOf(java.util.List::class.java, this)
            || MoreTypes.isTypeOf(java.util.Set::class.java, this))
}

fun Element.getAnnotationValue(annotation: Class<out Annotation>, fieldName: String): Any? {
    return MoreElements.getAnnotationMirror(this, annotation)
            .orNull()?.let {
        AnnotationMirrors.getAnnotationValue(it, fieldName)?.value
    }
}

private val ANNOTATION_VALUE_TO_INT_VISITOR = object : SimpleAnnotationValueVisitor6<Int?, Void>() {
    override fun visitInt(i: Int, p: Void?): Int? {
        return i
    }
}

private val ANNOTATION_VALUE_TO_BOOLEAN_VISITOR = object
    : SimpleAnnotationValueVisitor6<Boolean?, Void>() {
    override fun visitBoolean(b: Boolean, p: Void?): Boolean? {
        return b
    }
}

private val ANNOTATION_VALUE_TO_STRING_VISITOR = object
    : SimpleAnnotationValueVisitor6<String?, Void>() {
    override fun visitString(s: String?, p: Void?): String? {
        return s
    }
}

private val ANNOTATION_VALUE_STRING_ARR_VISITOR = object
    : SimpleAnnotationValueVisitor6<List<String>, Void>() {
    override fun visitArray(vals: MutableList<out AnnotationValue>?, p: Void?): List<String> {
        return vals?.map {
            ANNOTATION_VALUE_TO_STRING_VISITOR.visit(it)
        }?.filterNotNull() ?: emptyList()
    }
}

fun AnnotationValue.getAsInt(def: Int? = null): Int? {
    return ANNOTATION_VALUE_TO_INT_VISITOR.visit(this) ?: def
}

fun AnnotationValue.getAsString(def: String? = null): String? {
    return ANNOTATION_VALUE_TO_STRING_VISITOR.visit(this) ?: def
}

fun AnnotationValue.getAsBoolean(def: Boolean): Boolean {
    return ANNOTATION_VALUE_TO_BOOLEAN_VISITOR.visit(this) ?: def
}

fun AnnotationValue.getAsStringList(): List<String> {
    return ANNOTATION_VALUE_STRING_ARR_VISITOR.visit(this)
}
