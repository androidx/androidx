/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.signature

import androidx.pdf.models.Signature
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class SignatureViewTest {

    @Test
    fun settingSignatureData_updatesAllProperties() {
        val view = SignatureView(ApplicationProvider.getApplicationContext())

        val initialSignature =
            Signature.DrawnSignature(
                id = "123",
                pageNum = 0,
                xCoord = 30f,
                yCoord = 20f,
                width = 25f,
                height = 100f,
                isSelected = false,
                drawnPath = emptyList(),
            )

        view.signatureData = initialSignature

        assertThat(view.signatureData?.id).isEqualTo("123")
        assertThat(view.signatureData?.pageNum).isEqualTo(0)
        assertThat(view.signatureData?.xCoord).isEqualTo(30f)
        assertThat(view.signatureData?.yCoord).isEqualTo(20f)
        assertThat(view.signatureData?.width).isEqualTo(25f)
        assertThat(view.signatureData?.height).isEqualTo(100f)
        assertThat(view.signatureData?.isSelected).isFalse()
        assertThat((view.signatureData as? Signature.DrawnSignature)?.drawnPath).isEmpty()

        val updatedSignature =
            Signature.DrawnSignature(
                id = "456",
                pageNum = 1,
                xCoord = 50f,
                yCoord = 60f,
                width = 75f,
                height = 200f,
                isSelected = true,
                drawnPath = emptyList(),
            )

        view.signatureData = updatedSignature

        assertThat(view.signatureData?.id).isEqualTo("456")
        assertThat(view.signatureData?.pageNum).isEqualTo(1)
        assertThat(view.signatureData?.xCoord).isEqualTo(50f)
        assertThat(view.signatureData?.yCoord).isEqualTo(60f)
        assertThat(view.signatureData?.width).isEqualTo(75f)
        assertThat(view.signatureData?.height).isEqualTo(200f)
        assertThat(view.signatureData?.isSelected).isTrue()
        assertThat((view.signatureData as? Signature.DrawnSignature)?.drawnPath).isEmpty()
    }

    @Test
    fun settingSignatureData_callsInvalidate() {
        val view = SignatureView(ApplicationProvider.getApplicationContext())
        val shadowView = org.robolectric.Shadows.shadowOf(view)
        shadowView.clearWasInvalidated()

        val signature =
            Signature.DrawnSignature(
                id = "123",
                pageNum = 0,
                xCoord = 30f,
                yCoord = 20f,
                width = 25f,
                height = 100f,
                isSelected = false,
                drawnPath = emptyList(),
            )
        view.signatureData = signature
        assertThat(shadowView.wasInvalidated()).isTrue()
    }

    @Test
    fun settingSameSignatureData_doesNotCallInvalidate() {
        val view = SignatureView(ApplicationProvider.getApplicationContext())
        val shadowView = org.robolectric.Shadows.shadowOf(view)

        val signature =
            Signature.DrawnSignature(
                id = "123",
                pageNum = 0,
                xCoord = 30f,
                yCoord = 20f,
                width = 25f,
                height = 100f,
                isSelected = false,
                drawnPath = emptyList(),
            )

        view.signatureData = signature
        shadowView.clearWasInvalidated()
        view.signatureData = signature
        assertThat(shadowView.wasInvalidated()).isFalse()
    }

    @Test
    fun togglingSelectionState_callsInvalidate() {
        val view = SignatureView(ApplicationProvider.getApplicationContext())
        val shadowView = org.robolectric.Shadows.shadowOf(view)

        val unselectedSignature =
            Signature.DrawnSignature(
                id = "123",
                pageNum = 0,
                xCoord = 0f,
                yCoord = 0f,
                width = 0f,
                height = 0f,
                isSelected = false,
                drawnPath = emptyList(),
            )
        view.signatureData = unselectedSignature
        shadowView.clearWasInvalidated()

        val selectedSignature =
            Signature.DrawnSignature(
                id = "123",
                pageNum = 0,
                xCoord = 0f,
                yCoord = 0f,
                width = 0f,
                height = 0f,
                isSelected = true,
                drawnPath = emptyList(),
            )
        view.signatureData = selectedSignature

        assertThat(shadowView.wasInvalidated()).isTrue()
    }
}
