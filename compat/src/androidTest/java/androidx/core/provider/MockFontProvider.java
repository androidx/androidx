/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.core.provider.FontsContractCompat.Columns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a test Content Provider implementing {@link FontsContractCompat}.
 */
public class MockFontProvider extends ContentProvider {
    public static final String AUTHORITY = "androidx.core.provider.fonts.font";

    static final String[] FONT_FILES = {
            "samplefont.ttf", "large_a.ttf", "large_b.ttf", "large_c.ttf", "large_d.ttf"
    };
    public static final int INVALID_FONT_FILE_ID = -1;
    private static final int SAMPLE_FONT_FILE_0_ID = 0;
    private static final int LARGE_A_FILE_ID = 1;
    private static final int LARGE_B_FILE_ID = 2;
    private static final int LARGE_C_FILE_ID = 3;
    private static final int LARGE_D_FILE_ID = 4;

    static final String SINGLE_FONT_FAMILY_QUERY = "singleFontFamily";
    static final String SINGLE_FONT_FAMILY2_QUERY = "singleFontFamily2";
    static final String NOT_FOUND_QUERY = "notFound";
    static final String UNAVAILABLE_QUERY = "unavailable";
    static final String MALFORMED_QUERY = "malformed";
    static final String NOT_FOUND_SECOND_QUERY = "notFoundSecond";
    static final String NOT_FOUND_THIRD_QUERY = "notFoundThird";
    static final String NEGATIVE_ERROR_CODE_QUERY = "negativeCode";
    static final String MANDATORY_FIELDS_ONLY_QUERY = "mandatoryFields";
    static final String STYLE_TEST_QUERY = "styleTest";
    static final String INVALID_URI = "invalidURI";

    static class Font {
        Font(int id, int fileId, int ttcIndex, String varSettings, int weight, int italic,
                int resultCode, boolean returnAllFields) {
            mId = id;
            mFileId = fileId;
            mTtcIndex = ttcIndex;
            mVarSettings = varSettings;
            mWeight = weight;
            mItalic = italic;
            mResultCode = resultCode;
            mReturnAllFields = returnAllFields;
        }

        public int getId() {
            return mId;
        }

        public int getTtcIndex() {
            return mTtcIndex;
        }

        public String getVarSettings() {
            return mVarSettings;
        }

        public int getWeight() {
            return mWeight;
        }

        public int getItalic() {
            return mItalic;
        }

        public int getResultCode() {
            return mResultCode;
        }

        public int getFileId() {
            return mFileId;
        }

        public boolean isReturnAllFields() {
            return mReturnAllFields;
        }

        private final int mId;
        private final int mFileId;
        private final int mTtcIndex;
        private final String mVarSettings;
        private final int mWeight;
        private final int mItalic;
        private final int mResultCode;
        private final boolean mReturnAllFields;
    };

