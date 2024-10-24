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

package androidx.datastore.preferences.rxjava2;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesFactory;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.rxjava2.RxDataMigration;
import androidx.datastore.rxjava2.RxDataStore;
import androidx.test.core.app.ApplicationProvider;

import io.reactivex.Completable;
import io.reactivex.Single;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;

public class RxPreferencesDataStoreBuilderTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private RxDataStore<Preferences> mDataStore = null;

    private static final Preferences.Key<Integer> INTEGER_KEY =
            PreferencesKeys.intKey("int_key");

    private static Single<Preferences> incrementInteger(Preferences preferencesIn) {
        MutablePreferences prefs = preferencesIn.toMutablePreferences();
        Integer currentInt = prefs.get(INTEGER_KEY);
        prefs.set(INTEGER_KEY, currentInt != null ? currentInt + 1 : 1);
        return Single.just(prefs);
    }

    @After
    public void teardown() {
        if (mDataStore != null) {
            mDataStore.dispose();
            mDataStore.shutdownComplete().blockingAwait();
        }
    }

    @Test
    public void testConstructWithProduceFile() throws Exception {
        File file = tempFolder.newFile("temp.preferences_pb");

        mDataStore = new RxPreferenceDataStoreBuilder(() -> file).build();

        Single<Preferences> incrementInt = mDataStore.updateDataAsync(
                RxPreferencesDataStoreBuilderTest::incrementInteger);
        assertThat(incrementInt.blockingGet().get(INTEGER_KEY)).isEqualTo(1);
        mDataStore.dispose();
        mDataStore.shutdownComplete().blockingAwait();

        // Construct it again and confirm that the data is still there:
        mDataStore = new RxPreferenceDataStoreBuilder(() -> file).build();

        assertThat(mDataStore.data().blockingFirst().get(INTEGER_KEY))
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

        mDataStore = new RxPreferenceDataStoreBuilder(context, name).build();

        Single<Preferences> set1 = mDataStore.updateDataAsync(
                RxPreferencesDataStoreBuilderTest::incrementInteger);
        assertThat(set1.blockingGet().get(INTEGER_KEY)).isEqualTo(1);
        mDataStore.dispose();
        mDataStore.shutdownComplete().blockingAwait();

        // Construct it again and confirm that the data is still there:
        mDataStore = new RxPreferenceDataStoreBuilder(context, name).build();
        assertThat(mDataStore.data().blockingFirst().get(INTEGER_KEY)).isEqualTo(1);
        mDataStore.dispose();
        mDataStore.shutdownComplete().blockingAwait();


        // Construct it again with the expected file path and confirm that the data is there:
        mDataStore =
                new RxPreferenceDataStoreBuilder(
                        () ->
                                new File(context.getFilesDir().getPath()
                                        + "/datastore/" + name + ".preferences_pb")
                ).build();

        assertThat(mDataStore.data().blockingFirst().get(INTEGER_KEY)).isEqualTo(1);
    }

    @Test
    public void testMigrationsAreInstalledAndRun() throws Exception {
        RxDataMigration<Preferences> plusOneMigration = new RxDataMigration<Preferences>() {
            @Override
            public @NonNull Single<Boolean> shouldMigrate(@NonNull Preferences currentData) {
                return Single.just(true);
            }

            @Override
            public @NonNull Single<Preferences> migrate(@NonNull Preferences currentData) {
                return incrementInteger(currentData);
            }

            @Override
            public @NonNull Completable cleanUp() {
                return Completable.complete();
            }
        };

        mDataStore = new RxPreferenceDataStoreBuilder(() ->
                tempFolder.newFile("temp.preferences_pb"))
                .addRxDataMigration(plusOneMigration)
                .build();

        assertThat(mDataStore.data().blockingFirst().get(INTEGER_KEY))
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


        mDataStore = new RxPreferenceDataStoreBuilder(() -> file)
                .setCorruptionHandler(replaceFileCorruptionHandler)
                .build();

        assertThat(mDataStore.data().blockingFirst().get(INTEGER_KEY))
                .isEqualTo(99);
    }
}
