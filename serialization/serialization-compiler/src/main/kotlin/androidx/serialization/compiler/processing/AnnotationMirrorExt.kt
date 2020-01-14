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

package androidx.serialization.compiler.processing

import com.google.auto.common.AnnotationMirrors
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.util.SimpleAnnotationValueVisitor6
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaGetter

/** Get the annotation value for [property] using [AnnotationMirrors.getAnnotationValue] */
internal fun AnnotationMirror.getAnnotationValue(
    property: KProperty1<out Annotation, *>
): AnnotationValue {
    return AnnotationMirrors.getAnnotationValue(this, property.javaGetter!!.name)
}

/** Read a boolean value for [property]. */
internal operator fun AnnotationMirror.get(
    property: KProperty1<out Annotation, Boolean>
): Boolean {
    return BooleanValueVisitor.visit(getAnnotationValue(property), property)
}

/** Read an int value for [property]. */
internal operator fun AnnotationMirror.get(property: KProperty1<out Annotation, Int>): Int {
    return IntValueVisitor.visit(getAnnotationValue(property), property)
}

/** Read a string value for [property]. */
internal operator fun AnnotationMirror.get(property: KProperty1<out Annotation, String>): String {
    return StringValueVisitor.visit(getAnnotationValue(property), property)
}

/** Read an `Array<Annotation>` property as a list of annotation mirrors. */
internal fun AnnotationMirror.getAnnotationArray(
    property: KProperty1<out Annotation, Array<out Annotation>>
): List<AnnotationMirror> {
    return ArrayValueVisitor.visit(getAnnotationValue(property), property)
        .map { AnnotationMirrorValueVisitor.visit(it, property) }
}

/** Read an int array value for [property]. */
internal operator fun AnnotationMirror.get(
    property: KProperty1<out Annotation, IntArray>
): IntArray {
    val values = ArrayValueVisitor.visit(getAnnotationValue(property), property)
    return IntArray(values.size) { IntValueVisitor.visit(values[it], property) }
}

/** Read a string array value for [property]. */
internal operator fun AnnotationMirror.get(
    property: KProperty1<out Annotation, Array<String>>
): Array<String> {
    val values = ArrayValueVisitor.visit(getAnnotationValue(property), property)
    return Array(values.size) { StringValueVisitor.visit(values[it], property) }
}

/** Annotation value visitor that throws an exception by default. */
private abstract class TypedValueVisitor<T : Any>(
    private val expectedType: String
) : SimpleAnnotationValueVisitor6<T, KProperty1<out Annotation, *>>() {
    override fun defaultAction(o: Any, p: KProperty1<out Annotation, *>): T {
        throw IllegalArgumentException(
            "Expected annotation property ${p.name} to have type $expectedType but " +
                    "got ${o::class.qualifiedName}"
        )
    }
}

/** Annotation value visitor that reads an array property as a list of annotation values. */
private object ArrayValueVisitor : TypedValueVisitor<List<AnnotationValue>>(
    "List<AnnotationValue>"
) {
    override fun visitArray(
        vals: List<AnnotationValue>,
        p: KProperty1<out Annotation, *>?
    ): List<AnnotationValue> {
        return vals
    }
}

/** Annotation value visitor that reads an annotation property as an annotation mirror. */
private object AnnotationMirrorValueVisitor : TypedValueVisitor<AnnotationMirror>(
    "AnnotationMirror"
) {
    override fun visitAnnotation(
        a: AnnotationMirror,
        p: KProperty1<out Annotation, *>?
    ): AnnotationMirror {
        return a
    }
}

/** Annotation value visitor that reads a boolean property. */
private object BooleanValueVisitor : TypedValueVisitor<Boolean>("Boolean") {
    override fun visitBoolean(b: Boolean, p: KProperty1<out Annotation, *>?): Boolean {
        return b
    }
}

/** Annotation value visitor that reads an int property. */
private object IntValueVisitor : TypedValueVisitor<Int>("Int") {
    override fun visitInt(i: Int, p: KProperty1<out Annotation, *>?): Int {
        return i
    }
}

/** Annotation value visitor that reads a string property. */
private object StringValueVisitor : TypedValueVisitor<String>("String") {
    override fun visitString(s: String, p: KProperty1<out Annotation, *>?): String {
        return s
    }
}
