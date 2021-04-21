/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.renderer.internal;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.wear.tiles.proto.ResourceProto.AndroidImageResourceByResId;
import androidx.wear.tiles.renderer.internal.ResourceAccessors.AndroidImageResourceByResIdAccessor;

import com.google.common.util.concurrent.ListenableFuture;

/** Resource accessor for Android resources. */
public class AndroidResourceAccessor implements AndroidImageResourceByResIdAccessor {
    private final Resources mAndroidResources;

    /**
     * Constructor.
     *
     * @param androidResources An Android Resources instance for the tile provider's package. This
     *     is normally obtained from {@code PackageManager#getResourcesForApplication}.
     */
    public AndroidResourceAccessor(@NonNull Resources androidResources) {
        this.mAndroidResources = androidResources;
    }

    @Override
    @NonNull
    public ListenableFuture<Drawable> getDrawable(@NonNull AndroidImageResourceByResId resource) {
        try {
            return ResourceAccessors.createImmediateFuture(
                    mAndroidResources.getDrawable(resource.getResourceId(), null));
        } catch (NotFoundException e) {
            return ResourceAccessors.createFailedFuture(e);
        }
    }
}
