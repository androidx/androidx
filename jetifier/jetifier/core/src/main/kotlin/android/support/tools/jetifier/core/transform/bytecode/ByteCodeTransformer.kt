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

package android.support.tools.jetifier.core.transform.bytecode

import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.transform.TransformationContext
import android.support.tools.jetifier.core.transform.Transformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

/**
 * The [Transformer] responsible for java byte code refactoring.
 */
class ByteCodeTransformer internal constructor(context: TransformationContext) : Transformer {

    private val remapper: CoreRemapperImpl = CoreRemapperImpl(context)

    override fun canTransform(file: ArchiveFile) = file.isClassFile()

    override fun runTransform(file: ArchiveFile) {
        val reader = ClassReader(file.data)
        val writer = ClassWriter(0 /* flags */)

        val visitor = remapper.createClassRemapper(writer)

        reader.accept(visitor, 0 /* flags */)

        file.data = writer.toByteArray()
        file.updateRelativePath(remapper.rewritePath(file.relativePath))
    }
}