/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.webkit.internal;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.TypedValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
  * A Utility class for opening resources, assets and files for
  * {@link androidx.webkit.WebViewAssetLoader}.
  * Forked from the chromium project org.chromium.android_webview.AndroidProtocolHandler
  */
public class AssetHelper {

    /**
     * Default value to be used as MIME type if guessing MIME type failed.
     */
    public static final String DEFAULT_MIME_TYPE = "text/plain";

    private final @NonNull Context mContext;

    public AssetHelper(@NonNull Context context) {
        this.mContext = context;
    }

    private static @NonNull InputStream handleSvgzStream(@NonNull String path,
            @NonNull InputStream stream) throws IOException {
        return path.endsWith(".svgz") ? new GZIPInputStream(stream) : stream;
    }

    private static @NonNull String removeLeadingSlash(@NonNull String path) {
        if (path.length() > 1 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }

    private int getFieldId(@NonNull String resourceType, @NonNull String resourceName) {
        String packageName = mContext.getPackageName();
        return mContext.getResources().getIdentifier(resourceName, resourceType, packageName);
    }

    private int getValueType(int fieldId) {
        TypedValue value = new TypedValue();
        mContext.getResources().getValue(fieldId, value, true);
        return value.type;
    }

    /**
     * Open an InputStream for an Android resource.
     *
     * @param path Path of the form "resource_type/resource_name.ext".
     * @return An {@link InputStream} to the Android resource.
     */
    public @NonNull InputStream openResource(@NonNull String path)
            throws Resources.NotFoundException, IOException {
        path = removeLeadingSlash(path);
        // The path must be of the form "resource_type/resource_name.ext".
        String[] pathSegments = path.split("/", -1);
        if (pathSegments.length != 2) {
            throw new IllegalArgumentException("Incorrect resource path: " + path);
        }
        String resourceType = pathSegments[0];
        String resourceName = pathSegments[1];

        // Drop the file extension.
        int dotIndex = resourceName.lastIndexOf('.');
        if (dotIndex != -1) {
            resourceName = resourceName.substring(0, dotIndex);
        }
        int fieldId = getFieldId(resourceType, resourceName);
        int valueType = getValueType(fieldId);
        if (valueType != TypedValue.TYPE_STRING) {
            throw new IOException(
                    String.format("Expected %s resource to be of TYPE_STRING but was %d",
                            path, valueType));
        }
        return handleSvgzStream(path, mContext.getResources().openRawResource(fieldId));
    }

    /**
     * Open an InputStream for an Android asset.
     *
     * @param path Path to the asset file to load.
     * @return An {@link InputStream} to the Android asset.
     */
    public @NonNull InputStream openAsset(@NonNull String path) throws IOException {
        path = removeLeadingSlash(path);
        AssetManager assets = mContext.getAssets();
        return handleSvgzStream(path, assets.open(path, AssetManager.ACCESS_STREAMING));
    }

    /**
     * Open an {@code InputStream} for a file in application data directories.
     *
     * @param file The file to be opened.
     * @return An {@code InputStream} for the requested file.
     */
    public static @NonNull InputStream openFile(@NonNull File file) throws FileNotFoundException,
            IOException {
        FileInputStream fis = new FileInputStream(file);
        return handleSvgzStream(file.getPath(), fis);
    }

    /**
     * Resolves the given relative child string path against the given parent directory.
     * <p>
     * It resolves the given child path and creates a {@link File} object using the canonical path
     * of that file if its canonical path starts with the canonical path of the parent directory.
     *
     * @param parent {@link File} for the parent directory.
     * @param child Relative path for the child file.
     * @return {@link File} for the given child path or {@code null} if the given path doesn't
     *         resolve to be a child of the given parent.
     */
    public static @Nullable File getCanonicalFileIfChild(@NonNull File parent,
            @NonNull String child) throws IOException {
        String parentCanonicalPath = getCanonicalDirPath(parent);
        String childCanonicalPath = new File(parent, child).getCanonicalPath();
        if (childCanonicalPath.startsWith(parentCanonicalPath)) {
            return new File(childCanonicalPath);
        }
        return null;
    }

    /**
     * Returns the canonical path for the given directory with a {@code "/"} at the end if doesn't
     * have one.
     * <p>
     * Having a slash {@code "/"} at the end of a directory path is important when checking if a
     * directory is a parent of another child directory or a file.
     * E.g: the directory {@code "/some/path/to"} is not a parent of "/some/path/to_file". However,
     * it will pass the {@code parentPath.startsWith(childPath)} check.
     */
    public static @NonNull String getCanonicalDirPath(@NonNull File file) throws IOException {
        String canonicalPath = file.getCanonicalPath();
        if (!canonicalPath.endsWith("/")) canonicalPath += "/";
        return canonicalPath;
    }

    /**
     * Get the data dir for an application.
     *
     * @param context the {@link Context} used to get the data dir.
     * @return data dir {@link File} for that app.
     */
    public static @NonNull File getDataDir(@NonNull Context context) {
        return context.getDataDir();
    }

    /**
     * Use {@link MimeUtil#getMimeFromFileName} to guess MIME type or return the
     * {@link AssetHelper#DEFAULT_MIME_TYPE} if it can't guess.
     *
     * @param filePath path of the file to guess its MIME type.
     * @return MIME type guessed from file extension or {@link AssetHelper#DEFAULT_MIME_TYPE}.
     */
    public static @NonNull String guessMimeType(@NonNull String filePath) {
        String mimeType = MimeUtil.getMimeFromFileName(filePath);
        return mimeType == null ? DEFAULT_MIME_TYPE : mimeType;
    }
}
