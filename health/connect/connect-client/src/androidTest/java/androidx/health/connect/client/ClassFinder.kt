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

package androidx.health.connect.client

import androidx.health.connect.client.records.Record
import java.io.File
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
val RECORD_CLASSES: List<KClass<out Record>> by lazy {
    findClasses("androidx.health.connect.client.records")
        .filterNot { it.java.isInterface }
        .filter { it.simpleName.orEmpty().endsWith("Record") }
        .map { it as KClass<out Record> }
}

fun findClasses(packageName: String): Set<KClass<*>> {
    val resources =
        requireNotNull(Thread.currentThread().contextClassLoader)
            .getResources(packageName.replace('.', '/'))

    return buildSet {
        while (resources.hasMoreElements()) {
            val classNames = findClasses(resources.nextElement().file, packageName)
            for (className in classNames) {
                add(Class.forName(className).kotlin)
            }
        }
    }
}

private fun findClasses(directory: String, packageName: String): Set<String> = buildSet {
    if (directory.startsWith("file:") && ('!' in directory)) {
        addAll(unzipClasses(path = directory, packageName = packageName))
    }

    for (file in File(directory).takeIf(File::exists)?.listFiles() ?: emptyArray()) {
        if (file.isDirectory) {
            addAll(findClasses(file.absolutePath, "$packageName.${file.name}"))
        } else if (file.name.endsWith(".class")) {
            add("$packageName.${file.name.dropLast(6)}")
        }
    }
}

private fun unzipClasses(path: String, packageName: String): Set<String> =
    ZipInputStream(URL(path.substringBefore('!')).openStream()).use { zip ->
        buildSet {
            while (true) {
                val entry = zip.nextEntry ?: break
                val className = entry.formatClassName()
                if ((className != null) && className.startsWith(packageName)) {
                    add(className)
                }
            }
        }
    }

private fun ZipEntry.formatClassName(): String? =
    name
        .takeIf { it.endsWith(".class") }
        ?.replace("[$].*".toRegex(), "")
        ?.replace("[.]class".toRegex(), "")
        ?.replace('/', '.')
