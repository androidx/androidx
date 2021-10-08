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
    val element: ExecutableElement,
    val annotationValue: AnnotationValue
) : XAnnotationValue {
    override val name: String
        get() = element.simpleName.toString()

    override val value: Any? by lazy { UNWRAP_VISITOR.visit(annotationValue, env) }
}

private val UNWRAP_VISITOR = object : AbstractAnnotationValueVisitor8<Any?, JavacProcessingEnv>() {
    override fun visitBoolean(b: Boolean, env: JavacProcessingEnv) = b
    override fun visitByte(b: Byte, env: JavacProcessingEnv) = b
    override fun visitChar(c: Char, env: JavacProcessingEnv) = c
    override fun visitDouble(d: Double, env: JavacProcessingEnv) = d
    override fun visitFloat(f: Float, env: JavacProcessingEnv) = f
    override fun visitInt(i: Int, env: JavacProcessingEnv) = i
    override fun visitLong(i: Long, env: JavacProcessingEnv) = i
    override fun visitShort(s: Short, env: JavacProcessingEnv) = s

    override fun visitString(s: String?, env: JavacProcessingEnv) =
        if (s == "<error>") {
            throw TypeNotPresentException(s, null)
        } else {
            s
        }

    override fun visitType(t: TypeMirror, env: JavacProcessingEnv): JavacType {
        if (t.kind == TypeKind.ERROR) {
            throw TypeNotPresentException(t.toString(), null)
        }
        return env.wrap(t, kotlinType = null, XNullability.NONNULL)
    }

    override fun visitEnumConstant(c: VariableElement, env: JavacProcessingEnv): JavacEnumEntry {
        val type = c.asType()
        if (type.kind == TypeKind.ERROR) {
            throw TypeNotPresentException(type.toString(), null)
        }
        val enumTypeElement = MoreTypes.asTypeElement(type)
        return JavacEnumEntry(
            env = env,
            entryElement = c,
            enumTypeElement = JavacTypeElement.create(env, enumTypeElement) as XEnumTypeElement
        )
    }

    override fun visitAnnotation(a: AnnotationMirror, env: JavacProcessingEnv) =
        JavacAnnotation(env, a)

    override fun visitArray(vals: MutableList<out AnnotationValue>, env: JavacProcessingEnv) =
        vals.map { it.accept(this, env) }
}