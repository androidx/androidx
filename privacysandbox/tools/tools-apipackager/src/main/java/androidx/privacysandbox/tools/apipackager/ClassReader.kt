/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.privacysandbox.tools.core.PrivacySandboxParsingException
import kotlin.metadata.KmClass
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

// TODO(b/367361746): Move this object to tools-core to share code with the apigenerator.
internal object ClassReader {
    internal fun toClassNode(classContents: ByteArray): ClassNode {
        val reader = ClassReader(classContents)
        val classNode = ClassNode(Opcodes.ASM9)
        reader.accept(
            classNode,
            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
        )
        return classNode
    }

    internal fun parseKotlinMetadata(classNode: ClassNode): KmClass {
        val metadataValues =
            classNode.visibleAnnotationsWithType<Metadata>().firstOrNull()?.attributeMap
                ?: throw PrivacySandboxParsingException(
                    "Missing Kotlin metadata annotation in ${classNode.name}. " +
                        "Is this a valid Kotlin class?"
                )

        // ASM models annotation attributes as flat List<Objects>, so the unchecked cast is
        // inevitable when some of these objects have type parameters, like the lists below.
        @Suppress("UNCHECKED_CAST")
        val metadataAnnotation =
            Metadata(
                kind = metadataValues["k"] as Int?,
                metadataVersion = (metadataValues["mv"] as? List<Int>?)?.toIntArray(),
                data1 = (metadataValues["d1"] as? List<String>?)?.toTypedArray(),
                data2 = (metadataValues["d2"] as? List<String>?)?.toTypedArray(),
                extraInt = metadataValues["xi"] as? Int?,
                packageName = metadataValues["pn"] as? String?,
                extraString = metadataValues["xs"] as? String?,
            )

        return when (val metadata = KotlinClassMetadata.readStrict(metadataAnnotation)) {
            is KotlinClassMetadata.Class -> metadata.kmClass
            else ->
                throw PrivacySandboxParsingException(
                    "Unable to parse Kotlin metadata from ${classNode.name}. " +
                        "Is this a valid Kotlin class?"
                )
        }
    }

    internal inline fun <reified T> ClassNode.isAnnotatedWith(): Boolean {
        return visibleAnnotationsWithType<T>().isNotEmpty()
    }

    internal inline fun <reified T> ClassNode.visibleAnnotationsWithType(): List<AnnotationNode> {
        return (visibleAnnotations ?: listOf<AnnotationNode>())
            .filter { Type.getDescriptor(T::class.java) == it?.desc }
            .filterNotNull()
    }

    /**
     * Map of annotation attributes. This is a convenience wrapper around [AnnotationNode.values].
     */
    private val AnnotationNode.attributeMap: Map<String, Any>
        get() {
            values ?: return mapOf()
            val attributes = mutableMapOf<String, Any>()
            for (i in 0 until values.size step 2) {
                attributes[values[i] as String] = values[i + 1]
            }
            return attributes
        }
}
