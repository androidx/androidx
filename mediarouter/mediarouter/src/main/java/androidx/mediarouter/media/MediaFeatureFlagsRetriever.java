/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.mediarouter.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class for fetching feature flags declared in the AndroidManifest.xml.
 *
 * <p>To override the default value of a feature, media apps should declare meta-data in their
 * manifests with an override value. For example:
 *
 * <pre class="prettyprint">{@code
 * <application>
 *     <meta-data
 *     android:name="androidx.mediarouter.media.media_transfer_enabled"
 *     android:value="true" />
 * </application>
 * }</pre>
 */
@RestrictTo(LIBRARY)
/* package */ final class MediaFeatureFlagsRetriever {

    /**
     * Media transfer is a feature that media routing can be controlled via system UI. By using
     * this, media app users can re-route the media without opening the app activity again. Also,
     * the media can be transferred from one device to another device seamlessly, depending on the
     * devices. This feature is supported from Android 11.
     */
    /* package */ static final String FEATURE_FLAG_MEDIA_TRANSFER_ENABLED =
            "androidx.mediarouter.media.media_transfer_enabled";

    /** Gets SystemRoutes using {@link android.media.MediaRouter2}. */
    /* package */ static final String FEATURE_FLAG_SYSTEM_ROUTING_USING_MEDIA_ROUTER2 =
            "androidx.mediarouter.media.system_routing_using_media_router2";

    @StringDef({
        FEATURE_FLAG_MEDIA_TRANSFER_ENABLED,
        FEATURE_FLAG_SYSTEM_ROUTING_USING_MEDIA_ROUTER2
    })
    @Retention(RetentionPolicy.SOURCE)
    /* package */ @interface FeatureFlag {}

    @NonNull private final Bundle mMetaData;

    /* package */ MediaFeatureFlagsRetriever(@NonNull Bundle metaData) {
        this.mMetaData = metaData;
    }

    /* package */ static MediaFeatureFlagsRetriever fromContext(@NonNull Context context) {
        try {
            ApplicationInfo app = context.getPackageManager().getApplicationInfo(
                                    context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = app.metaData;
            if (metaData == null) {
                metaData = Bundle.EMPTY;
            }
            return new MediaFeatureFlagsRetriever(metaData);
        } catch (PackageManager.NameNotFoundException e) {
            return new MediaFeatureFlagsRetriever(Bundle.EMPTY);
        }
    }

    /* package */ boolean isFlagDeclared(@FeatureFlag String featureFlag) {
        return mMetaData.containsKey(featureFlag);
    }

    /* package */ boolean getBoolean(@FeatureFlag String featureFlag) {
        return mMetaData.getBoolean(featureFlag);
    }
}
