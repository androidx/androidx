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
package androidx.appsearch.platformstorage;

import android.content.Context;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appsearch.app.Features;
import androidx.core.util.Preconditions;

/**
 * An implementation of {@link Features}. Feature availability is dependent on Android API
 * level.
 */
final class FeaturesImpl implements Features {
    private static final String APPSEARCH_MODULE_NAME = "com.android.appsearch";

    // This will be set to -1 to indicate the AppSearch version code hasn't bee checked, then to
    // 0 if it is not found, or the version code if it is found.
    private static volatile long sAppSearchVersionCode = -1;

    // Context is used to check mainline module version, as support varies by module version.
    private final Context mContext;

    FeaturesImpl(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
    }

    @Override
    public boolean isFeatureSupported(@NonNull String feature) {
        switch (feature) {
            // Android T Features
            case Features.ADD_PERMISSIONS_AND_GET_VISIBILITY:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_BY_ID:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK:
                // fall through
            case Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;

            // Android U Features
            case Features.JOIN_SPEC_AND_QUALIFIED_ID:
                // fall through
            case Features.LIST_FILTER_QUERY_LANGUAGE:
                // fall through
            case Features.NUMERIC_SEARCH:
                // fall through
            case Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION:
                // fall through
            case Features.SEARCH_SPEC_PROPERTY_WEIGHTS:
                // fall through
            case Features.SEARCH_SUGGESTION:
                // fall through
            case Features.TOKENIZER_TYPE_RFC822:
                // fall through
            case Features.VERBATIM_SEARCH:
                // fall through
            case Features.SET_SCHEMA_CIRCULAR_REFERENCES:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

            // Beyond Android U features
            case Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA:
                // TODO(b/258715421) : Update to reflect support in Android U+ once this feature has
                // an extservices sdk that includes it.
                // fall through
            case Features.SCHEMA_SET_DELETION_PROPAGATION:
                // TODO(b/268521214) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SCHEMA_ADD_PARENT_TYPE:
                // TODO(b/269295094) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES:
                // TODO(b/289150947) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES:
                // TODO(b/296088047) : Update when feature is ready in service-appsearch.
                return false;
            default:
                return false;
        }
    }

    @Override
    public int getMaxIndexedProperties() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return 64;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            // Sixty-four properties were enabled in mainline module 'aml_ase_331311020'
            return getAppSearchVersionCode(mContext) >= 331311020 ? 64 : 16;
        } else {
            return 16;
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static long getAppSearchVersionCode(Context context) {
        if (sAppSearchVersionCode != -1) {
            return sAppSearchVersionCode;
        }
        synchronized (FeaturesImpl.class) {
            // Check again in case it was assigned while waiting
            if (sAppSearchVersionCode == -1) {
                long appsearchVersionCode = 0;
                try {
                    PackageManager packageManager = context.getPackageManager();
                    String appSearchPackageName =
                            ApiHelperForQ.getAppSearchPackageName(packageManager);
                    if (appSearchPackageName != null) {
                        PackageInfo pInfo = packageManager
                                .getPackageInfo(appSearchPackageName, PackageManager.MATCH_APEX);
                        appsearchVersionCode = ApiHelperForQ.getPackageInfoLongVersionCode(pInfo);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // Module not installed
                }
                sAppSearchVersionCode = appsearchVersionCode;
            }
        }
        return sAppSearchVersionCode;
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static class ApiHelperForQ {
        @DoNotInline
        static long getPackageInfoLongVersionCode(PackageInfo pInfo) {
            return pInfo.getLongVersionCode();
        }

        @DoNotInline
        static String getAppSearchPackageName(PackageManager packageManager)
                throws PackageManager.NameNotFoundException {
            ModuleInfo appSearchModule =
                    packageManager.getModuleInfo(APPSEARCH_MODULE_NAME, 1);
            return appSearchModule.getPackageName();
        }
    }
}
