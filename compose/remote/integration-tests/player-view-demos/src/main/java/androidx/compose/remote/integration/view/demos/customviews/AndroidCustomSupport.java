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

package androidx.compose.remote.integration.view.demos.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import androidx.compose.remote.core.CustomContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.utilities.IntMap;
import androidx.compose.remote.player.core.platform.AndroidComponentSupport;
import androidx.compose.remote.player.core.platform.AndroidCustomContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Platform-specific support manager delegating Native Android View instantiations,
 * configurations, measurements, and drawing pipelines under the player hierarchy.
 */
@SuppressLint("RestrictedApiAndroidX")
public class AndroidCustomSupport implements AndroidCustomContext {

    // Decoupled Property Constant Identifiers (backward compatibility)
    public static final int PROP_TEXT = SupportTextView.PROP_TEXT;
    public static final int PROP_TEXT_COLOR = SupportTextView.PROP_TEXT_COLOR;
    public static final int PROP_TEXT_SIZE = SupportTextView.PROP_TEXT_SIZE;
    public static final int PROP_BACKGROUND_COLOR = SupportTextView.PROP_BACKGROUND_COLOR;

    // Registered component support delegates matching config names
    private final HashMap<String, AndroidComponentSupport> mDelegates = new HashMap<>();

    @NonNull
    private final IntMap<View> mCustomViews = new IntMap<>();

    @NonNull
    private final IntMap<AndroidComponentSupport> mCustomDelegates = new IntMap<>();

    @Nullable
    private Context mContext;

    @Nullable
    private Canvas mCanvas;

    @Nullable
    private RemoteContext mRemoteContext;

    public AndroidCustomSupport() {
        // Register extensible view component support delegates
        mDelegates.put("TextView", new SupportTextView());
        mDelegates.put("ProgressBar", new SupportProgressBar(this));
    }

    @Override
    public void setRemoteContext(@Nullable RemoteContext remoteContext) {
        mRemoteContext = remoteContext;
    }

    public @Nullable RemoteContext getRemoteContext() {
        return mRemoteContext;
    }

    /**
     * Sets the active Android Context for view instantiation.
     */
    @Override
    public void setContext(@Nullable Context context) {
        mContext = context;
    }

    /**
     * Sets the active Android Canvas for custom view drawing.
     */
    @Override
    public void setCanvas(@Nullable Canvas canvas) {
        mCanvas = canvas;
    }

    /**
     * Creates a native view matching the requested configuration type.
     */
    @Override
    public void createCustom(int id, @NonNull String config) {
        if (mContext == null) {
            return;
        }

        AndroidComponentSupport delegate = null;
        for (String name : mDelegates.keySet()) {
            if (name.equalsIgnoreCase(config)) {
                delegate = mDelegates.get(name);
                break;
            }
        }

        if (delegate != null) {
            View view = delegate.createView(mContext);
            mCustomViews.put(id, view);
            mCustomDelegates.put(id, delegate);
        }
    }

    /**
     * Sets a String property configuration on the target view.
     */
    @Override
    public void configureCustom(int id, int type, @NonNull String value) {
        View view = mCustomViews.get(id);
        AndroidComponentSupport delegate = mCustomDelegates.get(id);
        if (view != null && delegate != null) {
            delegate.configure(view, type, value);
        }
    }

    /**
     * Sets an integer property configuration on the target view.
     */
    @Override
    public void configureCustom(int id, int type, int value) {
        View view = mCustomViews.get(id);
        AndroidComponentSupport delegate = mCustomDelegates.get(id);
        if (view != null && delegate != null) {
            delegate.configure(view, type, value);
        }
    }

    /**
     * Sets a float property configuration on the target view.
     */
    @Override
    public void configureCustom(int id, int type, float value) {
        View view = mCustomViews.get(id);
        AndroidComponentSupport delegate = mCustomDelegates.get(id);
        if (view != null && delegate != null) {
            delegate.configure(view, type, value);
        }
    }

    /**
     * Runs native Measurement Pass on the cached view and returns calculated dimensions.
     */
    @Override
    public void measureCustom(int id, float @NonNull [] bounds) {
        View view = mCustomViews.get(id);
        if (view != null) {
            float minWidth = bounds[0];
            float maxWidth = bounds[1];
            float minHeight = bounds[2];
            float maxHeight = bounds[3];
            System.out.println("input  " + Arrays.toString(bounds));
            int h = View.MeasureSpec.AT_MOST;
            if (minHeight == maxHeight) {
                h = View.MeasureSpec.EXACTLY;
            }
            int w = View.MeasureSpec.AT_MOST;
            if (minWidth == maxWidth) {
                w = View.MeasureSpec.EXACTLY;
            }
            int widthSpec = View.MeasureSpec.makeMeasureSpec(
                    (int) maxWidth,
                    maxWidth == Float.MAX_VALUE ? View.MeasureSpec.UNSPECIFIED : w
            );
            int heightSpec = View.MeasureSpec.makeMeasureSpec(
                    (int) maxHeight,
                    maxHeight == Float.MAX_VALUE ? View.MeasureSpec.UNSPECIFIED : h
            );

            view.measure(widthSpec, heightSpec);
            bounds[0] = bounds[1] = 0;
            bounds[2] = Math.max(minWidth, view.getMeasuredWidth());
            bounds[3] = Math.max(minHeight, view.getMeasuredHeight());
            System.out.println(">>>>>> " + Arrays.toString(bounds));
        }
    }

    /**
     * Runs native Layout Positioning Pass on the cached view.
     */
    @Override
    public void layoutCustom(int id, float @NonNull [] bounds) {
        View view = mCustomViews.get(id);
        System.out.println("layout >>>>> " + Arrays.toString(bounds));

        if (view != null) {
            float width = bounds[2];
            float height = bounds[3];
            view.layout(0, 0, (int) width, (int) height);
        }
    }

    @Override
    public boolean touchCustom(int id, int type, float x, float y) {
        View view = mCustomViews.get(id);
        if (view == null) {
            return false;
        }
        switch (type) {
            case CustomContext.TOUCH_DOWN:
                return view.dispatchTouchEvent(
                        android.view.MotionEvent.obtain(0, 0, android.view.MotionEvent.ACTION_DOWN,
                                x, y, 0));
            case CustomContext.TOUCH_DRAG:
                return view.dispatchTouchEvent(
                        android.view.MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, x, y, 0));
            case CustomContext.TOUCH_UP:
                return view.dispatchTouchEvent(
                        android.view.MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, x, y, 0));

        }
        return false;
    }

    /**
     * Renders the native custom view onto the player Canvas.
     */
    @Override
    public void drawCustom(int id) {
        if (mCanvas == null) {
            return;
        }
        View view = mCustomViews.get(id);
        if (view != null) {
            view.draw(mCanvas);
        }
    }
}
