/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.multidex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Exposes application secondary dex files as files in the application data
 * directory.
 */
final class MultiDexExtractor {

    private static final String TAG = MultiDex.TAG;

    /**
     * We look for additional dex files named {@code classes2.dex},
     * {@code classes3.dex}, etc.
     */
    private static final String DEX_PREFIX = "classes";
    private static final String DEX_SUFFIX = ".dex";

    private static final String EXTRACTED_NAME_EXT = ".classes";
    private static final String EXTRACTED_SUFFIX = ".zip";

    private static final int BUFFER_SIZE = 0x4000;

    /**
     * Extracts application secondary dexes into files in the application data
     * directory.
     *
     * @param dexDir
     *
     * @return a list of files that were created. The list may be empty if there
     *         are no secondary dex files.
     * @throws IOException if encounters a problem while reading or writing
     *         secondary dex files
     */
    static List<File> load(Context context, ApplicationInfo applicationInfo, File dexDir)
            throws IOException {

        String extractedFilePrefix = new File(applicationInfo.sourceDir).getName()
                + EXTRACTED_NAME_EXT;

        prepareDexDir(dexDir, extractedFilePrefix);

        final List<File> files = new ArrayList<File>();
        ZipFile apk = new ZipFile(applicationInfo.sourceDir);
        try {

            int secondaryNumber = 2;

            ZipEntry dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + DEX_SUFFIX);
            while (dexFile != null) {
                String fileName = extractedFilePrefix + secondaryNumber + EXTRACTED_SUFFIX;
                File extractedFile = new File(dexDir, fileName);
                files.add(extractedFile);

                if (!extractedFile.isFile()) {
                    extract(context, apk, dexFile, extractedFile, extractedFilePrefix);
                }
                secondaryNumber++;
                dexFile = apk.getEntry(DEX_PREFIX + secondaryNumber + DEX_SUFFIX);
            }
        } finally {
            try {
                apk.close();
            } catch (IOException e) {
                Log.w(TAG, "Failed to close resource", e);
            }
        }

        return files;
    }

    private static void prepareDexDir(File dexDir, final String extractedFilePrefix)
            throws IOException {
        dexDir.mkdir();
        if (!dexDir.isDirectory()) {
            throw new IOException("Failed to create dex directory " + dexDir.getPath());
        }

        // Clean possible old files
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.startsWith(extractedFilePrefix);
            }
        };
        File[] files = dexDir.listFiles(filter);
        if (files == null) {
            Log.w(TAG, "Failed to list secondary dex dir content (" + dexDir.getPath() + ").");
            return;
        }
        for (File oldFile : files) {
            if (!oldFile.delete()) {
                Log.w(TAG, "Failed to delete old file " + oldFile.getPath());
            }
        }
    }

    private static void extract(
            Context context, ZipFile apk, ZipEntry dexFile, File extractTo,
            String extractedFilePrefix) throws IOException, FileNotFoundException {

        InputStream in = apk.getInputStream(dexFile);
        ZipOutputStream out = null;
        File tmp = File.createTempFile(extractedFilePrefix, EXTRACTED_SUFFIX,
                extractTo.getParentFile());
        Log.i(TAG, "Extracting " + tmp.getPath());
        try {
            out = new ZipOutputStream(new FileOutputStream(tmp));
            try {
                ZipEntry classesDex = new ZipEntry("classes.dex");
                out.putNextEntry(classesDex);

                byte[] buffer = new byte[BUFFER_SIZE];
                int length = in.read(buffer);
                while (length > 0) {
                    out.write(buffer, 0, length);
                    length = in.read(buffer);
                }
            } finally {
                closeQuietly(out);
            }
            Log.i(TAG, "Renaming to " + extractTo.getPath());
            if (!tmp.renameTo(extractTo)) {
                throw new IOException("Failed to rename \"" + tmp.getAbsolutePath() + "\" to \"" +
                        extractTo.getAbsolutePath() + "\"");
            }
        } finally {
            closeQuietly(in);
            tmp.delete(); // return status ignored
        }
    }

    /**
     * Closes the given {@code Closeable}. Suppresses any IO exceptions.
     */
    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed to close resource", e);
        }
    }
}
