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

package androidx.appsearch.localstorage.visibilitystore;


import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.VisibilityConfig;
import androidx.appsearch.app.VisibilityPermissionConfig;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.AppSearchConfigImpl;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.LocalStorageIcingOptionsConfig;
import androidx.appsearch.localstorage.OptimizeStrategy;
import androidx.appsearch.localstorage.UnlimitedLimitConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;

// "V2 schema" refers to V2 of the VisibilityDocument schema, but no Visibility overlay schema
// present. Simulates backwards compatibility situations.
public class VisibilityStoreMigrationFromV2Test {

    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private File mFile;

    @Before
    public void setUp() throws Exception {
        // Give ourselves global query permissions
        mFile = mTemporaryFolder.newFolder();
    }

    @Test
    public void testVisibilityMigration_from2() throws Exception {
        // As such, we can treat V2 documents as V3 documents when upgrading, but we need to test
        // this.

        // Values for a "foo" client
        String packageNameFoo = "packageFoo";
        byte[] sha256CertFoo = new byte[32];
        PackageIdentifier packageIdentifierFoo =
                new PackageIdentifier(packageNameFoo, sha256CertFoo);

        // Values for a "bar" client
        String packageNameBar = "packageBar";
        byte[] sha256CertBar = new byte[32];
        PackageIdentifier packageIdentifierBar =
                new PackageIdentifier(packageNameBar, sha256CertBar);

        // Create AppSearchImpl with visibility document version 2;
        AppSearchImpl appSearchImplInV2 = AppSearchImpl.create(mFile,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()), /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        InternalSetSchemaResponse internalSetSchemaResponse = appSearchImplInV2.setSchema(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                // no overlay schema
                ImmutableList.of(VisibilityPermissionConfig.SCHEMA,
                        VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_SCHEMA),
                /*prefixedVisibilityBundles=*/ Collections.emptyList(),
                /*forceOverride=*/ true, // force push the old version into disk
                /*version=*/ 2, //SCHEMA_VERSION_NESTED_PERMISSION_SCHEMA
                /*setSchemaStatsBuilder=*/ null);

        GetSchemaResponse getSchemaResponse = appSearchImplInV2.getSchema(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                new CallerAccess(/*callingPackageName=*/VisibilityStore.VISIBILITY_PACKAGE_NAME));
        assertThat(getSchemaResponse.getSchemas()).contains(
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_SCHEMA);
        assertThat(getSchemaResponse.getSchemas()).doesNotContain(
                VisibilityToDocumentConverter.PUBLIC_ACL_OVERLAY_SCHEMA);

        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        // Build deprecated visibility documents in version 2
        String prefix = PrefixUtil.createPrefix("package", "database");
        VisibilityConfig visibilityConfigV2 = new VisibilityConfig.Builder(prefix + "Schema")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(
                        new PackageIdentifier(packageNameFoo, sha256CertFoo))
                .addVisibleToPackage(
                        new PackageIdentifier(packageNameBar, sha256CertBar))
                .addVisibleToPermissions(
                        ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                SetSchemaRequest.READ_CALENDAR))
                .addVisibleToPermissions(
                        ImmutableSet.of(SetSchemaRequest.READ_ASSISTANT_APP_SEARCH_DATA))
                .addVisibleToPermissions(
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                .build();
        GenericDocument visibilityDocumentV2 = VisibilityToDocumentConverter
                .createVisibilityDocument(visibilityConfigV2);

        // Set client schema into AppSearchImpl with empty VisibilityDocument since we need to
        // directly put old version of VisibilityDocument.
        internalSetSchemaResponse = appSearchImplInV2.setSchema(
                "package",
                "database",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Schema").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*schemaVersion=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Put deprecated visibility documents in version 2 to AppSearchImpl
        appSearchImplInV2.putDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                visibilityDocumentV2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/null);

        // Persist to disk and re-open the AppSearchImpl
        appSearchImplInV2.close();
        AppSearchImpl appSearchImpl = AppSearchImpl.create(mFile,
                new AppSearchConfigImpl(new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig()), /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        VisibilityConfig actualConfig = VisibilityToDocumentConverter.createVisibilityConfig(
                appSearchImpl.getDocument(
                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                        VisibilityStore.VISIBILITY_DATABASE_NAME,
                        VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                        /*id=*/ prefix + "Schema",
                        /*typePropertyPaths=*/ Collections.emptyMap()),
                /*publicAclDocument=*/null,
                /*visibleToConfigDocument=*/null);

        assertThat(actualConfig.isNotDisplayedBySystem()).isTrue();
        assertThat(actualConfig.getVisibleToPackages())
                .containsExactly(packageIdentifierFoo, packageIdentifierBar);
        assertThat(actualConfig.getVisibleToPermissions()).containsExactlyElementsIn(
                ImmutableSet.of(
                        ImmutableSet.of(SetSchemaRequest.READ_SMS, SetSchemaRequest.READ_CALENDAR),
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA),
                        ImmutableSet.of(SetSchemaRequest.READ_ASSISTANT_APP_SEARCH_DATA)));

        // Check that the visibility overlay schema was added.
        getSchemaResponse = appSearchImpl.getSchema(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                new CallerAccess(/*callingPackageName=*/VisibilityStore.VISIBILITY_PACKAGE_NAME));
        assertThat(getSchemaResponse.getSchemas())
                .contains(VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_SCHEMA);
        assertThat(getSchemaResponse.getSchemas())
                .contains(VisibilityToDocumentConverter.PUBLIC_ACL_OVERLAY_SCHEMA);

        // But no overlay document was created.
        AppSearchException e = assertThrows(AppSearchException.class,
                 () -> appSearchImpl.getDocument(
                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                        VisibilityStore.VISIBILITY_DATABASE_NAME,
                         VisibilityToDocumentConverter.PUBLIC_ACL_OVERLAY_NAMESPACE,
                        /*id=*/ prefix + "Schema",
                        /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(e.getMessage()).isEqualTo(
                "Document (VS#Pkg$VS#Db/publicAclOverlay, package$database/Schema) not found.");

        appSearchImpl.close();
    }
}

