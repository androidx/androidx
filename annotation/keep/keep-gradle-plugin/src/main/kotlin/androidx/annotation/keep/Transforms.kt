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

import com.android.tools.r8.keepanno.KeepAnno
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.objectweb.asm.ClassReader

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
            val visitor = KeepAnno.createClassVisitorForKeepRulesExtraction { rule ->
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
        val sortedRules = sortKeepRules(rulesAccumulator)

        for ((name, bytes) in entryContent) {
            if ((name == targetRuleEntryName) && sortedRules.isNotEmpty()) {
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
                    append(sortedRules)
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

        if (!entryContent.containsKey(targetRuleEntryName) && sortedRules.isNotEmpty()) {
            val combined = buildString {
                append("#################################################################\n")
                append("#   Rules generated from androidx.annotation.keep annotations   #\n")
                append("#################################################################\n")
                append(sortedRules)
                append("\n")
            }
            out.putNextEntry(ZipEntry(targetRuleEntryName))
            out.write(combined.toByteArray())
            out.closeEntry()
        }
    }
}

/**
 * Splits raw KeepAnno output into atomic rule blocks (each consisting of leading `#` comment lines
 * followed by one or more ProGuard rule lines) and sorts them lexicographically so generated rule
 * files are byte-for-byte deterministic regardless of filesystem traversal order or internal
 * hash-collection iteration order in KeepAnno.
 */
internal fun sortKeepRules(rawRules: CharSequence): String {
    if (rawRules.isEmpty()) return ""
    val blocks = mutableListOf<String>()
    val currentBlock = StringBuilder()
    var hasRule = false
    for (line in rawRules.lineSequence()) {
        if (line.isBlank()) continue
        val isComment = line.trimStart().startsWith("#")
        if (isComment && hasRule) {
            blocks.add(currentBlock.toString())
            currentBlock.setLength(0)
            hasRule = false
        }
        if (!isComment) {
            hasRule = true
        }
        currentBlock.append(line).append('\n')
    }
    if (currentBlock.isNotEmpty()) {
        blocks.add(currentBlock.toString())
    }
    blocks.sort()
    return blocks.joinToString("")
}
