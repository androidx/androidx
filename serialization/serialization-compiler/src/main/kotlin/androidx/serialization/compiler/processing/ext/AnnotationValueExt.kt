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

package androidx.serialization.compiler.processing.ext

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.util.SimpleAnnotationValueVisitor7

internal fun AnnotationValue.asInt(): Int {
    return accept(AsIntAnnotationValueVisitor, null)
}

private object AsIntAnnotationValueVisitor : AsTypeAnnotationValueVisitor<Int>("int") {
    override fun visitInt(i: Int, p: Nothing?): Int {
        return i
    }
}

internal fun AnnotationValue.asString(): String {
    return accept(AsStringAnnotationValueVisitor, null)
}

private object AsStringAnnotationValueVisitor : AsTypeAnnotationValueVisitor<String>("String") {
    override fun visitString(s: String, p: Nothing?): String {
        return s
    }
}

internal fun AnnotationValue.asList(): List<AnnotationValue> {
    return accept(AsListAnnotationValueVisitor, null)
}

private object AsListAnnotationValueVisitor :
    AsTypeAnnotationValueVisitor<List<AnnotationValue>>("List<AnnotationValue>") {

    override fun visitArray(vals: List<AnnotationValue>, p: Nothing?): List<AnnotationValue> {
        return vals
    }
}

internal fun AnnotationValue.asAnnotationMirror(): AnnotationMirror {
    return accept(AsAnnotationMirrorAnnotationValueVisitor, null)
}

private object AsAnnotationMirrorAnnotationValueVisitor :
    AsTypeAnnotationValueVisitor<AnnotationMirror>("AnnotationMirror") {

    override fun visitAnnotation(a: AnnotationMirror, p: Nothing?): AnnotationMirror {
        return a
    }
}

private abstract class AsTypeAnnotationValueVisitor<T>(
    private val expectedType: String
) : SimpleAnnotationValueVisitor7<T, Nothing?>() {
    override fun defaultAction(o: Any, p: Nothing?): T {
        throw IllegalArgumentException(
            "Expected annotation value of type $expectedType, got ${o::class.java.canonicalName}"
        )
    }
}
