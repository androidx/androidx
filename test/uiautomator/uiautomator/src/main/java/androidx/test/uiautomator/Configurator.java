/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.test.uiautomator;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

/**
 * Allows you to set key parameters for running uiautomator tests. The new
 * settings take effect immediately and can be changed any time during a test run.
 *
 * To modify parameters using Configurator, first obtain an instance by calling
 * {@link #getInstance()}. As a best practice, make sure you always save
 * the original value of any parameter that you are modifying. After running your
 * tests with the modified parameters, make sure to also restore
 * the original parameter values, otherwise this will impact other tests cases.
 */
public final class Configurator {
    private long mWaitForIdleTimeout = 10 * 1000;
    private long mWaitForSelector = 10 * 1000;
    private long mWaitForActionAcknowledgment = 3 * 1000;

    // Scroll timeout used only in InteractionController
    private long mScrollEventWaitTimeout = 1_000; // ms

    // Default is inject as fast as we can
    private long mKeyInjectionDelay = 0; // ms

    // Default tool type is a finger
    private int mToolType = MotionEvent.TOOL_TYPE_FINGER;

    // Default flags to use when calling Instrumentation.getUiAutomation(int)
    static final int DEFAULT_UIAUTOMATION_FLAGS = 0;
    private int mUiAutomationFlags = DEFAULT_UIAUTOMATION_FLAGS;

    // reference to self
    private static Configurator sConfigurator;

    private Configurator() {
        /* hide constructor */
    }

    /**
     * Retrieves a singleton instance of Configurator.
     *
     * @return Configurator instance
     */
    public static @NonNull Configurator getInstance() {
        if (sConfigurator == null) {
            sConfigurator = new Configurator();
        }
        return sConfigurator;
    }

    /**
     * Sets the timeout for waiting for the user interface to go into an idle
     * state before starting a uiautomator action.
     *
     * By default, all core uiautomator objects except {@link UiDevice} will perform
     * this wait before starting to search for the widget specified by the
     * object's {@link UiSelector}. Once the idle state is detected or the
     * timeout elapses (whichever occurs first), the object will start to wait
     * for the selector to find a match.
     * See {@link #setWaitForSelectorTimeout(long)}
     *
     * @param timeout Timeout value in milliseconds
     * @return self
     */
    public @NonNull Configurator setWaitForIdleTimeout(long timeout) {
        mWaitForIdleTimeout = timeout;
        return this;
    }

    /**
     * Gets the current timeout used for waiting for the user interface to go
     * into an idle state.
     *
     * By default, all core uiautomator objects except {@link UiDevice} will perform
     * this wait before starting to search for the widget specified by the
     * object's {@link UiSelector}. Once the idle state is detected or the
     * timeout elapses (whichever occurs first), the object will start to wait
     * for the selector to find a match.
     * See {@link #setWaitForSelectorTimeout(long)}
     *
     * @return Current timeout value in milliseconds
     */
    public long getWaitForIdleTimeout() {
        return mWaitForIdleTimeout;
    }

    /**
     * Sets the timeout for waiting for a widget to become visible in the user
     * interface so that it can be matched by a selector.
     *
     * Because user interface content is dynamic, sometimes a widget may not
     * be visible immediately and won't be detected by a selector. This timeout
     * allows the uiautomator framework to wait for a match to be found, up until
     * the timeout elapses.
     *
     * @param timeout Timeout value in milliseconds.
     * @return self
     */
    public @NonNull Configurator setWaitForSelectorTimeout(long timeout) {
        mWaitForSelector = timeout;
        return this;
    }

    /**
     * Gets the current timeout for waiting for a widget to become visible in
     * the user interface so that it can be matched by a selector.
     *
     * Because user interface content is dynamic, sometimes a widget may not
     * be visible immediately and won't be detected by a selector. This timeout
     * allows the uiautomator framework to wait for a match to be found, up until
     * the timeout elapses.
     *
     * @return Current timeout value in milliseconds
     */
    public long getWaitForSelectorTimeout() {
        return mWaitForSelector;
    }

