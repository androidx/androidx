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
 * limitations under the License
 */

package android.support.v17.leanback.view;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

public class ViewGroupOverlayHelper {

    public interface Impl {
        void addChildToOverlay(ViewGroup parent, View child);
        void removeChildFromOverlay(ViewGroup parent, View child);
    }

    private static class ImplStub implements Impl {

        @Override
        public void addChildToOverlay(ViewGroup parent, View child) {}

        @Override
        public void removeChildFromOverlay(ViewGroup parent, View child) {}
    }

    private static class ImplJbmr2 implements Impl {

        @Override
        public void addChildToOverlay(ViewGroup parent, View child) {
            ViewGroupOverlayHelperJbmr2.addChildToOverlay(parent, child);
        }

        @Override
        public void removeChildFromOverlay(ViewGroup parent, View child) {
            ViewGroupOverlayHelperJbmr2.removeChildFromOverlay(parent, child);
        }
    }

    private static Impl sInstance;

    private static Impl getInstance() {
        if (sInstance == null) {
            if (Build.VERSION.SDK_INT >= 18) {
                sInstance = new ImplJbmr2();
            } else {
                sInstance = new ImplStub();
            }
        }
        return sInstance;
    }

    public static boolean supportsOverlay() {
        return Build.VERSION.SDK_INT >= 18;
    }

    public static void addChildToOverlay(ViewGroup parent, View child) {
        getInstance().addChildToOverlay(parent, child);
    }

    public static void removeChildFromOverlay(ViewGroup parent, View child) {
        getInstance().removeChildFromOverlay(parent, child);
    }
}
