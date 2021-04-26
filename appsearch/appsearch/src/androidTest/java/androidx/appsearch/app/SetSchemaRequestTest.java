/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.app;

import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES;
import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.annotation.Document;
import androidx.collection.ArrayMap;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SetSchemaRequestTest {
// @exportToFramework:startStrip()
    @Document
    static class Card {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.StringProperty
                (indexingType = INDEXING_TYPE_PREFIXES, tokenizerType = TOKENIZER_TYPE_PLAIN)
        String mString;

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AnnotationProcessorTestBase.Card)) {
                return false;
            }
            AnnotationProcessorTestBase.Card otherCard = (AnnotationProcessorTestBase.Card) other;
            assertThat(otherCard.mNamespace).isEqualTo(this.mNamespace);
            assertThat(otherCard.mId).isEqualTo(this.mId);
            return true;
        }
    }

    static class Spade {}

    @Document
    static class King extends Spade {
        @Document.Id
        String mId;

        @Document.Namespace
        String mNamespace;

        @Document.StringProperty
                (indexingType = INDEXING_TYPE_PREFIXES, tokenizerType = TOKENIZER_TYPE_PLAIN)
        String mString;
    }

    @Document
    static class Queen extends Spade {
        @Document.Namespace
        String mNamespace;

        @Document.Id
        String mId;

        @Document.StringProperty
                (indexingType = INDEXING_TYPE_PREFIXES, tokenizerType = TOKENIZER_TYPE_PLAIN)
        String mString;
    }
// @exportToFramework:endStrip()

    private static Collection<String> getSchemaTypesFromSetSchemaRequest(SetSchemaRequest request) {
        HashSet<String> schemaTypes = new HashSet<>();
        for (AppSearchSchema schema : request.getSchemas()) {
            schemaTypes.add(schema.getSchemaType());
        }
        return schemaTypes;
    }

    @Test
    public void testInvalidSchemaReferences_fromDisplayedBySystem() {
        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class,
                () -> new SetSchemaRequest.Builder().setSchemaTypeDisplayedBySystem(
                        "InvalidSchema", false).build());
        assertThat(expected).hasMessageThat().contains("referenced, but were not added");
    }

    @Test
    public void testInvalidSchemaReferences_fromPackageVisibility() {
        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class,
                () -> new SetSchemaRequest.Builder().setSchemaTypeVisibilityForPackage(
                        "InvalidSchema", /*visible=*/ true, new PackageIdentifier(
                                "com.foo.package", /*sha256Certificate=*/ new byte[]{})).build());
        assertThat(expected).hasMessageThat().contains("referenced, but were not added");
    }

    @Test
    public void testSetSchemaTypeDisplayedBySystem_displayed() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();

        // By default, the schema is displayed.
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addSchemas(schema).build();
        assertThat(request.getSchemasNotDisplayedBySystem()).isEmpty();

        request = new SetSchemaRequest.Builder()
                .addSchemas(schema).setSchemaTypeDisplayedBySystem("Schema", true).build();
        assertThat(request.getSchemasNotDisplayedBySystem()).isEmpty();
    }

    @Test
    public void testSetSchemaTypeDisplayedBySystem_notDisplayed() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();
        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(schema).setSchemaTypeDisplayedBySystem("Schema", false).build();
        assertThat(request.getSchemasNotDisplayedBySystem()).containsExactly("Schema");
    }

// @exportToFramework:startStrip()
    @Test
    public void testSetDocumentClassDisplayedBySystem_displayed() throws Exception {
        // By default, the schema is displayed.
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addDocumentClasses(Card.class).build();
        assertThat(request.getSchemasNotDisplayedBySystem()).isEmpty();

        request =
                new SetSchemaRequest.Builder().addDocumentClasses(
                        Card.class).setDocumentClassDisplayedBySystem(
                        Card.class, true).build();
        assertThat(request.getSchemasNotDisplayedBySystem()).isEmpty();
    }

    @Test
    public void testSetDocumentClassDisplayedBySystem_notDisplayed() throws Exception {
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addDocumentClasses(
                        Card.class).setDocumentClassDisplayedBySystem(
                        Card.class, false).build();
        assertThat(request.getSchemasNotDisplayedBySystem()).containsExactly("Card");
    }
