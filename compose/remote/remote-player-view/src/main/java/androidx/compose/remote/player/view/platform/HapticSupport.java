/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.core.view.HapticFeedbackConstantsCompat;

/** Provides haptic support */
@RestrictTo(LIBRARY_GROUP)
public class HapticSupport {

    private static final int[] sHapticTable;

    static {
        // BEGIN-REMOVE-IN-PLATFORM
        /*
        // END-REMOVE-IN-PLATFORM
        sHapticTable = new int[] {
                android.view.HapticFeedbackConstants.NO_HAPTICS,
                android.view.HapticFeedbackConstants.LONG_PRESS,
                android.view.HapticFeedbackConstants.VIRTUAL_KEY,
                android.view.HapticFeedbackConstants.KEYBOARD_TAP,
                android.view.HapticFeedbackConstants.CLOCK_TICK,
                android.view.HapticFeedbackConstants.CONTEXT_CLICK,
                android.view.HapticFeedbackConstants.KEYBOARD_PRESS,
                android.view.HapticFeedbackConstants.KEYBOARD_RELEASE,
                android.view.HapticFeedbackConstants.VIRTUAL_KEY_RELEASE,
                android.view.HapticFeedbackConstants.TEXT_HANDLE_MOVE,
                android.view.HapticFeedbackConstants.GESTURE_START,
                android.view.HapticFeedbackConstants.GESTURE_END,
                android.view.HapticFeedbackConstants.CONFIRM,
                android.view.HapticFeedbackConstants.REJECT,
                android.view.HapticFeedbackConstants.TOGGLE_ON,
                android.view.HapticFeedbackConstants.TOGGLE_OFF,
                android.view.HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE,
                android.view.HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE,
                android.view.HapticFeedbackConstants.DRAG_START,
                android.view.HapticFeedbackConstants.SEGMENT_TICK,
                android.view.HapticFeedbackConstants.SEGMENT_FREQUENT_TICK,
        };
        // BEGIN-REMOVE-IN-PLATFORM
        */
        // END-REMOVE-IN-PLATFORM

        // BEGIN-REMOVE-IN-PLATFORM
        sHapticTable =
                new int[] {
                    HapticFeedbackConstantsCompat.NO_HAPTICS,
                    HapticFeedbackConstantsCompat.LONG_PRESS,
                    HapticFeedbackConstantsCompat.VIRTUAL_KEY,
                    HapticFeedbackConstantsCompat.KEYBOARD_TAP,
                    HapticFeedbackConstantsCompat.CLOCK_TICK,
                    HapticFeedbackConstantsCompat.CONTEXT_CLICK,
                    HapticFeedbackConstantsCompat.KEYBOARD_PRESS,
                    HapticFeedbackConstantsCompat.KEYBOARD_RELEASE,
                    HapticFeedbackConstantsCompat.VIRTUAL_KEY_RELEASE,
                    HapticFeedbackConstantsCompat.TEXT_HANDLE_MOVE,
                    HapticFeedbackConstantsCompat.GESTURE_START,
                    HapticFeedbackConstantsCompat.GESTURE_END,
                    HapticFeedbackConstantsCompat.CONFIRM,
                    HapticFeedbackConstantsCompat.REJECT,
                    HapticFeedbackConstantsCompat.TOGGLE_ON,
                    HapticFeedbackConstantsCompat.TOGGLE_OFF,
                    HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE,
                    HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_DEACTIVATE,
                    HapticFeedbackConstantsCompat.DRAG_START,
                    HapticFeedbackConstantsCompat.SEGMENT_TICK,
                    HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK,
                };
        // END-REMOVE-IN-PLATFORM
    }

    /**
     * Setup the haptic responses
     *
     * @param view
     */
    public void setupHaptics(RemoteComposeView view) {
        view.setHapticEngine(
                new CoreDocument.HapticEngine() {
                    @RequiresApi(api = Build.VERSION_CODES.S) // REMOVE IN PLATFORM
                    @Override
                    public void haptic(int type) {
                        view.performHapticFeedback(sHapticTable[type % sHapticTable.length]);
                    }
                });
    }
}
