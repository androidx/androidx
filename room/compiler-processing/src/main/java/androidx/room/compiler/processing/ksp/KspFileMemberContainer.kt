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

import androidx.room.compiler.processing.XAnnotated
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.javapoet.ClassName

/**
 * [XMemberContainer] implementation for KSFiles.
 */
internal class KspFileMemberContainer(
    private val env: KspProcessingEnv,
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
    override val className: ClassName by lazy {

        val pkgName = ksFile.packageName.asString().let {
            if (it == "<root>") {
                ""
            } else {
                it
            }
        }
        ClassName.get(
            pkgName, ksFile.findClassName()
        )
    }

    override fun kindName(): String {
        return "file"
    }

    override val fallbackLocationText: String = ksFile.filePath

    override val docComment: String? by lazy {
        // TODO: Not yet implemented in KSP.
        // https://github.com/google/ksp/issues/392
        null
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
}