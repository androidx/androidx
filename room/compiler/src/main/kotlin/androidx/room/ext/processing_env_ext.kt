/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.room.ext

import com.google.auto.common.GeneratedAnnotations
import com.squareup.javapoet.TypeName
import java.util.Locale
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

/**
 * Query a type element by KClass and return null if it does not exist
 */
fun ProcessingEnvironment.findTypeElement(
    klass: KClass<*>
): TypeElement? = findTypeElement(klass.java.canonicalName!!)

/**
 * Query a type element by TypeName and return null if it does not exist
 */
fun ProcessingEnvironment.findTypeElement(
    typeName: TypeName
): TypeElement? = findTypeElement(typeName.toString())

/**
 * Query a type element by qualified name and return null if it does not exist
 */
fun ProcessingEnvironment.findTypeElement(
    qName: String
): TypeElement? = elementUtils.getTypeElement(qName)

/**
 * Query a type element by KClass and throw if it does not exist
 */
fun ProcessingEnvironment.requireTypeElement(
    klass: KClass<*>
): TypeElement = requireTypeElement(klass.java.canonicalName!!)

/**
 * Query a type element by TypeName and throw if it does not exist
 */
fun ProcessingEnvironment.requireTypeElement(
    typeName: TypeName
): TypeElement = requireTypeElement(typeName.toString())

/**
 * Query a type element by qualified name and throw if it does not exist
 */
fun ProcessingEnvironment.requireTypeElement(
    qName: String
): TypeElement = checkNotNull(elementUtils.getTypeElement(qName)) {
    // we do not throw MissingTypeException here as this should be called only if the type should
    // be there
    "Couldn't find required type $qName"
}

/**
 * Query a TypeMirror by KClass and throw if it does not exist
 */
fun ProcessingEnvironment.requireTypeMirror(
    klass: KClass<*>
): TypeMirror = requireTypeMirror(klass.java.canonicalName!!)

/**
 * Query a TypeMirror by TypeName and throw if it does not exist
 */
fun ProcessingEnvironment.requireTypeMirror(
    typeName: TypeName
): TypeMirror = requireTypeMirror(typeName.toString())

/**
 * Query a TypeMirror by qualified name and throw if it does not exist
 */
fun ProcessingEnvironment.requireTypeMirror(
    qName: String
): TypeMirror = checkNotNull(findTypeMirror(qName)) {
    "couldn't find required type mirror $qName"
}

/**
 * Query a type mirror by KClass and return null if it does not exist
 */
fun ProcessingEnvironment.findTypeMirror(
    klass: KClass<*>
): TypeMirror? = findTypeMirror(klass.java.canonicalName!!)

/**
 * Query a type mirror by TypeName and return null if it does not exist
 */
fun ProcessingEnvironment.findTypeMirror(
    typeName: TypeName
): TypeMirror? = findTypeMirror(typeName.toString())

private val PRIMITIVE_TYPE_MAPPING = TypeKind.values().filter {
    it.isPrimitive
}.associateBy {
    it.name.toLowerCase(Locale.US)
}

/**
 * Query a type mirror by qualified name and return null if it does not exist
 */
fun ProcessingEnvironment.findTypeMirror(
    qName: String
): TypeMirror? {
    // first check primitives. Even though it is less likely, it is fast to check and can avoid a
    // call to the processor
    PRIMITIVE_TYPE_MAPPING[qName]?.let {
        return typeUtils.getPrimitiveType(it)
    }
    return findTypeElement(qName)?.type
}

fun ProcessingEnvironment.getGeneratedAnnotation(): TypeElement? {
    val element = GeneratedAnnotations.generatedAnnotation(elementUtils, sourceVersion)
    return if (element.isPresent) {
        element.get()
    } else {
        null
    }
}

fun ProcessingEnvironment.getArrayType(typeName: TypeName): ArrayType {
    return typeUtils.getArrayType(requireTypeMirror(typeName))
}
