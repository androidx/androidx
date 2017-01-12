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

package android.support.v4.app;

import android.app.Activity;
import android.app.SharedElementCallback;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.annotation.TargetApi;
import android.view.View;

import java.util.List;
import java.util.Map;

@RequiresApi(23)
@TargetApi(23)
class ActivityCompatApi23 {
    public interface OnSharedElementsReadyListenerBridge {
        void onSharedElementsReady();
    }

    public interface RequestPermissionsRequestCodeValidator {
        void validateRequestPermissionsRequestCode(int requestCode);
    }

    public static void requestPermissions(Activity activity, String[] permissions,
            int requestCode) {
        if (activity instanceof RequestPermissionsRequestCodeValidator) {
            ((RequestPermissionsRequestCodeValidator) activity)
                    .validateRequestPermissionsRequestCode(requestCode);
        }
        activity.requestPermissions(permissions, requestCode);
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activity,
            String permission) {
        return activity.shouldShowRequestPermissionRationale(permission);
    }

    public static void setEnterSharedElementCallback(Activity activity,
            SharedElementCallback23 callback) {
        activity.setEnterSharedElementCallback(createCallback(callback));
    }

    public static void setExitSharedElementCallback(Activity activity,
            SharedElementCallback23 callback) {
        activity.setExitSharedElementCallback(createCallback(callback));
    }

    private static SharedElementCallback createCallback(SharedElementCallback23 callback) {
        SharedElementCallback newListener = null;
        if (callback != null) {
            newListener = new SharedElementCallbackImpl(callback);
        }
        return newListener;
    }

    public abstract static class SharedElementCallback23
            extends ActivityCompatApi21.SharedElementCallback21 {
        public abstract void onSharedElementsArrived(List<String> sharedElementNames,
                List<View> sharedElements, OnSharedElementsReadyListenerBridge listener);
    }

    private static class SharedElementCallbackImpl extends SharedElementCallback {
        private SharedElementCallback23 mCallback;

        public SharedElementCallbackImpl(SharedElementCallback23 callback) {
            mCallback = callback;
        }

        @Override
        public void onSharedElementStart(List<String> sharedElementNames,
                List<View> sharedElements, List<View> sharedElementSnapshots) {
            mCallback.onSharedElementStart(sharedElementNames, sharedElements,
                    sharedElementSnapshots);
        }

        @Override
        public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements,
                List<View> sharedElementSnapshots) {
            mCallback.onSharedElementEnd(sharedElementNames, sharedElements,
                    sharedElementSnapshots);
        }

        @Override
        public void onRejectSharedElements(List<View> rejectedSharedElements) {
            mCallback.onRejectSharedElements(rejectedSharedElements);
        }

        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            mCallback.onMapSharedElements(names, sharedElements);
        }

        @Override
        public Parcelable onCaptureSharedElementSnapshot(View sharedElement,
                Matrix viewToGlobalMatrix, RectF screenBounds) {
            return mCallback.onCaptureSharedElementSnapshot(sharedElement, viewToGlobalMatrix,
                            screenBounds);
        }

        @Override
        public View onCreateSnapshotView(Context context, Parcelable snapshot) {
            return mCallback.onCreateSnapshotView(context, snapshot);
        }

        @Override
        public void onSharedElementsArrived(List<String> sharedElementNames,
                List<View> sharedElements, final OnSharedElementsReadyListener listener) {
            mCallback.onSharedElementsArrived(sharedElementNames, sharedElements,
                    new OnSharedElementsReadyListenerBridge() {
                        @Override
                        public void onSharedElementsReady() {
                            listener.onSharedElementsReady();
                        }
                    });
        }
    }
}
