/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.player.view.platform;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.player.core.RemoteComposeDocument;
import androidx.compose.remote.player.view.RemoteComposePlayer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * remote compose Document after being prepared
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemotePreparedDocument implements RemoteComposePlayer.PreparedDocument {
    private final RemoteComposeDocument mOriginalDoc;
    private final HashMap<Integer, Object> mResolvedData = new HashMap<>();
    private final RemoteContext mContext = new RemoteContext() {
        @Override
        public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {
        }

        @Override
        public float @Nullable [] getPathData(int instanceId) {
            return new float[0];
        }

        @Override
        public void loadVariableName(@NonNull String varName, int varId, int varType) {
        }

        @Override
        public void loadColor(int id, int color) {
        }

        @Override
        public void setNamedColorOverride(@NonNull String colorName, int color) {
        }

        @Override
        public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {
        }

        @Override
        public void clearNamedStringOverride(@NonNull String stringName) {
        }

        @Override
        public void setNamedBooleanOverride(@NonNull String booleanName, boolean value) {
        }

        @Override
        public void clearNamedBooleanOverride(@NonNull String booleanName) {
        }

        @Override
        public void setNamedIntegerOverride(@NonNull String integerName, int value) {
        }

        @Override
        public void clearNamedIntegerOverride(@NonNull String integerName) {
        }

        @Override
        public void setNamedFloatOverride(@NonNull String floatName, float value) {

        }

        @Override
        public void clearNamedFloatOverride(@NonNull String floatName) {

        }

        @Override
        public void setNamedLong(@NonNull String name, long value) {
        }

        @Override
        public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {
        }

        @Override
        public void clearNamedDataOverride(@NonNull String dataName) {
        }

        @Override
        public void addCollection(int id, @NonNull ArrayAccess collection) {
        }

        @Override
        public void putDataMap(int id, @NonNull DataMap map) {

        }

        @Override
        public @Nullable DataMap getDataMap(int id) {
            return null;
        }

        @Override
        public void runAction(int id, @NonNull String metadata) {
        }

        @Override
        public void runNamedAction(int id, @Nullable Object value) {
        }

        @Override
        public void putObject(int id, @NonNull Object value) {
        }

        @Override
        public @Nullable Object getObject(int id) {
            return null;
        }

        @Override
        public void hapticEffect(int type) {
        }

        @Override
        public void loadBitmap(int imageId,
                               short encoding,
                               short type,
                               int width,
                               int height,
                               byte @NonNull [] data) {
            Bitmap image = null;
            switch (encoding) {
                case BitmapData.ENCODING_INLINE:
                    switch (type) {
                        case BitmapData.TYPE_PNG_8888:

                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inJustDecodeBounds = true; // <-- do a bounds-only pass
                            BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                            if (opts.outWidth > width || opts.outHeight > height) {
                                throw new RuntimeException(
                                        "dimension don't match "
                                                + opts.outWidth
                                                + "x"
                                                + opts.outHeight
                                                + " vs "
                                                + width
                                                + "x"
                                                + height);
                            }

                            image = BitmapFactory.decodeByteArray(data, 0, data.length);
                            break;
                        case BitmapData.TYPE_PNG_ALPHA_8:
                            image = decodePreferringAlpha8(data);

                            // If needed convert to ALPHA_8.
                            if (!image.getConfig().equals(Bitmap.Config.ALPHA_8)) {
                                Bitmap alpha8Bitmap =
                                        Bitmap.createBitmap(
                                                image.getWidth(),
                                                image.getHeight(),
                                                Bitmap.Config.ALPHA_8);
                                Canvas canvas = new Canvas(alpha8Bitmap);
                                Paint paint = new Paint();
                                paint.setXfermode(
                                        new android.graphics.PorterDuffXfermode(
                                                android.graphics.PorterDuff.Mode.SRC));
                                canvas.drawBitmap(image, 0, 0, paint);
                                image.recycle(); // Release resources

                                image = alpha8Bitmap;
                            }
                            break;
                        case BitmapData.TYPE_RAW8888:
                            image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            int[] idata = new int[data.length / 4];
                            for (int i = 0; i < idata.length; i++) {
                                int p = i * 4;
                                idata[i] =
                                        (data[p] << 24)
                                                | (data[p + 1] << 16)
                                                | (data[p + 2] << 8)
                                                | data[p + 3];
                            }
                            image.setPixels(idata, 0, width, 0, 0, width, height);
                            break;
                        case BitmapData.TYPE_RAW8:
                            image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            int[] bdata = new int[data.length / 4];
                            for (int i = 0; i < bdata.length; i++) {

                                bdata[i] = 0x1010101 * data[i];
                            }
                            image.setPixels(bdata, 0, width, 0, 0, width, height);
                            break;
                    }
                    break;
                case BitmapData.ENCODING_FILE:
                    image = BitmapFactory.decodeFile(new String(data));
                    break;
                case BitmapData.ENCODING_URL:
                    try {
                        image = BitmapFactory.decodeStream(new URL(new String(data)).openStream());
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case BitmapData.ENCODING_EMPTY:
                    image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
            mResolvedData.put(imageId, image);
        }

        private Bitmap decodePreferringAlpha8(byte @NonNull [] data) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ALPHA_8;
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        }

        @Override
        public void loadText(int id, @NonNull String text) {
        }

        @Override
        public @Nullable String getText(int id) {
            return "";
        }

        @Override
        public void loadFloat(int id, float value) {
        }

        @Override
        public void overrideFloat(int id, float value) {
        }

        @Override
        public void loadInteger(int id, int value) {
        }

        @Override
        public void overrideInteger(int id, int value) {
        }

        @Override
        public void overrideText(int id, int valueId) {
        }

        @Override
        public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {
        }

        @Override
        public void loadShader(int id, @NonNull ShaderData value) {
        }

        @Override
        public float getFloat(int id) {
            return 0;
        }

        @Override
        public int getInteger(int id) {
            return 0;
        }

        @Override
        public long getLong(int id) {
            return 0;
        }

        @Override
        public int getColor(int id) {
            return 0;
        }

        @Override
        public void listensTo(int id, @NonNull VariableSupport variableSupport) {

        }

        @Override
        public int updateOps() {
            return 0;
        }

        @Override
        public @Nullable ShaderData getShader(int id) {
            return null;
        }

        @Override
        public void addClickArea(int id, int contentDescriptionId,
                                 float left,
                                 float top,
                                 float right,
                                 float bottom,
                                 int metadataId) {
        }
    };

    public RemotePreparedDocument(@NonNull RemoteComposeDocument doc) {
        mOriginalDoc = doc;
        BitmapData[] data = doc.getDocument().getBitmapDataSet();
        for (BitmapData d : data) {
            d.apply(mContext);
        }
    }

    @Override
    public @NonNull RemoteComposeDocument getOriginalDoc() {
        return mOriginalDoc;
    }


    public @NonNull Map<Integer, Object> getResolvedData() {
        return mResolvedData;
    }
}
