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

package androidx.pdf

import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry

internal object FragmentUtils {

    private const val TEST_DOCUMENT_FILE = "sample.pdf"

    internal fun scenarioLoadDocument(
        scenario: FragmentScenario<TestPdfViewerFragmentV2>,
        filename: String = TEST_DOCUMENT_FILE,
        nextState: Lifecycle.State,
        orientation: Int,
        onDocumentLoading: (() -> Unit)? = null
    ): FragmentScenario<TestPdfViewerFragmentV2> {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)

        scenario.moveToState(nextState)
        scenario.onFragment { it.requireActivity().requestedOrientation = orientation }

        onDocumentLoading?.invoke()

        // Load the document in the fragment
        scenario.onFragment { fragment ->
            // Increment pdf load idling resource to wait for background task to complete.
            fragment.pdfLoadingIdlingResource.increment()
            fragment.documentUri = TestUtils.saveStream(inputStream, fragment.requireContext())
        }

        return scenario
    }
}
