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

package com.android.tools.build.jetifier.processor.transform.proguard

import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.android.tools.build.jetifier.processor.transform.Transformer
import com.android.tools.build.jetifier.processor.transform.proguard.patterns.ReplacersRunner
import java.nio.charset.StandardCharsets

/**
 * The [Transformer] responsible for ProGuard files refactoring.
 */
class ProGuardTransformer internal constructor(context: TransformationContext) : Transformer {

    private val mapper = ProGuardTypesMapper(
        context)

    val replacer = ReplacersRunner(
        listOf(
            ProGuardClassSpecParser(mapper).replacer,
            ProGuardClassFilterParser(mapper).replacer
        ))

    override fun canTransform(file: ArchiveFile): Boolean {
        return file.isProGuardFile()
    }

    override fun runTransform(file: ArchiveFile) {
        val content = StringBuilder(file.data.toString(StandardCharsets.UTF_8)).toString()
        val result = replacer.applyReplacers(content)

        if (result == content) {
            return
        }

        file.setNewData(result.toByteArray())
    }
}

