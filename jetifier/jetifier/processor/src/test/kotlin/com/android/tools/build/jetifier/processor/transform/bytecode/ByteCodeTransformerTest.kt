/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.bytecode

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.processor.FileMapping
import com.android.tools.build.jetifier.processor.Processor
import org.junit.Test
import java.io.File

class ByteCodeTransformerTest {
    @Test(expected = InvalidByteCodeException::class)
    fun malformedBytecode_shouldThrowException() {
        val processor = Processor.createProcessor3(config = Config.EMPTY)
        processor.transform2(
            input = setOf(
                FileMapping(
                    File(javaClass
                        .getResource("/malformedBytecodeTest/malformedBytecodeArchive.zip").file),
                    File("test")
                )
            )
        )
    }

    @Test(expected = InvalidByteCodeException::class)
    fun malformedBytecode_androidXDetectionOn_shouldThrowException() {
        val processor = Processor.createProcessor3(config = Config.EMPTY)
        processor.transform2(
            input = setOf(
                FileMapping(
                    File(javaClass
                        .getResource("/malformedBytecodeTest/malformedBytecodeArchive.zip").file),
                    File("test")
                )
            ),
            skipLibsWithAndroidXReferences = true
        )
    }
}