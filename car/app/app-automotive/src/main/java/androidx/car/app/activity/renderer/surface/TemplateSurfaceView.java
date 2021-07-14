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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.activity.LogTags.TAG;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnTouchModeChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.car.app.activity.CarAppViewModel;
import androidx.car.app.activity.ErrorHandler;
import androidx.car.app.activity.ServiceDispatcher;
import androidx.car.app.activity.renderer.IProxyInputConnection;
import androidx.car.app.serialization.Bundleable;

/**
 * A surface view suitable for template rendering.
 *
 * <p>This view supports surface package even for builds lower than {@link Build.VERSION_CODES#R}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class TemplateSurfaceView extends SurfaceView {
    private static final boolean SUPPORTS_SURFACE_CONTROL =
            VERSION.SDK_INT >= Build.VERSION_CODES.R;
    @Nullable
    private OnCreateInputConnectionListener mOnCreateInputConnectionListener;

    @Nullable
    ISurfaceControl mSurfaceControl;
    @Nullable
    SurfacePackage mSurfacePackage;
    private boolean mIsInInputMode;

    // Package public to avoid synthetic accessor
    @Nullable
    ServiceDispatcher mServiceDispatcher;
    @Nullable
    private CarAppViewModel mViewModel;
    private final InputMethodManager mInputMethodManager =
            (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    private final SurfaceWrapperProvider mSurfaceWrapperProvider =
            new SurfaceWrapperProvider(this);
    private final OnTouchModeChangeListener mOnTouchModeChangeListener =
            new ViewTreeObserver.OnTouchModeChangeListener() {
                @Override
                public void onTouchModeChanged(boolean isInTouchMode) {
                    requireNonNull(mServiceDispatcher);

                    ISurfaceControl surfaceControl = mSurfaceControl;
                    if (surfaceControl != null) {
                        mServiceDispatcher.dispatch("onWindowFocusChanged", () ->
                                surfaceControl.onWindowFocusChanged(hasFocus(), isInTouchMode));
                    }
                }
            };

    public TemplateSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    /**
     * Sets the {@link ServiceDispatcher} to be used to communicate with the host.
     */
    public void setServiceDispatcher(@NonNull ServiceDispatcher serviceDispatcher) {
        mServiceDispatcher = serviceDispatcher;
    }

    /**
     * Sets the {@link CarAppViewModel} to be used to handle errors.
     */
    public void setViewModel(@NonNull CarAppViewModel viewModel) {
        mViewModel = viewModel;
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
    protected void onFocusChanged(boolean gainFocus, int direction,
            @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        requireNonNull(mServiceDispatcher);
        ISurfaceControl surfaceControl = mSurfaceControl;
        if (surfaceControl != null) {
            mServiceDispatcher.dispatch("onWindowFocusChanged", () ->
                    surfaceControl.onWindowFocusChanged(gainFocus, isInTouchMode()));
        }
    }

    @Override
    @Nullable
    public InputConnection onCreateInputConnection(@NonNull EditorInfo editorInfo) {
        requireNonNull(editorInfo);
        requireNonNull(mServiceDispatcher);

        if (!mIsInInputMode || mOnCreateInputConnectionListener == null) {
            return null;
        }

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

        EditorInfo hostEditorInfo =
                mServiceDispatcher.fetch("getEditorInfo", null,
                        proxyInputConnection::getEditorInfo);
        if (hostEditorInfo == null) {
            Log.e(TAG, "Unable to retrieve host EditorInfo");
            return null;
        }
        copyEditorInfo(hostEditorInfo, editorInfo);
        return new RemoteProxyInputConnection(mServiceDispatcher, proxyInputConnection);
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

    /** Notifies that there has been a text selection update. */
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd) {
        mInputMethodManager.updateSelection(this, oldSelStart, oldSelEnd, newSelStart, newSelEnd);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return mIsInInputMode;
    }

    @Override
    @NonNull
    public CharSequence getAccessibilityClassName() {
        return SurfaceView.class.getName();
    }

    @Override
    public boolean checkInputConnectionProxy(@Nullable View view) {
        return mIsInInputMode;
    }

    /**
     * Updates the surface package. The surface package can be either a
     * {@link android.view.SurfaceControlViewHost.SurfacePackage} or a {@link LegacySurfacePackage}.
     */
    public void setSurfacePackage(@NonNull Object surfacePackage) {
        requireNonNull(mViewModel);

        if (SUPPORTS_SURFACE_CONTROL && surfacePackage instanceof SurfacePackage) {
            setSurfacePackage((SurfacePackage) surfacePackage);
        } else if (surfacePackage instanceof LegacySurfacePackage) {
            setSurfacePackage((LegacySurfacePackage) surfacePackage);
        } else {
            Log.e(TAG, "Unrecognized surface package: " + surfacePackage);
            mViewModel.onError(ErrorHandler.ErrorType.HOST_INCOMPATIBLE);
        }
    }

    /**
     * Updates the surface control with the {@link LegacySurfacePackage}
     *
     * This is used in Android API level 29 to communicate UI events and focus with the host.
     */
    @SuppressLint({"ClickableViewAccessibility"})
    private void setSurfacePackage(LegacySurfacePackage surfacePackage) {
        requireNonNull(mServiceDispatcher);

        ISurfaceControl surfaceControl = surfacePackage.getSurfaceControl();
        SurfaceWrapper surfaceWrapper = mSurfaceWrapperProvider.createSurfaceWrapper();
        mServiceDispatcher.dispatch("setSurfaceWrapper", () ->
                surfaceControl.setSurfaceWrapper(Bundleable.create(surfaceWrapper)));
        mSurfaceControl = surfaceControl;
        setOnTouchListener((view, event) -> handleTouchEvent(event));
    }

    /**
     * Updates the surface control with the {@link SurfacePackage}.
     *
     * This is used in Android API level 30+ to communicate UI events with a remote
     * {@link SurfaceView}
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void setSurfacePackage(SurfacePackage surfacePackage) {
        // Although the Javadoc in SurfaceView#setChildSurfacePackage suggests that SurfaceView
        // should take care of releasing the SurfacePackage, we have found cases where a manual
        // release is necessary.
        if (mSurfacePackage != null) {
            Api30Impl.releaseSurfacePackage(mSurfacePackage);
        }
        mSurfacePackage = surfacePackage;
        Api30Impl.setSurfacePackage(this, (SurfacePackage) surfacePackage);
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
        requireNonNull(mServiceDispatcher);

        // Make a copy to avoid double recycling of the event.
        MotionEvent eventCopy = MotionEvent.obtain(requireNonNull(event));
        ISurfaceControl surfaceControl = mSurfaceControl;
        if (surfaceControl != null) {
            mServiceDispatcher.dispatch("onTouchEvent",
                    () -> surfaceControl.onTouchEvent(eventCopy));
            return true;
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

        @DoNotInline
        public static void releaseSurfacePackage(SurfacePackage surfacePackage) {
            surfacePackage.release();
        }
    }
}
