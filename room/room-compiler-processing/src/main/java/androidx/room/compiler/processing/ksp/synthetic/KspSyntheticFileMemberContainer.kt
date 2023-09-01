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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationBox
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.ksp.KspMemberContainer
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspType
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.javapoet.ClassName
import com.squareup.kotlinpoet.javapoet.toKClassName
import kotlin.reflect.KClass

/**
 * When a top level function/member is compiled, the generated Java class does not exist in KSP.
 *
 * This wrapper synthesizes one from the JVM binary name
 *
 * https://docs.oracle.com/javase/specs/jls/se7/html/jls-13.html#jls-13.1
 */
internal class KspSyntheticFileMemberContainer(
    internal val env: KspProcessingEnv,
    private val binaryName: String
) : KspMemberContainer, XEquality {
    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(binaryName)
    }

    override val type: KspType?
        get() = null

    override val declaration: KSDeclaration?
        get() = null

    @Deprecated(
        "Use asClassName().toJavaPoet() to be clear the name is for JavaPoet.",
        replaceWith = ReplaceWith(
            "asClassName().toJavaPoet()",
            "androidx.room.compiler.codegen.toJavaPoet"
        )
    )
    override val className: ClassName by lazy {
        xClassName.java
    }

    private val xClassName: XClassName by lazy {
        val packageName = binaryName.substringBeforeLast(
            delimiter = '.',
            missingDelimiterValue = ""
        )
        val shortNames = if (packageName == "") {
            binaryName
        } else {
            binaryName.substring(packageName.length + 1)
        }.split('$')
        val java = ClassName.get(
            packageName,
            shortNames.first(),
            *shortNames.drop(1).toTypedArray()
        )
        // Even though the generated Java class is not referencable from Kotlin code, instead of
        // using 'Unavailable', for parity we use the same JavaPoet name for KotlinPoet,
        val kotlin = java.toKClassName()
        XClassName(java, kotlin, XNullability.NONNULL)
    }

    override fun asClassName() = xClassName

    override fun kindName(): String {
        return "synthethic top level file"
    }

    override val fallbackLocationText: String
        get() = binaryName

    override val docComment: String?
        get() = null

    override val enclosingElement: XElement?
        get() = null

    override val closestMemberContainer: KspSyntheticFileMemberContainer
        get() = this

    override fun validate(): Boolean {
        return true
    }

    override fun <T : Annotation> getAnnotations(annotation: KClass<T>): List<XAnnotationBox<T>> {
        return emptyList()
    }

    override fun getAllAnnotations(): List<XAnnotation> {
        return emptyList()
    }

    override fun hasAnnotation(annotation: KClass<out Annotation>): Boolean {
        return false
    }

    override fun hasAnnotationWithPackage(pkg: String): Boolean {
        return false
    }

    override fun isFromJava(): Boolean = false

    override fun isFromKotlin(): Boolean = true
}
