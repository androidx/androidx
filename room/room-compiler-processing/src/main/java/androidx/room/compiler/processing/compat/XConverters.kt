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

package androidx.room.compiler.processing.compat

import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XExecutableType
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XFiler
import androidx.room.compiler.processing.XMessager
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XRoundEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.XVariableElement
import androidx.room.compiler.processing.javac.JavacAnnotation
import androidx.room.compiler.processing.javac.JavacAnnotationValue
import androidx.room.compiler.processing.javac.JavacElement
import androidx.room.compiler.processing.javac.JavacExecutableElement
import androidx.room.compiler.processing.javac.JavacExecutableType
import androidx.room.compiler.processing.javac.JavacFiler
import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.javac.JavacProcessingEnvMessager
import androidx.room.compiler.processing.javac.JavacRoundEnv
import androidx.room.compiler.processing.javac.JavacType
import androidx.room.compiler.processing.javac.JavacTypeElement
import androidx.room.compiler.processing.javac.JavacVariableElement
import androidx.room.compiler.processing.ksp.KspAnnotation
import androidx.room.compiler.processing.ksp.KspAnnotationValue
import androidx.room.compiler.processing.ksp.KspElement
import androidx.room.compiler.processing.ksp.KspExecutableElement
import androidx.room.compiler.processing.ksp.KspExecutableParameterElement
import androidx.room.compiler.processing.ksp.KspExecutableType
import androidx.room.compiler.processing.ksp.KspFieldElement
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspType
import androidx.room.compiler.processing.ksp.KspTypeElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticContinuationParameterElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticReceiverParameterElement
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeMirror

// Migration APIs for converting between Javac, KSP, and XProcessing types.
object XConverters {
    @JvmStatic
    fun XProcessingEnv.toJavac(): ProcessingEnvironment = (this as JavacProcessingEnv).delegate

    @JvmStatic
    fun XRoundEnv.toJavac(): RoundEnvironment = (this as JavacRoundEnv).delegate

    @JvmStatic
    fun XMessager.toJavac(): Messager = (this as JavacProcessingEnvMessager).delegate

    @JvmStatic
    fun XFiler.toJavac(): Filer = (this as JavacFiler).delegate

    @JvmStatic
    fun XElement.toJavac(): Element = (this as JavacElement).element

    @JvmStatic
    fun XTypeElement.toJavac(): TypeElement = (this as JavacTypeElement).element

    @JvmStatic
    fun XExecutableElement.toJavac(): ExecutableElement = (this as JavacExecutableElement).element

    @JvmStatic
    fun XVariableElement.toJavac(): VariableElement = (this as JavacVariableElement).element

    @JvmStatic
    fun XAnnotation.toJavac(): AnnotationMirror = (this as JavacAnnotation).mirror

    @JvmStatic
    fun XAnnotationValue.toJavac(): AnnotationValue = (this as JavacAnnotationValue).annotationValue

    @JvmStatic
    fun XType.toJavac(): TypeMirror = (this as JavacType).typeMirror

    @JvmStatic
    fun XExecutableType.toJavac(): ExecutableType = (this as JavacExecutableType).executableType

    @JvmStatic
    fun Element.toXProcessing(env: XProcessingEnv): XElement {
        return when (this) {
            is TypeElement -> this.toXProcessing(env)
            is ExecutableElement -> this.toXProcessing(env)
            is VariableElement -> this.toXProcessing(env)
            else -> error(
                "Don't know how to convert element of type '${this::class}' to a XElement"
            )
        }
    }

    @JvmStatic
    fun TypeElement.toXProcessing(env: XProcessingEnv): XTypeElement =
        (env as JavacProcessingEnv).wrapTypeElement(this)

    @JvmStatic
    fun ExecutableElement.toXProcessing(env: XProcessingEnv): XExecutableElement =
        (env as JavacProcessingEnv).wrapExecutableElement(this)

    @JvmStatic
    fun VariableElement.toXProcessing(env: XProcessingEnv): XVariableElement =
        (env as JavacProcessingEnv).wrapVariableElement(this)

    @JvmStatic
    fun AnnotationMirror.toXProcessing(env: XProcessingEnv): XAnnotation =
        JavacAnnotation(env as JavacProcessingEnv, this)

    @JvmStatic
    fun AnnotationValue.toXProcessing(method: ExecutableElement, env: XProcessingEnv):
        XAnnotationValue = JavacAnnotationValue(
            env as JavacProcessingEnv, method.toXProcessing(env) as XMethodElement, this
        )

    @JvmStatic
    fun Filer.toXProcessing(env: XProcessingEnv): XFiler =
        JavacFiler(env as JavacProcessingEnv, this)

    @JvmStatic
    fun Messager.toXProcessing(): XMessager =
        JavacProcessingEnvMessager(this)

    /**
     * Returns an [XType] for the given [TypeMirror].
     *
     * Warning: This method should be used only for migration since the returned [XType] will be
     * missing nullability information. Calling [XType#nullability] on these types will result in
     * an [IllegalStateException].
     */
    @JvmStatic
    fun TypeMirror.toXProcessing(env: XProcessingEnv): XType =
        (env as JavacProcessingEnv).wrap(this, null, null)

