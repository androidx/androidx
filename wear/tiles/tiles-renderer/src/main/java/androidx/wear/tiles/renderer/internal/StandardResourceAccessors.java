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

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.wear.tiles.proto.ResourceProto;

/** Utility class to get ResourceAccessors populated with standard options. */
public class StandardResourceAccessors {
    private StandardResourceAccessors() {}

    /**
     * Get a builder pre-populated with accessors for the resources of the app hosting the renderer.
     *
     * <p>Use {@code setFooAccessor} calls to change the pre-populated ones or add others.
     *
     * @param protoResources ProtoLayout resources for the current layout.
     * @param appContext Context for the app that both owns the resources and displays the layout.
     */
    @NonNull
    public static ResourceAccessors.Builder forLocalApp(
            @NonNull ResourceProto.Resources protoResources, @NonNull Context appContext) {
        AndroidResourceAccessor androidResourceAccessor =
                new AndroidResourceAccessor(appContext.getResources());

        InlineResourceAccessor inlineResourceAccessor = new InlineResourceAccessor(appContext);
        return ResourceAccessors.builder(protoResources)
                .setAndroidImageResourceByResIdAccessor(androidResourceAccessor)
                .setInlineImageResourceAccessor(inlineResourceAccessor);
    }

    /**
     * Get a builder pre-populated with accessors for the resources of a tile provider service.
     *
     * <p>Use {@code setFooAccessor} calls to change the pre-populated ones or add others.
     *
     * @param protoResources ProtoLayout resources for the current layout.
     * @param servicePackageName Package name for the service that owns the resources.
     * @param serviceAndroidResources Android resources from the service.
     * @param hostAppContext Context for the app hosting the renderer displaying the layout.
     */
    @NonNull
    public static ResourceAccessors.Builder forRemoteService(
            @NonNull ResourceProto.Resources protoResources,
            @NonNull String servicePackageName,
            @NonNull Resources serviceAndroidResources,
            @NonNull Context hostAppContext) {
        AndroidResourceAccessor androidResourceAccessor =
                new AndroidResourceAccessor(serviceAndroidResources);

        InlineResourceAccessor inlineResourceAccessor = new InlineResourceAccessor(hostAppContext);
        return ResourceAccessors.builder(protoResources)
                .setAndroidImageResourceByResIdAccessor(androidResourceAccessor)
                .setInlineImageResourceAccessor(inlineResourceAccessor);
    }
}
