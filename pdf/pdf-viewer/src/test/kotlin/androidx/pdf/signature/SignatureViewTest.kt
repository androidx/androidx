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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class SignatureViewTest {

    @Test
    fun setSignature_callsInvalidate() {
        val view = SignatureView(ApplicationProvider.getApplicationContext())
        val shadowView = shadowOf(view)
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

        view.setSignature(signature)

        assertThat(shadowView.wasInvalidated()).isTrue()
    }

    @Test
    fun setSameSignature_doesNotCallInvalidate() {
        val view = SignatureView(ApplicationProvider.getApplicationContext())
        val shadowView = shadowOf(view)

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

        view.setSignature(signature)
        shadowView.clearWasInvalidated()

        view.setSignature(signature)

        assertThat(shadowView.wasInvalidated()).isFalse()
    }

    @Test
    fun setDifferentSignature_updatesModelAndCallsInvalidate() {
        val view = SignatureView(ApplicationProvider.getApplicationContext())
        val shadowView = shadowOf(view)

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
        view.setSignature(initialSignature)

        shadowView.clearWasInvalidated()

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

        view.setSignature(updatedSignature)
        assertThat(shadowView.wasInvalidated()).isTrue()
    }
}
