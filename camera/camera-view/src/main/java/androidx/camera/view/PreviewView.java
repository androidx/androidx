/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Preview;

import java.util.concurrent.Executor;

/**
 * Custom View that displays camera feed for CameraX's Preview use case.
 *
 * <p> This class manages the Surface lifecycle, as well as the preview aspect ratio and
 * orientation. Internally, it uses one of the TextureView or SurfaceView to display the
 * camera feed.
 */
public class PreviewView extends FrameLayout {

    private Implementation mImplementation;

    public PreviewView(@NonNull Context context) {
        this(context, null);
    }

    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PreviewView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // TODO(b/17519540): support TextureView too.
        mImplementation = new SurfaceViewImplementation();
        mImplementation.init(this);
    }

    /**
     * Gets the {@link Preview.PreviewSurfaceCallback} to be used with
     * {@link Preview#setPreviewSurfaceCallback(Executor, Preview.PreviewSurfaceCallback)}.
     */
    @NonNull
    public Preview.PreviewSurfaceCallback getPreviewSurfaceCallback() {
        return mImplementation.getPreviewSurfaceCallback();
    }

    /**
     * Implements this interface to create PreviewView implementation.
     */
    interface Implementation {

        /**
         * Initializes the parent view with sub views.
         *
         * @param parent the containing parent {@link FrameLayout}.
         */
        void init(@NonNull FrameLayout parent);

        /**
         * Gets the {@link Preview.PreviewSurfaceCallback} to be used with {@link Preview}.
         */
        @NonNull
        Preview.PreviewSurfaceCallback getPreviewSurfaceCallback();
    }
}
