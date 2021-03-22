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

package androidx.wear.tiles.renderer;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.wear.tiles.proto.ResourceProto.AndroidImageResourceByResId;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Resource accessor for Android resources.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AndroidResourceAccessor
        implements ResourceAccessors.AndroidImageResourceByResIdAccessor {
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
    @SuppressLint("RestrictedApi") // TODO(b/183006740): Remove when prefix check is fixed.
    public ListenableFuture<Drawable> getDrawable(@NonNull AndroidImageResourceByResId resource) {
        ResolvableFuture<Drawable> future = ResolvableFuture.create();
        try {
            future.set(mAndroidResources.getDrawable(resource.getResourceId(), null));
        } catch (NotFoundException e) {
            future.setException(e);
        }

        return future;
    }
}
