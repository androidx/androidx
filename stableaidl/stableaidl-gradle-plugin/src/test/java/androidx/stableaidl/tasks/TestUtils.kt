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

package androidx.stableaidl.tasks

import java.io.File
import java.util.zip.ZipFile

internal fun createFile(name: String, parent: File): File {
    val newFile = parent.resolve(name)
    newFile.parentFile.mkdirs()
    newFile.createNewFile()
    return newFile
}

internal fun ZipFile.getEntryNames(): List<String> {
    val flattened = mutableListOf<String>()
    entries().iterator().forEach { entry -> flattened += entry.name }
    return flattened
}
