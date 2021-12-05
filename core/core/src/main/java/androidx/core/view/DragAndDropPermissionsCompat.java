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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.app.Activity;
import android.os.Build;
import android.view.DragAndDropPermissions;
import android.view.DragEvent;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Helper for accessing features in {@link DragAndDropPermissions} a backwards
 * compatible fashion.
 *
 * <p>
 * Learn more in the guide to using
 * <a href="/guide/topics/ui/drag-drop#DragPermissionsMultiWindow">drag permissions in multi-window
 * mode</a>.
 * </p>
 */
public final class DragAndDropPermissionsCompat {
    private final DragAndDropPermissions mDragAndDropPermissions;

    private DragAndDropPermissionsCompat(DragAndDropPermissions dragAndDropPermissions) {
        mDragAndDropPermissions = dragAndDropPermissions;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Nullable
    public static DragAndDropPermissionsCompat request(@NonNull Activity activity,
            @NonNull DragEvent dragEvent) {
        if (Build.VERSION.SDK_INT >= 24) {
            DragAndDropPermissions dragAndDropPermissions =
                    Api24Impl.requestDragAndDropPermissions(activity, dragEvent);
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
            Api24Impl.release(mDragAndDropPermissions);
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static DragAndDropPermissions requestDragAndDropPermissions(Activity activity,
                DragEvent event) {
            return activity.requestDragAndDropPermissions(event);
        }

        @DoNotInline
        static void release(DragAndDropPermissions dragAndDropPermissions) {
            dragAndDropPermissions.release();
        }
    }
}
