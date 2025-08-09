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

package androidx.core.view;

import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper class for accessing values in {@link android.view.HapticFeedbackConstants}.
 */
public final class HapticFeedbackConstantsCompat {

    /**
     * No haptic feedback should be performed. Applications may use this value to indicate skipping
     * a call to {@link android.view.View#performHapticFeedback} entirely, or else rely that it
     * will immediately return {@code false}.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 34: Same behavior, immediately returns false</li>
     * </ul>
     */
    public static final int NO_HAPTICS = -1;

    /**
     * The user has performed a long press on an object that is resulting in an action being
     * performed.
     */
    public static final int LONG_PRESS = 0;

    /**
     * The user has pressed on a virtual on-screen key.
     */
    public static final int VIRTUAL_KEY = 1;

    /**
     * The user has pressed a soft keyboard key.
     */
    public static final int KEYBOARD_TAP = 3;

    /**
     * The user has pressed either an hour or minute tick of a Clock.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static final int CLOCK_TICK = 4;

    /**
     * The user has performed a context click on an object.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 23: Same feedback as CLOCK_TICK</li>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static final int CONTEXT_CLICK = 6;

    /**
     * The user has pressed a virtual or software keyboard key.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 27: Same feedback as KEYBOARD_TAP</li>
     * </ul>
     */
    public static final int KEYBOARD_PRESS = KEYBOARD_TAP; // Platform constant is also the same.

    /**
     * The user has released a virtual keyboard key.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 27: No-op</li>
     * </ul>
     */
    public static final int KEYBOARD_RELEASE = 7;

    /**
     * The user has released a virtual key.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 27: No-op</li>
     * </ul>
     */
    public static final int VIRTUAL_KEY_RELEASE = 8;

    /**
     * The user has performed a selection/insertion handle move on text field.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 27: No-op</li>
     * </ul>
     */
    public static final int TEXT_HANDLE_MOVE = 9;

    /**
     * The user has started a gesture (e.g. on the soft keyboard).
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 30: Same feedback as VIRTUAL_KEY</li>
     * </ul>
     */
    public static final int GESTURE_START = 12;

    /**
     * The user has finished a gesture (e.g. on the soft keyboard).
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 30: Same feedback as CONTEXT_CLICK</li>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static final int GESTURE_END = 13;

    /**
     * A haptic effect to signal the confirmation or successful completion of a user interaction.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 30: Same feedback as VIRTUAL_KEY</li>
     * </ul>
     */
    public static final int CONFIRM = 16;

    /**
     * A haptic effect to signal the rejection or failure of a user interaction.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 30: Same feedback as LONG_PRESS</li>
     * </ul>
     */
    public static final int REJECT = 17;

    /**
     * The user has toggled a switch or button into the on position.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 34: Same feedback as CONTEXT_CLICK</li>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static final int TOGGLE_ON = 21;

    /**
     * The user has toggled a switch or button into the off position.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 34: Same feedback as CLOCK_TICK</li>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static final int TOGGLE_OFF = 22;

    /**
     * The user is executing a swipe/drag-style gesture, such as pull-to-refresh, where the
     * gesture action is “eligible” at a certain threshold of movement, and can be cancelled by
     * moving back past the threshold. This constant indicates that the user's motion has just
     * passed the threshold for the action to be activated on release.
     *
     * @see #GESTURE_THRESHOLD_DEACTIVATE
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 34: Same feedback as CONTEXT_CLICK</li>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static final int GESTURE_THRESHOLD_ACTIVATE = 23;

    /**
     * The user is executing a swipe/drag-style gesture, such as pull-to-refresh, where the
     * gesture action is “eligible” at a certain threshold of movement, and can be cancelled by
     * moving back past the threshold. This constant indicates that the user's motion has just
     * re-crossed back "under" the threshold for the action to be activated, meaning the gesture is
     * currently in a cancelled state.
     *
     * @see #GESTURE_THRESHOLD_ACTIVATE
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 34: Same feedback as CLOCK_TICK</li>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static final int GESTURE_THRESHOLD_DEACTIVATE = 24;

    /**
     * The user has started a drag-and-drop gesture. The drag target has just been "picked up".
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 34: Same feedback as LONG_PRESS</li>
     * </ul>
     */
    public static final int DRAG_START = 25;

