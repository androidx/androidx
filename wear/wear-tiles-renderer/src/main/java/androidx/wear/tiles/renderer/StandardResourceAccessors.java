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

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.wear.tiles.builders.ResourceBuilders;

/** Utility class to get ResourceAccessors populated with standard options. */
public class StandardResourceAccessors {
    private StandardResourceAccessors() {}

    /**
     * Get a builder pre-populated with accessors for the resources of the app hosting the renderer.
     *
     * <p>Use {@code setFooAccessor} calls to change the pre-populated ones or add others.
     *
     * @param appContext Context for the app that both owns the resources and displays the layout.
     * @param tileResources Resources for the current layout.
     */
    @NonNull
    public static ResourceAccessors.Builder forLocalApp(
            @NonNull Context appContext, @NonNull ResourceBuilders.Resources tileResources) {
        AndroidResourceAccessor androidResourceAccessor =
                new AndroidResourceAccessor(appContext.getResources());
        InlineResourceAccessor inlineResourceAccessor = new InlineResourceAccessor(appContext);
        return ResourceAccessors.builder(tileResources.toProto())
                .setAndroidImageResourceByResIdAccessor(androidResourceAccessor)
                .setInlineImageResourceAccessor(inlineResourceAccessor);
    }

    /**
     * Get a builder pre-populated with accessors for the resources of a tile provider service.
     *
     * <p>Use {@code setFooAccessor} calls to change the pre-populated ones or add others.
     *
     * @param hostAppContext Context for the app hosting the renderer displaying the layout.
     * @param tileResources Resources for the current layout.
     * @param serviceAndroidResources Android resources from the service.
     */
    @NonNull
    public static ResourceAccessors.Builder forRemoteService(
            @NonNull Context hostAppContext,
            @NonNull ResourceBuilders.Resources tileResources,
            @NonNull Resources serviceAndroidResources) {
        AndroidResourceAccessor androidResourceAccessor =
                new AndroidResourceAccessor(serviceAndroidResources);
        InlineResourceAccessor inlineResourceAccessor = new InlineResourceAccessor(hostAppContext);
        return ResourceAccessors.builder(tileResources.toProto())
                .setAndroidImageResourceByResIdAccessor(androidResourceAccessor)
                .setInlineImageResourceAccessor(inlineResourceAccessor);
    }
}
