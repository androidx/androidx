/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.providerevents

import android.app.Activity
import android.graphics.Bitmap
import android.os.Looper
import androidx.credentials.providerevents.exception.ImportCredentialsProviderConfigurationException
import androidx.credentials.providerevents.exception.RegisterExportProviderConfigurationException
import androidx.credentials.providerevents.transfer.CredentialTypes
import androidx.credentials.providerevents.transfer.ExportEntry
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProviderEventsManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var providerEventsManager: ProviderEventsManager

    @Before
    fun setup() {
        providerEventsManager = ProviderEventsManager.create(context)
    }

    @Test
    fun importCredentials_throws() =
        runBlocking<Unit> {
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }
            assertThrows<ImportCredentialsProviderConfigurationException> {
                providerEventsManager.importCredentials(
                    Activity(),
                    ImportCredentialsRequest(CXP_REQUEST),
                )
            }
        }

    @Test
    fun registerExport_throws() =
        runBlocking<Unit> {
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }
            assertThrows<RegisterExportProviderConfigurationException> {
                providerEventsManager.registerExport(
                    RegisterExportRequest(
                        listOf(
                            ExportEntry(
                                "id",
                                "account",
                                "user",
                                Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
                                setOf(CredentialTypes.CREDENTIAL_TYPE_BASIC_AUTH),
                            )
                        )
                    )
                )
            }
        }

    private companion object {
        const val CXP_REQUEST =
            """
        {
          "version": {
            "major": 1,
            "minor": 0
          },
          "hpke": [],
          "mode": "direct",
          "importerRpId": "com.google.android.apps.restore",
          "importerDisplayName": "Android Restore",
          "credentialTypes": [
            "basic-auth",
            "passkey"
          ]
        }
      """
    }
}
