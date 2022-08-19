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
import com.google.common.truth.Truth.assertWithMessage
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.junit.Test

/**
 * Integration tests for [ArtifactResolver]
 */
class ArtifactResolverTest {
    private val fakeFileSystem = FakeFileSystem().also {
        it.emulateUnix()
    }
    private val downloader = LocalMavenRepoDownloader(
        fileSystem = fakeFileSystem,
        internalFolder = fakeFileSystem.workingDirectory / "internal",
        externalFolder = fakeFileSystem.workingDirectory / "external"
    )

    @Test
    fun downloadAndroidXPrebuilt() {
        ArtifactResolver.resolveArtifacts(
            artifacts = listOf("androidx.room:room-runtime:2.5.0-SNAPSHOT"),
            additionalRepositories = listOf(
                ArtifactResolver.createAndroidXRepo(8657806)
            ),
            downloadObserver = downloader
        )
        assertThat(
            fakeFileSystem.allPathStrings()
        ).containsAtLeast(
            "/internal/androidx/room/room-runtime/2.5.0-SNAPSHOT/maven-metadata.xml",
            "/internal/androidx/room/room-common/2.5.0-SNAPSHOT/maven-metadata.xml",
        )
        // the downloaded file for snapshot will have a version.
        // If we assert exact name, it will fail when build is no longer available. Instead, we look
        // into files.
        val roomRuntimeFiles = fakeFileSystem.list(
            "/internal/androidx/room/room-runtime/2.5.0-SNAPSHOT/".toPath()
        )
        assertWithMessage(
            roomRuntimeFiles.joinToString("\n") { it.toString() }
        ).that(
            roomRuntimeFiles.any {
                it.name.startsWith("room-runtime-") &&
                        it.name.endsWith("aar")
            }).isTrue()
    }