// @exportToFramework:endStrip()

    @Test
    public void testSchemaTypeVisibilityForPackage_visible() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();

        // By default, the schema is not visible.
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addSchemas(schema).build();
        assertThat(request.getSchemasVisibleToPackages()).isEmpty();

        PackageIdentifier packageIdentifier = new PackageIdentifier("com.package.foo",
                new byte[]{100});
        Map<String, Set<PackageIdentifier>> expectedVisibleToPackagesMap = new ArrayMap<>();
        expectedVisibleToPackagesMap.put("Schema", Collections.singleton(packageIdentifier));

        request =
                new SetSchemaRequest.Builder().addSchemas(schema).setSchemaTypeVisibilityForPackage(
                        "Schema", /*visible=*/ true, packageIdentifier).build();
        assertThat(request.getSchemasVisibleToPackages()).containsExactlyEntriesIn(
                expectedVisibleToPackagesMap);
    }

    @Test
    public void testSchemaTypeVisibilityForPackage_notVisible() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();

        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addSchemas(schema).setSchemaTypeVisibilityForPackage(
                        "Schema", /*visible=*/ false, new PackageIdentifier("com.package.foo",
                                /*sha256Certificate=*/ new byte[]{})).build();
        assertThat(request.getSchemasVisibleToPackages()).isEmpty();
    }

    @Test
    public void testSchemaTypeVisibilityForPackage_deduped() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();

        PackageIdentifier packageIdentifier = new PackageIdentifier("com.package.foo",
                new byte[]{100});
        Map<String, Set<PackageIdentifier>> expectedVisibleToPackagesMap = new ArrayMap<>();
        expectedVisibleToPackagesMap.put("Schema", Collections.singleton(packageIdentifier));

        SetSchemaRequest request =
                new SetSchemaRequest.Builder()
                        .addSchemas(schema)
                        // Set it visible for "Schema"
                        .setSchemaTypeVisibilityForPackage("Schema", /*visible=*/
                                true, packageIdentifier)
                        // Set it visible for "Schema" again, which should be a no-op
                        .setSchemaTypeVisibilityForPackage("Schema", /*visible=*/
                                true, packageIdentifier)
                        .build();
        assertThat(request.getSchemasVisibleToPackages()).containsExactlyEntriesIn(
                expectedVisibleToPackagesMap);
    }

    @Test
    public void testSchemaTypeVisibilityForPackage_removed() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();

        SetSchemaRequest request =
                new SetSchemaRequest.Builder()
                        .addSchemas(schema)
                        // First set it as visible
                        .setSchemaTypeVisibilityForPackage("Schema", /*visible=*/
                                true, new PackageIdentifier("com.package.foo",
                                        /*sha256Certificate=*/ new byte[]{100}))
                        // Then make it not visible
                        .setSchemaTypeVisibilityForPackage("Schema", /*visible=*/
                                false, new PackageIdentifier("com.package.foo",
                                        /*sha256Certificate=*/ new byte[]{100}))
                        .build();

        // Nothing should be visible.
        assertThat(request.getSchemasVisibleToPackages()).isEmpty();
    }

