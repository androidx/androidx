/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.compose.remote.core.testing;

import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;

/**
 * A {@link RemoteContext} with no platform behind it, for tests that exercise operations directly
 * rather than through a player.
 *
 * <p>Only the state an operation actually reads back is real - floats, colours, paths and the
 * object table. Everything else is a no-op, because the point is to run {@code paint()} without a
 * device attached.
 */
public class HeadlessRemoteContext extends RemoteContext {

    public static class BitmapBuffer {
        public final int width;
        public final int height;
        public final int @NonNull [] pixels;

        public BitmapBuffer(int width, int height) {
            this.width = width;
            this.height = height;
            this.pixels = new int[width * height];
        }
    }

    private final HashMap<Integer, Object> mObjects = new HashMap<>();
    private final HashMap<Integer, Integer> mColors = new HashMap<>();
    private final HashMap<Integer, Float> mFloats = new HashMap<>();
    private final HashMap<Integer, Integer> mIntegers = new HashMap<>();
    private final HashMap<Integer, float[]> mPaths = new HashMap<>();
    private final HashMap<Integer, String> mTexts = new HashMap<>();
    private final HashMap<Integer, BitmapBuffer> mBitmaps = new HashMap<>();
    private final java.util.ArrayList<VariableSupport> mListeners = new java.util.ArrayList<>();

    @Override
    public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {
        mPaths.put(instanceId, floatPath);
        mRemoteComposeState.putPathData(instanceId, floatPath);
    }

    @Override
    public float @Nullable [] getPathData(int instanceId) {
        float[] data = mPaths.get(instanceId);
        return data != null ? data : mRemoteComposeState.getPathData(instanceId);
    }

    @Override
    public void loadVariableName(@NonNull String varName, int varId, int varType) {}

    @Override
    public void loadColor(int id, int color) {
        mColors.put(id, color);
        mRemoteComposeState.cacheData(id, (Object) color);
    }

    @Override
    public int getColor(int id) {
        Integer color = mColors.get(id);
        if (color != null) {
            return color;
        }
        return mRemoteComposeState.getColor(id);
    }

    @Override
    public void setNamedColorOverride(@NonNull String colorName, int color) {}

    @Override
    public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {}

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
    public void addCollection(int id, @NonNull ArrayAccess collection) {
        mRemoteComposeState.addCollection(id, collection);
    }

    @Override
    public void putDataMap(int id, @NonNull DataMap map) {
        mRemoteComposeState.putDataMap(id, map);
    }

    @Override
    public @Nullable DataMap getDataMap(int id) {
        return mRemoteComposeState.getDataMap(id);
    }

    @Override
    public void runAction(int id, @NonNull String metadata) {}

    @Override
    public void runNamedAction(int textId, @Nullable Object value) {}

    @Override
    public void putObject(int key, @NonNull Object command) {
        mObjects.put(key, command);
    }

    @Override
    public @Nullable Object getObject(int key) {
        return mObjects.get(key);
    }

    @Override
    public void hapticEffect(int type) {}

    @Override
    public void loadSound(int soundId, byte @NonNull [] data) {}

    @Override
    public void playSound(int soundId) {}

    @Override
    public void loadBitmap(
            int imageId, short encoding, short type, int width, int height, byte @NonNull [] map) {
        if (width > 0 && height > 0) {
            mBitmaps.put(imageId, new BitmapBuffer(width, height));
        }
    }

    /** Retrieve a bitmap buffer registered via {@link #loadBitmap} or {@link #putBitmap}. */
    public @Nullable BitmapBuffer getBitmap(int imageId) {
        return mBitmaps.get(imageId);
    }

    /** Explicitly register an in-memory bitmap buffer for unit testing textured meshes. */
    public void putBitmap(int imageId, @NonNull BitmapBuffer bitmap) {
        mBitmaps.put(imageId, bitmap);
    }

    @Override
    public void loadText(int id, @NonNull String text) {
        mTexts.put(id, text);
        mRemoteComposeState.cacheData(id, text);
    }

    @Override
    public @Nullable String getText(int id) {
        String text = mTexts.get(id);
        if (text != null) {
            return text;
        }
        Object cached = mRemoteComposeState.getFromId(id);
        return cached instanceof String ? (String) cached : null;
    }

    @Override
    public void loadFloat(int id, float value) {
        mFloats.put(id, value);
        mRemoteComposeState.updateFloat(id, value);
    }

    @Override
    public void overrideFloat(int id, float value) {
        mFloats.put(id, value);
        mRemoteComposeState.overrideFloat(id, value);
    }

    @Override
    public void loadInteger(int id, int value) {
        mIntegers.put(id, value);
        mRemoteComposeState.updateInteger(id, value);
    }

    @Override
    public void overrideInteger(int id, int value) {
        mIntegers.put(id, value);
        mRemoteComposeState.overrideInteger(id, value);
    }

    @Override
    public void overrideText(int id, int valueId) {}

    @Override
    public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {
        mRemoteComposeState.cacheData(id, animatedFloat);
        animatedFloat.updateVariables(this);
    }

    @Override
    public void loadShader(int id, @NonNull ShaderData value) {}

    @Override
    public float getFloat(int id) {
        Float value = mFloats.get(id);
        if (value != null) {
            return value;
        }
        return mRemoteComposeState.getFloat(id);
    }

    @Override
    public int getInteger(int id) {
        Integer value = mIntegers.get(id);
        if (value != null) {
            return value;
        }
        return mRemoteComposeState.getInteger(id);
    }

    @Override
    public long getLong(int id) {
        return 0L;
    }

    @Override
    public void listensTo(int id, @NonNull VariableSupport variableSupport) {
        mRemoteComposeState.listenToVar(id, variableSupport);
        if (!mListeners.contains(variableSupport)) {
            mListeners.add(variableSupport);
        }
    }

    @Override
    public int updateOps() {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).updateVariables(this);
        }
        return mListeners.size();
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
}
