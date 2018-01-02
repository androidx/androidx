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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.view.DragAndDropPermissions;
import android.view.DragEvent;

/**
 * Helper for accessing features in {@link android.view.DragAndDropPermissions}
 * introduced after API level 13 in a backwards compatible fashion.
 */
public final class DragAndDropPermissionsCompat {

    interface DragAndDropPermissionsCompatImpl {
        Object request(Activity activity, DragEvent dragEvent);
        void release(Object dragAndDropPermissions);
    }

    static class BaseDragAndDropPermissionsCompatImpl implements DragAndDropPermissionsCompatImpl {
        @Override
        public Object request(Activity activity, DragEvent dragEvent) {
            return null;
        }

        @Override
        public void release(Object dragAndDropPermissions) {
            // no-op
        }
    }

    @RequiresApi(24)
    static class Api24DragAndDropPermissionsCompatImpl
            extends BaseDragAndDropPermissionsCompatImpl {
        @Override
        public Object request(Activity activity, DragEvent dragEvent) {
            return activity.requestDragAndDropPermissions(dragEvent);
        }

        @Override
        public void release(Object dragAndDropPermissions) {
            ((DragAndDropPermissions) dragAndDropPermissions).release();
        }
    }

    private static DragAndDropPermissionsCompatImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= 24) {
            IMPL = new Api24DragAndDropPermissionsCompatImpl();
        } else {
            IMPL = new BaseDragAndDropPermissionsCompatImpl();
        }
    }

    private Object mDragAndDropPermissions;

    private DragAndDropPermissionsCompat(Object dragAndDropPermissions) {
        mDragAndDropPermissions = dragAndDropPermissions;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    public static DragAndDropPermissionsCompat request(Activity activity, DragEvent dragEvent) {
        Object dragAndDropPermissions = IMPL.request(activity, dragEvent);
        if (dragAndDropPermissions != null) {
            return new DragAndDropPermissionsCompat(dragAndDropPermissions);
        }
        return null;
    }

    /*
     * Revoke the permission grant explicitly.
     */
    public void release() {
        IMPL.release(mDragAndDropPermissions);
    }
}
