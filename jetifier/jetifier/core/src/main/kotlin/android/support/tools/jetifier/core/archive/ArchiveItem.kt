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


/**
 * Abstraction to represent archive and its files as a one thing.
 */
interface ArchiveItem {

    /**
     * Relative path of the item according to its location in the archive. For instance the root
     * archive has '/' as a relative path. Files in a nested archive have a path relative to that
     * archive not to the parent of the archive.
     */
    val relativePath : String

    /**
     * Accepts visitor.
     */
    fun accept(visitor: ArchiveItemVisitor)

    /**
     * Writes its internal data (or other nested files) into the given output stream.
     */
    fun writeSelfTo(outputStream: OutputStream)

}