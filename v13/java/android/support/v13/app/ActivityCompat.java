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

package android.support.v13.app;

import android.app.Activity;
import android.support.v13.view.DragAndDropPermissionsCompat;
import android.view.DragEvent;

/**
 * Helper for accessing features in {@link android.app.Activity}
 * introduced after API level 13 in a backwards compatible fashion.
 */
public class ActivityCompat extends android.support.v4.app.ActivityCompat {

    /**
     * Create {@link DragAndDropPermissionsCompat} object bound to this activity and controlling
     * the access permissions for content URIs associated with the {@link android.view.DragEvent}.
     * @param dragEvent Drag event to request permission for
     * @return The {@link DragAndDropPermissionsCompat} object used to control access to the content
     * URIs. {@code null} if no content URIs are associated with the event or if permissions could
     * not be granted.
     */
    public static DragAndDropPermissionsCompat requestDragAndDropPermissions(Activity activity,
                                                                             DragEvent dragEvent) {
        return DragAndDropPermissionsCompat.request(activity, dragEvent);
    }

    private ActivityCompat() {}
}