// @exportToFramework:startStrip()
    @Test
    public void testSetDocumentClassVisibilityForPackage_visible() throws Exception {
        // By default, the schema is not visible.
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addDocumentClasses(Card.class).build();
        assertThat(request.getSchemasVisibleToPackages()).isEmpty();

        PackageIdentifier packageIdentifier = new PackageIdentifier("com.package.foo",
                new byte[]{100});
        Map<String, Set<PackageIdentifier>> expectedVisibleToPackagesMap = new ArrayMap<>();
        expectedVisibleToPackagesMap.put("Card", Collections.singleton(packageIdentifier));

        request =
                new SetSchemaRequest.Builder().addDocumentClasses(
                        Card.class).setDocumentClassVisibilityForPackage(
                        Card.class, /*visible=*/ true, packageIdentifier).build();
        assertThat(request.getSchemasVisibleToPackages()).containsExactlyEntriesIn(
                expectedVisibleToPackagesMap);
    }

    @Test
    public void testSetDocumentClassVisibilityForPackage_notVisible() throws Exception {
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addDocumentClasses(
                        Card.class).setDocumentClassVisibilityForPackage(
                        Card.class, /*visible=*/ false,
                        new PackageIdentifier("com.package.foo", /*sha256Certificate=*/
                                new byte[]{})).build();
        assertThat(request.getSchemasVisibleToPackages()).isEmpty();
    }

    @Test
    public void testSetDocumentClassVisibilityForPackage_deduped() throws Exception {
        // By default, the schema is not visible.
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addDocumentClasses(Card.class).build();
        assertThat(request.getSchemasVisibleToPackages()).isEmpty();

        PackageIdentifier packageIdentifier = new PackageIdentifier("com.package.foo",
                new byte[]{100});
        Map<String, Set<PackageIdentifier>> expectedVisibleToPackagesMap = new ArrayMap<>();
        expectedVisibleToPackagesMap.put("Card", Collections.singleton(packageIdentifier));

        request =
                new SetSchemaRequest.Builder()
                        .addDocumentClasses(Card.class)
                        .setDocumentClassVisibilityForPackage(Card.class, /*visible=*/
                                true, packageIdentifier)
                        .setDocumentClassVisibilityForPackage(Card.class, /*visible=*/
                                true, packageIdentifier)
                        .build();
        assertThat(request.getSchemasVisibleToPackages()).containsExactlyEntriesIn(
                expectedVisibleToPackagesMap);
    }

    @Test
    public void testSetDocumentClassVisibilityForPackage_removed() throws Exception {
        // By default, the schema is not visible.
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addDocumentClasses(Card.class).build();
        assertThat(request.getSchemasVisibleToPackages()).isEmpty();

        request =
                new SetSchemaRequest.Builder()
                        .addDocumentClasses(Card.class)
                        // First set it as visible
                        .setDocumentClassVisibilityForPackage(Card.class, /*visible=*/
                                true, new PackageIdentifier("com.package.foo",
                                        /*sha256Certificate=*/ new byte[]{100}))
                        // Then make it not visible
                        .setDocumentClassVisibilityForPackage(Card.class, /*visible=*/
                                false, new PackageIdentifier("com.package.foo",
                                        /*sha256Certificate=*/ new byte[]{100}))
                        .build();

        // Nothing should be visible.
        assertThat(request.getSchemasVisibleToPackages()).isEmpty();
    }

    @Test
    public void testAddDocumentClasses_byCollection() throws Exception {
        Set<Class<? extends Spade>> cardClasses = ImmutableSet.of(Queen.class, King.class);
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addDocumentClasses(cardClasses)
                        .build();
        assertThat(getSchemaTypesFromSetSchemaRequest(request)).containsExactly("Queen",
                "King");
    }

    @Test
    public void testAddDocumentClasses_byCollectionWithSeparateCalls() throws
            Exception {
        SetSchemaRequest request =
                new SetSchemaRequest.Builder().addDocumentClasses(ImmutableSet.of(Queen.class))
                        .addDocumentClasses(ImmutableSet.of(King.class)).build();
        assertThat(getSchemaTypesFromSetSchemaRequest(request)).containsExactly("Queen",
                "King");
    }

    @Test
    public void testSetVersion() throws
            Exception {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new SetSchemaRequest.Builder()
                .addDocumentClasses(ImmutableSet.of(Queen.class)).setVersion(0).build());
        assertThat(exception).hasMessageThat().contains("Version must be a positive number");
        SetSchemaRequest request = new SetSchemaRequest.Builder()
                        .addDocumentClasses(ImmutableSet.of(Queen.class)).setVersion(1).build();
        assertThat(request.getVersion()).isEqualTo(1);
    }
// @exportToFramework:endStrip()
}
