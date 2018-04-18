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

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.google.common.truth.Truth
import org.junit.Test
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

class MetaInfTransformerTest {

    @Test
    fun rewriteVersion_forward() {
        testRewrite(
            given = "28.0.0-SNAPSHOT",
            expected = "1.0.0-SNAPSHOT",
            filePath = Paths.get("something/META-INF", "support_preference-v7.version"),
            reverseMode = false
        )
    }

    @Test
    fun rewriteVersion_reversed() {
        testRewrite(
            given = "1.0.0-SNAPSHOT",
            expected = "28.0.0-SNAPSHOT",
            filePath = Paths.get("something/META-INF", "support_preference-v7.version"),
            reverseMode = true
        )
    }

    @Test
    fun rewriteVersion_notSLRewrite_shouldSkip() {
        testRewrite(
            given = "28.0.0-SNAPSHOT",
            expected = "28.0.0-SNAPSHOT",
            filePath = Paths.get("something/META-INF", "support_preference-v7.version"),
            reverseMode = false,
            rewritingSupportLib = false,
            expectedCanTransform = false
        )
    }

    @Test
    fun rewriteVersion_notMatchingVersion_shouldNoOp() {
        testRewrite(
            given = "test",
            expected = "test",
            filePath = Paths.get("something/META-INF", "support_preference-v7.version")
        )
    }

    @Test
    fun rewriteVersion_notValidSuffix_shouldSkip() {
        testRewrite(
            given = "28.0.0-SNAPSHOT",
            expected = "28.0.0-SNAPSHOT",
            filePath = Paths.get("something/META-INF", "support_preference-v7.none"),
            expectedCanTransform = false
        )
    }

    @Test
    fun rewriteVersion_notInMetaInfDir_shouldSkip() {
        testRewrite(
            given = "28.0.0-SNAPSHOT",
            expected = "28.0.0-SNAPSHOT",
            filePath = Paths.get("something/else", "support_preference-v7.version"),
            expectedCanTransform = false
        )
    }

    private fun testRewrite(
        given: String,
        expected: String,
        filePath: Path,
        reverseMode: Boolean = false,
        expectedCanTransform: Boolean = true,
        rewritingSupportLib: Boolean = true
    ) {
        val context = TransformationContext(Config.EMPTY,
            rewritingSupportLib = rewritingSupportLib,
            isInReversedMode = reverseMode)
        val transformer = MetaInfTransformer(context)

        val file = ArchiveFile(filePath, given.toByteArray())

        val canTransform = transformer.canTransform(file)
        if (canTransform) {
            transformer.runTransform(file)
        }

        val strResult = file.data.toString(Charset.defaultCharset())

        Truth.assertThat(canTransform).isEqualTo(expectedCanTransform)
        Truth.assertThat(strResult).isEqualTo(expected)
    }
}