    @JvmStatic
    fun XProcessingEnv.toKS(): SymbolProcessorEnvironment = (this as KspProcessingEnv).delegate

    @JvmStatic
    fun XProcessingEnv.toKSResolver(): Resolver = (this as KspProcessingEnv).resolver

    @JvmStatic
    fun XTypeElement.toKS(): KSClassDeclaration = (this as KspTypeElement).declaration

    @JvmStatic
    fun XElement.toKS(): KSAnnotated =
        when (this) {
            is KspElement -> this.declaration
            is KspSyntheticPropertyMethodElement -> this.field.declaration
            is KspSyntheticContinuationParameterElement -> this.enclosingElement.declaration
            is KspSyntheticReceiverParameterElement -> this.enclosingElement.declaration
            else -> error("Don't know how to convert element of type '${this::class}' to KSP")
        }

    @JvmStatic
    fun XExecutableElement.toKS(): KSFunctionDeclaration =
        when (this) {
            is KspExecutableElement -> this.declaration
            is KspSyntheticPropertyMethodElement ->
                error("Kotlin properties don't have getters/setters in KSP")
            else -> error("Don't know how to convert element of type '${this::class}' to KSP")
        }

    @JvmStatic
    fun XFieldElement.toKS(): KSPropertyDeclaration = (this as KspFieldElement).declaration

    @JvmStatic
    fun XExecutableParameterElement.toKS(): KSValueParameter =
        (this as KspExecutableParameterElement).declaration as KSValueParameter

    @JvmStatic
    fun XAnnotation.toKS(): KSAnnotation = (this as KspAnnotation).ksAnnotated

    @JvmStatic
    fun XAnnotationValue.toKS(): KSValueArgument = (this as KspAnnotationValue).valueArgument

    @JvmStatic
    fun XType.toKS(): KSType = (this as KspType).ksType

    @JvmStatic
    fun KSClassDeclaration.toXProcessing(env: XProcessingEnv): XTypeElement =
        (env as KspProcessingEnv).wrapClassDeclaration(this)

    @JvmStatic
    fun KSFunctionDeclaration.toXProcessing(env: XProcessingEnv): XExecutableElement =
        KspExecutableElement.create(env as KspProcessingEnv, this)

    @JvmStatic
    fun KSValueParameter.toXProcessing(env: XProcessingEnv): XVariableElement =
        KspExecutableParameterElement.create(env as KspProcessingEnv, this)

    @JvmStatic
    fun KSPropertyDeclaration.toXProcessing(env: XProcessingEnv): XVariableElement =
        KspFieldElement.create(env as KspProcessingEnv, this)

    @JvmStatic
    fun KSAnnotation.toXProcessing(env: XProcessingEnv): XAnnotation =
        KspAnnotation(env as KspProcessingEnv, this)

    @JvmStatic
    fun KSValueArgument.toXProcessing(env: XProcessingEnv): XAnnotationValue {
        val kspAnnotation = (this.parent as KSAnnotation).toXProcessing(env) as KspAnnotation
        return KspAnnotationValue(
            env as KspProcessingEnv,
            kspAnnotation,
            kspAnnotation.type,
            this
        )
    }

    @JvmStatic
    fun KSType.toXProcessing(env: XProcessingEnv): XType =
        (env as KspProcessingEnv).wrap(this, true)

    @Deprecated("This will be removed in a future version of XProcessing.")
    @JvmStatic
    fun XType.getProcessingEnv(): XProcessingEnv {
        return when (this) {
            is JavacType -> this.env
            is KspType -> this.env
            else -> error("Unexpected type: $this")
        }
    }

    @Deprecated("This will be removed in a future version of XProcessing.")
    @JvmStatic
    fun XElement.getProcessingEnv(): XProcessingEnv {
        return when (this) {
            is JavacElement -> this.env
            is KspElement -> this.env
            is KspSyntheticContinuationParameterElement -> this.env
            is KspSyntheticPropertyMethodElement -> this.env
            is KspSyntheticReceiverParameterElement -> this.env
            else -> error("Unexpected element: $this")
        }
    }

    @Deprecated("This will be removed in a future version of XProcessing.")
    @JvmStatic
    fun XAnnotation.getProcessingEnv(): XProcessingEnv {
        return when (this) {
            is JavacAnnotation -> this.env
            is KspAnnotation -> this.env
            else -> error("Unexpected annotation type: $this")
        }
    }

    @Deprecated("This will be removed in a future version of XProcessing.")
    @JvmStatic
    fun XAnnotationValue.getProcessingEnv(): XProcessingEnv {
        return when (this) {
            is JavacAnnotationValue -> this.env
            is KspAnnotationValue -> this.env
            else -> error("Unexpected annotation value: $this")
        }
    }

    @Deprecated("This will be removed in a future version of XProcessing.")
    @JvmStatic
    fun XExecutableType.getProcessingEnv(): XProcessingEnv {
        return when (this) {
            is JavacExecutableType -> this.env
            is KspExecutableType -> this.env
            else -> error("Unexpected executable type: $this")
        }
    }
}
