/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appsearch.localstorage.visibilitystore;

import android.util.Log;

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.collection.ArraySet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class containing Private Compute Core (PCC) data encapsulation write-side rules.
 */
@HideInPlatform
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class PccEncapsulationUtil {
    private static final String TAG = "AppSearchPccUtil";

    private PccEncapsulationUtil() {}

    /**
     * Returns whether the given {@code uid} is a Private Compute Core UID according to {@code
     * visibilityChecker}.
     */
    public static boolean isPccUid(@Nullable VisibilityChecker visibilityChecker, int uid) {
        return visibilityChecker != null && visibilityChecker.isPrivateComputeCoreUid(uid);
    }

    /**
     * Updates {@link InternalVisibilityConfig} items in-place for a {@code setSchema} request to
     * populate the caller's {@code writerUid}.
     *
     * <p>Every schema in the request has its {@link InternalVisibilityConfig} in {@code
     * visibilityConfigs} updated in-place so that {@link InternalVisibilityConfig#getWriterUid()}
     * is set to {@code callingUid}. If a schema in {@code schemas} does not have a corresponding
     * entry in {@code visibilityConfigs}, a default InternalVisibilityConfig is created and
     * added to {@code visibilityConfigs}.
     *
     * @param callingUid Operating system UID of the caller.
     * @param schemas List of {@link AppSearchSchema} objects included in the {@code setSchema}
     *     request.
     * @param visibilityConfigs List of {@link InternalVisibilityConfig} objects from the request,
     *     updated in-place with caller's {@code writerUid}.
     */
    @OptIn(markerClass = androidx.appsearch.app.ExperimentalAppSearchApi.class)
    public static void updateSetSchemaVisibilityConfigs(
            int callingUid,
            @NonNull List<AppSearchSchema> schemas,
            @NonNull List<InternalVisibilityConfig> visibilityConfigs) {
        if (!Flags.enablePccDataEncapsulation()
                || callingUid == InternalVisibilityConfig.INVALID_UID) {
            return;
        }

        // Iterate through the input visibility configs (which have writerUid set to INVALID_UID
        // by default) and populate the writerUid field.
        Set<String> handledSchemas = new ArraySet<>(schemas.size());
        for (int i = 0; i < visibilityConfigs.size(); i++) {
            InternalVisibilityConfig config = visibilityConfigs.get(i);
            if (config.getWriterUid() != callingUid) {
                config =
                        new InternalVisibilityConfig.Builder(config)
                                .setWriterUid(callingUid)
                                .build();
                visibilityConfigs.set(i, config);
            }
            handledSchemas.add(config.getSchemaType());
        }

        for (int i = 0; i < schemas.size(); i++) {
            String schemaType = schemas.get(i).getSchemaType();
            // Create a new visibility config for the schema type if it was not included in the
            // input VisibilityConfigs. This means that the schema type has default
            // package-private visibility. With PCC enabled we need to populate the config's
            // WriterUid field.
            if (!handledSchemas.contains(schemaType)) {
                visibilityConfigs.add(
                        new InternalVisibilityConfig.Builder(schemaType)
                                .setWriterUid(callingUid)
                                .build());
            }
        }
    }

    /**
     * Calculates the unprefixed schema types in a {@code setSchema} request that are being
     * downgraded from Private Compute Core (PCC) ownership to non-PCC ownership and therefore must
     * have their existing documents wiped.
     *
     * <p>This method enforces transition rules for schemas under PCC data encapsulation:
     *
     * <ul>
     *   <li><b>Implicit Upgrade (Non-PCC to PCC):</b> If an existing schema was previously written
     *       by a non-PCC component and is now being modified by a PCC caller, it is implicitly
     *       upgraded to a PCC-owned schema.
     *   <li><b>Downgrade and Wipe (PCC to Non-PCC):</b> If an existing schema was previously
     *       written by a PCC component and is now being modified by a non-PCC caller, it represents
     *       a PCC-to-non-PCC downgrade. To prevent PCC data leakage, all existing documents under
     *       these schemas must be wiped before applying the new non-PCC schema. These schemas are
     *       identified and returned.
     * </ul>
     *
     * @param visibilityStore The {@link VisibilityStore} instance.
     * @param visibilityChecker The {@link VisibilityChecker} instance.
     * @param callingPackageName Package name of the calling app.
     * @param databaseName Name of the database being updated.
     * @param callingUid Operating system UID of the caller.
     * @param schemas List of {@link AppSearchSchema} objects included in the {@code setSchema}
     *     request.
     * @return List of unprefixed PCC schema types that must be wiped before a non-PCC host can
     *     replace them, or an empty list if no wipe is needed.
     */
    @OptIn(markerClass = androidx.appsearch.app.ExperimentalAppSearchApi.class)
    public static @NonNull List<String> calculatePccSchemasToWipe(
            @Nullable VisibilityStore visibilityStore,
            @Nullable VisibilityChecker visibilityChecker,
            @NonNull String callingPackageName,
            @NonNull String databaseName,
            int callingUid,
            @NonNull List<AppSearchSchema> schemas) {
        if (!Flags.enablePccDataEncapsulation()
                || callingUid == InternalVisibilityConfig.INVALID_UID
                || visibilityStore == null
                || visibilityChecker == null) {
            // Skip and return -- use arrayList here to avoid mixing mutable and immutable return
            // types
            return new ArrayList<>();
        }

        boolean isPccCaller = isPccUid(visibilityChecker, callingUid);
        String prefix = PrefixUtil.createPrefix(callingPackageName, databaseName);

        List<String> pccSchemasToWipeUnprefixed = new ArrayList<>();
        for (int i = 0; i < schemas.size(); i++) {
            AppSearchSchema schema = schemas.get(i);
            String schemaType = schema.getSchemaType();
            String prefixedSchema = prefix + schemaType;
            InternalVisibilityConfig existingConfig = visibilityStore.getVisibility(prefixedSchema);

            if (existingConfig != null
                    && existingConfig.getWriterUid() != InternalVisibilityConfig.INVALID_UID) {
                boolean isExistingPcc = isPccUid(visibilityChecker, existingConfig.getWriterUid());
                if (!isExistingPcc && isPccCaller) {
                    Log.i(
                            TAG,
                            "Upgrading schema "
                                    + prefixedSchema
                                    + " to PCC schema by UID "
                                    + callingUid);
                } else if (isExistingPcc && !isPccCaller) {
                    pccSchemasToWipeUnprefixed.add(schemaType);
                }
            }
        }
        return pccSchemasToWipeUnprefixed;
    }
}
