/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.slidingpanelayout.widget.helpers

import android.view.View
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

public fun slideClose(): ViewAction? {
    return ViewActions.actionWithAssertions(
        GeneralSwipeAction(
            Swipe.FAST,
            GeneralLocation.CENTER_LEFT,
            GeneralLocation.CENTER_RIGHT,
            Press.FINGER
        )
    )
}

public fun slideOpen(): ViewAction? {
    return ViewActions.actionWithAssertions(
        GeneralSwipeAction(
            Swipe.FAST,
            GeneralLocation.CENTER_RIGHT,
            GeneralLocation.CENTER_LEFT,
            Press.FINGER
        )
    )
}

public fun openPane(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(SlidingPaneLayout::class.java)
        }

        override fun getDescription(): String {
            return "Open the list pane"
        }

        override fun perform(uiController: UiController?, view: View?) {
            var slidingPaneLayout: SlidingPaneLayout? = view as? SlidingPaneLayout
            if (uiController == null || slidingPaneLayout == null) return
            uiController.loopMainThreadUntilIdle()
            slidingPaneLayout.openPane()
            uiController.loopMainThreadUntilIdle()
        }
    }
}