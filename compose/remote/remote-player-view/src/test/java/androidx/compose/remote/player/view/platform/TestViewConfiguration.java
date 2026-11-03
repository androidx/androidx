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

package androidx.compose.remote.player.view.platform;

import android.view.ViewConfiguration;

import org.jspecify.annotations.Nullable;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowViewConfiguration;

/**
 * A {@link ViewConfiguration} shadow that lets a test choose the gesture values of the platform.
 *
 * <p>Set the values before the player is created: the player reads them when it is created. A
 * value that is not set keeps the default of Robolectric. Call {@link #resetGestureValues()} after
 * each test.
 */
@Implements(ViewConfiguration.class)
public class TestViewConfiguration extends ShadowViewConfiguration {
    private static @Nullable Integer sScaledTouchSlop = null;
    private static @Nullable Integer sScaledDoubleTapSlop = null;
    private static @Nullable Integer sLongPressTimeout = null;
    private static @Nullable Integer sDoubleTapTimeout = null;

    /** Sets the value of {@link ViewConfiguration#getScaledTouchSlop()}, in pixels. */
    public static void setScaledTouchSlop(int scaledTouchSlop) {
        sScaledTouchSlop = scaledTouchSlop;
    }

    /** Sets the value of {@link ViewConfiguration#getScaledDoubleTapSlop()}, in pixels. */
    public static void setScaledDoubleTapSlop(int scaledDoubleTapSlop) {
        sScaledDoubleTapSlop = scaledDoubleTapSlop;
    }

    /** Sets the value of {@link ViewConfiguration#getLongPressTimeout()}, in milliseconds. */
    public static void setLongPressTimeout(int longPressTimeout) {
        sLongPressTimeout = longPressTimeout;
    }

    /** Sets the value of {@link ViewConfiguration#getDoubleTapTimeout()}, in milliseconds. */
    public static void setDoubleTapTimeout(int doubleTapTimeout) {
        sDoubleTapTimeout = doubleTapTimeout;
    }

    /** Goes back to the defaults of Robolectric for all the values. */
    public static void resetGestureValues() {
        sScaledTouchSlop = null;
        sScaledDoubleTapSlop = null;
        sLongPressTimeout = null;
        sDoubleTapTimeout = null;
    }

    @Implementation
    @Override
    protected int getScaledTouchSlop() {
        return sScaledTouchSlop != null ? sScaledTouchSlop : super.getScaledTouchSlop();
    }

    @Implementation
    @Override
    protected int getScaledDoubleTapSlop() {
        return sScaledDoubleTapSlop != null ? sScaledDoubleTapSlop : super.getScaledDoubleTapSlop();
    }

    @Implementation
    protected static int getLongPressTimeout() {
        return sLongPressTimeout != null
                ? sLongPressTimeout
                : ShadowViewConfiguration.getLongPressTimeout();
    }

    @Implementation
    protected static int getDoubleTapTimeout() {
        return sDoubleTapTimeout != null
                ? sDoubleTapTimeout
                : ShadowViewConfiguration.getDoubleTapTimeout();
    }
}
