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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.SetSchemaResponse;

import org.junit.Test;

import java.util.Arrays;

public class SetSchemaResponseCtsTest {
    @Test
    public void testRebuild() {
        SetSchemaResponse.MigrationFailure failure1 = new SetSchemaResponse.MigrationFailure(
                "namespace",
                "failure1",
                "schemaType",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_INTERNAL_ERROR, "errorMessage"));
        SetSchemaResponse.MigrationFailure failure2 = new SetSchemaResponse.MigrationFailure(
                "namespace",
                "failure2",
                "schemaType",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_INTERNAL_ERROR, "errorMessage"));

        SetSchemaResponse.Builder builder = new SetSchemaResponse.Builder()
                .addDeletedType("delete1")
                .addIncompatibleType("incompatible1")
                .addMigratedType("migrated1")
                .addMigrationFailure(failure1);
        SetSchemaResponse original = builder.build();
        assertThat(original.getDeletedTypes()).containsExactly("delete1");
        assertThat(original.getIncompatibleTypes()).containsExactly("incompatible1");
        assertThat(original.getMigratedTypes()).containsExactly("migrated1");
        assertThat(original.getMigrationFailures()).containsExactly(failure1);

        SetSchemaResponse rebuild = builder
                .addDeletedType("delete2")
                .addIncompatibleType("incompatible2")
                .addMigratedType("migrated2")
                .addMigrationFailure(failure2)
                .build();

        // rebuild won't effect the original object
        assertThat(original.getDeletedTypes()).containsExactly("delete1");
        assertThat(original.getIncompatibleTypes()).containsExactly("incompatible1");
        assertThat(original.getMigratedTypes()).containsExactly("migrated1");
        assertThat(original.getMigrationFailures()).containsExactly(failure1);

        assertThat(rebuild.getDeletedTypes()).containsExactly("delete1", "delete2");
        assertThat(rebuild.getIncompatibleTypes()).containsExactly("incompatible1",
                "incompatible2");
        assertThat(rebuild.getMigratedTypes()).containsExactly("migrated1", "migrated2");
        assertThat(rebuild.getMigrationFailures()).containsExactly(failure1, failure2);
    }

    @Test
    public void testPluralAdds() {
        SetSchemaResponse.MigrationFailure failure1 = new SetSchemaResponse.MigrationFailure(
                "namespace",
                "failure1",
                "schemaType",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_INTERNAL_ERROR, "errorMessage"));

        SetSchemaResponse.Builder builder = new SetSchemaResponse.Builder()
                .addDeletedTypes(Arrays.asList("delete1"))
                .addIncompatibleTypes(Arrays.asList("incompatible1"))
                .addMigratedTypes(Arrays.asList("migrated1"))
                .addMigrationFailures(Arrays.asList(failure1));
        SetSchemaResponse singleEntries = builder.build();
        assertThat(singleEntries.getDeletedTypes()).containsExactly("delete1");
        assertThat(singleEntries.getIncompatibleTypes()).containsExactly("incompatible1");
        assertThat(singleEntries.getMigratedTypes()).containsExactly("migrated1");
        assertThat(singleEntries.getMigrationFailures()).containsExactly(failure1);

        SetSchemaResponse.MigrationFailure failure2 = new SetSchemaResponse.MigrationFailure(
                "namespace",
                "failure2",
                "schemaType",
                AppSearchResult.newFailedResult(
                        AppSearchResult.RESULT_INTERNAL_ERROR, "errorMessage"));
        SetSchemaResponse multiEntries = builder
                .addDeletedTypes(Arrays.asList("delete2", "deleted3", "deleted4"))
                .addIncompatibleTypes(Arrays.asList("incompatible2"))
                .addMigratedTypes(Arrays.asList("migrated2", "migrate3"))
                .addMigrationFailures(Arrays.asList(failure2))
                .build();

        assertThat(multiEntries.getDeletedTypes()).containsExactly("delete1", "delete2", "deleted3",
                "deleted4");
        assertThat(multiEntries.getIncompatibleTypes()).containsExactly("incompatible1",
                "incompatible2");
        assertThat(multiEntries.getMigratedTypes()).containsExactly("migrated1", "migrated2",
                "migrate3");
        assertThat(multiEntries.getMigrationFailures()).containsExactly(failure1, failure2);
    }

    @Test
    public void testMigrationFailure() {
        AppSearchResult<Void> expectedResult = AppSearchResult.newFailedResult(
                AppSearchResult.RESULT_INTERNAL_ERROR, "This is errorMessage.");
        SetSchemaResponse.MigrationFailure migrationFailure =
                new SetSchemaResponse.MigrationFailure("testNamespace", "testId",
                        "testSchemaType", expectedResult);
        assertThat(migrationFailure.getNamespace()).isEqualTo("testNamespace");
        assertThat(migrationFailure.getSchemaType()).isEqualTo("testSchemaType");
        assertThat(migrationFailure.getDocumentId()).isEqualTo("testId");
        assertThat(migrationFailure.getAppSearchResult()).isEqualTo(expectedResult);
        assertThat(migrationFailure.toString()).isEqualTo("MigrationFailure { schemaType:"
                + " testSchemaType, namespace: testNamespace, documentId: testId, "
                + "appSearchResult: [FAILURE(2)]: This is errorMessage.}");
    }
}