    private static final Map<String, Font[]> QUERY_MAP;
    static {
        HashMap<String, Font[]> map = new HashMap<>();
        int id = 1;

        map.put(SINGLE_FONT_FAMILY_QUERY, new Font[] {
                new Font(id++, SAMPLE_FONT_FILE_0_ID, 0, "'wght' 100", 400, 0,
                        Columns.RESULT_CODE_OK, true),
        });

        map.put(SINGLE_FONT_FAMILY2_QUERY, new Font[] {
                new Font(id++, SAMPLE_FONT_FILE_0_ID, 0, "'wght' 100", 700, 1,
                        Columns.RESULT_CODE_OK, true),
        });

        map.put(NOT_FOUND_QUERY, new Font[] {
                new Font(0, 0, 0, null, 400, 0, Columns.RESULT_CODE_FONT_NOT_FOUND, true),
        });

        map.put(UNAVAILABLE_QUERY, new Font[] {
                new Font(0, 0, 0, null, 400, 0, Columns.RESULT_CODE_FONT_UNAVAILABLE, true),
        });

        map.put(MALFORMED_QUERY, new Font[] {
                new Font(0, 0, 0, null, 400, 0, Columns.RESULT_CODE_MALFORMED_QUERY, true),
        });

        map.put(NOT_FOUND_SECOND_QUERY, new Font[] {
                new Font(id++, SAMPLE_FONT_FILE_0_ID, 0, null, 700, 0, Columns.RESULT_CODE_OK,
                        true),
                new Font(0, 0, 0, null, 400, 0, Columns.RESULT_CODE_FONT_NOT_FOUND, true),
        });

        map.put(NOT_FOUND_THIRD_QUERY, new Font[] {
                new Font(id++, SAMPLE_FONT_FILE_0_ID, 0, null, 700, 0, Columns.RESULT_CODE_OK,
                        true),
                new Font(0, 0, 0, null, 400, 0, Columns.RESULT_CODE_FONT_NOT_FOUND, true),
                new Font(id++, SAMPLE_FONT_FILE_0_ID, 0, null, 700, 0, Columns.RESULT_CODE_OK,
                        true),
        });

        map.put(NEGATIVE_ERROR_CODE_QUERY, new Font[] {
                new Font(id++, SAMPLE_FONT_FILE_0_ID, 0, null, 700, 0, -5, true),
        });

        map.put(MANDATORY_FIELDS_ONLY_QUERY, new Font[] {
                new Font(id++, SAMPLE_FONT_FILE_0_ID, 0, null, 400, 0,
                        Columns.RESULT_CODE_OK, false),
        });

        map.put(STYLE_TEST_QUERY, new Font[] {
                new Font(id++, LARGE_A_FILE_ID, 0, null, 400, 0 /* normal */,
                        Columns.RESULT_CODE_OK, true),
                new Font(id++, LARGE_B_FILE_ID, 0, null, 400, 1 /* italic */,
                        Columns.RESULT_CODE_OK, true),
                new Font(id++, LARGE_C_FILE_ID, 0, null, 700, 0 /* normal */,
                        Columns.RESULT_CODE_OK, true),
                new Font(id++, LARGE_D_FILE_ID, 0, null, 700, 1 /* italic */,
                        Columns.RESULT_CODE_OK, true),
        });

        map.put(INVALID_URI, new Font[] {
                new Font(id++, INVALID_FONT_FILE_ID, 0, null, 400, 0,
                        Columns.RESULT_CODE_OK, true),
        });

        QUERY_MAP = Collections.unmodifiableMap(map);
    }

    private static Cursor buildCursor(Font[] in) {
        if (!in[0].mReturnAllFields) {
            MatrixCursor cursor = new MatrixCursor(new String[] { Columns._ID, Columns.FILE_ID });
            for (Font font : in) {
                cursor.addRow(new Object[] { font.getId(), font.getFileId() });
            }
            return cursor;
        }
        MatrixCursor cursor = new MatrixCursor(new String[] {
                Columns._ID, Columns.TTC_INDEX, Columns.VARIATION_SETTINGS, Columns.WEIGHT,
                Columns.ITALIC, Columns.RESULT_CODE, Columns.FILE_ID});
        for (Font font : in) {
            cursor.addRow(
                    new Object[] { font.getId(), font.getTtcIndex(), font.getVarSettings(),
                    font.getWeight(), font.getItalic(), font.getResultCode(), font.getFileId() });
        }
        return cursor;
    }

    public static void prepareFontFiles(Context context) {
        final AssetManager mgr = context.getAssets();
        for (String file : FONT_FILES) {
            InputStream is = null;
            try {
                is = mgr.open("fonts/" + file);
                File copied = getCopiedFile(context, file);
                File parent = copied.getParentFile();
                if (!parent.isDirectory()) {
                    parent.mkdirs();
                    parent.setReadable(true, false);
                    parent.setExecutable(true, false);
                }
                copy(is, copied);
                copied.setReadable(true, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Do nothing.
                    }
                }
            }
        }
    }

    /**
     * The caller is responsible for closing the given InputStream.
     */
    private static void copy(InputStream is, File file) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, false);
            byte[] buffer = new byte[1024];
            int readLen;
            while ((readLen = is.read(buffer)) != -1) {
                fos.write(buffer, 0, readLen);
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void cleanUpFontFiles(Context context) {
        for (String file : FONT_FILES) {
            getCopiedFile(context, file).delete();
        }
    }

    public static File getCopiedFile(Context context, String path) {
        final File cacheDir = new File(context.getFilesDir(), "fontCache");
        return new File(cacheDir, path);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        final int id = (int) ContentUris.parseId(uri);
        if (id < 0) {
            return null;
        }
        final File targetFile = getCopiedFile(getContext(), FONT_FILES[id]);
        try {
            return ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(
                    "Failed to found font file. You might forget call prepareFontFiles in setUp");
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.android.provider.font";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return buildCursor(QUERY_MAP.get(selectionArgs[0]));
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("insert is not supported.");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("delete is not supported.");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("update is not supported.");
    }
}
