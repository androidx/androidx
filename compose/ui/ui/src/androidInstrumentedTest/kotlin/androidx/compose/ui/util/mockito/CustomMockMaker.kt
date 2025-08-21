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

package androidx.compose.ui.util.mockito

import android.os.Build
import com.android.dx.mockito.DexmakerMockMaker
import com.android.dx.mockito.inline.InlineDexmakerMockMaker
import org.mockito.invocation.MockHandler
import org.mockito.mock.MockCreationSettings
import org.mockito.plugins.InlineMockMaker
import org.mockito.plugins.MockMaker

/**
 * There is no official supported method for mixing dexmaker-mockito with dexmaker-mockito-inline,
 * so this has to be done manually.
 *
 * Inside the build.gradle, dexmaker-mockito is taken first and preferred, and this custom
 * implementation is responsible for delegating to the inline variant should the regular variant
 * fail to instantiate a mock.
 *
 * This allows Mockito to mock final classes on test run on API 28+ devices, while still functioning
 * for normal non-final mocks API <28.
 *
 * This class was originally forked from the CustomMockMaker in androidx core (util).
 */
class CustomMockMaker : InlineMockMaker {

    companion object {
        private val MOCK_MAKERS =
            mutableListOf<MockMaker>(DexmakerMockMaker()).apply {
                // Inline only works on API 28+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    this += InlineDexmakerMockMaker()
                }
            }
    }

    override fun <T> createMock(settings: MockCreationSettings<T>, handler: MockHandler<*>): T? {
        var lastException: Exception? = null
        MOCK_MAKERS.filter { it.isTypeMockable(settings.typeToMock).mockable() }
            .forEach {
                val mock =
                    try {
                        it.createMock(settings, handler)
                    } catch (e: Exception) {
                        lastException = e
                        null
                    }

                if (mock != null) {
                    return mock
                }
            }

        lastException?.let { throw it }
        return null
    }

    override fun getHandler(mock: Any?): MockHandler<*>? {
        MOCK_MAKERS.forEach {
            val handler = it.getHandler(mock)
            if (handler != null) {
                return handler
            }
        }
        return null
    }

    override fun resetMock(
        mock: Any?,
        newHandler: MockHandler<*>?,
        settings: MockCreationSettings<*>?,
    ) {
        MOCK_MAKERS.forEach { it.resetMock(mock, newHandler, settings) }
    }

    override fun isTypeMockable(type: Class<*>?): MockMaker.TypeMockability? {
        MOCK_MAKERS.forEachIndexed { index, mockMaker ->
            val mockability = mockMaker.isTypeMockable(type)
            // Prefer the first mockable instance, or the last one available
            if (mockability.mockable() || index == MOCK_MAKERS.size - 1) {
                return mockability
            }
        }
        return null
    }

    override fun clearMock(mock: Any?) {
        MOCK_MAKERS.forEach { (it as? InlineMockMaker)?.clearMock(mock) }
    }

    override fun clearAllMocks() {
        MOCK_MAKERS.forEach { (it as? InlineMockMaker)?.clearAllMocks() }
    }
}
