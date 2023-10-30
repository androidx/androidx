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

import android.os.Bundle
import androidx.window.core.ExperimentalWindowApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalWindowApi::class)
class OverlayControllerTest {
    private val mockBackend = mock<EmbeddingBackend>()
    private val overlayController = OverlayController(mockBackend)

    @Test
    fun testSetOverlayCreateParams() {
        val options = Bundle()
        val params = OverlayCreateParams()
        overlayController.setOverlayCreateParams(options, params)

        verify(mockBackend).setOverlayCreateParams(options, params)
    }
}
