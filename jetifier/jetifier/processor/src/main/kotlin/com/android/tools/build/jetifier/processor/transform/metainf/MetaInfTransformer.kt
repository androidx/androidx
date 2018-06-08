/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.metainf

import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.android.tools.build.jetifier.processor.transform.Transformer
import java.nio.charset.StandardCharsets

/**
 * Transformer for META-INF/(.*).version files.
 *
 * Replaces version files from the META-INF directory. This should be used only for processing
 * of the support library itself.
 */
class MetaInfTransformer internal constructor(
    private val context: TransformationContext
) : Transformer {

    companion object {
        const val FROM_VERSION = "28.0.0-SNAPSHOT"

        const val TO_VERSION = "1.0.0-SNAPSHOT"

        const val META_INF_DIR = "meta-inf"

        const val VERSION_FILE_SUFFIX = ".version"
    }

    override fun canTransform(file: ArchiveFile): Boolean {
        return context.rewritingSupportLib
            && file.relativePath.toString().contains(META_INF_DIR, ignoreCase = true)
            && file.fileName.endsWith(VERSION_FILE_SUFFIX, ignoreCase = true)
    }

    override fun runTransform(file: ArchiveFile) {
        val sb = StringBuilder(file.data.toString(StandardCharsets.UTF_8))

        var from = FROM_VERSION
        var to = TO_VERSION
        if (context.isInReversedMode) {
            from = TO_VERSION
            to = FROM_VERSION
        }

        if (sb.toString() != from) {
            return
        }

        file.setNewData(to.toByteArray())
    }
}