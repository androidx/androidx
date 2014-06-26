/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.app.SharedElementListener;
import android.view.View;

import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.Map;

class ActivityCompat21 {

    public static void finishAfterTransition(Activity activity) {
        activity.finishAfterTransition();
    }

    public static void setEnterSharedElementListener(Activity activity,
            SharedElementListener21 listener) {
        activity.setEnterSharedElementListener(createListener(listener));
    }

    public static void setExitSharedElementListener(Activity activity,
            SharedElementListener21 listener) {
        activity.setExitSharedElementListener(createListener(listener));
    }

    public abstract static class SharedElementListener21 {
        public abstract void setSharedElementStart(List<String> sharedElementNames,
                List<View> sharedElements, List<View> sharedElementSnapshots);

        public abstract void setSharedElementEnd(List<String> sharedElementNames,
                List<View> sharedElements, List<View> sharedElementSnapshots);

        public abstract void handleRejectedSharedElements(List<View> rejectedSharedElements);

        public abstract void remapSharedElements(List<String> names,
                Map<String, View> sharedElements);
    }

    private static SharedElementListener createListener(SharedElementListener21 listener) {
        SharedElementListener newListener = null;
        if (listener != null) {
            newListener = new SharedElementListenerImpl(listener);
        }
        return newListener;
    }

    private static class SharedElementListenerImpl extends SharedElementListener {
        private SharedElementListener21 mListener;

        public SharedElementListenerImpl(SharedElementListener21 listener) {
            mListener = listener;
        }

        @Override
        public void setSharedElementStart(List<String> sharedElementNames,
                List<View> sharedElements, List<View> sharedElementSnapshots) {
            mListener.setSharedElementStart(sharedElementNames, sharedElements,
                    sharedElementSnapshots);
        }

        @Override
        public void setSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements,
                List<View> sharedElementSnapshots) {
            mListener.setSharedElementEnd(sharedElementNames, sharedElements,
                    sharedElementSnapshots);
        }

        @Override
        public void handleRejectedSharedElements(List<View> rejectedSharedElements) {
            mListener.handleRejectedSharedElements(rejectedSharedElements);
        }

        @Override
        public void remapSharedElements(List<String> names, Map<String, View> sharedElements) {
            mListener.remapSharedElements(names, sharedElements);
        }
    }
}
