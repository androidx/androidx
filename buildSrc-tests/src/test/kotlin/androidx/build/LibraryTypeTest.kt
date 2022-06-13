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

package androidx.build

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LibraryTypeTest {
    @Test
    fun publishedLibrary() {
        val libraryType = LibraryType.PUBLISHED_LIBRARY
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.Yes::class.java)
        assertThat(libraryType.sourceJars).isTrue()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.DEVICE)
    }

    @Test
    fun publishedTestLibrary() {
        val libraryType = LibraryType.PUBLISHED_TEST_LIBRARY
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.Yes::class.java)
        assertThat(libraryType.sourceJars).isTrue()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.DEVICE)
    }

    @Test
    fun publishedNativeLibrary() {
        val libraryType = LibraryType.PUBLISHED_NATIVE_LIBRARY
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.Yes::class.java)
        assertThat(libraryType.sourceJars).isTrue()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.DEVICE)
    }

    @Test
    fun internalTestLibrary() {
        val libraryType = LibraryType.INTERNAL_TEST_LIBRARY
        assertThat(libraryType.publish).isEqualTo(Publish.NONE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isFalse()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.DEVICE)
    }

    @Test
    fun samples() {
        val libraryType = LibraryType.SAMPLES
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isTrue()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.DEVICE)
    }

    @Test
    fun lint() {
        val libraryType = LibraryType.LINT
        assertThat(libraryType.publish).isEqualTo(Publish.NONE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isFalse()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.HOST)
    }

    @Test
    fun compilerDaemon() {
        val libraryType = LibraryType.COMPILER_DAEMON
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isFalse()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.HOST)
    }

    @Test
    fun compilerPlugin() {
        val libraryType = LibraryType.COMPILER_PLUGIN
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isFalse()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.HOST)
    }

    @Test
    fun gradlePlugin() {
        val libraryType = LibraryType.GRADLE_PLUGIN
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isFalse()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.HOST)
    }

    @Test
    fun annotationProcessor() {
        val libraryType = LibraryType.ANNOTATION_PROCESSOR
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isFalse()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.HOST)
    }

    @Test
    fun annotationProcessorUtils() {
        val libraryType = LibraryType.ANNOTATION_PROCESSOR_UTILS
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isTrue()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.HOST)
    }

    @Test
    fun otherCodeProcessor() {
        val libraryType = LibraryType.OTHER_CODE_PROCESSOR
        assertThat(libraryType.publish).isEqualTo(Publish.SNAPSHOT_AND_RELEASE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isFalse()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.HOST)
    }

    @Test
    fun idePlugin() {
        val libraryType = LibraryType.IDE_PLUGIN
        assertThat(libraryType.publish).isEqualTo(Publish.NONE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isFalse()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.DEVICE)
    }

    @Test
    fun unset() {
        val libraryType = LibraryType.UNSET
        assertThat(libraryType.publish).isEqualTo(Publish.NONE)
        assertThat(libraryType.checkApi).isInstanceOf(RunApiTasks.No::class.java)
        assertThat(libraryType.sourceJars).isFalse()
        assertThat(libraryType.compilationTarget).isEqualTo(CompilationTarget.DEVICE)
    }
}