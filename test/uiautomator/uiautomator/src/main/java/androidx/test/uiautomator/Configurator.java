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
 * Allows you to set key parameters for running UiAutomator tests. The new settings take effect
 * immediately and can be changed at any time during a test run.
 *
 * <p>To modify parameters, first obtain an instance by calling {@link #getInstance()}. As a best
 * practice, make sure you always save the original value of any parameter that you are modifying.
 * After running your tests with the modified parameters, make sure to also restore the original
 * parameter values, otherwise this will impact other tests cases.
 */
public final class Configurator {

    private long mWaitForIdleTimeout = 10_000; // ms
    private long mWaitForSelector = 10_000; // ms
    private long mWaitForActionAcknowledgment = 3_000; // ms

    // Scroll timeout used only in InteractionController
    private long mScrollEventWaitTimeout = 1_000; // ms

    // Default is inject as fast as we can
    private long mKeyInjectionDelay = 0; // ms

    // Default tool type is a finger
    private int mToolType = MotionEvent.TOOL_TYPE_FINGER;

    // Default flags to use when calling Instrumentation.getUiAutomation(int)
    static final int DEFAULT_UIAUTOMATION_FLAGS = 0;
    private int mUiAutomationFlags = DEFAULT_UIAUTOMATION_FLAGS;

    // Singleton instance.
    private static Configurator sConfigurator;

    private Configurator() {}

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
     * Sets the timeout for waiting for the user interface to go into an idle state before
     * starting a UiAutomator action.
     *
     * @param timeout Timeout value in milliseconds
     * @return self
     */
    public @NonNull Configurator setWaitForIdleTimeout(long timeout) {
        mWaitForIdleTimeout = timeout;
        return this;
    }

    /**
     * Gets the current timeout used for waiting for the user interface to go into an idle state
     * before starting a UiAutomator action.
     *
     * @return current timeout in milliseconds
     */
    public long getWaitForIdleTimeout() {
        return mWaitForIdleTimeout;
    }

    /**
     * Sets the timeout for waiting for a {@link UiObject} to become visible in the user
     * interface so that it can be matched by a {@link UiSelector}.
     *
     * <p>For {@link UiObject}s, the underlying node is fetched with this timeout before every
     * action.
     *
     * <p>Has no effect on {@link UiObject2}s, which cache the underlying node. Instead, use
     * {@link UiDevice#wait} and {@link Until#findObject} to wait for the object to become visible.
     *
     * @param timeout Timeout value in milliseconds
     * @return self
     */
    public @NonNull Configurator setWaitForSelectorTimeout(long timeout) {
        mWaitForSelector = timeout;
        return this;
    }

    /**
     * Gets the current timeout for waiting for a {@link UiObject} to become visible in the user
     * interface so that it can be matched by a {@link UiSelector}.
     *
     * @return current timeout in milliseconds
     */
    public long getWaitForSelectorTimeout() {
        return mWaitForSelector;
    }

    /**
     * Sets the timeout for waiting for an acknowledgement of a {@link UiScrollable} scroll action.
     *
     * <p>The acknowledgment is an {@link android.view.accessibility.AccessibilityEvent}
     * corresponding to the scroll action that lets the framework determine if it was successful.
     * Generally, this timeout should not be modified.
     *
     * <p>Has no effect on {@link UiObject2} scrolls.
     *
     * @param timeout Timeout value in milliseconds
     * @return self
     */
    public @NonNull Configurator setScrollAcknowledgmentTimeout(long timeout) {
        mScrollEventWaitTimeout = timeout;
        return this;
    }

    /**
     * Gets the current timeout for waiting for an acknowledgement of a {@link UiScrollable}
     * scroll action.
     *
     * @return current timeout in milliseconds
     */
    public long getScrollAcknowledgmentTimeout() {
        return mScrollEventWaitTimeout;
    }

    /**
     * Sets the timeout for waiting for an acknowledgment of a {@link UiObject} click.
     *
     * <p>The acknowledgment is an {@link android.view.accessibility.AccessibilityEvent}
     * corresponding to a content change that lets the framework determine if the action was
     * successful. Generally, this timeout should not be modified.
     *
     * <p>Has no effect on {@link UiDevice} and {@link UiObject2} clicks.
     *
     * @param timeout Timeout value in milliseconds
     * @return self
     */
    public @NonNull Configurator setActionAcknowledgmentTimeout(long timeout) {
        mWaitForActionAcknowledgment = timeout;
        return this;
    }

    /**
     * Gets the current timeout for waiting for an acknowledgment of a {@link UiObject} click.
     *
     * @return current timeout in milliseconds
     */
    public long getActionAcknowledgmentTimeout() {
        return mWaitForActionAcknowledgment;
    }

    /**
     * Sets a delay between key presses when injecting text input.
     *
     * @param delay Delay value in milliseconds
     * @return self
     * @deprecated This parameter is no longer used (text is set directly rather than by key).
     */
    @Deprecated
    public @NonNull Configurator setKeyInjectionDelay(long delay) {
        mKeyInjectionDelay = delay;
        return this;
    }

    /**
     * Gets the current delay between key presses when injecting text input.
     *
     * @return current delay in milliseconds
     * @deprecated This parameter is no longer used (text is set directly rather than by key).
     */
    @Deprecated
    public long getKeyInjectionDelay() {
        return mKeyInjectionDelay;
    }

    /**
     * Sets the tool type to use for motion events.
     *
     * @param toolType The tool type to use
     * @return self
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
     *
     * @return current tool type
     * @see MotionEvent#getToolType(int)
     */
    public int getToolType() {
        return mToolType;
    }

    /**
     * Sets the flags to use when obtaining a {@link android.app.UiAutomation} instance.
     *
     * @param flags The UiAutomation flags to use
     * @return self
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
     * @return UiAutomation flags
     * @see android.app.Instrumentation#getUiAutomation(int)
     * @see android.app.UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
     */
    public int getUiAutomationFlags() {
        return mUiAutomationFlags;
    }
}
