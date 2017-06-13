/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.view;

import android.view.KeyEvent;
import android.view.View;

/**
 * Helper for accessing features in {@link KeyEvent}.
 *
 * @deprecated Use {@link KeyEvent} directly.
 */
@Deprecated
public final class KeyEventCompat {
    /**
     * @deprecated Call {@link KeyEvent#normalizeMetaState(int)} directly. This method will
     * be removed in a future release.
     */
    @Deprecated
    public static int normalizeMetaState(int metaState) {
        return KeyEvent.normalizeMetaState(metaState);
    }

    /**
     * @deprecated Call {@link KeyEvent#metaStateHasModifiers(int, int)} directly. This method will
     * be removed in a future release.
     */
    @Deprecated
    public static boolean metaStateHasModifiers(int metaState, int modifiers) {
        return KeyEvent.metaStateHasModifiers(metaState, modifiers);
    }

    /**
     * @deprecated Call {@link KeyEvent#metaStateHasNoModifiers(int)} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static boolean metaStateHasNoModifiers(int metaState) {
        return KeyEvent.metaStateHasNoModifiers(metaState);
    }

    /**
     * @deprecated Call {@link KeyEvent#hasModifiers(int)} directly. This method will be removed in
     * a future release.
     */
    @Deprecated
    public static boolean hasModifiers(KeyEvent event, int modifiers) {
        return event.hasModifiers(modifiers);
    }

    /**
     * @deprecated Call {@link KeyEvent#hasNoModifiers()} directly. This method will be removed in a
     * future release.
     */
    @Deprecated
    public static boolean hasNoModifiers(KeyEvent event) {
        return event.hasNoModifiers();
    }

    /**
     * @deprecated Call {@link KeyEvent#startTracking()} directly. This method will be removed in a
     * future release.
     */
    @Deprecated
    public static void startTracking(KeyEvent event) {
        event.startTracking();
    }

    /**
     * @deprecated Call {@link KeyEvent#isTracking()} directly. This method will be removed in a
     * future release.
     */
    @Deprecated
    public static boolean isTracking(KeyEvent event) {
        return event.isTracking();
    }

    /**
     * @deprecated Call {@link View#getKeyDispatcherState()} directly. This method will be removed
     * in a future release.
     */
    @Deprecated
    public static Object getKeyDispatcherState(View view) {
        return view.getKeyDispatcherState();
    }

    /**
     * @deprecated Call
     * {@link KeyEvent#dispatch(KeyEvent.Callback, KeyEvent.DispatcherState, Object)} directly.
     * This method will be removed in a future release.
     */
    @Deprecated
    public static boolean dispatch(KeyEvent event, KeyEvent.Callback receiver, Object state,
                Object target) {
        return event.dispatch(receiver, (KeyEvent.DispatcherState) state, target);
    }

    /**
     * @deprecated Call {@link KeyEvent#isCtrlPressed()} directly. This method will be removed
     * in a future release.
     */
    @Deprecated
    public static boolean isCtrlPressed(KeyEvent event) {
        return event.isCtrlPressed();
    }

    private KeyEventCompat() {}
}
