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

package androidx.privacysandbox.tools.apigenerator.parser

import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.tools.core.PrivacySandboxParsingException
import androidx.privacysandbox.tools.core.model.Constant
import androidx.privacysandbox.tools.core.model.Types
import java.nio.file.Path
import kotlin.metadata.KmClass
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

data class AnnotatedClasses(
    val services: Set<ClassAndConstants>,
    val values: Set<ClassAndConstants>,
    val callbacks: Set<ClassAndConstants>,
    val interfaces: Set<ClassAndConstants>,
)

data class ClassAndConstants(
    val kClass: KmClass,
    val constants: List<Constant>,
)

internal object AnnotatedClassReader {
    val annotations = listOf(PrivacySandboxService::class)

    fun readAnnotatedClasses(stubClassPath: Path): AnnotatedClasses {
        val services = mutableSetOf<ClassAndConstants>()
        val values = mutableSetOf<ClassAndConstants>()
        val callbacks = mutableSetOf<ClassAndConstants>()
        val interfaces = mutableSetOf<ClassAndConstants>()

        stubClassPath
            .toFile()
            .walk()
            .filter { it.isFile && it.extension == "class" }
            .map { toClassNode(it.readBytes()) }
            .forEach { classNode ->
                // Data classes and enum classes store their constants on the object itself, rather
                // than in the companion's class file, so we extract the constants from amongst the
                // other fields on the annotated value/interface.
                // Thankfully, data class fields are always non-static, and enum variants are always
                // of the enum's type (hence not primitive or string, which consts must be).
                // The const-allowed-types check also filters out the Companion and the VALUES
                // array.
                val constants =
                    classNode.fields
                        .filter { it.access.hasFlag(PUBLIC_STATIC_FINAL_ACCESS) }
                        .filter { it.desc in constAllowedTypes.keys }
                        .map { Constant(it.name, getConstType(it.desc), it.value) }
                        .toList()
                if (classNode.isAnnotatedWith<PrivacySandboxService>()) {
                    services.add(ClassAndConstants(parseKotlinMetadata(classNode), constants))
                }
                // TODO(b/323369085): Validate that enum variants don't have methods
                if (classNode.isAnnotatedWith<PrivacySandboxValue>()) {
                    values.add(ClassAndConstants(parseKotlinMetadata(classNode), constants))
                }
                if (classNode.isAnnotatedWith<PrivacySandboxCallback>()) {
                    callbacks.add(ClassAndConstants(parseKotlinMetadata(classNode), constants))
                }
                if (classNode.isAnnotatedWith<PrivacySandboxInterface>()) {
                    interfaces.add(ClassAndConstants(parseKotlinMetadata(classNode), constants))
                }
            }
        return AnnotatedClasses(
            services = services.toSet(),
            values = values.toSet(),
            callbacks = callbacks.toSet(),
            interfaces = interfaces.toSet()
        )
    }

    private fun toClassNode(classContents: ByteArray): ClassNode {
        val reader = ClassReader(classContents)
        val classNode = ClassNode(Opcodes.ASM9)
        reader.accept(
            classNode,
            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
        )
        return classNode
    }

    private fun parseKotlinMetadata(classNode: ClassNode): KmClass {
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

    private inline fun <reified T> ClassNode.isAnnotatedWith(): Boolean {
        return visibleAnnotationsWithType<T>().isNotEmpty()
    }

    private inline fun <reified T> ClassNode.visibleAnnotationsWithType(): List<AnnotationNode> {
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

    private val constAllowedTypes =
        mapOf(
            "Ljava/lang/String;" to Types.string,
            "I" to Types.int,
            "Z" to Types.boolean,
            "B" to Types.byte,
            "C" to Types.char,
            "D" to Types.double,
            "F" to Types.float,
            "J" to Types.long,
            "S" to Types.short
        )

    private fun getConstType(desc: String): androidx.privacysandbox.tools.core.model.Type {
        return constAllowedTypes[desc]
            ?: throw PrivacySandboxParsingException("Unrecognised constant type: '$desc'")
    }
}

// See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.5
// TODO: Once we upgrade to Java 22 we can import these constants from
//  java.lang.classfile
private const val PUBLIC_STATIC_FINAL_ACCESS =
    0x0001 or // public
        0x0008 or // static
        0x0010 // final

private fun Int.hasFlag(flag: Int) = flag and this == flag
