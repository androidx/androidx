/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.archive

import java.io.OutputStream
import java.nio.file.Path

/**
 * Abstraction to represent archive and its files as a one thing.
 */
interface ArchiveItem {

    /**
     * Relative path of the item according to its location in the archive.
     *
     * Files in a nested archive have a path relative to that archive not to the parent of
     * the archive. The root archive has the file system path set as its relative path.
     */
    val relativePath : Path

    /**
     * Name of the file.
     */
    val fileName : String

    /**
     * Accepts visitor.
     */
    fun accept(visitor: ArchiveItemVisitor)

    /**
     * Writes its internal data (or other nested files) into the given output stream.
     */
    fun writeSelfTo(outputStream: OutputStream)


    fun isPomFile() = fileName.equals("pom.xml", ignoreCase = true)

    fun isClassFile() = fileName.endsWith(".class", ignoreCase = true)

    fun isXmlFile() = fileName.endsWith(".xml", ignoreCase = true)

}