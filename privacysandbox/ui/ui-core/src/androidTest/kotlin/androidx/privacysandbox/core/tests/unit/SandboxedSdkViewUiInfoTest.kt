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

package androidx.privacysandbox.core.tests.unit

import android.graphics.Rect
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SandboxedUiAdapterSignalOptions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SandboxedSdkViewUiInfoTest {

    val info =
        SandboxedSdkViewUiInfo(
            100,
            200,
            Rect(0, 0, 100, 200),
            1.0f,
            listOf(Rect(0, 0, 10, 10), Rect(10, 10, 20, 20)),
        )

    @Test
    fun bundlingAndUnbundlingCreatesSameObject() {
        val newInfo = SandboxedSdkViewUiInfo.fromBundle(SandboxedSdkViewUiInfo.toBundle(info))
        assertThat(newInfo).isEqualTo(info)
    }

    @Test
    fun pruningObstructions_removesObstructionsFromBundle() {
        val infoBundle = SandboxedSdkViewUiInfo.toBundle(info)
        SandboxedSdkViewUiInfo.pruneBundle(
            infoBundle,
            setOf(SandboxedUiAdapterSignalOptions.GEOMETRY),
        )
        val infoWithoutObstructions = SandboxedSdkViewUiInfo.fromBundle(infoBundle)
        assertThat(infoWithoutObstructions.obstructedGeometry).isEmpty()
    }

    @Test
    fun pruningWithAllSupportedSignalOptions_doesntChangeBundle() {
        val infoBundle = SandboxedSdkViewUiInfo.toBundle(info)
        SandboxedSdkViewUiInfo.pruneBundle(
            infoBundle,
            setOf(
                SandboxedUiAdapterSignalOptions.GEOMETRY,
                SandboxedUiAdapterSignalOptions.OBSTRUCTIONS,
            ),
        )
        val updatedInfo = SandboxedSdkViewUiInfo.fromBundle(infoBundle)
        assertThat(updatedInfo).isEqualTo(info)
    }
}
