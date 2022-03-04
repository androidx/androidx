/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.textclassifier;

import android.os.Build;
import android.os.Bundle;
import android.view.textclassifier.TextClassifier;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.Collection;
import java.util.Collections;

/**
 * Wraps {@link TextClassifier.EntityConfig}.
 */
@RequiresApi(28)
final class PlatformEntityConfigWrapper {
    private static final String KEY_PLATFORM_ENTITY_CONFIG = "platform_entity_config";

    private final BaseImpl mImpl;

    PlatformEntityConfigWrapper(@NonNull TextClassifier.EntityConfig platformEntityConfig) {
        if (Build.VERSION.SDK_INT >= 29) {
            mImpl = new Api29Impl(platformEntityConfig);
        } else {
            mImpl = new BaseImpl(platformEntityConfig);
        }
    }

    Collection<String> resolveEntityTypes(
            @Nullable Collection<String> defaultEntityTypes) {
        return mImpl.resolveEntityTypes(defaultEntityTypes);
    }

    @NonNull
    Collection<String> getHints() {
        return mImpl.getHints();
    }

    boolean shouldIncludeDefaultEntityTypes() {
        return mImpl.shouldIncludeDefaultEntityTypes();
    }

    @NonNull
    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_PLATFORM_ENTITY_CONFIG, mImpl.mPlatformEntityConfig);
        return bundle;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    static PlatformEntityConfigWrapper createFromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        TextClassifier.EntityConfig entityConfig = bundle.getParcelable(KEY_PLATFORM_ENTITY_CONFIG);
        if (entityConfig == null) {
            return null;
        }
        return new PlatformEntityConfigWrapper(entityConfig);
    }

    @RequiresApi(28)
    private static class BaseImpl {
        TextClassifier.EntityConfig mPlatformEntityConfig;

        BaseImpl(@NonNull TextClassifier.EntityConfig platformEntityConfig) {
            mPlatformEntityConfig = Preconditions.checkNotNull(platformEntityConfig);
        }

        Collection<String> resolveEntityTypes(
                @Nullable Collection<String> defaultEntityTypes) {
            return mPlatformEntityConfig.resolveEntityListModifications(
                    defaultEntityTypes == null
                            ? Collections.<String>emptyList()
                            : defaultEntityTypes);
        }

        @NonNull
        Collection<String> getHints() {
            return mPlatformEntityConfig.getHints();
        }

        boolean shouldIncludeDefaultEntityTypes() {
            return !mPlatformEntityConfig.getHints().isEmpty();
        }
    }

    @RequiresApi(29)
    private static final class Api29Impl extends BaseImpl {

        Api29Impl(@NonNull TextClassifier.EntityConfig platformEntityConfig) {
            super(platformEntityConfig);
        }

        @Override
        boolean shouldIncludeDefaultEntityTypes() {
            return mPlatformEntityConfig.shouldIncludeTypesFromTextClassifier();
        }
    }
}
