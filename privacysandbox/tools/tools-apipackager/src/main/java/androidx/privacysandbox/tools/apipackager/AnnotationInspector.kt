/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apipackager

import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.tools.internal.GeneratedPublicApi
import java.nio.file.Path
import kotlin.io.path.readBytes
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

internal object AnnotationInspector {
    private val privacySandboxAnnotations =
        setOf(
            PrivacySandboxCallback::class,
            PrivacySandboxInterface::class,
            PrivacySandboxService::class,
            PrivacySandboxValue::class,
            GeneratedPublicApi::class,
        )

    fun hasPrivacySandboxAnnotation(classFile: Path): Boolean {
        val reader = ClassReader(classFile.readBytes())
        val annotationExtractor = AnnotationExtractor()
        reader.accept(
            annotationExtractor,
            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
        )
        return annotationExtractor.hasPrivacySandboxAnnotation
    }

    private class AnnotationExtractor : ClassVisitor(Opcodes.ASM9) {
        private val privacySandboxAnnotationDescriptors =
            privacySandboxAnnotations.map { Type.getDescriptor(it.java) }.toSet()
        var hasPrivacySandboxAnnotation = false
            private set

        override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
            if (!hasPrivacySandboxAnnotation) {
                descriptor?.let {
                    hasPrivacySandboxAnnotation = privacySandboxAnnotationDescriptors.contains(it)
                }
            }
            return super.visitAnnotation(descriptor, visible)
        }
    }
}
