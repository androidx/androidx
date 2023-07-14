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

package androidx.room.compiler.processing

import androidx.kruth.assertThat
import androidx.room.compiler.processing.util.runProcessorTest
import kotlin.io.path.Path
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XFilerTest {

    @Test
    fun writeResource_sourceFile_java() {
        runProcessorTest {
            try {
                it.processingEnv.filer.writeResource(Path("Test.java"), emptyList()).close()
                fail("Expected an exception!")
            } catch (ex: java.lang.IllegalArgumentException) {
                assertThat(ex.message).isEqualTo(
                    "Could not create resource file with a source type extension. " +
                        "File must not be neither '.java' nor '.kt', but was: Test.java"
                )
            }
        }
    }

    @Test
    fun writeResource_sourceFile_kotlin() {
        runProcessorTest {
            try {
                it.processingEnv.filer.writeResource(Path("Test.kt"), emptyList()).close()
                fail("Expected an exception!")
            } catch (ex: IllegalArgumentException) {
                assertThat(ex.message).isEqualTo(
                    "Could not create resource file with a source type extension. " +
                        "File must not be neither '.java' nor '.kt', but was: Test.kt"
                )
            }
        }
    }
}
