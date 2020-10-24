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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ImageSaveLocationValidatorTest {

    private static final Uri ANY_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    @Test
    public void canSaveToValidWritableFile() throws IOException {
        final File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        final ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(saveLocation).build();

        assertThat(ImageSaveLocationValidator.isValid(outputOptions)).isTrue();
    }

    @Test
    public void canSaveToFileInCache() {
        final File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
        final String fileName = System.currentTimeMillis() + ".jpg";
        final File saveLocation = new File(cacheDir, fileName);
        saveLocation.deleteOnExit();
        final ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(saveLocation).build();

        assertThat(ImageSaveLocationValidator.isValid(outputOptions)).isTrue();
    }

    @Test
    public void cannotSaveToReadOnlyFile() throws IOException {
        final File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.setReadOnly();
        saveLocation.deleteOnExit();
        final ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(saveLocation).build();

        assertThat(ImageSaveLocationValidator.isValid(outputOptions)).isFalse();
    }

    @Test
    public void canSaveToMediaStore() {
        // Return a valid insertion Uri for the image
        TestContentProvider.sOnInsertCallback = uri -> Uri.parse(uri.toString() + "/1");
        Robolectric.buildContentProvider(TestContentProvider.class).create(ANY_URI.getAuthority());

        final ImageCapture.OutputFileOptions outputOptions = buildOutputFileOptions();
        assertThat(ImageSaveLocationValidator.isValid(outputOptions)).isTrue();
    }

    @Test
    public void cannotSaveToMediaStore_providerFailsInsertion() {
        // Make the provider fail to return a valid insertion Uri
        TestContentProvider.sOnInsertCallback = uri -> null;
        Robolectric.buildContentProvider(TestContentProvider.class).create(ANY_URI.getAuthority());

        final ImageCapture.OutputFileOptions outputOptions = buildOutputFileOptions();
        assertThat(ImageSaveLocationValidator.isValid(outputOptions)).isFalse();
    }

    @Test
    public void cannotSaveToMediaStore_providerCrashesOnInsertion() {
        // Make the provider crash when trying to return a valid insertion Uri
        TestContentProvider.sOnInsertCallback = uri -> {
            throw new IllegalArgumentException();
        };
        Robolectric.buildContentProvider(TestContentProvider.class).create(ANY_URI.getAuthority());

        final ImageCapture.OutputFileOptions outputOptions = buildOutputFileOptions();
        assertThat(ImageSaveLocationValidator.isValid(outputOptions)).isFalse();
    }

    @NonNull
    private ImageCapture.OutputFileOptions buildOutputFileOptions() {
        final Context context = ApplicationProvider.getApplicationContext();
        final ContentResolver resolver = context.getContentResolver();
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "test");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        return new ImageCapture.OutputFileOptions
                .Builder(resolver, ANY_URI, contentValues)
                .build();
    }

    static class TestContentProvider extends ContentProvider {

        @NonNull
        static Function<Uri, Uri> sOnInsertCallback = uri -> null;

        @Override
        public boolean onCreate() {
            return false;
        }

        @Nullable
        @Override
        public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                @Nullable String selection, @Nullable String[] selectionArgs,
                @Nullable String sortOrder) {
            return null;
        }

        @Nullable
        @Override
        public String getType(@NonNull Uri uri) {
            return null;
        }

        @Nullable
        @Override
        public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
            return sOnInsertCallback.apply(uri);
        }

        @Override
        public int delete(@NonNull Uri uri, @Nullable String selection,
                @Nullable String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(@NonNull Uri uri, @Nullable ContentValues values,
                @Nullable String selection, @Nullable String[] selectionArgs) {
            return 0;
        }
    }
}