    /**
     * The user is switching between a series of potential choices, for example items in a list
     * or discrete points on a slider.
     *
     * <p>See also {@link #SEGMENT_FREQUENT_TICK} for cases where density of choices is high, and
     * the haptics should be lighter or suppressed for a better user experience.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 34: Same feedback as CONTEXT_CLICK</li>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static final int SEGMENT_TICK = 26;

    /**
     * The user is switching between a series of many potential choices, for example minutes on a
     * clock face, or individual percentages. This constant is expected to be very soft, so as
     * not to be uncomfortable when performed a lot in quick succession. If the device can’t make
     * a suitably soft vibration, then it may not make any vibration.
     *
     * <p>Some specializations of this constant exist for specific actions, notably
     * {@link #CLOCK_TICK} and {@link #TEXT_HANDLE_MOVE}.
     *
     * <p>See also {@link #SEGMENT_TICK}.
     *
     * <p>Compatibility:
     * <ul>
     *     <li>API &lt; 34: Same feedback as CLOCK_TICK</li>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static final int SEGMENT_FREQUENT_TICK = 27;

    /** First constant value, excluding {@link #NO_HAPTICS} constant. */
    @VisibleForTesting
    static final int FIRST_CONSTANT_INT = LONG_PRESS;

    /** Last constant value used. */
    @VisibleForTesting
    static final int LAST_CONSTANT_INT = SEGMENT_FREQUENT_TICK;

    /**
     * Flag for {@link ViewCompat#performHapticFeedback(android.view.View, int, int)}: Ignore the
     * setting in the view for whether to perform haptic feedback, do it always.
     */
    public static final int FLAG_IGNORE_VIEW_SETTING = 0x0001;

    /** Haptic feedback types. */
    @IntDef(value = {
            NO_HAPTICS,
            LONG_PRESS,
            VIRTUAL_KEY,
            KEYBOARD_TAP,
            CLOCK_TICK,
            CONTEXT_CLICK,
            KEYBOARD_PRESS,
            KEYBOARD_RELEASE,
            VIRTUAL_KEY_RELEASE,
            TEXT_HANDLE_MOVE,
            GESTURE_START,
            GESTURE_END,
            CONFIRM,
            REJECT,
            TOGGLE_ON,
            TOGGLE_OFF,
            GESTURE_THRESHOLD_ACTIVATE,
            GESTURE_THRESHOLD_DEACTIVATE,
            DRAG_START,
            SEGMENT_TICK,
            SEGMENT_FREQUENT_TICK
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface HapticFeedbackType {
    }

    /** Flags for performing haptic feedback. */
    @IntDef(flag = true, value = {
            FLAG_IGNORE_VIEW_SETTING
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface HapticFeedbackFlags {
    }

    /**
     * Returns a haptic feedback constant that is available for this platform build.
     *
     * @param feedbackConstant The feedback constant requested
     * @return The same constant, if supported by this platform build, or a supported fallback.
     */
    @HapticFeedbackType
    static int getFeedbackConstantOrFallback(@HapticFeedbackType int feedbackConstant) {
        if (feedbackConstant == NO_HAPTICS) {
            // Skip fallback logic if constant is no-op.
            return NO_HAPTICS;
        }
        if (Build.VERSION.SDK_INT < 34) {
            switch (feedbackConstant) {
                case DRAG_START:
                    feedbackConstant = LONG_PRESS;
                    break;
                case TOGGLE_ON:
                case SEGMENT_TICK:
                case GESTURE_THRESHOLD_ACTIVATE:
                    feedbackConstant = CONTEXT_CLICK;
                    break;
                case TOGGLE_OFF:
                case SEGMENT_FREQUENT_TICK:
                case GESTURE_THRESHOLD_DEACTIVATE:
                    feedbackConstant = CLOCK_TICK;
                    break;
            }
        }
        if (Build.VERSION.SDK_INT < 30) {
            switch (feedbackConstant) {
                case REJECT:
                    feedbackConstant = LONG_PRESS;
                    break;
                case CONFIRM:
                case GESTURE_START:
                    feedbackConstant = VIRTUAL_KEY;
                    break;
                case GESTURE_END:
                    feedbackConstant = CONTEXT_CLICK;
                    break;
            }
        }
        if (Build.VERSION.SDK_INT < 27) {
            switch (feedbackConstant) {
                case KEYBOARD_RELEASE:
                case VIRTUAL_KEY_RELEASE:
                case TEXT_HANDLE_MOVE:
                    feedbackConstant = NO_HAPTICS;
                    break;
            }
        }
        return feedbackConstant;
    }

    private HapticFeedbackConstantsCompat() {}
}
