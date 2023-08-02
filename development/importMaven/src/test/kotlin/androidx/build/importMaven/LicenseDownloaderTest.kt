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
import okio.FileSystem
import okio.Path
import org.junit.Test

class LicenseDownloaderTest {

    @Test // see: b/130834419, ensure we fallback to Github
    fun githubLicense() {
        val licenseDownloader = LicenseDownloader(enableGithubApi = true)
        val artifactPath = EnvironmentConfig.supportRoot /
                "../../prebuilts/androidx/external/" /
                "org/hamcrest/hamcrest-parent/1.3/hamcrest-parent-1.3.pom"
        val fetchedBytes = FileSystem.SYSTEM.read(
            artifactPath
        ) {
            licenseDownloader.fetchLicenseFromPom(
                bytes = readByteArray()
            )
        }
        assertThat(
            fetchedBytes
        ).isNotNull()
    }

    @Test // see: b/234867553
    fun checkLicenseContents() {
        // download for a know artifact and ensure it matches what we have
        val artifactPath = EnvironmentConfig.supportRoot /
                "../../prebuilts/androidx/external/org/jetbrains/kotlin/kotlin-stdlib/1.6.21"
        checkLocalLicense(
            localPom = artifactPath / "kotlin-stdlib-1.6.21.pom",
            localLicense = artifactPath / "LICENSE"
        )
    }

    private fun checkLocalLicense(
        localPom: Path,
        localLicense: Path
    ) {
        val licenseDownloader = LicenseDownloader(enableGithubApi = false)
        val fetchedBytes = FileSystem.SYSTEM.read(
            localPom
        ) {
            licenseDownloader.fetchLicenseFromPom(
                bytes = readByteArray()
            )
        }?.toString(Charsets.UTF_8)
        val localBytes = FileSystem.SYSTEM.read(
            localLicense
        ) {
            readUtf8()
        }
        assertThat(
            fetchedBytes
        ).isEqualTo(
            localBytes
        )
    }
}