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

import androidx.annotation.NonNull;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.Migrator;

import org.junit.Test;

public class AppSearchMigratorTest {

    @Test
    public void testOnUpgrade() {
        Migrator migrator = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return true;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return new GenericDocument.Builder<>(document.getNamespace(), document.getId(),
                        document.getSchemaType())
                        .setCreationTimestampMillis(document.getCreationTimestampMillis())
                        .setScore(document.getScore())
                        .setTtlMillis(document.getTtlMillis())
                        .setPropertyString("migration",
                                "Upgrade the document from version " + currentVersion
                                        + " to version " + finalVersion)
                        .build();
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }
        };

        GenericDocument input = new GenericDocument.Builder<>("namespace", "id",
                "schemaType")
                .setCreationTimestampMillis(12345L)
                .setScore(100)
                .setTtlMillis(54321L).build();

        GenericDocument expected = new GenericDocument.Builder<>("namespace", "id",
                "schemaType")
                .setCreationTimestampMillis(12345L)
                .setScore(100)
                .setTtlMillis(54321L)
                .setPropertyString("migration",
                        "Upgrade the document from version 3 to version 5")
                .build();

        GenericDocument output = migrator.onUpgrade(/*currentVersion=*/3,
                /*finalVersion=*/5, input);
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testOnDowngrade() {
        Migrator migrator = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return true;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return new GenericDocument.Builder<>(document.getNamespace(), document.getId(),
                        document.getSchemaType())
                        .setCreationTimestampMillis(document.getCreationTimestampMillis())
                        .setScore(document.getScore())
                        .setTtlMillis(document.getTtlMillis())
                        .setPropertyString("migration",
                                "Downgrade the document from version " + currentVersion
                                        + " to version " + finalVersion)
                        .build();
            }
        };

        GenericDocument input = new GenericDocument.Builder<>("namespace", "id",
                "schemaType")
                .setCreationTimestampMillis(12345L)
                .setScore(100)
                .setTtlMillis(54321L).build();

        GenericDocument expected = new GenericDocument.Builder<>("namespace", "id",
                "schemaType")
                .setCreationTimestampMillis(12345L)
                .setScore(100)
                .setTtlMillis(54321L)
                .setPropertyString("migration",
                        "Downgrade the document from version 6 to version 4")
                .build();

        GenericDocument output = migrator.onDowngrade(/*currentVersion=*/6,
                /*finalVersion=*/4, input);
        assertThat(output).isEqualTo(expected);
    }
}
