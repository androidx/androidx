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

import android.content.res.Configuration
import android.graphics.Rect
import androidx.core.view.WindowInsetsCompat
import androidx.window.WindowSdkExtensionsRule
import androidx.window.core.PredicateAdapter
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Verifies [OverlayControllerImpl]
 */
@Suppress("GuardedBy")
class OverlayControllerImplTest {

    @get:Rule
    val testRule = WindowSdkExtensionsRule()

    private lateinit var overlayController: OverlayControllerImpl

    @Before
    fun setUp() {
        testRule.overrideExtensionVersion(5)

        overlayController = OverlayControllerImpl(
            mock<ActivityEmbeddingComponent>(),
            EmbeddingAdapter(PredicateAdapter(ClassLoader.getSystemClassLoader())),
        )
    }

    /** Verifies the behavior of [OverlayControllerImpl.calculateOverlayAttributes] */
    @Test
    fun testCalculateOverlayAttributes() {
        assertThat(overlayController.calculateOverlayAttributes(DEFAULT_OVERLAY_ATTRS))
            .isEqualTo(DEFAULT_OVERLAY_ATTRS)

        overlayController.overlayAttributesCalculator = { _ -> CALCULATED_OVERLAY_ATTRS }

        assertWithMessage("Calculated overlay attrs must be reported if calculator exists.")
            .that(overlayController.calculateOverlayAttributes(DEFAULT_OVERLAY_ATTRS))
            .isEqualTo(CALCULATED_OVERLAY_ATTRS)
    }

    private fun OverlayControllerImpl.calculateOverlayAttributes(
        initialOverlayAttrs: OverlayAttributes?
    ): OverlayAttributes = calculateOverlayAttributes(
        TAG_TEST,
        initialOverlayAttrs,
        WindowMetrics(Rect(), WindowInsetsCompat.CONSUMED),
        Configuration(),
        WindowLayoutInfo(emptyList())
    )

    companion object {
        private const val TAG_TEST = "test"

        private val DEFAULT_OVERLAY_ATTRS = OverlayAttributes.Builder().build()

        private val CALCULATED_OVERLAY_ATTRS = OverlayAttributes.Builder()
            .setBounds(EmbeddingBounds.BOUNDS_HINGE_RIGHT)
            .build()
    }
}
