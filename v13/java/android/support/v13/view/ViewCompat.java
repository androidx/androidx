/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v13.view;

import android.content.ClipData;
import android.support.v4.os.BuildCompat;
import android.view.View;

/**
 * Helper for accessing features in {@link View} introduced after API
 * level 13 in a backwards compatible fashion.
 */
public class ViewCompat extends android.support.v4.view.ViewCompat {
    interface ViewCompatImpl {
        boolean startDragAndDrop(View v, ClipData data, View.DragShadowBuilder shadowBuilder,
                                 Object localState, int flags);
        void cancelDragAndDrop(View v);
        void updateDragShadow(View v, View.DragShadowBuilder shadowBuilder);
    }

    private static class BaseViewCompatImpl implements ViewCompatImpl {
        @Override
        public boolean startDragAndDrop(View v, ClipData data, View.DragShadowBuilder shadowBuilder,
                Object localState, int flags) {
            return v.startDrag(data, shadowBuilder, localState, flags);
        }

        @Override
        public void cancelDragAndDrop(View v) {
            // no-op
        }

        @Override
        public void updateDragShadow(View v, View.DragShadowBuilder shadowBuilder) {
            // no-op
        }
    }

    private static class Api24ViewCompatImpl implements ViewCompatImpl {
        @Override
        public boolean startDragAndDrop(View v, ClipData data, View.DragShadowBuilder shadowBuilder,
                Object localState, int flags) {
            return ViewCompatApi24.startDragAndDrop(
                    v, data, shadowBuilder, localState, flags);
        }

        @Override
        public void cancelDragAndDrop(View v) {
            ViewCompatApi24.cancelDragAndDrop(v);
        }

        @Override
        public void updateDragShadow(View v, View.DragShadowBuilder shadowBuilder) {
            ViewCompatApi24.updateDragShadow(v, shadowBuilder);
        }
    }

    static ViewCompatImpl IMPL;
    static {
        if (BuildCompat.isAtLeastN()) {
            IMPL = new Api24ViewCompatImpl();
        } else {
            IMPL = new BaseViewCompatImpl();
        }
    }

    /**
     * Start the drag and drop operation.
     */
    public static boolean startDragAndDrop(View v, ClipData data,
            View.DragShadowBuilder shadowBuilder, Object localState, int flags) {
        return IMPL.startDragAndDrop(v, data, shadowBuilder, localState, flags);
    }

    /**
     * Cancel the drag and drop operation.
     */
    public static void cancelDragAndDrop(View v) {
        IMPL.cancelDragAndDrop(v);
    }

    /**
     * Update the drag shadow while drag and drop is in progress.
     */
    public static void updateDragShadow(View v, View.DragShadowBuilder shadowBuilder) {
        IMPL.updateDragShadow(v, shadowBuilder);
    }

    private ViewCompat() {
    }
}
