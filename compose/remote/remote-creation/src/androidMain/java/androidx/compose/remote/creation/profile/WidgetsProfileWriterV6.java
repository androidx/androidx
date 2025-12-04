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
package androidx.compose.remote.creation.profile;

import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;

import android.graphics.Color;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.CreationDisplayInfo;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** RemoteComposeWriter for Widgets in Baklava (Api level 6) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WidgetsProfileWriterV6 extends RemoteComposeWriterAndroid {

    public WidgetsProfileWriterV6(@NonNull CreationDisplayInfo creationDisplayInfo,
            @Nullable String contentDescription, @NonNull Profile profile) {
        super(creationDisplayInfo, contentDescription, profile);
        mMaxValidFloatExpressionOperation = AnimatedFloatExpression.getMaxOpForLevel(
                profile.getApiLevel());
    }

    /**
     * Creates a {@link RemoteComposeWriter} for widgets on Baklava (API level 6).
     *
     * @param creationDisplayInfo Information about the display where the content will be rendered.
     * @param contentDescription  A content description for the root composable, used for
     *                            accessibility.
     * @param profile             The profile defining the capabilities and API level of the
     *                            remote environment.
     * @return A new instance of {@link RemoteComposeWriter} configured for widgets.
     */
    public static @NonNull RemoteComposeWriter createWriter(
            @NonNull CreationDisplayInfo creationDisplayInfo,
            @Nullable String contentDescription, @NonNull Profile profile) {
        WidgetsProfileWriterV6 widgetsProfileWriterV6 = new WidgetsProfileWriterV6(
                creationDisplayInfo, contentDescription, profile);

        if (contentDescription != null) {
            int contentDescriptionId = widgetsProfileWriterV6.addText(contentDescription);
            widgetsProfileWriterV6.getBuffer().addRootContentDescription(contentDescriptionId);
        }
        widgetsProfileWriterV6.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER, RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_BOUNDS);

        return widgetsProfileWriterV6;
    }

    public WidgetsProfileWriterV6(
            int width, int height, @NonNull String contentDescription, @NonNull Profile profile) {
        super(
                width,
                height,
                contentDescription,
                profile.getApiLevel(),
                profile.getOperationsProfiles(),
                profile.getPlatform());
        mMaxValidFloatExpressionOperation =
                AnimatedFloatExpression.getMaxOpForLevel(profile.getApiLevel());
    }

    @Override
    public void setRootContentBehavior(int scroll, int alignment, int sizing, int mode) {
        mBuffer.setRootContentBehavior(scroll, alignment, sizing, mode);
    }

    /**
     * Example of intercepting an invalid call on the writer
     *
     * @param data the font data
     * @return the id of the font use in painter.setTypeface(id)
     */
    @Override
    public int addFont(byte @NonNull [] data) {
        throw new RuntimeException("Adding custom fonts is not available in V6");
    }

    /**
     * Intercepts invalid image operations.
     */
    @Override
    public void image(@NonNull RecordingModifier modifier, int imageId, int scaleType,
            float alpha) {
        if (Float.isNaN(alpha)) {
            throw new IllegalArgumentException("Invalid alpha in V6");
        }
        super.image(modifier, imageId, scaleType, alpha);
    }

    /**
     * Intercepts invalid text operations.
     */
    @Override
    public void startTextComponent(@NonNull RecordingModifier modifier, int textId, int color,
            float fontSize, int fontStyle, float fontWeight, @Nullable String fontFamily,
            int textAlign, int overflow, int maxLines) {
        if (Float.isNaN(fontSize)) {
            throw new IllegalArgumentException("Invalid fontSize in V6");
        }
        super.startTextComponent(modifier, textId, color, fontSize, fontStyle, fontWeight,
                fontFamily, textAlign, overflow, maxLines);
    }

    /**
     * Example of validating parameters on the writer Create an animated float based on a
     * reverse-Polish notation expression
     *
     * @param ops Combination
     * @return the id of the expression as a Nan float
     */
    @Override
    public @NonNull Float floatExpression(float @NonNull ... ops) {
        validateOps(ops);
        return super.floatExpression(ops);
    }

    /**
     * Example of validating parameters on the writer Add a float expression that is a computation
     * based on variables. see packAnimation
     *
     * @param value     A RPN style float operation i.e. "4, 3, ADD" outputs 7
     * @param animation Array of floats that represents animation
     * @return NaN id of the result of the calculation
     */
    @Override
    public float floatExpression(float @NonNull [] value, float @Nullable [] animation) {
        validateOps(value);
        return super.floatExpression(value, animation);
    }

    /**
     * set the Matrix relative to the path
     *
     * @param pathId   the id of the path object
     * @param fraction the position on path
     * @param vOffset  the vertical offset to position the string
     * @param flags    flags to set path 1=position only , 2 = Tangent
     */
    @Override
    public void matrixFromPath(int pathId, float fraction, float vOffset, int flags) {
        throw new RuntimeException("matrixFromPath is not available in V6");
    }

    @Override
    public int addNamedColor(@NonNull String name, int color) {
        Utils.log("addNamedColor " + name);

        if (name.equals("android.textColorPrimary")) {
            int cpId = super.addNamedColor("android.colorPrimaryDark", Color.CYAN);

            int colorA = super.addNamedColor("android.colorControlActivated", Color.WHITE);

            float val = floatExpression(Rc.Time.TIME_IN_SEC, 0, MUL, 0.2f, ADD);
            short tweenColor = addColorExpression((short) cpId, (short) colorA, val);
            //  comment line below to observe on working systems
            super.setColorName(tweenColor, "android.textColorPrimary");

            return tweenColor;
        }

        if (name.equals("android.textColorSecondary")) {
            // TODO calibrate to a better color
            int cpId = super.addNamedColor("android.colorPrimaryDark", Color.CYAN);

            int colorA = super.addNamedColor("android.colorControlActivated", Color.WHITE);

            float val = floatExpression(Rc.Time.TIME_IN_SEC, 0, MUL, 0.5f, ADD);
            short tweenColor = addColorExpression((short) cpId, (short) colorA, val);
            //  comment line below to observe on working systems
            super.setColorName(tweenColor, "android.textColorSecondary");
            return tweenColor;
        }

        return super.addNamedColor(name, color);
    }
    // todo DAY of Month. etc.

    /**
     * Add a light and dark themed color
     *
     * @param lightName  the name of the light color
     * @param lightValue the value of the light color
     * @param darkName   the name of the dark color
     * @param darkValue  the value of the dark color
     * @return the id of the color
     */
    public short addThemedColor(@Nullable String lightName, int lightValue,
            @Nullable String darkName, int darkValue) {
        int lightId = mState.createNextAvailableId();
        int darkId = mState.createNextAvailableId();
        int light_mode;
        if (Rc.System.sLightMode == 0f) {
            light_mode = mState.createNextAvailableId();
            Rc.System.sLightMode = Utils.asNan(light_mode);
        } else {
            light_mode = Utils.idFromNan(Rc.System.sLightMode);
        }

        if (lightName != null) {
            mBuffer.setNamedVariable(lightId, lightName, NamedVariable.COLOR_TYPE);
        }
        if (darkName != null) {
            mBuffer.setNamedVariable(darkId, darkName, NamedVariable.COLOR_TYPE);
        }

        int retId = mState.createNextAvailableId();
        mBuffer.addColor(lightId, lightValue);
        mBuffer.addColor(darkId, darkValue);
        mBuffer.addFloat(light_mode, 0);

        setTheme(Rc.Theme.DARK);
        startLoop(0, 0f, 1f, 1f);
        mBuffer.addColorExpression(retId, (short) darkId, (short) lightId, 0);
        endLoop();
        setTheme(Rc.Theme.LIGHT);
        startLoop(0, 0f, 1f, 1f);
        mBuffer.addColorExpression(retId, (short) darkId, (short) lightId, 1);
        endLoop();
        setTheme(Rc.Theme.UNSPECIFIED);

        return (short) retId;
    }

}