    /**
     * Sets the timeout for waiting for an acknowledgement of an
     * uiautomtor scroll swipe action.
     *
     * The acknowledgment is an <a href="http://developer.android.com/reference/android/view/accessibility/AccessibilityEvent.html">AccessibilityEvent</a>,
     * corresponding to the scroll action, that lets the framework determine if
     * the scroll action was successful. Generally, this timeout should not be modified.
     * See {@link UiScrollable}
     *
     * @param timeout Timeout value in milliseconds
     * @return self
     */
    public @NonNull Configurator setScrollAcknowledgmentTimeout(long timeout) {
        mScrollEventWaitTimeout = timeout;
        return this;
    }

    /**
     * Gets the timeout for waiting for an acknowledgement of an
     * uiautomtor scroll swipe action.
     *
     * The acknowledgment is an <a href="http://developer.android.com/reference/android/view/accessibility/AccessibilityEvent.html">AccessibilityEvent</a>,
     * corresponding to the scroll action, that lets the framework determine if
     * the scroll action was successful. Generally, this timeout should not be modified.
     * See {@link UiScrollable}
     *
     * @return current timeout in milliseconds
     */
    public long getScrollAcknowledgmentTimeout() {
        return mScrollEventWaitTimeout;
    }

    /**
     * Sets the timeout for waiting for an acknowledgment of generic uiautomator
     * actions, such as clicks, text setting, and menu presses.
     *
     * The acknowledgment is an <a href="http://developer.android.com/reference/android/view/accessibility/AccessibilityEvent.html">AccessibilityEvent</a>,
     * corresponding to an action, that lets the framework determine if the
     * action was successful. Generally, this timeout should not be modified.
     * See {@link UiObject}
     *
     * @param timeout Timeout value in milliseconds
     * @return self
     */
    public @NonNull Configurator setActionAcknowledgmentTimeout(long timeout) {
        mWaitForActionAcknowledgment = timeout;
        return this;
    }

    /**
     * Gets the current timeout for waiting for an acknowledgment of generic
     * uiautomator actions, such as clicks, text setting, and menu presses.
     *
     * The acknowledgment is an <a href="http://developer.android.com/reference/android/view/accessibility/AccessibilityEvent.html">AccessibilityEvent</a>,
     * corresponding to an action, that lets the framework determine if the
     * action was successful. Generally, this timeout should not be modified.
     * See {@link UiObject}
     *
     * @return current timeout in milliseconds
     */
    public long getActionAcknowledgmentTimeout() {
        return mWaitForActionAcknowledgment;
    }

    /**
     * Sets a delay between key presses when injecting text input.
     * See {@link UiObject#setText(String)}
     *
     * @param delay Delay value in milliseconds
     * @return self
     */
    public @NonNull Configurator setKeyInjectionDelay(long delay) {
        mKeyInjectionDelay = delay;
        return this;
    }

    /**
     * Gets the current delay between key presses when injecting text input.
     * See {@link UiObject#setText(String)}
     *
     * @return current delay in milliseconds
     */
    public long getKeyInjectionDelay() {
        return mKeyInjectionDelay;
    }

    /**
     * Sets the current tool type to use for motion events.
     * @see MotionEvent#getToolType(int)
     * @see MotionEvent#TOOL_TYPE_FINGER
     * @see MotionEvent#TOOL_TYPE_STYLUS
     * @see MotionEvent#TOOL_TYPE_MOUSE
     * @see MotionEvent#TOOL_TYPE_ERASER
     * @see MotionEvent#TOOL_TYPE_UNKNOWN
     */
    public @NonNull Configurator setToolType(final int toolType) {
        mToolType = toolType;
        return this;
    }

    /**
     * Gets the current tool type to use for motion events.
     * @see MotionEvent#getToolType(int)
     */
    public int getToolType() {
        return mToolType;
    }

    /**
     * Sets the flags to use when obtaining a {@link android.app.UiAutomation} instance.
     *
     * @param flags The UiAutomation flags to use.
     * @return A reference to this object.
     * @see android.app.Instrumentation#getUiAutomation(int)
     * @see android.app.UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
     */
    public @NonNull Configurator setUiAutomationFlags(int flags) {
        mUiAutomationFlags = flags;
        return this;
    }

    /**
     * Gets the current flags that are used to obtain a {@link android.app.UiAutomation} instance.
     *
     * @return The UiAutomation flags.
     * @see android.app.Instrumentation#getUiAutomation(int)
     * @see android.app.UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
     */
    public int getUiAutomationFlags() {
        return mUiAutomationFlags;
    }
}
