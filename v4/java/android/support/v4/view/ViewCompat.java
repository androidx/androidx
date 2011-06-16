/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.view;

import android.view.View;

/**
 * Helper for accessing newer features in View.
 */
public class ViewCompat {
    interface ViewCompatImpl {
        public boolean canScrollHorizontally(View v, int direction);
        public boolean canScrollVertically(View v, int direction);
    }

    static class BaseViewCompatImpl implements ViewCompatImpl {
        public boolean canScrollHorizontally(View v, int direction) {
            return false;
        }
        public boolean canScrollVertically(View v, int direction) {
            return false;
        }
    }

    static class ICSViewCompatImpl implements ViewCompatImpl {
        public boolean canScrollHorizontally(View v, int direction) {
            return ViewCompatICS.canScrollHorizontally(v, direction);
        }
        public boolean canScrollVertically(View v, int direction) {
            return ViewCompatICS.canScrollVertically(v, direction);
        }
    }

    static final ViewCompatImpl IMPL;
    static {
        // TODO Change this to a real check once ICS's API level is finalized
        if (android.os.Build.VERSION.CODENAME.equals("IceCreamSandwich")) {
        //if (android.os.Build.VERSION.SDK_INT >= 14) {
            IMPL = new ICSViewCompatImpl();
        } else {
            IMPL = new BaseViewCompatImpl();
        }
    }

    public static boolean canScrollHorizontally(View v, int direction) {
        return IMPL.canScrollHorizontally(v, direction);
    }

    public static boolean canScrollVertically(View v, int direction) {
        return IMPL.canScrollVertically(v, direction);
    }
}