    @Test
    fun testAndroidArtifactsWithMetadata() {
        ArtifactResolver.resolveArtifacts(
            listOf("androidx.room:room-runtime:2.4.2"),
            downloadObserver = downloader
        )
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/internal/androidx/room/room-runtime/2.4.2/".toPath().expectedAar(
                signed = false,
                "room-runtime-2.4.2"
            ) + "/internal/androidx/room/room-common/2.4.2/".toPath().expectedJar(
                signed = false,
                "room-common-2.4.2"
            ) + "/internal/androidx/sqlite/sqlite-framework/2.2.0".toPath().expectedAar(
                signed = false,
                "sqlite-framework-2.2.0"
            ) + "/internal/androidx/annotation/annotation/1.1.0".toPath().expectedFiles(
                signed = false,
                // this annotations artifact is old, it doesn't have module metadata
                "annotation-1.1.0.jar", "annotation-1.1.0.pom"
            )
        )
        // don't copy licenses for internal artifacts
        assertThat(fakeFileSystem.allPathStrings()).doesNotContain(
            "/internal/androidx/room/room-runtime/2.4.2/LICENSE"
        )
    }

    @Test
    fun testGmavenJar() {
        ArtifactResolver.resolveArtifacts(
            listOf("androidx.room:room-common:2.4.2"),
            downloadObserver = downloader
        )
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/internal/androidx/room/room-common/2.4.2/".toPath().expectedJar(
                signed = false,
                "room-common-2.4.2"
            ) + "/internal/androidx/annotation/annotation/1.1.0".toPath().expectedFiles(
                signed = false,
                // this annotations artifact is old, it doesn't have module metadata
                "annotation-1.1.0.jar", "annotation-1.1.0.pom"
            )
        )
        // don't copy licenses for internal artifacts
        assertThat(fakeFileSystem.allPathStrings()).doesNotContain(
            "/internal/androidx/room/room-runtime/2.4.2/LICENSE"
        )
    }

    @Test
    fun ensureInternalPomsAreReWritten() {
        val bytes = this::class.java
            .getResourceAsStream("/pom-with-aar-deps.pom")!!.readBytes()
        downloader.onDownload("notAndroidx/subject.pom", bytes)
        val externalContents = fakeFileSystem.read(
            "external/notAndroidx/subject.pom".toPath()
        ) {
            this.readUtf8()
        }.lines().map { it.trim() }
        assertThat(externalContents).contains("<type>aar</type>")
        assertThat(externalContents).doesNotContain("<!--<type>aar</type>-->")

        downloader.onDownload("androidx/subject.pom", bytes)
        val internalContents = fakeFileSystem.read(
            "internal/androidx/subject.pom".toPath()
        ) {
            this.readUtf8()
        }.lines().map { it.trim() }
        assertThat(internalContents).doesNotContain("<type>aar</type>")
        assertThat(internalContents).contains("<!--<type>aar</type>-->")

        // assert that original sha/md5 is preserved when file is re-written.
        val sha1 = LocalMavenRepoDownloader.digest(
            contents = bytes,
            fileName = "_",
            algorithm = "SHA1"
        ).second.toString(Charsets.UTF_8)
        val md5 = LocalMavenRepoDownloader.digest(
            contents = bytes,
            fileName = "_",
            algorithm = "MD5"
        ).second.toString(Charsets.UTF_8)
        assertThat(
            fakeFileSystem.readText("external/notAndroidx/subject.pom.md5")
        ).isEqualTo(md5)
        assertThat(
            fakeFileSystem.readText("internal/androidx/subject.pom.md5")
        ).isEqualTo(md5)
        assertThat(
            fakeFileSystem.readText("external/notAndroidx/subject.pom.sha1")
        ).isEqualTo(sha1)
        assertThat(
            fakeFileSystem.readText("internal/androidx/subject.pom.sha1")
        ).isEqualTo(sha1)
    }

    @Test
    fun testKotlinArtifactsWithKmp_fetchInherited() = testKotlinArtifactsWithKmp(true)

    @Test
    fun testKotlinArtifactsWithKmp() = testKotlinArtifactsWithKmp(false)

    private fun testKotlinArtifactsWithKmp(explicitlyFetchInherited: Boolean) {
        ArtifactResolver.resolveArtifacts(
            listOf("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1"),
            downloadObserver = downloader,
            explicitlyFetchInheritedDependencies = explicitlyFetchInherited
        )
        val expectedKlibs = listOf(
            "atomicfu-linuxx64/0.17.0/atomicfu-linuxx64-0.17.0.klib",
            "atomicfu-macosarm64/0.17.0/atomicfu-macosarm64-0.17.0.klib",
            "atomicfu-macosx64/0.17.0/atomicfu-macosx64-0.17.0.klib"
        ).map {
            "/external/org/jetbrains/kotlinx/$it"
        }
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/external/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.6.1".toPath()
                .expectedFiles(
                    signed = true,
                    "kotlinx-coroutines-core-1.6.1-all.jar",
                    "kotlinx-coroutines-core-1.6.1-sources.jar",
                    "kotlinx-coroutines-core-1.6.1.module",
                ) + "/external/org/jetbrains/kotlin/kotlin-stdlib-common/1.6.0".toPath()
                .expectedFiles(
                    signed = true,
                    "kotlin-stdlib-common-1.6.0.jar",
                    "kotlin-stdlib-common-1.6.0.pom",
                ) + expectedKlibs +
                    "/external/org/jetbrains/kotlin/kotlin-stdlib-common/1.6.0/LICENSE"
        )
        // atomic-fu jvm is not a dependency
        val atomicFuJvmFiles =
            "/external/org/jetbrains/kotlinx/atomicfu-jvm/0.17.0".toPath().expectedJar(
                signed = true,
                "atomicfu-jvm-0.17.0"
            )
        if (explicitlyFetchInherited) {
            assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
                atomicFuJvmFiles
            )
        } else {
            assertThat(fakeFileSystem.allPathStrings()).doesNotContain(
                atomicFuJvmFiles
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun invalidArtifact() {
        ArtifactResolver.resolveArtifacts(
            listOf("this.artifact.doesnot:exists:1.0.0"),
            downloadObserver = downloader
        )
    }

    @Test
    fun testDownloadAtomicFu() {
        ArtifactResolver.resolveArtifacts(
            listOf("org.jetbrains.kotlinx:atomicfu:0.17.0"),
            downloadObserver = downloader
        )
        val expectedKlibs = listOf(
            "atomicfu-linuxx64/0.17.0/atomicfu-linuxx64-0.17.0.klib",
            "atomicfu-macosarm64/0.17.0/atomicfu-macosarm64-0.17.0.klib",
            "atomicfu-macosx64/0.17.0/atomicfu-macosx64-0.17.0.klib"
        ).map {
            "/external/org/jetbrains/kotlinx/$it"
        }
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/external/org/jetbrains/kotlinx/atomicfu-jvm/0.17.0".toPath()
                .expectedJar(
                    signed = true,
                    "atomicfu-jvm-0.17.0"
                ) + expectedKlibs +
                    "/external/org/jetbrains/kotlin/kotlin-stdlib-common/1.6.0/LICENSE"
        )
    }

    @Test
    fun testSignatureFiles() {
        ArtifactResolver.resolveArtifacts(
            listOf("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.1"),
            downloadObserver = downloader
        )
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/external/org/jetbrains/kotlinx/kotlinx-coroutines-test-linuxx64/1.6.1/".toPath()
                .expectedFiles(
                    signed = true,
                    "kotlinx-coroutines-test-linuxx64-1.6.1.klib",
                    "kotlinx-coroutines-test-linuxx64-1.6.1.module",
                )
        )
    }

    @Test
    fun testSignedArtifactWithoutKeyServerEntry() {
        // https://repo1.maven.org/maven2/org/assertj/assertj-core/3.11.1/
        // these artifacts are signed but their signature is not on any public key-server
        // we cannot trust them so instead these builds should rely on shas
        ArtifactResolver.resolveArtifacts(
            listOf("org.assertj:assertj-core:3.11.1"),
            downloadObserver = downloader
        )
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/external/org/assertj/assertj-core/3.11.1/".toPath().expectedFiles(
                signed = true,
                "assertj-core-3.11.1-sources.jar",
                "assertj-core-3.11.1.jar",
                "assertj-core-3.11.1.pom",
            )
        )
    }

    @Test
    fun noArtifactsAreFetchedWhenInternalRepoIsProvided() {
        val localRepoUris = EnvironmentConfig.supportRoot.let {
            listOf(
                "file:///$it/../../prebuilts/androidx/external",
                "file:///$it/../../prebuilts/androidx/internal",
            )
        }
        ArtifactResolver.resolveArtifacts(
            listOf("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1"),
            downloadObserver = downloader,
            localRepositories = localRepoUris
        )
        assertThat(fakeFileSystem.allPaths).isEmpty()
    }

    @Test
    fun testMetalavaDownload() {
        ArtifactResolver.resolveArtifacts(
            artifacts = listOf(
                "com.android.tools.metalava:metalava:1.0.0-alpha06"
            ),
            additionalRepositories = listOf(
                ArtifactResolver.createMetalavaRepo(8634556)
            ),
            downloadObserver = downloader
        )

        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/external/com/android/tools/metalava/metalava/1.0.0-alpha06/".toPath().expectedJar(
                signed = false,
                "metalava-1.0.0-alpha06"
            ).filterNot {
                // metalava doesn't ship sources.
                it.contains("sources")
            }
        )
    }

    @Test
    fun oldArtifactWithoutMetadata() {
        ArtifactResolver.resolveArtifacts(
            artifacts = listOf(
                "androidx.databinding:viewbinding:4.1.2"
            ),
            downloadObserver = downloader
        )
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/external/androidx/databinding/viewbinding/4.1.2".toPath().expectedAar(
                signed = false,
                "viewbinding-4.1.2"
            ).filterNot {
                // only pom in this artifact
                it.contains("module")
            }
        )
    }

    @Test
    fun testRunner() {
        ArtifactResolver.resolveArtifacts(
            artifacts = listOf(
                "androidx.test:runner:1.4.0"
            ),
            downloadObserver = downloader
        )
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/internal/androidx/test/runner/1.4.0/".toPath().expectedAar(
                signed = false,
                "runner-1.4.0"
            ).filterNot {
                // old artifact without metadata
                it.contains("module")
            }
        )
    }

    /**
     * Assert that if same artifact is referenced by two libraries but one of them uses a newer
     * version, we fetch both versions.
     */
    @Test
    fun isolateConfigurations() {
        ArtifactResolver.resolveArtifacts(
            listOf(
                "androidx.room:room-runtime:2.4.2",
                "androidx.room:room-runtime:2.4.0",
            ),
            downloadObserver = downloader
        )
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeastElementsIn(
            "/internal/androidx/room/room-common/2.4.2/".toPath().expectedJar(
                signed = false,
                "room-common-2.4.2"
            ) +
                    "/internal/androidx/room/room-common/2.4.0/".toPath().expectedJar(
                        signed = false,
                        "room-common-2.4.0"
                    )
        )
    }

    @Test
    fun downloadWithGradlePluginSpecs() {
        ArtifactResolver.resolveArtifacts(
            listOf("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.0"),
            downloadObserver = downloader
        )
        assertThat(fakeFileSystem.allPathStrings()).containsAtLeast(
            "/external/org/jetbrains/kotlin/kotlin-gradle-plugin/1.7.0/" +
                    "kotlin-gradle-plugin-1.7.0-gradle70.jar",
            "/external/org/jetbrains/kotlin/kotlin-gradle-plugin/1.7.0/" +
                    "kotlin-gradle-plugin-1.7.0.jar"
        )
    }

    private fun FakeFileSystem.allPathStrings() = allPaths.map {
        it.toString()
    }

    private fun FileSystem.readText(path: String) =
        this.openReadOnly(path.toPath()).source().buffer().use {
            it.readUtf8()
        }

    /**
     * Utility method to easily create files in a given folder path
     */
    private fun Path.expectedFiles(
        signed: Boolean,
        vararg fileNames: String,
    ): List<String> {
        val originals = if (signed) {
            fileNames.flatMap {
                listOf(it, "$it.asc")
            }
        } else {
            fileNames.toList()
        }

        return originals.map { "$this/$it" }.flatMap {
            listOf(it, "$it.md5", "$it.sha1")
        }
    }

    private fun Path.expectedAar(
        signed: Boolean,
        prefix: String,
    ) = expectedFiles(
        signed = signed,
        *COMMON_FILES_FOR_AARS.map {
            "$prefix$it"
        }.toTypedArray()
    ) + expectedFiles(
        // gradle doesn't need the signature for pom when there is a module file.
        signed = false,
        "$prefix.pom"
    )

    private fun Path.expectedJar(
        signed: Boolean,
        prefix: String,
    ) = expectedFiles(
        signed = signed,
        *COMMON_FILES_FOR_JARS.map {
            "$prefix$it"
        }.toTypedArray()
    ) + expectedFiles(
        // gradle doesn't need the signature for pom when there is a module file.
        signed = false,
        "$prefix.pom"
    )

    companion object {
        val COMMON_FILES_FOR_AARS = listOf(".aar", "-sources.jar", ".module")
        val COMMON_FILES_FOR_JARS = listOf(".jar", "-sources.jar", ".module")
    }
}