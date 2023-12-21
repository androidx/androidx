/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build.lint.aidl

import androidx.com.android.tools.idea.lang.aidl.AidlFileType
import androidx.com.android.tools.idea.lang.aidl.AidlParserDefinition
import androidx.com.android.tools.idea.lang.aidl.psi.AidlDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlFile
import androidx.com.android.tools.idea.lang.aidl.psi.AidlInterfaceDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlMethodDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlParcelableDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlUnionDeclaration
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.OtherFileScanner
import com.android.tools.lint.detector.api.Scope
import com.intellij.core.CoreFileTypeRegistry
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import java.io.File

/**
 * Abstract class for detectors running against AIDL definitions (e.g. .aidl files).
 */
abstract class AidlDefinitionDetector : Detector(), OtherFileScanner {

    override fun getApplicableFiles() = Scope.OTHER_SCOPE

    override fun beforeCheckEachProject(context: Context) {
        val aidlFileType = AidlFileType.INSTANCE

        // We only need to register the language parser once per daemon process...
        LanguageParserDefinitions.INSTANCE.apply {
            synchronized(this) {
                val existingParsers = forLanguage(aidlFileType.language)
                if (existingParsers == null) {
                    addExplicitExtension(aidlFileType.language, AidlParserDefinition())
                }
            }
        }

        // ...but we need to register the file type every time we run Gradle.
        (CoreFileTypeRegistry.getInstance() as CoreFileTypeRegistry).apply {
            synchronized(this) {
                val existingFileType = getFileTypeByExtension(aidlFileType.defaultExtension)
                if (existingFileType == UnknownFileType.INSTANCE) {
                    registerFileType(aidlFileType, aidlFileType.defaultExtension)
                }
            }
        }
    }

    override fun run(context: Context) {
        if (context.file.extension == AidlFileType.DEFAULT_ASSOCIATED_EXTENSION) {
            ioFileToAidlFile(context, context.file)
                .allAidlDeclarations
                .forEach { declaration ->
                    visitAidlDeclaration(context, declaration)
                }
        }
    }

    private fun visitAidlDeclaration(context: Context, aidlDeclaration: AidlDeclaration) {
        when (aidlDeclaration) {
            is AidlInterfaceDeclaration ->
                visitAidlInterfaceDeclaration(context, aidlDeclaration)
            is AidlParcelableDeclaration ->
                visitAidlParcelableDeclaration(context, aidlDeclaration)
            is AidlUnionDeclaration -> {
                listOf(
                    aidlDeclaration.interfaceDeclarationList,
                    aidlDeclaration.parcelableDeclarationList,
                    aidlDeclaration.unionDeclarationList
                ).forEach { declarationList ->
                    declarationList.forEach { declaration ->
                        visitAidlDeclaration(context, declaration)
                    }
                }
            }
        }
    }

    /**
     * Visitor for `interface`s defined in AIDL.
     */
    open fun visitAidlInterfaceDeclaration(context: Context, node: AidlInterfaceDeclaration) {
        // Stub.
    }

    /**
     * Visitor for methods defined on `interface`s in AIDL.
     */
    open fun visitAidlMethodDeclaration(context: Context, node: AidlMethodDeclaration) {
        // Stub.
    }

    /**
     * Visitor for `parcelable`s defined in AIDL.
     */
    open fun visitAidlParcelableDeclaration(context: Context, node: AidlParcelableDeclaration) {
        // Stub.
    }
}

internal fun ioFileToAidlFile(context: Context, file: File): AidlFile {
    val vFile = CoreLocalFileSystem().findFileByIoFile(file)!!
    val psiManager = PsiManager.getInstance(context.project.ideaProject)
    val singleRootFileViewProvider = SingleRootFileViewProvider(
        psiManager, vFile, false, AidlFileType.INSTANCE
    )
    return AidlFile(singleRootFileViewProvider)
}

fun AidlDeclaration.getLocation() = Location.create(
    File(containingFile.viewProvider.virtualFile.path),
    containingFile.text,
    textRange.startOffset,
    textRange.endOffset
)
