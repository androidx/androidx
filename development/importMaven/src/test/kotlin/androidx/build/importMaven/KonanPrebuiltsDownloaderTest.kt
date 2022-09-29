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

package androidx.build.importMaven

import com.google.common.truth.Truth.assertThat
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.Test

class KonanPrebuiltsDownloaderTest {
    @Test
    fun download() {
        val fakeFileSystem = FakeFileSystem().apply {
            emulateUnix()
        }
        val downloader = KonanPrebuiltsDownloader(
            fileSystem = fakeFileSystem,
            downloadPath = "konans".toPath(),
            testMode = true
        )
        downloader.download("1.6.21")
        assertThat(
            fakeFileSystem.allPaths
        ).containsAtLeast(
            "/konans/llvm-11.1.0-linux-x64-essentials.tar.gz".toPath(),
            "/konans/apple-llvm-20200714-macos-aarch64-essentials.tar.gz".toPath(),
            "/konans/apple-llvm-20200714-macos-x64-essentials.tar.gz".toPath(),
            "/konans/libffi-3.2.1-2-linux-x86-64.tar.gz".toPath(),
            "/konans/libffi-3.3-1-macos-arm64.tar.gz".toPath(),
            "/konans/libffi-3.2.1-3-darwin-macos.tar.gz".toPath(),
            "/konans/x86_64-unknown-linux-gnu-gcc-8.3.0-glibc-2.19-kernel-4.9-2.tar.gz".toPath(),
            "/konans/lldb-4-linux.tar.gz".toPath()
        )
    }
}