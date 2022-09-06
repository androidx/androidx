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

import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.Method
import androidx.privacysandbox.tools.core.Parameter
import androidx.privacysandbox.tools.core.ParsedApi
import androidx.privacysandbox.tools.core.Type
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmType
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.ClassReader.SKIP_DEBUG
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type.getDescriptor
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

internal object ApiStubParser {
    /**
     * Parses the API annotated by a Privacy Sandbox SDK from its compiled classes.
     *
     * @param sdkInterfaceDescriptors Path to SDK interface descriptors. This should be a jar
     *      file with a set of compiled SDK classes and at least one of them should be a valid
     *      Kotlin interface annotated with @PrivacySandboxService.
     */
    internal fun parse(sdkInterfaceDescriptors: Path): ParsedApi {
        val services = unzipClasses(sdkInterfaceDescriptors)
            .filter { it.isPrivacySandboxService }
            .map(::parseClass)
            .toSet()
        if (services.isEmpty()) throw IllegalArgumentException(
            "Unable to find valid interfaces annotated with @PrivacySandboxService."
        )
        return ParsedApi(services)
    }

    private fun unzipClasses(stubClassPath: Path): List<ClassNode> =
        ZipInputStream(stubClassPath.toFile().inputStream()).use { zipInputStream ->
            buildList {
                var zipEntry: ZipEntry? = zipInputStream.nextEntry
                while (zipEntry != null) {
                    if (zipEntry.name.endsWith(".class")) {
                        add(toClassNode(zipInputStream.readBytes()))
                    }
                    zipEntry = zipInputStream.nextEntry
                }
            }
        }

    private fun toClassNode(classContents: ByteArray): ClassNode {
        val reader = ClassReader(classContents)
        val classNode = ClassNode(Opcodes.ASM9)
        reader.accept(classNode, SKIP_CODE or SKIP_DEBUG or SKIP_FRAMES)
        return classNode
    }

    private fun parseClass(classNode: ClassNode): AnnotatedInterface {
        val kotlinMetadata = parseKotlinMetadata(classNode)

        // Package names are separated with slashes and nested classes are separated with dots.
        // (e.g com/example/OuterClass.InnerClass).
        val (packageName, className) = kotlinMetadata.name.split('/').run {
            dropLast(1).joinToString(separator = ".") to last()
        }

        if (!Flag.Class.IS_INTERFACE(kotlinMetadata.flags)) {
            throw IllegalArgumentException(
                "$packageName.$className is not a Kotlin interface but it's annotated with " +
                    "@PrivacySandboxService."
            )
        }

        if (className.contains('.')) {
            throw IllegalArgumentException(
                "$packageName.$className is an inner interface so it can't be annotated with " +
                    "@PrivacySandboxService."
            )
        }

        return AnnotatedInterface(
            className,
            packageName,
            kotlinMetadata.functions.map(this::parseMethod),
        )
    }

    private fun parseKotlinMetadata(classNode: ClassNode): KmClass {
        val metadataValues =
            classNode.visibleAnnotationsWithType<Metadata>().firstOrNull()?.attributeMap
                ?: throw IllegalArgumentException(
                    "Missing Kotlin metadata annotation in ${classNode.name}. " +
                        "Is this a valid Kotlin class?"
                )

        // ASM models annotation attributes as flat List<Objects>, so the unchecked cast is
        // inevitable when some of these objects have type parameters, like the lists below.
        @Suppress("UNCHECKED_CAST")
        val header = KotlinClassHeader(
            kind = metadataValues["k"] as Int?,
            metadataVersion = (metadataValues["mv"] as? List<Int>?)?.toIntArray(),
            data1 = (metadataValues["d1"] as? List<String>?)?.toTypedArray(),
            data2 = (metadataValues["d2"] as? List<String>?)?.toTypedArray(),
            extraInt = metadataValues["xi"] as? Int?,
            packageName = metadataValues["pn"] as? String?,
            extraString = metadataValues["xs"] as? String?,
        )

        return when (val metadata = KotlinClassMetadata.read(header)) {
            is KotlinClassMetadata.Class -> metadata.toKmClass()
            else -> throw IllegalArgumentException(
                "Unable to parse Kotlin metadata from ${classNode.name}. " +
                    "Is this a valid Kotlin class?"
            )
        }
    }

    private fun parseMethod(function: KmFunction): Method {
        return Method(
            function.name,
            function.valueParameters.map { Parameter(it.name, parseType(it.type)) },
            parseType(function.returnType),
        )
    }

    private fun parseType(type: KmType): Type {
        return when (val classifier = type.classifier) {
            is KmClassifier.Class -> Type(classifier.name.replace('/', '.'))
            else -> throw IllegalArgumentException(
                "Unsupported type in API description: $type"
            )
        }
    }
}

val ClassNode.isPrivacySandboxService: Boolean get() {
    return visibleAnnotationsWithType<PrivacySandboxService>().isNotEmpty()
}

inline fun <reified T> ClassNode.visibleAnnotationsWithType(): List<AnnotationNode> {
    return (visibleAnnotations ?: listOf<AnnotationNode>())
        .filter { getDescriptor(T::class.java) == it?.desc }
        .filterNotNull()
}

/** Map of annotation attributes. This is a convenience wrapper around [AnnotationNode.values]. */
val AnnotationNode.attributeMap: Map<String, Any>
    get() {
        values ?: return mapOf()
        val attributes = mutableMapOf<String, Any>()
        for (i in 0 until values.size step 2) {
            attributes[values[i] as String] = values[i + 1]
        }
        return attributes
    }
