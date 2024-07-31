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

package androidx.credentials.provider

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.SigningInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4::class)
@SmallTest
class CallingAppInfoApi28Test {

    companion object {
        lateinit var signingInfo: SigningInfo
        lateinit var packageName: String

        private const val ORIGIN = "origin"

        private const val ALLOWLIST_VALID_JSON =
            "{\"apps\": [\n" +
                "   {\n" +
                "      \"type\": \"android\", \n" +
                "      \"info\": {\n" +
                "         \"package_name\": \"androidx.credentials.test\",\n" +
                "         \"signatures\" : [\n" +
                "         {\"build\": \"release\",\n" +
                "             \"cert_fingerprint_sha256\": \"EE:B9:3D:E4:98:2F:A1:2E:AD:5B:C1:16:7A" +
                ":6E:10:BD:23:49:B4:04:65:C4:3A:01:CC:54:06:4D:E5:2A:38:04\"\n" +
                "         },\n" +
                "         {\"build\": \"ud\",\n" +
                "         \"cert_fingerprint_sha256\": \"59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:" +
                "36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32\"\n" +
                "         }]\n" +
                "      }\n" +
                "    }\n" +
                "]}\n" +
                "\n"

        private const val ALLOWLIST_VALID_DEBUG_CERT_JSON =
            "{\"apps\": [\n" +
                "   {\n" +
                "      \"type\": \"android\", \n" +
                "      \"info\": {\n" +
                "         \"package_name\": \"androidx.credentials.test\",\n" +
                "         \"signatures\" : [\n" +
                "         {\"build\": \"ud\",\n" +
                "             \"cert_fingerprint_sha256\": \"EE:B9:3D:E4:98:2F:A1:2E:AD:5B:C1:16:7A" +
                ":6E:10:BD:23:49:B4:04:65:C4:3A:01:CC:54:06:4D:E5:2A:38:04\"\n" +
                "         },\n" +
                "         {\"build\": \"release\",\n" +
                "         \"cert_fingerprint_sha256\": \"59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:" +
                "36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32\"\n" +
                "         }]\n" +
                "      }\n" +
                "    }\n" +
                "]}\n" +
                "\n"

        private const val ALLOWLIST_NO_MATCH_PKG_JSON =
            "{\"apps\": [\n" +
                "   {\n" +
                "      \"type\": \"android\", \n" +
                "      \"info\": {\n" +
                "         \"package_name\": \"androidx.sample\",\n" +
                "         \"signatures\" : [\n" +
                "         {\"build\": \"release\",\n" +
                "             \"cert_fingerprint_sha256\": \"EE:B9:3D:E4:98:2F:A1:2E:AD:5B:C1:16:7A" +
                ":6E:10:BD:23:49:B4:04:65:C4:3A:01:CC:54:06:4D:E5:2A:38:04\"\n" +
                "         },\n" +
                "         {\"build\": \"ud\",\n" +
                "         \"cert_fingerprint_sha256\": \"59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:" +
                "36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32\"\n" +
                "         }]\n" +
                "      }\n" +
                "    }\n" +
                "]}\n" +
                "\n"

        private const val ALLOWLIST_NO_MATCH_CERT_JSON =
            "{\"apps\": [\n" +
                "   {\n" +
                "      \"type\": \"android\", \n" +
                "      \"info\": {\n" +
                "         \"package_name\": \"androidx.credentials.test\",\n" +
                "         \"signatures\" : [\n" +
                "         {\"build\": \"release\",\n" +
                "             \"cert_fingerprint_sha256\": \"EE:B8:3D:E4:98:2F:A1:2E:AD:5B:C1:16:7A" +
                ":6E:10:BD:23:49:B4:04:65:C4:3A:01:CC:54:06:4D:E5:2A:38:04\"\n" +
                "         },\n" +
                "         {\"build\": \"ud\",\n" +
                "         \"cert_fingerprint_sha256\": \"59:0D:0D:7B:33:6A:BD:FB:54:CD:3D:8B:" +
                "36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32\"\n" +
                "         }]\n" +
                "      }\n" +
                "    }\n" +
                "]}\n" +
                "\n"

        private const val ALLOWLIST_NON_ANDROID_JSON =
            "{\"apps\": [\n" +
                "   {\n" +
                "      \"type\": \"non-android\", \n" +
                "      \"info\": {\n" +
                "         \"package_name\": \"androidx.credentials.test\",\n" +
                "         \"signatures\" : [\n" +
                "         {\"build\": \"release\",\n" +
                "             \"cert_fingerprint_sha256\": \"EE:B9:3D:E4:98:2F:A1:2E:AD:5B:C1:16:7A" +
                ":6E:10:BD:23:49:B4:04:65:C4:3A:01:CC:54:06:4D:E5:2A:38:04\"\n" +
                "         },\n" +
                "         {\"build\": \"ud\",\n" +
                "         \"cert_fingerprint_sha256\": \"59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:" +
                "36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32\"\n" +
                "         }]\n" +
                "      }\n" +
                "    }\n" +
                "]}\n" +
                "\n"

        private const val ALLOWLIST_INVALID_APPS_TAG_JSON =
            "{\"apps2\": [\n" +
                "   {\n" +
                "      \"type\": \"android\", \n" +
                "      \"info\": {\n" +
                "         \"package_name\": \"com.example.myapp\",\n" +
                "         \"signatures\" : [\n" +
                "         {\"build\": \"release\",\n" +
                "             \"cert_fingerprint_sha256\": \"59:0D:2D:7B:33:6A:BD:FB:54:CD:3D" +
                ":8B:36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32\"\n" +
                "         },\n" +
                "         {\"build\": \"userdebug\",\n" +
                "         \"cert_fingerprint_sha256\": \"59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:" +
                "36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32\"\n" +
                "         }]\n" +
                "      }\n" +
                "    }\n" +
                "]}\n" +
                "\n"

        private const val ALLOWLIST_MISSING_PACKAGE_NAME_JSON =
            "{\"apps\": [\n" +
                "   {\n" +
                "      \"type\": \"android\", \n" +
                "      \"info\": {\n" +
                "         \"signatures\" : [\n" +
                "         {\"build\": \"release\",\n" +
                "             \"cert_fingerprint_sha256\": \"59:0D:2D:7B:33:6A:BD:FB:54:CD:3D" +
                ":8B:36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32\"\n" +
                "         },\n" +
                "         {\"build\": \"userdebug\",\n" +
                "         \"cert_fingerprint_sha256\": \"59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:" +
                "36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32\"\n" +
                "         }]\n" +
                "      }\n" +
                "    }\n" +
                "]}\n" +
                "\n"

        @BeforeClass
        @JvmStatic
        fun initiateSigningInfo() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            packageName = context.packageName
            try {
                val packageInfo =
                    context.packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                assertNotNull(packageInfo.signingInfo)
                signingInfo = packageInfo.signingInfo!!
            } catch (_: PackageManager.NameNotFoundException) {}
        }
    }

    @Test
    fun constructor_success() {
        CallingAppInfo("name", SigningInfo())
    }

    @Test
    fun constructor_success_withOrigin() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.getOrigin(ALLOWLIST_VALID_JSON)).isEqualTo(ORIGIN)
        assertThat(callingAppInfo.origin).isEqualTo(ORIGIN)
    }

    @Test
    fun getOrigin_validReleaseCert_success() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.getOrigin(ALLOWLIST_VALID_JSON)).isEqualTo(ORIGIN)
    }

    @Test
    fun getOrigin_validDebugCert_success() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.getOrigin(ALLOWLIST_VALID_DEBUG_CERT_JSON)).isEqualTo(ORIGIN)
    }

    @Test
    fun constructor_fail_emptyPackageName() {
        assertThrows(
            "Expected exception from no package name",
            IllegalArgumentException::class.java
        ) {
            CallingAppInfo("", signingInfo, ALLOWLIST_VALID_JSON)
        }
    }

    @Test
    fun getOrigin_emptyPrivilegedAllowlist_throwsException() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.origin).isEqualTo(ORIGIN)
        assertThrows(
            "Expected exception from empty privilegedAllowList",
            IllegalArgumentException::class.java
        ) {
            callingAppInfo.getOrigin("")
        }
    }

    @Test
    fun getOrigin_missingPackageNameInAllowlist_throwsException() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.origin).isEqualTo(ORIGIN)
        assertThrows(
            "Expected exception from invalid json - no package name",
            IllegalArgumentException::class.java
        ) {
            callingAppInfo.getOrigin(ALLOWLIST_MISSING_PACKAGE_NAME_JSON)
        }
    }

    @Test
    fun getOrigin_invalidJSON_throwsException() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.origin).isEqualTo(ORIGIN)
        assertThrows(
            "Expected exception from emptyPrivilegedAllowList",
            IllegalArgumentException::class.java
        ) {
            callingAppInfo.getOrigin("invalid_json")
        }
    }

    @Test
    fun getOrigin_nonAndroidJSON_returnsNull() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.origin).isEqualTo(ORIGIN)
        assertThrows("Expected exception from incorrect type", IllegalStateException::class.java) {
            callingAppInfo.getOrigin(ALLOWLIST_NON_ANDROID_JSON)
        }
    }

    @Test
    fun getOrigin_invalidAppTagNameJSONFormat_throwsException() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.origin).isEqualTo(ORIGIN)
        assertThrows(
            "Expected exception from incorrect apps tag",
            IllegalArgumentException::class.java
        ) {
            callingAppInfo.getOrigin(ALLOWLIST_INVALID_APPS_TAG_JSON)
        }
    }

    @Test
    fun getOrigin_noMatchPackageName1_throwsException() {
        val callingAppInfo = CallingAppInfo("incorrect_package_name", signingInfo, ORIGIN)

        assertThat(callingAppInfo.origin).isEqualTo(ORIGIN)
        assertThrows(
            "Expected exception from no matching package name",
            IllegalStateException::class.java
        ) {
            callingAppInfo.getOrigin(ALLOWLIST_VALID_JSON)
        }
    }

    @Test
    fun getOrigin_noMatchPackageName2_throwsException() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.origin).isEqualTo(ORIGIN)
        assertThrows(
            "Expected exception from no matching package name",
            IllegalStateException::class.java
        ) {
            callingAppInfo.getOrigin(ALLOWLIST_NO_MATCH_PKG_JSON)
        }
    }

    @Test
    fun getOrigin_noMatchCert_throwsException() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertThat(callingAppInfo.origin).isEqualTo(ORIGIN)
        assertThrows(
            "Expected exception from no matching cert",
            IllegalStateException::class.java
        ) {
            callingAppInfo.getOrigin(ALLOWLIST_NO_MATCH_CERT_JSON)
        }
    }

    @Test
    fun getOrigin_noMatchCert_originNull_throwsException() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, null)

        assertThat(callingAppInfo.origin).isNull()
        assertThat(callingAppInfo.getOrigin(ALLOWLIST_NO_MATCH_CERT_JSON)).isNull()
    }

    fun isOriginPopulated_originPopulated_returnsTrue() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, ORIGIN)

        assertTrue(callingAppInfo.isOriginPopulated())
    }

    fun isOriginPopulated_originNotPopulated_returnsFalse() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo)

        assertFalse(callingAppInfo.isOriginPopulated())
    }

    fun isOriginPopulated_originPopulatedAsNull_returnsFalse() {
        val callingAppInfo = CallingAppInfo(packageName, signingInfo, null)

        assertFalse(callingAppInfo.isOriginPopulated())
    }
}
