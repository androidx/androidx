/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XNullability
import com.google.auto.common.MoreTypes
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.AbstractAnnotationValueVisitor8

internal class JavacAnnotationValue(
    val env: JavacProcessingEnv,
    val method: ExecutableElement,
    val annotationValue: AnnotationValue,
    private val valueProvider: () -> Any? = {
        UNWRAP_VISITOR.visit(annotationValue, VisitorData(env, method))
    }
) : XAnnotationValue {
    override val name: String
        get() = method.simpleName.toString()

    override val value: Any? by lazy { valueProvider.invoke() }
}

private data class VisitorData(val env: JavacProcessingEnv, val method: ExecutableElement)

private val UNWRAP_VISITOR = object : AbstractAnnotationValueVisitor8<Any?, VisitorData>() {
    override fun visitBoolean(b: Boolean, data: VisitorData) = b
    override fun visitByte(b: Byte, data: VisitorData) = b
    override fun visitChar(c: Char, data: VisitorData) = c
    override fun visitDouble(d: Double, data: VisitorData) = d
    override fun visitFloat(f: Float, data: VisitorData) = f
    override fun visitInt(i: Int, data: VisitorData) = i
    override fun visitLong(i: Long, data: VisitorData) = i
    override fun visitShort(s: Short, data: VisitorData) = s

    override fun visitString(s: String?, data: VisitorData) =
        if (s == "<error>") {
            throw TypeNotPresentException(s, null)
        } else {
            s
        }

    override fun visitType(t: TypeMirror, data: VisitorData): JavacType {
        if (t.kind == TypeKind.ERROR) {
            throw TypeNotPresentException(t.toString(), null)
        }
        return data.env.wrap(t, kotlinType = null, XNullability.NONNULL)
    }

    override fun visitEnumConstant(c: VariableElement, data: VisitorData): JavacEnumEntry {
        val type = c.asType()
        if (type.kind == TypeKind.ERROR) {
            throw TypeNotPresentException(type.toString(), null)
        }
        val enumTypeElement = MoreTypes.asTypeElement(type)
        return JavacEnumEntry(
            env = data.env,
            entryElement = c,
            enumTypeElement = JavacTypeElement.create(data.env, enumTypeElement) as XEnumTypeElement
        )
    }

    override fun visitAnnotation(a: AnnotationMirror, data: VisitorData) =
        JavacAnnotation(data.env, a)

    override fun visitArray(vals: MutableList<out AnnotationValue>, data: VisitorData) =
        vals.map { JavacAnnotationValue(data.env, data.method, it) { it.accept(this, data) } }
}