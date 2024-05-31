/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.stableaidl.tasks

import java.util.zip.ZipFile
import kotlin.test.assertContains
import org.apache.tools.zip.ZipOutputStream
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StableAidlPackageApiTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun testPackageDirectoryIntoAar() {
        val aarSourceDir = temporaryFolder.newFolder("aarSourceDir")
        val aarContents = createFile("AndroidManifest.xml", aarSourceDir)

        val packagedDir = temporaryFolder.newFolder("packagedDir")
        createFile("android/os/MyParcelable.aidl", packagedDir)

        val outputDir = temporaryFolder.newFolder("outputDir")
        val aarFile = createFile("aarFile.aar", outputDir)
        ZipOutputStream(aarFile.outputStream()).use { stream ->
            stream.addFileRecursive(null, aarContents)
        }
        val updatedAarFile = createFile("updatedAarFile.aar", outputDir)

        with(ProjectBuilder.builder().withProjectDir(temporaryFolder.newFolder()).build()) {
            aidlPackageApiDelegate(aarFile, updatedAarFile, project.fileTree(packagedDir), "test")
        }

        ZipFile(updatedAarFile).use { zip ->
            val zipEntryNames = zip.getEntryNames()
            assertContains(zipEntryNames, "aidl/android/os/MyParcelable.aidl")
            assertContains(zipEntryNames, "AndroidManifest.xml")
        }
    }
}
