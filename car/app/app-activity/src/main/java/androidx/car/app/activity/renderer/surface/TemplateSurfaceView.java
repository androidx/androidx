/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity.renderer.surface;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_DPAD_CENTER;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.activity.LogTags.TAG;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnTouchModeChangeListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.car.app.activity.renderer.IProxyInputConnection;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

/**
 * A surface view suitable for template rendering.
 *
 * <p>This view supports surface package even for builds lower than {@link Build.VERSION_CODES.R}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class TemplateSurfaceView extends SurfaceView {
    private static final boolean SUPPORTS_SURFACE_CONTROL =
            VERSION.SDK_INT >= Build.VERSION_CODES.R;

    /**
     * StateDescription for a {@link View} to support direct manipulation mode. It's also used as
     * class name of {@link AccessibilityEvent} to indicate that the {@link AccessibilityEvent}
     * represents a request to toggle direct manipulation mode.
     *
     * This value should not change, even if the actual package containing this class is different
     * as this value must match the value defined at
     * <a href="https://android.googlesource.com/platform/packages/apps/Car/libs/+/refs/heads/androi
     * d11-release/car-ui-lib/src/com/android/car/ui/utils/DirectManipulationHelper.java#38">DIRECT_
     * MANIPULATION</a>
     */
    private static final String DIRECT_MANIPULATION = "com.android.car.ui.utils"
            + ".DIRECT_MANIPULATION";

    @Nullable
    private RotaryEventCallback mRotaryEventCallback;
    @Nullable
    private OnCreateInputConnectionListener mOnCreateInputConnectionListener;
    @Nullable
    private OnBackPressedListener mOnBackPressedListener;

    @Nullable
    ISurfaceControl mSurfaceControl;
    private boolean mIsInInputMode;

    private final InputMethodManager mInputMethodManager =
            (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    private final SurfaceWrapperProvider mSurfaceWrapperProvider =
            new SurfaceWrapperProvider(this);
    private final OnTouchModeChangeListener mOnTouchModeChangeListener =
            new ViewTreeObserver.OnTouchModeChangeListener() {
                @Override
                public void onTouchModeChanged(boolean isInTouchMode) {
                    try {
                        if (mSurfaceControl != null) {
                            mSurfaceControl.onWindowFocusChanged(hasFocus(), isInTouchMode);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "Remote connection lost", e);
                    }
                }
            };

    public TemplateSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    /**
     * Registers a {@link RotaryEventCallback} that is notified of rotary events.
     */
    public void registerRotaryEventCallback(@Nullable RotaryEventCallback callback) {
        mRotaryEventCallback = callback;
    }

    /**
     * Registers a {@link OnCreateInputConnectionListener} that is notified of invocations on
     * {@link #onCreateInputConnection(EditorInfo)}.
     */
    public void setOnCreateInputConnectionListener(
            @Nullable OnCreateInputConnectionListener listener) {
        mOnCreateInputConnectionListener = listener;
    }

    /**
     * Registers a {@link OnBackPressedListener} that is notified of back button presses.
     */
    public void setOnBackPressedListener(@Nullable OnBackPressedListener listener) {
        mOnBackPressedListener = listener;
    }

    /**
     * Returns the surface token used to create a {@link android.view.SurfaceControlViewHost}, or
     * null if not available.
     */
    @Nullable
    public IBinder getSurfaceToken() {
        if (SUPPORTS_SURFACE_CONTROL) {
            return Api30Impl.getHostToken(this);
        }

        return null;
    }

    @Override
    @NonNull
    public AccessibilityNodeInfo createAccessibilityNodeInfo() {
        AccessibilityNodeInfo accessibilityNodeInfo = super.createAccessibilityNodeInfo();
        // Indicate this as an editable view so the rotary service does not remove the focus when
        // IME is presented.
        accessibilityNodeInfo.setEditable(mIsInInputMode);
        return accessibilityNodeInfo;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
            @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        try {
            if (mSurfaceControl != null) {
                mSurfaceControl.onWindowFocusChanged(gainFocus, isInTouchMode());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
        }
        enableDirectManipulationMode(this, gainFocus);
    }

    @Override
    @Nullable
    public InputConnection onCreateInputConnection(@NonNull EditorInfo editorInfo) {
        requireNonNull(editorInfo);

        if (!mIsInInputMode || mOnCreateInputConnectionListener == null) {
            return null;
        }

        try {
            IProxyInputConnection proxyInputConnection =
                    mOnCreateInputConnectionListener.onCreateInputConnection(editorInfo);

            // Clear the input and return null if inputConnectionListener is null or there is no
            // open input connection on the host.
            if (proxyInputConnection == null) {
                Log.e(TAG,
                        "InputConnectionListener has not been received yet. Canceling the input");
                onStopInput();
                return null;
            }
            copyEditorInfo(proxyInputConnection.getEditorInfo(), editorInfo);
            return new RemoteProxyInputConnection(proxyInputConnection);

        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
        }

        return null;
    }

    /**
     * Enables or disables direct manipulation mode. This method sends an {@link AccessibilityEvent}
     * to tell the Rotary service to enter or exit direct manipulation mode. Typically pressing
     * the center button of the rotary controller with a direct manipulation view focused will
     * enter direct manipulation mode, while pressing the Back button will exit direct
     * manipulation mode.
     *
     * @param view   the direct manipulation view
     * @param enable true to enter direct manipulation mode, false to exit direct manipulation mode
     * @return whether the AccessibilityEvent was sent
     */
    private boolean enableDirectManipulationMode(@NonNull View view, boolean enable) {
        requireNonNull(view);
        AccessibilityManager accessibilityManager = (AccessibilityManager)
                view.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager == null || !accessibilityManager.isEnabled()) {
            return false;
        }
        AccessibilityEvent event = AccessibilityEvent.obtain();
        event.setClassName(DIRECT_MANIPULATION);
        event.setSource(view);
        event.setEventType(enable
                ? AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
                : AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
        accessibilityManager.sendAccessibilityEvent(event);
        return true;
    }

    private void copyEditorInfo(@NonNull EditorInfo from, @NonNull EditorInfo to) {
        requireNonNull(from);
        requireNonNull(to);
        to.inputType = from.inputType;
        to.imeOptions = from.imeOptions;
        to.privateImeOptions = from.privateImeOptions;
        to.actionLabel = from.actionLabel;
        to.actionId = from.actionId;
        to.initialSelStart = from.initialSelStart;
        to.initialSelEnd = from.initialSelEnd;
        to.initialCapsMode = from.initialCapsMode;
        to.hintText = from.hintText;
        to.label = from.label;
        to.packageName = from.packageName;
        to.fieldId = from.fieldId;
        to.fieldName = from.fieldName;
        to.extras = from.extras;
        to.hintLocales = from.hintLocales;
        to.contentMimeTypes = from.contentMimeTypes;
    }

    /** Notifies to start the input, i.e. to show the keyboard. */
    public void onStartInput() {
        if (!hasFocus()) {
            requestFocus();
        }

        mIsInInputMode = true;
        mInputMethodManager.restartInput(this);
        mInputMethodManager.showSoftInput(this, 0);
    }

    /** Notifies to stop the input, i.e. to hide the keyboard. */
    public void onStopInput() {
        if (mIsInInputMode) {
            mIsInInputMode = false;
            mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return mIsInInputMode;
    }

    @Override
    public boolean checkInputConnectionProxy(@Nullable View view) {
        return mIsInInputMode;
    }

    /**
     * Updates the surface package. The surface package can be either a
     * {@link android.view.SurfaceControlViewHost.SurfacePackage} or a {@link LegacySurfacePackage}.
     */
    public void setSurfacePackage(@NonNull Bundleable bundle) {
        Object surfacePackage;
        try {
            surfacePackage = bundle.get();
        } catch (BundlerException e) {
            Log.e(TAG, "Unable to deserialize surface package.");
            return;
        }

        if (SUPPORTS_SURFACE_CONTROL && surfacePackage instanceof SurfacePackage) {
            Api30Impl.setSurfacePackage(this, (SurfacePackage) surfacePackage);
        } else if (surfacePackage instanceof LegacySurfacePackage) {
            setSurfacePackage((LegacySurfacePackage) surfacePackage);
        } else {
            Log.e(TAG, "Unrecognized surface package");
        }
    }

    /**
     * Updates the surface control with the {@link LegacySurfacePackage}.
     *
     * This control is used to communicate the UI events and focus with the host.
     */
    @SuppressLint({"ClickableViewAccessibility"})
    private void setSurfacePackage(LegacySurfacePackage surfacePackage) {
        ISurfaceControl surfaceControl = surfacePackage.getSurfaceControl();
        SurfaceWrapper surfaceWrapper = mSurfaceWrapperProvider.createSurfaceWrapper();
        try {
            surfaceControl.setSurfaceWrapper(Bundleable.create(surfaceWrapper));
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            return;
        } catch (BundlerException e) {
            Log.e(TAG, "Unable to serialize surface wrapper", e);
        }
        mSurfaceControl = surfaceControl;
        setOnTouchListener((view, event) -> handleTouchEvent(event));
        setOnKeyListener((view, keyCode, event) -> handleKeyEvent(event));
        setOnGenericMotionListener((view, event) -> handleGenericMotionEvent(event));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnTouchModeChangeListener(mOnTouchModeChangeListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnTouchModeChangeListener(mOnTouchModeChangeListener);
    }

    /** Passes the touch events to the host. */
    boolean handleTouchEvent(@NonNull MotionEvent event) {
        // Make a copy to avoid double recycling of the event.
        MotionEvent eventCopy = MotionEvent.obtain(requireNonNull(event));
        try {
            if (mSurfaceControl != null) {
                mSurfaceControl.onTouchEvent(eventCopy);
                return true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
        }

        return false;
    }

    /** Passes the generic motion events to the host. */
    boolean handleGenericMotionEvent(@NonNull MotionEvent event) {
        if (requireNonNull(event).getActionMasked() == MotionEvent.ACTION_SCROLL) {
            int steps = (int) event.getAxisValue(MotionEvent.AXIS_SCROLL);
            boolean isClockwise = steps > 0;
            if (mRotaryEventCallback != null) {
                mRotaryEventCallback.onRotate(steps, isClockwise);
            }
            return true;
        }
        return false;
    }

    /** Passes the appropriate key events to the host for rotary support. */
    boolean handleKeyEvent(KeyEvent event) {
        if (event.getAction() == ACTION_DOWN) {
            return false;
        }

        switch (event.getKeyCode()) {
            case KEYCODE_BACK:
                if (mOnBackPressedListener != null) {
                    mOnBackPressedListener.onBackPressed();
                    return true;
                }
                break;
            case KEYCODE_DPAD_CENTER:
                if (mRotaryEventCallback != null) {
                    mRotaryEventCallback.onSelect();
                    return true;
                }
                break;
            case KEYCODE_DPAD_RIGHT:
            case KEYCODE_DPAD_LEFT:
            case KEYCODE_DPAD_UP:
            case KEYCODE_DPAD_DOWN:
                if (mRotaryEventCallback != null) {
                    boolean success = mRotaryEventCallback.onNudge(event.getKeyCode());
                    if (!success) {
                        // Quit direct manipulation mode if the nudge event cannot be handled.
                        enableDirectManipulationMode(this, false);
                        return false;
                    }
                    return true;
                }
                break;
            default:
                return false;
        }

        return false;
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api30Impl {
        private Api30Impl() {
        }

        @DoNotInline
        static IBinder getHostToken(TemplateSurfaceView view) {
            return view.getHostToken();
        }

        @DoNotInline
        static void setSurfacePackage(TemplateSurfaceView view, SurfacePackage surfacePackage) {
            view.setChildSurfacePackage(surfacePackage);
        }
    }
}
