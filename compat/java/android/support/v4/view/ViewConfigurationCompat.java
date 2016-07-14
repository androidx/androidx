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

import android.view.ViewConfiguration;

/**
 * Helper for accessing features in {@link ViewConfiguration}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public final class ViewConfigurationCompat {
    /**
     * Interface for the full API.
     */
    interface ViewConfigurationVersionImpl {
        boolean hasPermanentMenuKey(ViewConfiguration config);
    }

    /**
     * Interface implementation that doesn't use anything about v4 APIs.
     */
    static class BaseViewConfigurationVersionImpl implements ViewConfigurationVersionImpl {
        @Override
        public boolean hasPermanentMenuKey(ViewConfiguration config) {
            // Pre-HC devices will always have a menu button
            return true;
        }
    }

    /**
     * Interface implementation for devices with at least v11 APIs.
     */
    static class HoneycombViewConfigurationVersionImpl extends BaseViewConfigurationVersionImpl {
        @Override
        public boolean hasPermanentMenuKey(ViewConfiguration config) {
            // There is no way to check on Honeycomb so we assume false
            return false;
        }
    }

    /**
     * Interface implementation for devices with at least v14 APIs.
     */
    static class IcsViewConfigurationVersionImpl extends HoneycombViewConfigurationVersionImpl {
        @Override
        public boolean hasPermanentMenuKey(ViewConfiguration config) {
            return ViewConfigurationCompatICS.hasPermanentMenuKey(config);
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final ViewConfigurationVersionImpl IMPL;
    static {
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            IMPL = new IcsViewConfigurationVersionImpl();
        } else if (android.os.Build.VERSION.SDK_INT >= 11) {
            IMPL = new HoneycombViewConfigurationVersionImpl();
        } else {
            IMPL = new BaseViewConfigurationVersionImpl();
        }
    }

    // -------------------------------------------------------------------

    /**
     * Call {@link ViewConfiguration#getScaledPagingTouchSlop()}.
     *
     * @deprecated Call {@link ViewConfiguration#getScaledPagingTouchSlop()} directly.
     * This method will be removed in a future release.
     */
    @Deprecated
    public static int getScaledPagingTouchSlop(ViewConfiguration config) {
        return config.getScaledPagingTouchSlop();
    }

    /**
     * Report if the device has a permanent menu key available to the user, in a backwards
     * compatible way.
     */
    public static boolean hasPermanentMenuKey(ViewConfiguration config) {
        return IMPL.hasPermanentMenuKey(config);
    }

    private ViewConfigurationCompat() {}
}
