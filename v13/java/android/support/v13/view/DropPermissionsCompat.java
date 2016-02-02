/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.Activity;
import android.support.v4.os.BuildCompat;
import android.view.DragEvent;

/**
 * Helper for accessing features in {@link android.view.DropPermissions}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public final class DropPermissionsCompat {

    interface DropPermissionsCompatImpl {
        Object request(Activity activity, DragEvent dragEvent);
        void release(Object dropPermissions);
    }

    static class BaseDropPermissionsCompatImpl implements DropPermissionsCompatImpl {
        @Override
        public Object request(Activity activity, DragEvent dragEvent) {
            return null;
        }

        @Override
        public void release(Object dropPermissions) {
            // no-op
        }
    }

    static class Api24DropPermissionsCompatImpl extends BaseDropPermissionsCompatImpl {
        @Override
        public Object request(Activity activity, DragEvent dragEvent) {
            return DropPermissionsCompatApi24.request(activity, dragEvent);
        }

        @Override
        public void release(Object dropPermissions) {
            DropPermissionsCompatApi24.release(dropPermissions);
        }
    }

    private static DropPermissionsCompatImpl IMPL;
    static {
        if (BuildCompat.isAtLeastN()) {
            IMPL = new Api24DropPermissionsCompatImpl();
        } else {
            IMPL = new BaseDropPermissionsCompatImpl();
        }
    }

    private Object mDropPermissions;

    private DropPermissionsCompat(Object dropPermissions) {
        mDropPermissions = dropPermissions;
    }

    /** @hide */
    public static DropPermissionsCompat request(Activity activity, DragEvent dragEvent) {
        Object dropPermissions = IMPL.request(activity, dragEvent);
        if (dropPermissions != null) {
            return new DropPermissionsCompat(dropPermissions);
        }
        return null;
    }

    /*
     * Revoke the permission grant explicitly.
     */
    public void release() {
        IMPL.release(mDropPermissions);
    }
}
