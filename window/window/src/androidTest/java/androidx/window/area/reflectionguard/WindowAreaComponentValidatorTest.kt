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

package androidx.window.area.reflectionguard

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import androidx.window.extensions.area.ExtensionWindowAreaPresentation
import androidx.window.extensions.area.ExtensionWindowAreaStatus
import androidx.window.extensions.area.WindowAreaComponent
import androidx.window.extensions.core.util.function.Consumer
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Unit test for [WindowAreaComponentValidator]
 */
class WindowAreaComponentValidatorTest {

    /**
     * Test that validator returns true if the component fully implements [WindowAreaComponent]
     */
    @Test
    fun isWindowAreaComponentValid_fullImplementation() {
        assertTrue(
            WindowAreaComponentValidator.isWindowAreaComponentValid(
                WindowAreaComponentFullImplementation::class.java, 2))
        assertTrue(
            WindowAreaComponentValidator.isWindowAreaComponentValid(
                WindowAreaComponentFullImplementation::class.java, 3))
    }

    /**
     * Test that validator returns correct results for API Level 2 [WindowAreaComponent]
     * implementation.
     */
    @Test
    fun isWindowAreaComponentValid_apiLevel2() {
        assertTrue(
            WindowAreaComponentValidator.isWindowAreaComponentValid(
                WindowAreaComponentApiV2Implementation::class.java, 2))
        assertFalse(
            WindowAreaComponentValidator.isWindowAreaComponentValid(
                IncompleteWindowAreaComponentApiV2Implementation::class.java, 3))
    }

    /**
     * Test that validator returns correct results for API Level 3 [WindowAreaComponent]
     * implementation.
     */
    @Test
    fun isWindowAreaComponentValid_apiLevel3() {
        assertTrue(
            WindowAreaComponentValidator.isWindowAreaComponentValid(
                WindowAreaComponentApiV3Implementation::class.java, 2))
        assertTrue(
            WindowAreaComponentValidator.isWindowAreaComponentValid(
                WindowAreaComponentApiV3Implementation::class.java, 3))
    }

    /**
     * Test that validator returns false if the component implementation is incomplete
     */
    @Test
    fun isWindowAreaComponentValid_falseIfIncompleteImplementation() {
        assertFalse(
            WindowAreaComponentValidator.isWindowAreaComponentValid(
                IncompleteWindowAreaComponentApiV2Implementation::class.java, 2))
    }

    /**
     * Test that validator returns true if the [ExtensionWindowAreaStatus] is valid
     */
    @Test
    fun isExtensionWindowAreaStatusValid_trueIfValid() {
        assertTrue(
            WindowAreaComponentValidator.isExtensionWindowAreaStatusValid(
                ValidExtensionWindowAreaStatus::class.java, 2))
        assertTrue(
            WindowAreaComponentValidator.isExtensionWindowAreaStatusValid(
                ValidExtensionWindowAreaStatus::class.java, 3))
    }

    /**
     * Test that validator returns false if the [ExtensionWindowAreaStatus] is incomplete
     */
    @Test
    fun isExtensionWindowAreaStatusValid_falseIfIncomplete() {
        assertFalse(
            WindowAreaComponentValidator.isExtensionWindowAreaStatusValid(
                IncompleteExtensionWindowAreaStatus::class.java, 2))
        assertFalse(
            WindowAreaComponentValidator.isExtensionWindowAreaStatusValid(
                IncompleteExtensionWindowAreaStatus::class.java, 3))
    }

    /**
     * Test that validator returns true if the [ExtensionWindowAreaPresentation] is valid
     */
    @Test
    fun isExtensionWindowAreaPresentationValid_trueIfValid() {
        assertTrue(
            WindowAreaComponentValidator.isExtensionWindowAreaPresentationValid(
                ValidExtensionWindowAreaPresentation::class.java, 3))
    }

    /**
     * Test that validator returns false if the [ExtensionWindowAreaPresentation] is incomplete
     */
    @Test
    fun isExtensionWindowAreaPresentationValid_falseIfIncomplete() {
        assertFalse(
            WindowAreaComponentValidator.isExtensionWindowAreaPresentationValid(
                IncompleteExtensionWindowAreaPresentation::class.java, 3))
    }

    private class WindowAreaComponentFullImplementation : WindowAreaComponent {
        override fun addRearDisplayStatusListener(consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        override fun removeRearDisplayStatusListener(consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        override fun startRearDisplaySession(activity: Activity, consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        override fun endRearDisplaySession() {
            throw NotImplementedError("Not implemented")
        }
    }

    private class WindowAreaComponentApiV2Implementation : WindowAreaComponentApi2Requirements {
        override fun addRearDisplayStatusListener(consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        override fun removeRearDisplayStatusListener(consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        override fun startRearDisplaySession(activity: Activity, consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        override fun endRearDisplaySession() {
            throw NotImplementedError("Not implemented")
        }
    }

    private class WindowAreaComponentApiV3Implementation : WindowAreaComponentApi3Requirements {
        override fun addRearDisplayStatusListener(consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        override fun removeRearDisplayStatusListener(consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        override fun startRearDisplaySession(activity: Activity, consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        override fun endRearDisplaySession() {
            throw NotImplementedError("Not implemented")
        }

        override fun addRearDisplayPresentationStatusListener(
            consumer: Consumer<ExtensionWindowAreaStatus>
        ) {
            throw NotImplementedError("Not implemented")
        }

        override fun removeRearDisplayPresentationStatusListener(
            consumer: Consumer<ExtensionWindowAreaStatus>
        ) {
            throw NotImplementedError("Not implemented")
        }

        override fun startRearDisplayPresentationSession(
            activity: Activity,
            consumer: Consumer<Int>
        ) {
            throw NotImplementedError("Not implemented")
        }

        override fun endRearDisplayPresentationSession() {
            throw NotImplementedError("Not implemented")
        }

        override fun getRearDisplayPresentation(): ExtensionWindowAreaPresentation? {
            throw NotImplementedError("Not implemented")
        }
    }

    private class IncompleteWindowAreaComponentApiV2Implementation {
        @Suppress("UNUSED_PARAMETER")
        fun addRearDisplayStatusListener(consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }

        @Suppress("UNUSED_PARAMETER")
        fun removeRearDisplayStatusListener(consumer: Consumer<Int>) {
            throw NotImplementedError("Not implemented")
        }
    }

    private class ValidExtensionWindowAreaPresentation : ExtensionWindowAreaPresentation {
        override fun getPresentationContext(): Context {
            throw NotImplementedError("Not implemented")
        }

        override fun setPresentationView(view: View) {
            throw NotImplementedError("Not implemented")
        }
    }

    private class IncompleteExtensionWindowAreaPresentation {
        fun getPresentationContext(): Context {
            throw NotImplementedError("Not implemented")
        }
    }

    private class ValidExtensionWindowAreaStatus : ExtensionWindowAreaStatus {
        override fun getWindowAreaStatus(): Int {
            throw NotImplementedError("Not implemented")
        }

        override fun getWindowAreaDisplayMetrics(): DisplayMetrics {
            throw NotImplementedError("Not implemented")
        }
    }

    private class IncompleteExtensionWindowAreaStatus {
        fun getWindowAreaStatus(): Int {
            throw NotImplementedError("Not implemented")
        }
    }
}
