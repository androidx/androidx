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

package androidx.datastore.preferences.rxjava3;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesFactory;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.rxjava3.RxDataMigration;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class RxPreferencesDataStoreBuilderTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final Preferences.Key<Integer> INTEGER_KEY =
            PreferencesKeys.intKey("int_key");

    private static Single<Preferences> incrementInteger(Preferences preferencesIn) {
        MutablePreferences prefs = preferencesIn.toMutablePreferences();
        Integer currentInt = prefs.get(INTEGER_KEY);
        prefs.set(INTEGER_KEY, currentInt != null ? currentInt + 1 : 1);
        return Single.just(prefs);
    }

    @Test
    public void testConstructWithProduceFile() throws Exception {
        File file = tempFolder.newFile("temp.preferences_pb");

        RxDataStore<Preferences> dataStore =
                new RxPreferenceDataStoreBuilder(() -> file).build();

        Single<Preferences> incrementInt = dataStore.updateDataAsync(
                RxPreferencesDataStoreBuilderTest::incrementInteger);
        assertThat(incrementInt.blockingGet().get(INTEGER_KEY)).isEqualTo(1);
        dataStore.dispose();
        dataStore.shutdownComplete().blockingAwait();

        // Construct it again and confirm that the data is still there:
        dataStore = new RxPreferenceDataStoreBuilder(() -> file).build();

        assertThat(dataStore.data().blockingFirst().get(INTEGER_KEY))
                .isEqualTo(1);
    }


    @Test
    public void testConstructWithContextAndName() throws Exception {

        Context context = ApplicationProvider.getApplicationContext();
        String name = "my_data_store";

        File prefsFile = new File(context.getFilesDir().getPath()
                + "/datastore/" + name + ".preferences_pb");
        if (prefsFile.exists()) {
            prefsFile.delete();
        }

        RxDataStore<Preferences> dataStore =
                new RxPreferenceDataStoreBuilder(context, name).build();

        Single<Preferences> set1 = dataStore.updateDataAsync(
                RxPreferencesDataStoreBuilderTest::incrementInteger);
        assertThat(set1.blockingGet().get(INTEGER_KEY)).isEqualTo(1);
        dataStore.dispose();
        dataStore.shutdownComplete().blockingAwait();

        // Construct it again and confirm that the data is still there:
        dataStore = new RxPreferenceDataStoreBuilder(context, name).build();
        assertThat(dataStore.data().blockingFirst().get(INTEGER_KEY)).isEqualTo(1);
        dataStore.dispose();
        dataStore.shutdownComplete().blockingAwait();


        // Construct it again with the expected file path and confirm that the data is there:
        dataStore =
                new RxPreferenceDataStoreBuilder(
                        () ->
                                new File(context.getFilesDir().getPath()
                                        + "/datastore/" + name + ".preferences_pb")
                ).build();

        assertThat(dataStore.data().blockingFirst().get(INTEGER_KEY)).isEqualTo(1);
    }

    @Test
    public void testMigrationsAreInstalledAndRun() throws Exception {
        RxDataMigration<Preferences> plusOneMigration = new RxDataMigration<Preferences>() {
            @NonNull
            @Override
            public Single<Boolean> shouldMigrate(@NonNull Preferences currentData) {
                return Single.just(true);
            }

            @NonNull
            @Override
            public Single<Preferences> migrate(@NonNull Preferences currentData) {
                return incrementInteger(currentData);
            }

            @NonNull
            @Override
            public Completable cleanUp() {
                return Completable.complete();
            }
        };

        RxDataStore<Preferences> dataStore =
                new RxPreferenceDataStoreBuilder(() ->
                        tempFolder.newFile("temp.preferences_pb"))
                        .addRxDataMigration(plusOneMigration)
                        .build();

        assertThat(dataStore.data().blockingFirst().get(INTEGER_KEY))
                .isEqualTo(1);
    }


    @Test
    public void testCorruptionHandlerIsUsed() throws Exception {

        File file = tempFolder.newFile("temp.preferences_pb");

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(0); // will cause corruption exception
        }

        ReplaceFileCorruptionHandler<Preferences> replaceFileCorruptionHandler =
                new ReplaceFileCorruptionHandler<Preferences>(exception -> {
                    MutablePreferences mutablePreferences =
                            PreferencesFactory.createMutable();
                    mutablePreferences.set(INTEGER_KEY, 99);
                    return (Preferences) mutablePreferences;
                });


        RxDataStore<Preferences> dataStore =
                new RxPreferenceDataStoreBuilder(() -> file)
                        .setCorruptionHandler(replaceFileCorruptionHandler)
                        .build();

        assertThat(dataStore.data().blockingFirst().get(INTEGER_KEY))
                .isEqualTo(99);
    }
}