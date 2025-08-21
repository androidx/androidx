/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.RestrictTo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SystemClock;
import androidx.compose.remote.core.TouchListener;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.core.types.LongConstant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * An implementation of Context for Android.
 *
 * <p>This is used to play the RemoteCompose operations on Android.
 */
@RestrictTo(LIBRARY_GROUP)
public class AndroidRemoteContext extends RemoteContext {
    private static final boolean CHECK_DATA_SIZE = true;

    private boolean mA11yAnimationEnabled = true;

    /** Default constructor, uses a {@link SystemClock} as the clock. */
    public AndroidRemoteContext() {
        this(new SystemClock());
    }

    /**
     * Context for the Android Implementation.
     *
     * @param clock The clock used for tracking time.
     */
    public AndroidRemoteContext(Clock clock) {
        super(clock);
    }

    /**
     * Sets the Canvas to be used by the RemoteContext for drawing operations. Typically received in
     * onDraw. If a PaintContext already exists, it will be reset and updated with the new Canvas.
     * Otherwise, a new AndroidPaintContext will be created. The width and height of the context are
     * also updated based on the new Canvas.
     *
     * @param canvas The Android Canvas to be used for drawing.
     */
    public void useCanvas(Canvas canvas) {
        if (mPaintContext == null) {
            mPaintContext = new AndroidPaintContext(this, canvas);
        } else {
            // need to make sure to update the canvas for the current one
            mPaintContext.reset();
            ((AndroidPaintContext) mPaintContext).setCanvas(canvas);
        }
        mWidth = canvas.getWidth();
        mHeight = canvas.getHeight();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Data handling
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void loadPathData(int instanceId, float @NonNull [] floatPath) {
        mRemoteComposeState.putPathData(instanceId, floatPath);
    }

    @Override
    public float[] getPathData(int instanceId) {
        return mRemoteComposeState.getPathData(instanceId);
    }

    static class VarName {
        String mName;
        int mId;
        int mType;

        VarName(String name, int id, int type) {
            mName = name;
            mId = id;
            mType = type;
        }
    }

    HashMap<String, VarName> mVarNameHashMap = new HashMap<>();

    @Override
    public void loadVariableName(@NonNull String varName, int varId, int varType) {
        mVarNameHashMap.put(varName, new VarName(varName, varId, varType));
    }

    @Override
    public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {
        if (mVarNameHashMap.get(stringName) != null) {
            int id = mVarNameHashMap.get(stringName).mId;
            overrideText(id, value);
        }
    }

    @Override
    public void clearNamedStringOverride(@NonNull String stringName) {
        if (mVarNameHashMap.get(stringName) != null) {
            int id = mVarNameHashMap.get(stringName).mId;
            clearDataOverride(id);
        }
        mVarNameHashMap.put(stringName, null);
    }

    @Override
    public void setNamedIntegerOverride(@NonNull String integerName, int value) {
        if (mVarNameHashMap.get(integerName) != null) {
            int id = mVarNameHashMap.get(integerName).mId;
            overrideInt(id, value);
        }
    }

    @Override
    public void clearNamedIntegerOverride(@NonNull String integerName) {
        if (mVarNameHashMap.get(integerName) != null) {
            int id = mVarNameHashMap.get(integerName).mId;
            clearIntegerOverride(id);
        }
        mVarNameHashMap.put(integerName, null);
    }

    @Override
    public void setNamedFloatOverride(@NonNull String floatName, float value) {
        if (mVarNameHashMap.get(floatName) != null) {
            int id = mVarNameHashMap.get(floatName).mId;
            overrideFloat(id, value);
        }
    }

    @Override
    public void clearNamedFloatOverride(@NonNull String floatName) {
        if (mVarNameHashMap.get(floatName) != null) {
            int id = mVarNameHashMap.get(floatName).mId;
            clearFloatOverride(id);
        }
        mVarNameHashMap.put(floatName, null);
    }

    @Override
    public void setNamedLong(@NonNull String name, long value) {
        VarName entry = mVarNameHashMap.get(name);
        if (entry != null) {
            int id = entry.mId;
            LongConstant longConstant = (LongConstant) mRemoteComposeState.getObject(id);
            longConstant.setValue(value);
        }
    }

    @Override
    public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {
        if (mVarNameHashMap.get(dataName) != null) {
            int id = mVarNameHashMap.get(dataName).mId;
            overrideData(id, value);
        }
    }

    @Override
    public void clearNamedDataOverride(@NonNull String dataName) {
        if (mVarNameHashMap.get(dataName) != null) {
            int id = mVarNameHashMap.get(dataName).mId;
            clearDataOverride(id);
        }
        mVarNameHashMap.put(dataName, null);
    }

    /**
     * Override a color to force it to be the color provided
     *
     * @param colorName name of color
     * @param color
     */
    public void setNamedColorOverride(@NonNull String colorName, int color) {
        if (mVarNameHashMap.get(colorName) != null) {
            int id = mVarNameHashMap.get(colorName).mId;
            mRemoteComposeState.overrideColor(id, color);
        }
    }

    @Override
    public void addCollection(int id, @NonNull ArrayAccess collection) {
        mRemoteComposeState.addCollection(id, collection);
    }

    @Override
    public void putDataMap(int id, @NonNull DataMap map) {
        mRemoteComposeState.putDataMap(id, map);
    }

    @Override
    public DataMap getDataMap(int id) {
        return mRemoteComposeState.getDataMap(id);
    }

    @Override
    public void runAction(int id, @NonNull String metadata) {
        mDocument.performClick(this, id, metadata);
    }

    @Override
    public void runNamedAction(int id, @Nullable Object value) {
        String text = getText(id);
        if (text != null) {
            mDocument.runNamedAction(text, value);
        }
    }

    /**
     * Decode a byte array into an image and cache it using the given imageId
     *
     * @param imageId the id of the image
     * @param encoding how the data is encoded 0 = png, 1 = raw, 2 = url
     * @param type the type of the data 0 = RGBA 8888, 1 = 888, 2 = 8 gray
     * @param width with of image to be loaded largest dimension is 32767
     * @param height height of image to be loaded
     * @param data a byte array containing the image information
     */
    @Override
    public void loadBitmap(
            int imageId, short encoding, short type, int width, int height, byte @NonNull [] data) {
        if (!mRemoteComposeState.containsId(imageId)) {
            Bitmap image = null;
            switch (encoding) {
                case BitmapData.ENCODING_INLINE:
                    switch (type) {
                        case BitmapData.TYPE_PNG_8888:
                            if (CHECK_DATA_SIZE) {
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
            mRemoteComposeState.cacheData(imageId, image);
        }
    }

    private Bitmap decodePreferringAlpha8(byte @NonNull [] data) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ALPHA_8;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    @Override
    public void loadText(int id, @NonNull String text) {
        if (!mRemoteComposeState.containsId(id)) {
            mRemoteComposeState.cacheData(id, text);
        } else {
            mRemoteComposeState.updateData(id, text);
        }
    }

    /**
     * Overrides the text associated with a given ID.
     *
     * @param id The ID of the text to override.
     * @param text The new text value.
     */
    public void overrideText(int id, String text) {
        mRemoteComposeState.overrideData(id, text);
    }

    /**
     * Overrides the integer value associated with a given ID.
     *
     * @param id The ID of the integer to override.
     * @param value The new integer value.
     */
    public void overrideInt(int id, int value) {
        mRemoteComposeState.overrideInteger(id, value);
    }

    /**
     * Overrides the data associated with a given ID.
     *
     * @param id The ID of the data to override.
     * @param value The new data value.
     */
    public void overrideData(int id, Object value) {
        mRemoteComposeState.overrideData(id, value);
    }

    /**
     * Clears any data override for the given ID.
     *
     * @param id The ID for which to clear the data override.
     */
    public void clearDataOverride(int id) {
        mRemoteComposeState.clearDataOverride(id);
    }

    /**
     * Clears any integer override for the given ID.
     *
     * @param id The ID for which to clear the integer override.
     */
    public void clearIntegerOverride(int id) {
        mRemoteComposeState.clearIntegerOverride(id);
    }

    /**
     * Clears any float override for the given ID.
     *
     * @param id The ID for which to clear the float override.
     */
    public void clearFloatOverride(int id) {
        mRemoteComposeState.clearFloatOverride(id);
    }

    @Override
    public String getText(int id) {
        return (String) mRemoteComposeState.getFromId(id);
    }

    @Override
    public void loadFloat(int id, float value) {
        mRemoteComposeState.updateFloat(id, value);
    }

    @Override
    public void overrideFloat(int id, float value) {
        mRemoteComposeState.overrideFloat(id, value);
    }

    @Override
    public void loadInteger(int id, int value) {
        mRemoteComposeState.updateInteger(id, value);
    }

    /**
     * Overrides the integer value associated with a given ID.
     *
     * @param id The ID of the integer to override.
     * @param value The new integer value.
     */
    public void overrideInteger(int id, int value) {
        mRemoteComposeState.overrideInteger(id, value);
    }

    /**
     * Overrides the text associated with a given ID, using a text value from another ID.
     *
     * @param id The ID of the text to override.
     * @param valueId The ID of the text value to use for the override.
     */
    public void overrideText(int id, int valueId) {
        String text = getText(valueId);
        overrideText(id, text);
    }

    @Override
    public void loadColor(int id, int color) {
        mRemoteComposeState.updateColor(id, color);
    }

    @Override
    public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {
        mRemoteComposeState.cacheData(id, animatedFloat);
    }

    @Override
    public void loadShader(int id, @NonNull ShaderData value) {
        mRemoteComposeState.cacheData(id, value);
    }

    @Override
    public float getFloat(int id) {
        return (float) mRemoteComposeState.getFloat(id);
    }

    @Override
    public void putObject(int id, @NonNull Object value) {
        mRemoteComposeState.updateObject(id, value);
    }

    @Override
    public Object getObject(int id) {
        return mRemoteComposeState.getObject(id);
    }

    @Override
    public int getInteger(int id) {
        return mRemoteComposeState.getInteger(id);
    }

    @Override
    public long getLong(int id) {
        return ((LongConstant) mRemoteComposeState.getObject(id)).getValue();
    }

    @Override
    public int getColor(int id) {
        return mRemoteComposeState.getColor(id);
    }

    @Override
    public void listensTo(int id, @NonNull VariableSupport variableSupport) {
        mRemoteComposeState.listenToVar(id, variableSupport);
    }

    @Override
    public @Nullable ArrayList<VariableSupport> getListeners(int id) {
        return mRemoteComposeState.getListeners(id);
    }

    @Override
    public int updateOps() {
        return mRemoteComposeState.getOpsToUpdate(this, currentTime);
    }

    @Override
    @Nullable
    public ShaderData getShader(int id) {
        return (ShaderData) mRemoteComposeState.getFromId(id);
    }

    @Override
    public void addTouchListener(@NonNull TouchListener touchExpression) {
        mDocument.addTouchListener(touchExpression);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Click handling
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void addClickArea(
            int id,
            int contentDescriptionId,
            float left,
            float top,
            float right,
            float bottom,
            int metadataId) {
        String contentDescription = (String) mRemoteComposeState.getFromId(contentDescriptionId);
        String metadata = (String) mRemoteComposeState.getFromId(metadataId);
        mDocument.addClickArea(id, contentDescription, left, top, right, bottom, metadata);
    }

    /**
     * Vibrate the device
     *
     * @param type 0 = none, 1-21 ,see HapticFeedbackConstants
     */
    public void hapticEffect(int type) {
        mDocument.haptic(type);
    }

    /**
     * Enable or disable animations for accessibility.
     *
     * @param animationEnabled true to enable animations, false to disable them.
     */
    public void setAccessibilityAnimationEnabled(boolean animationEnabled) {
        this.mA11yAnimationEnabled = animationEnabled;
    }

    @Override
    public boolean isAnimationEnabled() {
        if (mA11yAnimationEnabled) {
            return super.isAnimationEnabled();
        } else {
            return false;
        }
    }
}
