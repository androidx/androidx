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

package androidx.window.embedding

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Rect
import androidx.window.WindowSdkExtensionsRule
import androidx.window.core.PredicateAdapter
import androidx.window.embedding.OverlayController.Companion.OVERLAY_FEATURE_VERSION
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

/** Verifies [OverlayControllerImpl] */
@Suppress("GuardedBy")
class OverlayControllerImplTest {

    @get:Rule val testRule = WindowSdkExtensionsRule()

    private lateinit var overlayController: TestableOverlayControllerImpl

    @Before
    fun setUp() {
        testRule.overrideExtensionVersion(OVERLAY_FEATURE_VERSION)

        overlayController = TestableOverlayControllerImpl()
    }

    /** Verifies the behavior of [OverlayControllerImpl.calculateOverlayAttributes] */
    @SuppressLint("NewApi")
    @Test
    fun testCalculateOverlayAttributes() {
        assertThat(overlayController.calculateOverlayAttributes(DEFAULT_OVERLAY_ATTRS))
            .isEqualTo(DEFAULT_OVERLAY_ATTRS)

        overlayController.overlayAttributesCalculator = { _ -> CALCULATED_OVERLAY_ATTRS }

        assertWithMessage("Calculated overlay attrs must be reported if calculator exists.")
            .that(overlayController.calculateOverlayAttributes(DEFAULT_OVERLAY_ATTRS))
            .isEqualTo(CALCULATED_OVERLAY_ATTRS)

        overlayController.updateOverlayAttributes(TAG_TEST, UPDATED_OVERLAY_ATTRS)

        assertWithMessage("Calculated overlay attrs must be reported if calculator exists.")
            .that(overlayController.calculateOverlayAttributes(DEFAULT_OVERLAY_ATTRS))
            .isEqualTo(CALCULATED_OVERLAY_ATTRS)

        overlayController.overlayAttributesCalculator = null

        assertWithMessage("#updateOverlayAttributes should also update the current overlay attrs.")
            .that(overlayController.calculateOverlayAttributes(DEFAULT_OVERLAY_ATTRS))
            .isEqualTo(UPDATED_OVERLAY_ATTRS)
    }

    private fun OverlayControllerImpl.calculateOverlayAttributes(
        initialOverlayAttrs: OverlayAttributes?
    ): OverlayAttributes =
        calculateOverlayAttributes(
            TAG_TEST,
            initialOverlayAttrs,
            WindowMetrics(Rect(), density = 1f),
            Configuration(),
            WindowLayoutInfo(emptyList())
        )

    companion object {
        private const val TAG_TEST = "test"

        private val DEFAULT_OVERLAY_ATTRS = OverlayAttributes.Builder().build()

        private val CALCULATED_OVERLAY_ATTRS =
            OverlayAttributes.Builder().setBounds(EmbeddingBounds.BOUNDS_HINGE_RIGHT).build()

        private val UPDATED_OVERLAY_ATTRS =
            OverlayAttributes.Builder().setBounds(EmbeddingBounds.BOUNDS_HINGE_BOTTOM).build()
    }

    private class TestableOverlayControllerImpl(
        mockExtension: ActivityEmbeddingComponent = mock<ActivityEmbeddingComponent>(),
    ) :
        OverlayControllerImpl(
            mockExtension,
            EmbeddingAdapter(PredicateAdapter(ClassLoader.getSystemClassLoader()))
        ) {
        val overlayTagToAttributesMap = HashMap<String, OverlayAttributes>()

        override fun getUpdatedOverlayAttributes(overlayTag: String): OverlayAttributes? =
            overlayTagToAttributesMap[overlayTag]

        override fun updateOverlayAttributes(
            overlayTag: String,
            overlayAttributes: OverlayAttributes
        ) {
            overlayTagToAttributesMap[overlayTag] = overlayAttributes
        }
    }
}
