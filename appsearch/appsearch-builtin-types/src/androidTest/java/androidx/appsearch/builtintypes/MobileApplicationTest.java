/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.doGet;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.Uri;

import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.builtintypes.testutil.FrameworkSchemaUtil;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MobileApplicationTest {
    @Test
    public void testBuilder() {
        MobileApplication.Builder builder = new MobileApplication.Builder("appid",
                "app-ns", "com.example.app", new byte[] {1, 2, 3});
        builder.setDisplayName("display name");
        builder.setAlternateNames(Arrays.asList("alternate name 1", "alternate name 2"));
        builder.setIconUri(Uri.parse("android.resource://com.example.app/drawable/12345"));
        builder.setUpdatedTimestampMillis(1234567890L);
        builder.setClassName("com.example.app.MainActivity");
        MobileApplication mobileApplication = builder.build();

        assertThat(mobileApplication.getId()).isEqualTo("appid");
        assertThat(mobileApplication.getNamespace()).isEqualTo("app-ns");
        assertThat(mobileApplication.getPackageName()).isEqualTo("com.example.app");
        assertThat(mobileApplication.getDisplayName()).isEqualTo("display name");
        assertThat(mobileApplication.getAlternateNames())
                .containsExactly("alternate name 1", "alternate name 2");
        assertThat(mobileApplication.getIconUri())
                .isEqualTo(Uri.parse("android.resource://com.example.app/drawable/12345"));
        assertThat(mobileApplication.getUpdatedTimestampMillis()).isEqualTo(1234567890L);
        assertThat(mobileApplication.getClassName()).isEqualTo("com.example.app.MainActivity");
    }

    @Test
    public void testBuilder_copy() {
        String id = "appid";
        String namespace = "app-ns";
        String packageName = "com.example.app";
        String name = "display name";
        List<String> alternateNames = Arrays.asList("alternate name 1", "alternate name 2");
        Uri iconUri = Uri.parse("android.resource://com.example.app/drawable/12345");
        byte[] sha256Certificate = new byte[] {1, 2, 3};
        long updatedTimestamp = 1234567890L;
        String className = "com.example.app.MainActivity";
        MobileApplication mobileApplication =
                new MobileApplication.Builder(id, namespace, packageName, sha256Certificate)
                        .setDisplayName(name)
                        .setAlternateNames(alternateNames)
                        .setIconUri(iconUri)
                        .setUpdatedTimestampMillis(updatedTimestamp)
                        .setClassName(className)
                        .build();

        assertThat(mobileApplication.getNamespace()).isEqualTo(namespace);
        assertThat(mobileApplication.getId()).isEqualTo(id);
        assertThat(mobileApplication.getAlternateNames()).isEqualTo(alternateNames);
        assertThat(mobileApplication.getIconUri()).isEqualTo(iconUri);
        assertThat(mobileApplication.getSha256Certificate()).isEqualTo(sha256Certificate);
        assertThat(mobileApplication.getUpdatedTimestampMillis()).isEqualTo(updatedTimestamp);
        assertThat(mobileApplication.getClassName()).isEqualTo(className);
    }

    @Test
    public void testBuilder_immutableAfterBuilt() {
        MobileApplication.Builder builder = new MobileApplication.Builder("appid", "apps-ns",
                "com.example.app", new byte[] {1, 2, 3});
        builder.setDisplayName("display name");
        builder.setAlternateNames(Arrays.asList("alternate name 1", "alternate name 2"));
        builder.setIconUri(Uri.parse("android.resource://com.example.app/drawable/12345"));
        builder.setUpdatedTimestampMillis(1234567890L);
        builder.setClassName("com.example.app.MainActivity");
        MobileApplication mobileApplication = builder.build();

        builder
                .setDisplayName("new display name")
                .setAlternateNames(Arrays.asList("new alternate name 1", "new alternate name 2"))
                .setIconUri(Uri.parse("android.resource://com.example.app/drawable/98765"))
                .setUpdatedTimestampMillis(9876543210L)
                .setClassName("com.example.app.NewMainActivity");

        // assert the original hasn't changed
        assertThat(mobileApplication.getAlternateNames())
                .containsExactly("alternate name 1", "alternate name 2");
        assertThat(mobileApplication.getIconUri())
                .isEqualTo(Uri.parse("android.resource://com.example.app/drawable/12345"));
        assertThat(mobileApplication.getUpdatedTimestampMillis()).isEqualTo(1234567890L);
        assertThat(mobileApplication.getClassName()).isEqualTo("com.example.app.MainActivity");
    }

    @Test
    public void testGenericDocument() throws Exception {
        String id = "appid";
        String namespace = "app-ns";
        String name = "display name";
        List<String> alternateNames = Arrays.asList("alternate name 1", "alternate name 2");
        Uri iconUri = Uri.parse("android.resource://com.example.app/drawable/12345");
        byte[] sha256Certificate = new byte[] {1, 2, 3};
        long updatedTimestamp = 1234567890L;
        String className = "com.example.app.MainActivity";
        String packageName = "com.example.app";

        MobileApplication mobileApplication =
                new MobileApplication.Builder(id, namespace, packageName, sha256Certificate)
                        .setDisplayName(name)
                        .setAlternateNames(alternateNames)
                        .setIconUri(iconUri)
                        .setUpdatedTimestampMillis(updatedTimestamp)
                        .setClassName(className)
                        .build();

        GenericDocument doc = GenericDocument.fromDocumentClass(mobileApplication);
        assertThat(doc.getSchemaType()).isEqualTo("builtin:MobileApplication");
        assertThat(doc.getNamespace()).isEqualTo(namespace);
        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getPropertyString("packageName")).isEqualTo(packageName);
        assertThat(doc.getPropertyBytes("sha256Certificate")).isEqualTo(sha256Certificate);
        assertThat(doc.getPropertyString("displayName")).isEqualTo(name);
        assertThat(
                doc.getPropertyStringArray("alternateNames"))
                .asList()
                .isEqualTo(alternateNames);
        assertThat(doc.getPropertyString("iconUri")).isEqualTo(iconUri.toString());
        assertThat(doc.getPropertyLong("updatedTimestamp")).isEqualTo(updatedTimestamp);
        assertThat(doc.getPropertyString("className")).isEqualTo(className);

        // Make timestamps the same
        GenericDocument newGenericDocument = new GenericDocument.Builder<>(
                GenericDocument.fromDocumentClass(doc.toDocumentClass(MobileApplication.class)))
                .setCreationTimestampMillis(doc.getCreationTimestampMillis()).build();
        assertThat(newGenericDocument).isEqualTo(doc);
    }

    @Test
    public void testSchemaType_matchingBetweenJetpackAndFramework() throws Exception {
        // Directly construct GenericDocument here like Framework does, instead of using the
        // MobileApplication java, since it will be updated in the future.
        GenericDocument genericDocMobileApplication =
                new GenericDocument.Builder<>("apps", "com.example.app",
                        "builtin:MobileApplication")
                        .setPropertyString(FrameworkSchemaUtil
                                        .MOBILE_APPLICATION_PROPERTY_ALTERNATE_NAMES,
                                "alternate name 1", "alternate name 2")
                        .setPropertyString(FrameworkSchemaUtil
                                .MOBILE_APPLICATION_PROPERTY_PACKAGE_NAME, "com.example.app")
                        .setPropertyBytes(FrameworkSchemaUtil
                                        .MOBILE_APPLICATION_PROPERTY_SHA256_CERTIFICATE,
                                new byte[] {1, 2, 3})
                        .setPropertyString(FrameworkSchemaUtil
                                .MOBILE_APPLICATION_PROPERTY_DISPLAY_NAME, "display name")
                        .setPropertyString(FrameworkSchemaUtil
                                        .MOBILE_APPLICATION_PROPERTY_ICON_URI,
                                "android.resource://com.example.app/drawable/12345")
                        .setPropertyLong(FrameworkSchemaUtil
                                .MOBILE_APPLICATION_PROPERTY_UPDATED_TIMESTAMP, 1234567890L)
                        .setPropertyString(FrameworkSchemaUtil
                                        .MOBILE_APPLICATION_PROPERTY_CLASS_NAME,
                                "com.example.app.MainActivity")
                        .build();

        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession session = LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(context, "forTest").build()).get();
        session.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(FrameworkSchemaUtil.MOBILE_APPLICATION_SCHEMA)
                .setForceOverride(true)
                .build()).get();
        try {
            checkIsBatchResultSuccess(session.putAsync(new PutDocumentsRequest.Builder()
                    .addGenericDocuments(genericDocMobileApplication).build()));

            GetByDocumentIdRequest request =
                    new GetByDocumentIdRequest.Builder("apps").addIds("com.example.app").build();
            List<GenericDocument> outDocuments = doGet(session, request);
            assertThat(outDocuments).hasSize(1);
            MobileApplication mobileApplication =
                    outDocuments.get(0).toDocumentClass(MobileApplication.class);
            assertThat(mobileApplication.getNamespace()).isEqualTo("apps");
            assertThat(mobileApplication.getId()).isEqualTo("com.example.app");
            assertThat(mobileApplication.getPackageName()).isEqualTo("com.example.app");
            assertThat(mobileApplication.getSha256Certificate()).isEqualTo(new byte[]{1, 2, 3});
            assertThat(mobileApplication.getDisplayName()).isEqualTo("display name");
            assertThat(mobileApplication.getIconUri().toString())
                    .isEqualTo("android.resource://com.example.app/drawable/12345");
            assertThat(mobileApplication.getUpdatedTimestampMillis()).isEqualTo(1234567890L);
            assertThat(mobileApplication.getClassName())
                    .isEqualTo("com.example.app.MainActivity");
        } finally {
            // Clear the db
            session.setSchemaAsync(
                    new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        }
    }

    @Test
    public void testBuildMobileApplication_nullAlternateNames() throws Exception {
        // Create a minimal mobile application where alternate names specifically is null
        GenericDocument genericDocMobileApplication =
                new GenericDocument.Builder<>("apps", "com.example.app",
                        "builtin:MobileApplication")
                        .setPropertyString(FrameworkSchemaUtil
                                .MOBILE_APPLICATION_PROPERTY_PACKAGE_NAME, "com.example.app")
                        .setPropertyBytes(FrameworkSchemaUtil
                                        .MOBILE_APPLICATION_PROPERTY_SHA256_CERTIFICATE,
                                new byte[] {1, 2, 3})
                        .build();

        // This should not throw an error
        MobileApplication converted =
                genericDocMobileApplication.toDocumentClass(MobileApplication.class);
        assertThat(converted.getPackageName()).isEqualTo("com.example.app");
        assertThat(converted.getAlternateNames()).isEmpty();
    }
}
