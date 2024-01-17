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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XNullability
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate
import com.squareup.javapoet.ClassName
import com.squareup.kotlinpoet.javapoet.toKClassName

/**
 * [XMemberContainer] implementation for KSFiles.
 */
internal class KspFileMemberContainer(
    internal val env: KspProcessingEnv,
    private val ksFile: KSFile
) : KspMemberContainer,
    XAnnotated by KspAnnotated.create(
        env = env,
        delegate = ksFile,
        filter = KspAnnotated.UseSiteFilter.FILE
    ) {
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
        val pkgName = ksFile.packageName.asString().let {
            if (it == "<root>") {
                ""
            } else {
                it
            }
        }
        val java = ClassName.get(
            pkgName, ksFile.findClassName()
        )
        val kotlin = java.toKClassName()
        XClassName(java, kotlin, XNullability.NONNULL)
    }

    override fun asClassName() = xClassName

    override fun kindName(): String {
        return "file"
    }

    override val fallbackLocationText: String = ksFile.filePath

    override val docComment: String?
        get() = null

    override val enclosingElement: XElement?
        get() = null

    override val closestMemberContainer: KspFileMemberContainer
        get() = this

    override fun validate(): Boolean {
        return ksFile.validate()
    }

    companion object {
        private fun KSFile.findClassName(): String {
            return annotations.firstOrNull {
                it.useSiteTarget == AnnotationUseSiteTarget.FILE &&
                    it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                    JvmName::class.qualifiedName
            }?.arguments?.firstOrNull {
                it.name?.asString() == "name"
            }?.value?.toString() ?: fileName.replace(".kt", "Kt")
        }
    }

    override fun isFromJava(): Boolean = false

    override fun isFromKotlin(): Boolean = true
}
