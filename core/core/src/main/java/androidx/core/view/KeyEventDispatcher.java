/*
 * Copyright 2018 The Android Open Source Project
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Allows dependent components to enable full {@link KeyEvent} dispatch compatibility in core.
 * To use this, implement {@link Component} and call the dispatch methods at appropriate times.
 *
 * This must be used for some core compatibility features to function fully.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class KeyEventDispatcher {
    // reflection accessors
    private static boolean sActionBarFieldsFetched = false;
    private static Method sActionBarOnMenuKeyMethod = null;
    private static boolean sDialogFieldsFetched = false;
    private static Field sDialogKeyListenerField = null;

    private KeyEventDispatcher() {
        // Not instantiable
    }

    /**
     * Call this before dispatching a {@link KeyEvent} to the view hierarchy.
     *
     * @param root the root of the hierarchy that this event will be dispatched to
     * @param event the event to dispatch
     * @return {@code true} if the event was consumed in this stage, {@code false} otherwise
     */
    public static boolean dispatchBeforeHierarchy(@NonNull View root, @NonNull KeyEvent event) {
        return ViewCompat.dispatchUnhandledKeyEventBeforeHierarchy(root, event);
    }

    /**
     * Call this when dispatching a key event. This is usually called in lieu of the
     * Window.Callback dispatchKeyEvent implementation (eg. instead of calling
     * super.dispatchKeyEvent in an Activity).
     *
     * @param component the component implementation
     * @param root the root of the view hierarchy that this event will be dispatched to
     * @param callback a {@link Window.Callback} implementation or {@code null} if there isn't one
     * @param event the event to dispatch
     * @return {@code true} if the event was consumed, {@code false} otherwise
     *
     */
    public static boolean dispatchKeyEvent(@NonNull Component component,
            @Nullable View root, @Nullable Window.Callback callback, @NonNull KeyEvent event) {
        if (component == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 28) {
            return component.superDispatchKeyEvent(event);
        }
        if (callback instanceof Activity) {
            return activitySuperDispatchKeyEventPre28((Activity) callback, event);
        } else if (callback instanceof Dialog) {
            return dialogSuperDispatchKeyEventPre28((Dialog) callback, event);
        }
        return (root != null && ViewCompat.dispatchUnhandledKeyEventBeforeCallback(root, event))
                || component.superDispatchKeyEvent(event);
    }

    private static boolean actionBarOnMenuKeyEventPre28(ActionBar actionBar, KeyEvent event) {
        if (!sActionBarFieldsFetched) {
            try {
                sActionBarOnMenuKeyMethod =
                        actionBar.getClass().getMethod("onMenuKeyEvent", KeyEvent.class);
            } catch (NoSuchMethodException e) {
            }
            sActionBarFieldsFetched = true;
        }
        if (sActionBarOnMenuKeyMethod != null) {
            try {
                return (Boolean) sActionBarOnMenuKeyMethod.invoke(actionBar, event);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        return false;
    }

    private static boolean activitySuperDispatchKeyEventPre28(Activity activity, KeyEvent event) {
        activity.onUserInteraction();

        Window win = activity.getWindow();

        // Let action bars open menus in response to the menu key prioritized over
        // the window handling it
        if (win.hasFeature(Window.FEATURE_ACTION_BAR)) {
            ActionBar actionBar = activity.getActionBar();
            final int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_MENU
                    && actionBar != null && actionBarOnMenuKeyEventPre28(actionBar, event)) {
                return true;
            }
        }

        if (win.superDispatchKeyEvent(event)) {
            return true;
        }
        View decor = win.getDecorView();
        if (ViewCompat.dispatchUnhandledKeyEventBeforeCallback(decor, event)) {
            return true;
        }
        return event.dispatch(activity, decor != null
                ? decor.getKeyDispatcherState() : null, activity);
    }

    private static DialogInterface.OnKeyListener getDialogKeyListenerPre28(Dialog dialog) {
        if (!sDialogFieldsFetched) {
            try {
                sDialogKeyListenerField = Dialog.class.getDeclaredField("mOnKeyListener");
                sDialogKeyListenerField.setAccessible(true);
            } catch (NoSuchFieldException e) {
            }
            sDialogFieldsFetched = true;
        }

        if (sDialogKeyListenerField != null) {
            try {
                return (DialogInterface.OnKeyListener) sDialogKeyListenerField.get(dialog);
            } catch (IllegalAccessException e) {
            }
        }
        return null;
    }

    private static boolean dialogSuperDispatchKeyEventPre28(Dialog dialog, KeyEvent event) {
        DialogInterface.OnKeyListener onKeyListener = getDialogKeyListenerPre28(dialog);
        if ((onKeyListener != null) && (onKeyListener.onKey(dialog, event.getKeyCode(), event))) {
            return true;
        }
        Window win = dialog.getWindow();
        if (win.superDispatchKeyEvent(event)) {
            return true;
        }
        View decor = win.getDecorView();
        if (ViewCompat.dispatchUnhandledKeyEventBeforeCallback(decor, event)) {
            return true;
        }
        return event.dispatch(dialog, decor != null
                ? decor.getKeyDispatcherState() : null, dialog);
    }

    /**
     * Implement this in any component that dispatches {@link KeyEvent}s.
     */
    public interface Component {
        /**
         * Expected to call into the super implementation of
         * {@link Window.Callback#dispatchKeyEvent}.
         *
         * If you're not implementing Window.Callback, this should contain dispatch logic
         * that occurs <b>after</b> {@link android.view.View.OnUnhandledKeyEventListener}s.
         *
         * @param event The event being dispatched
         * @return {@code true} if consuming the event, {@code false} otherwise
         */
        boolean superDispatchKeyEvent(KeyEvent event);
    }
}
