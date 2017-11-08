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

package android.support.tools.jetifier.core.transform.proguard

import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.transform.TransformationContext
import android.support.tools.jetifier.core.transform.Transformer
import android.support.tools.jetifier.core.transform.proguard.patterns.ReplacersRunner
import java.nio.charset.StandardCharsets

/**
 * The [Transformer] responsible for ProGuard files refactoring.
 */
class ProGuardTransformer internal constructor(context: TransformationContext) : Transformer {

    private val mapper = ProGuardTypesMapper(context)

    val replacer = ReplacersRunner(listOf(
        ProGuardClassSpecParser(mapper).replacer,
        ProGuardClassFilterParser(mapper).replacer
    ))

    override fun canTransform(file: ArchiveFile): Boolean {
        return file.isProGuardFile()
    }

    override fun runTransform(file: ArchiveFile) {
        val sb = StringBuilder(file.data.toString(StandardCharsets.UTF_8))
        val result = replacer.applyReplacers(sb.toString())
        file.data = result.toByteArray()
    }
}

