/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import androidx.ui.unit.Density
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.semantics.inMutuallyExclusiveGroup
import androidx.ui.foundation.semantics.selected
import androidx.ui.foundation.semantics.toggleableState
import androidx.ui.semantics.hidden
import androidx.ui.semantics.testTag
import androidx.ui.test.helpers.FakeSemanticsTreeInteraction
import org.junit.Test

class AssertsTests {

    @Test
    fun assertIsVisible_forVisibleElement_isOk() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.hidden = false
                })
        }

        findByTag("test")
            .assertIsVisible()
    }

    @Test(expected = AssertionError::class)
    fun assertIsVisible_forNotVisibleElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.hidden = true
                })
        }

        findByTag("test")
            .assertIsVisible()
    }

    @Test
    fun assertIsHidden_forHiddenElement_isOk() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.hidden = true
                })
        }

        findByTag("test")
            .assertIsHidden()
    }

    @Test(expected = AssertionError::class)
    fun assertIsHidden_forNotHiddenElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.hidden = false
                })
        }

        findByTag("test")
            .assertIsHidden()
    }

    @Test
    fun assertIsOn_forCheckedElement_isOk() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.toggleableState = ToggleableState.On
                })
        }

        findByTag("test")
            .assertIsOn()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOn_forUncheckedElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.toggleableState = ToggleableState.Off
                })
        }

        findByTag("test")
            .assertIsOn()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOn_forNotToggleableElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                })
        }

        findByTag("test")
            .assertIsOn()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOff_forCheckedElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.toggleableState = ToggleableState.On
                })
        }

        findByTag("test")
            .assertIsOff()
    }

    @Test
    fun assertIsOff_forUncheckedElement_isOk() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.toggleableState = ToggleableState.Off
                })
        }

        findByTag("test")
            .assertIsOff()
    }

    @Test(expected = AssertionError::class)
    fun assertIsOff_forNotToggleableElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                })
        }

        findByTag("test")
            .assertIsOff()
    }

    @Test(expected = AssertionError::class)
    fun assertIsSelected_forNotSelectedElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.selected = false
                })
        }

        findByTag("test")
            .assertIsSelected()
    }

    @Test
    fun assertIsSelected_forSelectedElement_isOk() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.selected = true
                })
        }

        findByTag("test")
            .assertIsSelected()
    }

    @Test(expected = AssertionError::class)
    fun assertIsSelected_forNotSelectableElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                })
        }

        findByTag("test")
            .assertIsSelected()
    }

    @Test(expected = AssertionError::class)
    fun assertIsUnselected_forSelectedElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.selected = true
                })
        }

        findByTag("test")
            .assertIsUnselected()
    }

    @Test
    fun assertIsUnselected_forUnselectedElement_isOk() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.selected = false
                })
        }

        findByTag("test")
            .assertIsUnselected()
    }

    @Test(expected = AssertionError::class)
    fun assertIsUnselected_forNotSelectableElement_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                })
        }

        findByTag("test")
            .assertIsUnselected()
    }

    @Test(expected = AssertionError::class)
    fun assertItemInExclusiveGroup_forItemNotInGroup_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.inMutuallyExclusiveGroup = false
                })
        }

        findByTag("test")
            .assertIsInMutuallyExclusiveGroup()
    }

    @Test(expected = AssertionError::class)
    fun assertItemInExclusiveGroup_forItemWithoutProperty_throwsError() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                })
        }

        findByTag("test")
            .assertIsInMutuallyExclusiveGroup()
    }

    @Test
    fun assertItemInExclusiveGroup_forItemInGroup_isOk() {
        semanticsTreeInteractionFactory = { selector ->
            FakeSemanticsTreeInteraction(selector)
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.inMutuallyExclusiveGroup = true
                })
        }

        findByTag("test")
            .assertIsInMutuallyExclusiveGroup()
    }

    @Test
    fun assertSizesTest_testPixelAssertion() {
        val size = PxSize(50.ipx, 31.ipx)
        val spec = CollectedSizes(size, Density(0f))
        spec.assertWidthEqualsTo { 50.ipx }
        spec.assertHeightEqualsTo { 31.ipx }
    }

    @Test
    fun assertSizesTest_testDpAssertion() {
        val size = PxSize(50.ipx, 30.ipx)
        val spec = CollectedSizes(size, Density(2f))
        spec.assertWidthEqualsTo(25.dp)
        spec.assertHeightEqualsTo(15.dp)
    }

    @Test
    fun assertSizesTest_testSquare() {
        val size = PxSize(50.ipx, 50.ipx)
        val spec = CollectedSizes(size, Density(2f))
        spec.assertIsSquareWithSize(25.dp)
        spec.assertIsSquareWithSize { 50.ipx }
    }
}