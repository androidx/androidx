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

package androidx.pdf.view

import android.R as androidR
import android.graphics.Point
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.pdf.R
import androidx.pdf.selection.ContextMenuComponent
import androidx.pdf.selection.PdfSelectionMenuKeys
import androidx.pdf.selection.SelectionMenuComponent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewSelectionMenuTest {

    @Before
    fun before() {
        val fakePdfDocument = FakePdfDocument(List(100) { Point(500, 1000) })
        PdfViewTestActivity.onCreateCallback = { activity ->
            val container = FrameLayout(activity)
            container.addView(
                PdfView(activity).apply {
                    pdfDocument = fakePdfDocument
                    id = R.id.pdf_view
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            activity.setContentView(container)
        }
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testContextMenu_withDefaultOptions() {
        val selectionMenuItemPreparer = SelectionMenuItemPreparer()

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(withId(R.id.pdf_view)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val localPdfView = view as PdfView
                localPdfView.setSelectionMenuItemPreparer(selectionMenuItemPreparer)
            }
        }
        // long click to trigger selection
        longClickAtCenter()

        assert(selectionMenuItemPreparer.components.size == 2)
        onView(withText(androidR.string.copy))
            .inRoot(RootMatchers.isPlatformPopup())
            .check(matches(isDisplayed()))
        onView(withText(androidR.string.selectAll))
            .inRoot(RootMatchers.isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testContextMenu_afterAddingAddCommentItem() {
        var addCommentClickCounter = 0
        val addCommentLabel = "Add comment"

        val selectionMenuItemPreparer = SelectionMenuItemPreparer { components ->
            components.add(
                SelectionMenuComponent(AddCommentKey, addCommentLabel) {
                    // Increment counter to assert onClick is called
                    addCommentClickCounter++
                }
            )
        }

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(withId(R.id.pdf_view)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val localPdfView = view as PdfView
                localPdfView.setSelectionMenuItemPreparer(selectionMenuItemPreparer)
            }
        }
        // long click to trigger selection
        longClickAtCenter()

        assert(selectionMenuItemPreparer.components.size == 3)
        onView(withText(androidR.string.copy))
            .inRoot(RootMatchers.isPlatformPopup())
            .check(matches(isDisplayed()))
        onView(withText(androidR.string.selectAll))
            .inRoot(RootMatchers.isPlatformPopup())
            .check(matches(isDisplayed()))
        // Assert new option actually shown on context menu
        onView(withText(addCommentLabel))
            .inRoot(RootMatchers.isPlatformPopup())
            .check(matches(isDisplayed()))
            .perform(click())
        // Assert click trigger
        assert(addCommentClickCounter == 1)
    }

    @Test
    fun testContextMenu_afterRemovingSelectAll() {
        val selectionMenuItemPreparer = SelectionMenuItemPreparer { components ->
            components.removeAll { it.key == PdfSelectionMenuKeys.SelectAllKey }
        }

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(withId(R.id.pdf_view)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val localPdfView = view as PdfView
                localPdfView.setSelectionMenuItemPreparer(selectionMenuItemPreparer)
            }
        }
        // long click to trigger selection
        longClickAtCenter()

        assert(selectionMenuItemPreparer.components.size == 1)
        onView(withText(androidR.string.copy))
            .inRoot(RootMatchers.isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    private fun longClickAtCenter() {
        onView(withId(R.id.pdf_view))
            .perform(
                GeneralClickAction(
                    Tap.LONG,
                    { view ->
                        GeneralLocation.CENTER.calculateCoordinates(view).map { it }.toFloatArray()
                    },
                    Press.THUMB,
                    InputDevice.SOURCE_UNKNOWN,
                    MotionEvent.BUTTON_PRIMARY,
                )
            )
    }

    class SelectionMenuItemPreparer(
        private val newMenuItems: ((components: MutableList<ContextMenuComponent>) -> Unit)? = null
    ) : PdfView.SelectionMenuItemPreparer {
        public var components = mutableListOf<ContextMenuComponent>()

        override fun onPrepareSelectionMenuItems(components: MutableList<ContextMenuComponent>) {
            newMenuItems?.invoke(components)
            this.components = components
        }
    }

    private object AddCommentKey
}
