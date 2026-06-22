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

package androidx.camera.testing.impl;

import static androidx.camera.core.impl.utils.Exif.createFromFile;
import static androidx.camera.core.impl.utils.Exif.createFromInputStream;

import android.graphics.ImageFormat;

import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.Exif;
import androidx.core.util.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Utility class for creating fake {@link Exif}s for testing.
 */
public class ExifUtil {

    private static final String TEMP_FILE_PREFIX = "exif_temp_file_prefix";
    private static final String TEMP_FILE_SUFFIX = "exif_temp_file_suffix";

    private ExifUtil() {
    }

    /**
     * Create a fake {@link Exif} instance from the given JPEG bytes.
     */
    public static @NonNull Exif createExif(byte @NonNull [] jpegBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(jpegBytes);
        return createFromInputStream(byteArrayInputStream);
    }

    /**
     * Updates the exif info of the given JPEG and return a new JPEG byte array.
     */
    public static byte @NonNull [] updateExif(byte @NonNull [] jpegBytes,
            @NonNull Consumer<Exif> exifUpdater) throws IOException {
        File tempFile = saveBytesToFile(jpegBytes);
        tempFile.deleteOnExit();
        Exif exif = createFromFile(tempFile);
        exifUpdater.accept(exif);
        exif.save();
        return readBytesFromFile(tempFile);
    }

    private static File saveBytesToFile(byte @NonNull [] jpegBytes) throws IOException {
        File tempDir = null;
        String tmpDirProperty = System.getProperty("java.io.tmpdir");
        if (tmpDirProperty != null) {
            tempDir = new File(tmpDirProperty);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
        }
        File file = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, tempDir);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(jpegBytes);
        }
        return file;
    }

    private static byte[] readBytesFromFile(@NonNull File file) throws IOException {
        byte[] buffer = new byte[1024];
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream(1024)) {
            int read;
            while (true) {
                read = in.read(buffer);
                if (read == -1) break;
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    /**
     * Gets the {@link Exif} instance from the {@link ImageProxy}.
     */
    public static @Nullable Exif getExif(@NonNull ImageProxy image) {
        if (image.getFormat() == ImageFormat.JPEG) {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            buffer.rewind();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            try {
                return Exif.createFromInputStream(new ByteArrayInputStream(data));
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
}
