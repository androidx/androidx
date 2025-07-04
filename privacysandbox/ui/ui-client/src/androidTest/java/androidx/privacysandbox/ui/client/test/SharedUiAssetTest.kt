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

package androidx.privacysandbox.ui.client.test

import android.content.Context
import android.view.View
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.client.view.SharedUiAsset
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
class SharedUiAssetTest {
    private companion object {
        const val ASSET_ID = "asset-id"
    }

    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var sandboxedSdkView: SandboxedSdkView

    private val sandboxedUiAdapter = TestSandboxedUiAdapter()

    @get:Rule var activityScenarioRule = ActivityScenarioRule(UiLibActivity::class.java)

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        activityScenarioRule.withActivity {
            view = View(context)
            sandboxedSdkView = SandboxedSdkView(context)
        }
    }

    @Test
    fun getView_returnsCorrectValue() {
        val sharedUiAsset = SharedUiAsset(view, ASSET_ID)

        assertThat(sharedUiAsset.view).isEqualTo(view)
    }

    @Test
    fun getAssetId_returnsCorrectValue() {
        val sharedUiAsset = SharedUiAsset(view, ASSET_ID)

        assertThat(sharedUiAsset.assetId).isEqualTo(ASSET_ID)
    }

    @Test
    fun getSandboxedUiAdapter_notSet_returnsNull() {
        val sharedUiAsset = SharedUiAsset(view, ASSET_ID)

        assertThat(sharedUiAsset.sandboxedUiAdapter).isNull()
    }

    @Test
    fun getSandboxedUiAdapter_setForSandboxedSdkView_returnsCorrectValue() {
        val sharedUiAsset =
            SharedUiAsset(sandboxedSdkView, ASSET_ID, sandboxedUiAdapter = sandboxedUiAdapter)

        assertThat(sharedUiAsset.sandboxedUiAdapter).isEqualTo(sandboxedUiAdapter)
    }

    @Test
    fun setSandboxedUiAdapter_setForView_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                SharedUiAsset(view, ASSET_ID, sandboxedUiAdapter = sandboxedUiAdapter)
            }
        assertThat(exception.message)
            .isEqualTo(
                "${SandboxedUiAdapter::class.qualifiedName} can only be set for ${SandboxedSdkView::class.qualifiedName} assets"
            )
    }
}
