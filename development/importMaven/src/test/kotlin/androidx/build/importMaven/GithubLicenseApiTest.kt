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
import org.junit.Test

class GithubLicenseApiTest {
    @Test
    fun invalidRepoAndOwner() {
        val response = GithubLicenseApiClient().getProjectLicense(
            "https://github.com/this-repository-surely-does-not-exist/i-bet-really"
        )
        assertThat(response).isNull()
    }

    @Test
    fun fetchLicense() {
        val response = GithubLicenseApiClient().getProjectLicense(
            "https://github.com/androidX/androidx"
        )
        val androidXLicense = this::class.java.getResource(
            "/androidx-license.txt"
        )?.openStream()?.use {
            it.readAllBytes()
        }?.toString(Charsets.UTF_8)!!
        assertThat(response?.trim()).isEqualTo(androidXLicense.trim())
    }

    @Test
    fun parseRepo() {
        with(GithubLicenseApiClient()) {
            assertThat(
                "https://github.com/hamcrest/JavaHamcrest".extractGithubOwnerAndRepo()
            ).isEqualTo("hamcrest" to "JavaHamcrest")
            assertThat(
                "https://www.github.com/hamcrest/JavaHamcrest".extractGithubOwnerAndRepo()
            ).isEqualTo("hamcrest" to "JavaHamcrest")
            assertThat(
                "not url".extractGithubOwnerAndRepo()
            ).isNull()
            assertThat(
                "https://www.notgithub.com/foo/bar".extractGithubOwnerAndRepo()
            ).isNull()
            assertThat(
                "https://www.github.com/foo/bar/baz".extractGithubOwnerAndRepo()
            ).isNull()
            assertThat(
                "https://www.github.com/foo/bar/".extractGithubOwnerAndRepo()
            ).isEqualTo("foo" to "bar")
        }
    }
}