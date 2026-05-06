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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.compose.remote.player.core.platform.BitmapLoader;
import androidx.compose.remote.player.core.platform.RemoteBitmapDecoder;
import androidx.compose.remote.player.view.RemoteComposePlayer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** remote compose Document after being prepared */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemotePreparedDocument implements RemoteComposePlayer.PreparedDocument {
    private final RemoteDocument mOriginalDoc;
    private final BitmapLoader mBitmapLoader;
    private final HashMap<Integer, Object> mResolvedData = new HashMap<>();
    private final RemoteContext mContext =
            new RemoteContext() {
                @Override
                public void loadPathData(
                        int instanceId, int winding, float @NonNull [] floatPath) {}

                @Override
                public float @Nullable [] getPathData(int instanceId) {
                    return new float[0];
                }

                @Override
                public void loadVariableName(@NonNull String varName, int varId, int varType) {}

                @Override
                public void loadColor(int id, int color) {}

                @Override
                public void setNamedColorOverride(@NonNull String colorName, int color) {}

                @Override
                public void setNamedStringOverride(
                        @NonNull String stringName, @NonNull String value) {}

                @Override
                public void clearNamedStringOverride(@NonNull String stringName) {}

                @Override
                public void setNamedBooleanOverride(@NonNull String booleanName, boolean value) {}

                @Override
                public void clearNamedBooleanOverride(@NonNull String booleanName) {}

                @Override
                public void setNamedIntegerOverride(@NonNull String integerName, int value) {}

                @Override
                public void clearNamedIntegerOverride(@NonNull String integerName) {}

                @Override
                public void setNamedFloatOverride(@NonNull String floatName, float value) {}

                @Override
                public void clearNamedFloatOverride(@NonNull String floatName) {}

                @Override
                public void setNamedLong(@NonNull String name, long value) {}

                @Override
                public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {}

                @Override
                public void clearNamedDataOverride(@NonNull String dataName) {}

                @Override
                public void addCollection(int id, @NonNull ArrayAccess collection) {}

                @Override
                public void putDataMap(int id, @NonNull DataMap map) {}

                @Override
                public @Nullable DataMap getDataMap(int id) {
                    return null;
                }

                @Override
                public void runAction(int id, @NonNull String metadata) {}

                @Override
                public void runNamedAction(int id, @Nullable Object value) {}

                @Override
                public void putObject(int id, @NonNull Object value) {}

                @Override
                public @Nullable Object getObject(int id) {
                    return null;
                }

                @Override
                public void hapticEffect(int type) {}

                @Override
                public void loadBitmap(
                        int imageId,
                        short encoding,
                        short type,
                        int width,
                        int height,
                        byte @NonNull [] data) {
                    Bitmap image =
                            RemoteBitmapDecoder.decodeBitmap(
                                    imageId, encoding, type, width, height, data, mBitmapLoader);
                    mResolvedData.put(imageId, image);
                }

                @Override
                public void loadText(int id, @NonNull String text) {}

                @Override
                public @Nullable String getText(int id) {
                    return "";
                }

                @Override
                public void loadFloat(int id, float value) {}

                @Override
                public void overrideFloat(int id, float value) {}

                @Override
                public void loadInteger(int id, int value) {}

                @Override
                public void overrideInteger(int id, int value) {}

                @Override
                public void overrideText(int id, int valueId) {}

                @Override
                public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {}

                @Override
                public void loadShader(int id, @NonNull ShaderData value) {}

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
                public void listensTo(int id, @NonNull VariableSupport variableSupport) {}

                @Override
                public int updateOps() {
                    return 0;
                }

                @Override
                public @Nullable ShaderData getShader(int id) {
                    return null;
                }

                @Override
                public void addClickArea(
                        int id,
                        int contentDescriptionId,
                        float left,
                        float top,
                        float right,
                        float bottom,
                        int metadataId) {}
            };

    public RemotePreparedDocument(@NonNull RemoteDocument doc, @NonNull BitmapLoader bitmapLoader) {
        mOriginalDoc = doc;
        mBitmapLoader = bitmapLoader;
        BitmapData[] data = doc.getDocument().getBitmapDataSet();
        for (BitmapData d : data) {
            d.apply(mContext);
        }
    }

    @Override
    public @NonNull RemoteDocument getOriginalDoc() {
        return mOriginalDoc;
    }

    public @NonNull Map<Integer, Object> getResolvedData() {
        return mResolvedData;
    }
}
