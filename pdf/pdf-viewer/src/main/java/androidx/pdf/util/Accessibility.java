/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Helper methods for interacting with accessibility services. Could be a static utility, but
 * instead is a singleton for easier testing.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public class Accessibility {
    public static final Accessibility INSTANCE = new Accessibility();

    /**
     *
     */
    @NonNull
    public static final Accessibility get() {
        return INSTANCE;
    }

    /** Return if accessibility touch exploration is enabled. */
    public boolean isTouchExplorationEnabled(@NonNull Context context) {
        return getAccessibilityManager(context).isTouchExplorationEnabled();
    }

    /** Returns if accessibility services are currently enabled. */
    public boolean isAccessibilityEnabled(@NonNull Context context) {
        return getAccessibilityManager(context).isEnabled();
    }

    /**
     * Makes an announcement to the accessibility services if accessibility is enabled.
     *
     * @param context The context under which to dispatch the a11y event.
     * @param source  The source of the announcement.
     * @param message The message to be announced.
     */
    public void announce(@NonNull Context context, @NonNull View source, @NonNull String message) {
        if (!isAccessibilityEnabled(context)) {
            return;
        }

        AccessibilityManager am = getAccessibilityManager(context);
        AccessibilityEvent evt = AccessibilityEvent.obtain();
        evt.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        evt.setClassName(source.getClass().getName());
        evt.setPackageName(context.getPackageName());
        evt.getText().add(message);
        evt.setSource(source);
        am.sendAccessibilityEvent(evt);
    }

    /**
     * Calls to {@link #announce(Context, View, String)} using the text from the provided {@code
     * messageId}.
     */
    public void announce(@NonNull Context context, @NonNull View source, int messageId) {
        announce(context, source, context.getString(messageId));
    }

    /** @return The accessibility manager for the provided {@code context}. */
    private static AccessibilityManager getAccessibilityManager(Context context) {
        return context.getSystemService(AccessibilityManager.class);
    }

    private Accessibility() {
        // Singleton.
    }
}
