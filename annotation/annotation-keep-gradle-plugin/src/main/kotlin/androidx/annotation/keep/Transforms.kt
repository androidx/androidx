/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.annotation.keep

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.objectweb.asm.ClassReader

internal fun modifyAar(input: File, output: File) {
    val zipFile = ZipFile(input)
    val rulesAccumulator = StringBuilder()
    val outputStream = ZipOutputStream(output.outputStream().buffered())
    // Existing content
    var proguardTxt: String? = null
    outputStream.use {
        zipFile.use {
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                when (entry.name) {
                    "classes.jar" -> {
                        val classesJarOutputStream = ByteArrayOutputStream()
                        val jarInputStream = ZipInputStream(zipFile.getInputStream(entry))
                        jarTransform(
                            inputStream = jarInputStream,
                            outputStream = ZipOutputStream(classesJarOutputStream),
                            rulesAccumulator = rulesAccumulator,
                        )
                        outputStream.putNextEntry(ZipEntry("classes.jar"))
                        outputStream.write(classesJarOutputStream.toByteArray())
                        outputStream.closeEntry()
                    }
                    "proguard.txt" -> {
                        // Keep track of the proguard file, but don't write it yet.
                        proguardTxt = zipFile.getInputStream(entry).bufferedReader().readText()
                    }
                    else -> {
                        outputStream.putNextEntry(ZipEntry(entry.name))
                        zipFile.getInputStream(entry).copyTo(out = outputStream)
                        outputStream.closeEntry()
                    }
                }
            }
            // Add the proguard rules to root proguard.txt
            val outputProguardTxt = StringBuilder()
            if (proguardTxt != null) {
                with(outputProguardTxt) {
                    append("#################################################################\n")
                    append("#                     Preexisting AAR Rules                     #\n")
                    append("#################################################################\n")
                    append(proguardTxt)
                    append("\n")
                }
            }
            // Add the accumulated rules
            if (rulesAccumulator.isNotEmpty()) {
                with(outputProguardTxt) {
                    append("#################################################################\n")
                    append("#   Rules generated from androidx.annotation.keep annotations   #\n")
                    append("#################################################################\n")
                    append(rulesAccumulator)
                    append("\n")
                }
            }
            if (outputProguardTxt.isNotEmpty()) {
                outputStream.putNextEntry(ZipEntry("proguard.txt"))
                outputStream.write(outputProguardTxt.toString().toByteArray())
                outputStream.closeEntry()
            }
        }
    }
}

internal fun modifyJar(input: File, output: File) {
    val inputStream = ZipInputStream(input.inputStream().buffered())
    val outputStream = ZipOutputStream(output.outputStream().buffered())
    val rulesAccumulator = StringBuilder()
    jarTransform(
        inputStream = inputStream,
        outputStream = outputStream,
        rulesAccumulator = rulesAccumulator,
    )
}

internal fun jarTransform(
    inputStream: ZipInputStream,
    outputStream: ZipOutputStream,
    rulesAccumulator: StringBuilder,
) {
    // A map of entry names and content
    val entryContent = mutableMapOf<String, ByteArray>()
    var entry = inputStream.nextEntry
    while (entry != null) {
        val entryName = entry.name
        val content = inputStream.readBytes()
        if (entryName.endsWith(".class")) {
            val reader = ClassReader(content)
            val visitor = KeepAnnoStub.createClassVisitorForKeepRulesExtraction { rule ->
                rulesAccumulator.append(rule)
            }
            reader.accept(visitor, ClassReader.EXPAND_FRAMES)
            entryContent[entryName] = content
        } else {
            entryContent[entryName] = content
        }
        entry = inputStream.nextEntry
    }

    // https://developer.android.com/topic/performance/app-optimization/library-optimization
    outputStream.use { out ->
        val targetedR8Entry =
            entryContent.keys.find { it.startsWith("META-INF/com.android.tools/") }
        val proguardEntry = entryContent.keys.find { it.startsWith("META-INF/proguard/") }

        val targetRuleEntryName =
            targetedR8Entry ?: proguardEntry ?: "META-INF/com.android.tools/r8/proguard.txt"

        for ((name, bytes) in entryContent) {
            if ((name == targetRuleEntryName) && rulesAccumulator.isNotEmpty()) {
                val existingText = String(bytes)
                val combined = buildString {
                    append("#################################################################\n")
                    append("#                     Preexisting AAR Rules                     #\n")
                    append("#################################################################\n")
                    append(existingText)
                    append("\n")
                    append("#################################################################\n")
                    append("#   Rules generated from androidx.annotation.keep annotations   #\n")
                    append("#################################################################\n")
                    append(rulesAccumulator)
                    append("\n")
                }
                out.putNextEntry(ZipEntry(name))
                out.write(combined.toByteArray())
                out.closeEntry()
            } else {
                out.putNextEntry(ZipEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
        }

        if (!entryContent.containsKey(targetRuleEntryName) && rulesAccumulator.isNotEmpty()) {
            val combined = buildString {
                append("#################################################################\n")
                append("#   Rules generated from androidx.annotation.keep annotations   #\n")
                append("#################################################################\n")
                append(rulesAccumulator)
                append("\n")
            }
            out.putNextEntry(ZipEntry(targetRuleEntryName))
            out.write(combined.toByteArray())
            out.closeEntry()
        }
    }
}
