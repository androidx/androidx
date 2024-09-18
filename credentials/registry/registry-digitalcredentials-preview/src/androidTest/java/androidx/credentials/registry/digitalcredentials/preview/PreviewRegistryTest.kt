/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.registry.digitalcredentials.preview

import android.graphics.Bitmap
import androidx.credentials.registry.digitalcredentials.mdoc.MdocEntry
import androidx.credentials.registry.digitalcredentials.mdoc.MdocField
import androidx.credentials.registry.digitalcredentials.preview.PreviewRegistry.Companion.getDefaultPreviewMatcher
import androidx.credentials.registry.digitalcredentials.preview.PreviewRegistry.Companion.toCredentialBytes
import androidx.credentials.registry.provider.digitalcredentials.VerificationEntryDisplayData
import androidx.credentials.registry.provider.digitalcredentials.VerificationFieldDisplayData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PreviewRegistryTest {
    @Test
    fun construction_defaultId_success() {
        val entry1 =
            MdocEntry(
                docType = "org.iso.18013.5.1.mDL",
                fields =
                    listOf(
                        MdocField(
                            "fieldName1",
                            "fieldVal1",
                            setOf(VerificationFieldDisplayData("displayName1"))
                        ),
                        MdocField(
                            "fieldName2",
                            null,
                            setOf(VerificationFieldDisplayData("displayName2"))
                        )
                    ),
                entryDisplayData =
                    setOf(
                        VerificationEntryDisplayData(
                            title = "document-1",
                            subtitle = "test-subtitle",
                            icon = Bitmap.createBitmap(4, 4, Bitmap.Config.ALPHA_8)
                        )
                    ),
                id = "id-1"
            )
        val entry2 =
            MdocEntry(
                docType = "org.iso.18013.5.1.mDL",
                fields =
                    listOf(
                        MdocField(
                            "fieldName",
                            "fieldVal",
                            setOf(VerificationFieldDisplayData("displayName1"))
                        )
                    ),
                entryDisplayData =
                    setOf(
                        VerificationEntryDisplayData(
                            title = "document-2",
                            subtitle = null,
                            icon = Bitmap.createBitmap(32, 32, Bitmap.Config.ALPHA_8)
                        )
                    ),
                id = "id-2"
            )

        val registry = PreviewRegistry(credentialEntries = listOf(entry1, entry2))

        assertThat(registry.id)
            .isEqualTo("androidx.credentials.registry.digitalcredentials.preview.ID_PREVIEW")
        assertThat(registry.matcher).isEqualTo(getDefaultPreviewMatcher())
        assertThat(registry.credentials).isEqualTo(listOf(entry1, entry2).toCredentialBytes())
    }

    @Test
    fun construction_nonDefaultId_success() {
        val entry =
            MdocEntry(
                docType = "org.iso.18013.5.1.mDL",
                fields =
                    listOf(
                        MdocField(
                            "fieldName1",
                            "fieldVal1",
                            setOf(VerificationFieldDisplayData("displayName1"))
                        ),
                        MdocField(
                            "fieldName2",
                            null,
                            setOf(VerificationFieldDisplayData("displayName2"))
                        )
                    ),
                entryDisplayData =
                    setOf(
                        VerificationEntryDisplayData(
                            title = "document-1",
                            subtitle = "test-subtitle",
                            icon = Bitmap.createBitmap(4, 4, Bitmap.Config.ALPHA_8)
                        )
                    ),
                id = "id-1"
            )

        val registry = PreviewRegistry(credentialEntries = listOf(entry), id = "registry-id")

        assertThat(registry.id).isEqualTo("registry-id")
        assertThat(registry.matcher).isEqualTo(getDefaultPreviewMatcher())
        assertThat(registry.credentials).isEqualTo(listOf(entry).toCredentialBytes())
    }
}
