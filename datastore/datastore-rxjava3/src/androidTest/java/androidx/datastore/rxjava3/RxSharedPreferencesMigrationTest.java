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

package androidx.datastore.rxjava3;

import static androidx.testutils.AssertionsKt.assertThrows;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.datastore.core.DataMigration;
import androidx.datastore.core.DataStore;
import androidx.datastore.migrations.SharedPreferencesView;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.truth.Truth;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import io.reactivex.rxjava3.core.Single;

public class RxSharedPreferencesMigrationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final String mSharedPrefsName = "shared_prefs_name";


    private Context mContext;
    private SharedPreferences mSharedPrefs;
    private File mDatastoreFile;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mSharedPrefs = mContext.getSharedPreferences(mSharedPrefsName, Context.MODE_PRIVATE);
        mDatastoreFile = temporaryFolder.newFile("test_file.preferences_pb");

        assertThat(mSharedPrefs.edit().clear().commit()).isTrue();
    }

    @Test
    public void testShouldMigrateSkipsMigration() {
        RxSharedPreferencesMigration<Byte> skippedMigration =
                new RxSharedPreferencesMigration<Byte>() {
                    @NotNull
                    @Override
                    public Single<Boolean> shouldMigrate(Byte currentData) {
                        return Single.just(false);
                    }

                    @NotNull
                    @Override
                    public Single<Byte> migrate(
                            @NotNull SharedPreferencesView sharedPreferencesView,
                            Byte currentData) {
                        return Single.error(
                                new IllegalStateException("We shouldn't reach this point!"));
                    }
                };


        DataMigration<Byte> spMigration =
                getSpMigrationBuilder(skippedMigration).build();

        DataStore<Byte> dataStoreWithMigrations = getDataStoreWithMigration(spMigration);

        Truth.assertThat(RxDataStore.data(dataStoreWithMigrations).blockingFirst()).isEqualTo(0);
    }

    @Test
    public void testSharedPrefsViewContainsSpecifiedKeys() {
        String includedKey = "key1";
        int includedVal = 99;
        String notMigratedKey = "key2";

        assertThat(mSharedPrefs.edit().putInt(includedKey, includedVal).putInt(notMigratedKey,
                123).commit()).isTrue();

        DataMigration<Byte> dataMigration =
                getSpMigrationBuilder(
                        new DefaultMigration() {
                            @NotNull
                            @Override
                            public Single<Byte> migrate(
                                    @NotNull SharedPreferencesView sharedPreferencesView,
                                    Byte currentData) {
                                assertThat(sharedPreferencesView.contains(includedKey)).isTrue();
                                assertThat(sharedPreferencesView.getAll().size()).isEqualTo(1);
                                assertThrows(IllegalStateException.class,
                                        () -> sharedPreferencesView.getInt(notMigratedKey, -1));

                                return Single.just((byte) 50);
                            }
                        }
                ).setKeysToMigrate(includedKey).build();

        DataStore<Byte> byteStore = getDataStoreWithMigration(dataMigration);

        assertThat(RxDataStore.data(byteStore).blockingFirst()).isEqualTo(50);

        assertThat(mSharedPrefs.contains(includedKey)).isFalse();
        assertThat(mSharedPrefs.contains(notMigratedKey)).isTrue();
    }


    @Test
    public void testSharedPrefsViewWithAllKeysSpecified() {
        String includedKey = "key1";
        String includedKey2 = "key2";
        int value = 99;

        assertThat(mSharedPrefs.edit().putInt(includedKey, value).putInt(includedKey2,
                value).commit()).isTrue();

        DataMigration<Byte> dataMigration =
                getSpMigrationBuilder(
                        new DefaultMigration() {
                            @NotNull
                            @Override
                            public Single<Byte> migrate(
                                    @NotNull SharedPreferencesView sharedPreferencesView,
                                    Byte currentData) {
                                assertThat(sharedPreferencesView.contains(includedKey)).isTrue();
                                assertThat(sharedPreferencesView.contains(includedKey2)).isTrue();
                                assertThat(sharedPreferencesView.getAll().size()).isEqualTo(2);

                                return Single.just((byte) 50);
                            }
                        }
                ).build();

        DataStore<Byte> byteStore = getDataStoreWithMigration(dataMigration);

        assertThat(RxDataStore.data(byteStore).blockingFirst()).isEqualTo(50);

        assertThat(mSharedPrefs.contains(includedKey)).isFalse();
        assertThat(mSharedPrefs.contains(includedKey2)).isFalse();
    }

    @Test
    public void testDeletesEmptySharedPreferences() {
        String key = "key";
        String value = "value";
        assertThat(mSharedPrefs.edit().putString(key, value).commit()).isTrue();

        DataMigration<Byte> dataMigration =
                getSpMigrationBuilder(new DefaultMigration()).setDeleteEmptyPreferences(
                        true).build();
        DataStore<Byte> byteStore = getDataStoreWithMigration(dataMigration);
        assertThat(RxDataStore.data(byteStore).blockingFirst()).isEqualTo(0);

        // Check that the shared preferences files are deleted
        File prefsDir = new File(mContext.getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, mSharedPrefsName + ".xml");
        File backupPrefsFile = new File(prefsFile.getPath() + ".bak");
        assertThat(prefsFile.exists()).isFalse();
        assertThat(backupPrefsFile.exists()).isFalse();
    }

    private RxSharedPreferencesMigrationBuilder<Byte> getSpMigrationBuilder(
            RxSharedPreferencesMigration<Byte> rxSharedPreferencesMigration) {
        return new RxSharedPreferencesMigrationBuilder<Byte>(mContext, mSharedPrefsName,
                rxSharedPreferencesMigration);
    }

    private DataStore<Byte> getDataStoreWithMigration(DataMigration<Byte> dataMigration) {
        return new RxDataStoreBuilder<Byte>(() -> mDatastoreFile, new TestingSerializer())
                .addDataMigration(dataMigration).build();
    }


    private static class DefaultMigration implements RxSharedPreferencesMigration<Byte> {

        @NotNull
        @Override
        public Single<Boolean> shouldMigrate(Byte currentData) {
            return Single.just(true);
        }

        @NotNull
        @Override
        public Single<Byte> migrate(@NotNull SharedPreferencesView sharedPreferencesView,
                Byte currentData) {
            return Single.just(currentData);
        }
    }
}
