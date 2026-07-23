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

package androidx.pdf.view

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Point
import android.graphics.RectF
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.FakePdfDocument
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.selection.SelectionModel
import androidx.pdf.selection.SelectionStateManager
import androidx.pdf.view.PdfView.Companion.findActivity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfViewSavedStateTest {

    private class TestActivity : Activity() {
        var changingConfigurations: Boolean = false

        override fun isChangingConfigurations(): Boolean = changingConfigurations
    }

    private class TestPdfView(context: android.content.Context) : PdfView(context) {
        public override fun onSaveInstanceState(): android.os.Parcelable? =
            super.onSaveInstanceState()
    }

    @OptIn(ExperimentalPdfApi::class)
    private fun createFakeSelectionStateManager(
        initialSelection: SelectionModel?
    ): SelectionStateManager =
        SelectionStateManager(
            pdfDocument = FakePdfDocument(),
            backgroundScope = TestScope(),
            handleTouchTargetSizePx = 48,
            errorFlow = MutableSharedFlow(),
            pageLayoutManager = null,
            pageManager = null,
            initialSelection = initialSelection,
        )

    @Test
    fun findActivity_withContextWrapperChain_returnsActivity() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val wrapper1 = ContextWrapper(activity)
        val wrapper2 = ContextWrapper(wrapper1)

        assertThat(wrapper2.findActivity()).isEqualTo(activity)
    }

    @Test
    fun onSaveInstanceState_whenChangingConfigurations_savesFullSelectionModel() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        activity.changingConfigurations = true
        val contextWrapper = ContextWrapper(activity)

        val pdfView = TestPdfView(contextWrapper)
        val boundary = SelectionBoundary(0, Point(10, 10), false)
        val pageSelections =
            listOf(
                PageSelection(
                    1,
                    boundary,
                    boundary,
                    listOf(PdfPageTextContent(listOf(RectF(0f, 0f, 100f, 100f)), "Sample Text")),
                )
            )
        val fullModel = SelectionModel.create(pageSelections)
        assertThat(fullModel?.isPlaceholder).isFalse()

        pdfView.selectionStateManager = createFakeSelectionStateManager(fullModel)

        val savedState = pdfView.onSaveInstanceState() as PdfViewSavedState
        assertThat(savedState.selectionModel).isNotNull()
        assertThat(savedState.selectionModel?.isPlaceholder).isFalse()
    }

    @Test
    fun onSaveInstanceState_whenNotChangingConfigurations_savesPlaceholderSelectionModel() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        activity.changingConfigurations = false
        val contextWrapper = ContextWrapper(activity)

        val pdfView = TestPdfView(contextWrapper)
        val boundary = SelectionBoundary(0, Point(10, 10), false)
        val pageSelections =
            listOf(
                PageSelection(
                    1,
                    boundary,
                    boundary,
                    listOf(PdfPageTextContent(listOf(RectF(0f, 0f, 100f, 100f)), "Sample Text")),
                )
            )
        val fullModel = SelectionModel.create(pageSelections)
        assertThat(fullModel?.isPlaceholder).isFalse()

        pdfView.selectionStateManager = createFakeSelectionStateManager(fullModel)

        val savedState = pdfView.onSaveInstanceState() as PdfViewSavedState
        assertThat(savedState.selectionModel).isNotNull()
        assertThat(savedState.selectionModel?.isPlaceholder).isTrue()
    }
}
