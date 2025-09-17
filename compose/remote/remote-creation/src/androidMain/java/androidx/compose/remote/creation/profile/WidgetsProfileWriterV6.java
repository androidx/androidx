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
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** RemoteComposeWriter for Widgets in Baklava (Api level 6) */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WidgetsProfileWriterV6 extends RemoteComposeWriterAndroid {

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

}
