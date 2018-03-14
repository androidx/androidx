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

package androidx.core.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Activity;
import android.os.Build;
import android.view.DragAndDropPermissions;
import android.view.DragEvent;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Helper for accessing features in {@link android.view.DragAndDropPermissions} a backwards
 * compatible fashion.
 */
public final class DragAndDropPermissionsCompat {
    private Object mDragAndDropPermissions;

    private DragAndDropPermissionsCompat(Object dragAndDropPermissions) {
        mDragAndDropPermissions = dragAndDropPermissions;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Nullable
    public static DragAndDropPermissionsCompat request(Activity activity, DragEvent dragEvent) {
        if (Build.VERSION.SDK_INT >= 24) {
            DragAndDropPermissions dragAndDropPermissions =
                    activity.requestDragAndDropPermissions(dragEvent);
            if (dragAndDropPermissions != null) {
                return new DragAndDropPermissionsCompat(dragAndDropPermissions);
            }
        }
        return null;
    }

    /**
     * Revoke the permission grant explicitly.
     */
    public void release() {
        if (Build.VERSION.SDK_INT >= 24) {
            ((DragAndDropPermissions) mDragAndDropPermissions).release();
        }
    }
}
