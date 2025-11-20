/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core;

import static androidx.compose.remote.core.CurveTestUtils.textDraw;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.CUBIC_ACCELERATE;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.CUBIC_ANTICIPATE;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.CUBIC_CUSTOM;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.CUBIC_DECELERATE;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.CUBIC_LINEAR;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.CUBIC_OVERSHOOT;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.CUBIC_STANDARD;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.EASE_OUT_BOUNCE;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.EASE_OUT_ELASTIC;
import static androidx.compose.remote.core.operations.utilities.easing.Easing.SPLINE_CUSTOM;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

/** Test supported Easing Curves */
public class FloatAnimationTest {
    @NonNull
    private String draw(@NonNull FloatAnimation fa, float duration) {
        float[] pos = new float[100];
        float[] val = new float[100];

        for (int i = 0; i < pos.length; i++) {
            pos[i] = i * duration / (pos.length - 1);
            val[i] = fa.get(pos[i]);
        }
        return textDraw(80, 30, pos, val, true);
    }

    private String drawWrap(FloatAnimation fa, float duration) {

        float[] pos = new float[100];
        float[] val = new float[100];

        for (int i = 0; i < pos.length; i++) {
            pos[i] = i * duration / (pos.length - 1);
            val[i] = fa.get(pos[i]);
        }
        return textDraw(60, 30, pos, val, true);
    }

    /**
     * Base test function
     *
     * @param type
     * @param duration
     * @param spec
     * @param initial
     * @param wrap
     * @param exp
     */
    public void baseTest(
            int type, float duration, float[] spec, float initial, float wrap, String exp) {
        FloatAnimation floatAnimation = new FloatAnimation(type, duration, spec, initial, wrap);
        if (!Float.isNaN(initial)) {
            float iv = floatAnimation.getInitialValue();
            assertEquals(initial, iv, 0.001f);
        } else {
            floatAnimation.setInitialValue(0);
        }
        floatAnimation.setTargetValue(1);
        float d = floatAnimation.getDuration();
        assertEquals(duration, d, 0.000001f);

        String str = draw(floatAnimation, duration);
        assertEquals("did not match ", exp, str);
    }

    /**
     * wrap test
     *
     * @param type
     * @param duration
     * @param spec
     * @param start
     * @param end
     * @param wrap
     * @param exp
     */
    public void wrapTest(
            int type,
            float duration,
            float[] spec,
            float start,
            float end,
            float wrap,
            String exp) {
        FloatAnimation floatAnimation = new FloatAnimation(type, duration, spec, Float.NaN, wrap);

        floatAnimation.setInitialValue(start);

        floatAnimation.setTargetValue(end);
        float d = floatAnimation.getDuration();
        assertEquals(duration, d, 0.000001f);

        String str = drawWrap(floatAnimation, duration);
        assertEquals("did not match ", exp, str);
    }

    /**
     * Base test function
     *
     * @param type
     * @param start
     * @param end
     * @param wrap
     * @param exp
     */
    public void wrapTest(int type, float start, float end, float wrap, String exp) {
        wrapTest(type, 1.0f, null, start, end, wrap, exp);
    }

    /**
     * Base test function
     *
     * @param type
     * @param spec
     * @param exp
     */
    public void baseTest(int type, float[] spec, String exp) {
        baseTest(type, 1.0f, spec, Float.NaN, Float.NaN, exp);
    }

    /**
     * Base test function
     *
     * @param type
     * @param duration
     * @param spec
     * @param exp
     */
    public void baseTest(int type, float duration, float[] spec, String exp) {
        baseTest(type, duration, spec, Float.NaN, Float.NaN, exp);
    }

    /**
     * Base test function
     *
     * @param type
     * @param exp
     */
    public void baseTest(int type, String exp) {
        baseTest(type, 1.0f, null, Float.NaN, Float.NaN, exp);
    }

    @Test
    public void testWrapCubicStandard() {
        String exp;
        exp =
                "|                                                           *| 359.0\n"
                        + "|                                             ************** |\n"
                        + "|                                        ******              |\n"
                        + "|                                     ***                    |\n"
                        + "|                                  ***                       |\n"
                        + "|                                **                          | 297.276\n"
                        + "|                              ***                           |\n"
                        + "|                             *                              |\n"
                        + "|                           **                               |\n"
                        + "|                          *                                 |\n"
                        + "|                         *                                  | 235.552\n"
                        + "|                       **                                   |\n"
                        + "|                       *                                    |\n"
                        + "|                      *                                     |\n"
                        + "|                     *                                      |\n"
                        + "|                    *                                       | 173.828\n"
                        + "|                   *                                        |\n"
                        + "|                   *                                        |\n"
                        + "|                  *                                         |\n"
                        + "|                 *                                          |\n"
                        + "|                *                                           | 112.103\n"
                        + "|                *                                           |\n"
                        + "|              **                                            |\n"
                        + "|              *                                             |\n"
                        + "|             *                                              |\n"
                        + "|            *                                               | 50.379\n"
                        + "|          **                                                |\n"
                        + "|         **                                                 |\n"
                        + "|       **                                                   |\n"
                        + "|*******                                                     | 1.0\n"
                        + "0.0                                                        1.0\n";
        wrapTest(CUBIC_STANDARD, 1, -1, 360, exp);
        exp =
                "|                                                           *| 359.0\n"
                        + "|                                             ************** |\n"
                        + "|                                        ******              |\n"
                        + "|                                     ***                    |\n"
                        + "|                                  ***                       |\n"
                        + "|                                **                          | 297.276\n"
                        + "|                              ***                           |\n"
                        + "|                             *                              |\n"
                        + "|                           **                               |\n"
                        + "|                          *                                 |\n"
                        + "|                         *                                  | 235.552\n"
                        + "|                       **                                   |\n"
                        + "|                       *                                    |\n"
                        + "|                      *                                     |\n"
                        + "|                     *                                      |\n"
                        + "|                    *                                       | 173.828\n"
                        + "|                   *                                        |\n"
                        + "|                   *                                        |\n"
                        + "|                  *                                         |\n"
                        + "|                 *                                          |\n"
                        + "|                *                                           | 112.103\n"
                        + "|                *                                           |\n"
                        + "|              **                                            |\n"
                        + "|              *                                             |\n"
                        + "|             *                                              |\n"
                        + "|            *                                               | 50.379\n"
                        + "|          **                                                |\n"
                        + "|         **                                                 |\n"
                        + "|       **                                                   |\n"
                        + "|*******                                                     | 1.0\n"
                        + "0.0                                                        1.0\n";
        wrapTest(CUBIC_STANDARD, 1, 359, 360, exp);
        exp =
                "|                                                           *| 361.0\n"
                        + "|                                             ************** |\n"
                        + "|                                        ******              |\n"
                        + "|                                     ***                    |\n"
                        + "|                                  ***                       |\n"
                        + "|                                **                          | 360.655\n"
                        + "|                              ***                           |\n"
                        + "|                             *                              |\n"
                        + "|                           **                               |\n"
                        + "|                          *                                 |\n"
                        + "|                         *                                  | 360.31\n"
                        + "|                       **                                   |\n"
                        + "|                       *                                    |\n"
                        + "|                      *                                     |\n"
                        + "|                     *                                      |\n"
                        + "|                    *                                       | 359.966\n"
                        + "|                   *                                        |\n"
                        + "|                   *                                        |\n"
                        + "|                  *                                         |\n"
                        + "|                 *                                          |\n"
                        + "|                *                                           | 359.621\n"
                        + "|                *                                           |\n"
                        + "|              **                                            |\n"
                        + "|              *                                             |\n"
                        + "|             *                                              |\n"
                        + "|            *                                               | 359.276\n"
                        + "|          **                                                |\n"
                        + "|         **                                                 |\n"
                        + "|       **                                                   |\n"
                        + "|*******                                                     | 359.0\n"
                        + "0.0                                                        1.0\n";
        wrapTest(CUBIC_STANDARD, 359, 1, 360, exp);
        exp =
                "|                                                           *| 359.0\n"
                        + "|                                             ************** |\n"
                        + "|                                        ******              |\n"
                        + "|                                     ***                    |\n"
                        + "|                                  ***                       |\n"
                        + "|                                **                          | 297.276\n"
                        + "|                              ***                           |\n"
                        + "|                             *                              |\n"
                        + "|                           **                               |\n"
                        + "|                          *                                 |\n"
                        + "|                         *                                  | 235.552\n"
                        + "|                       **                                   |\n"
                        + "|                       *                                    |\n"
                        + "|                      *                                     |\n"
                        + "|                     *                                      |\n"
                        + "|                    *                                       | 173.828\n"
                        + "|                   *                                        |\n"
                        + "|                   *                                        |\n"
                        + "|                  *                                         |\n"
                        + "|                 *                                          |\n"
                        + "|                *                                           | 112.103\n"
                        + "|                *                                           |\n"
                        + "|              **                                            |\n"
                        + "|              *                                             |\n"
                        + "|             *                                              |\n"
                        + "|            *                                               | 50.379\n"
                        + "|          **                                                |\n"
                        + "|         **                                                 |\n"
                        + "|       **                                                   |\n"
                        + "|*******                                                     | 1.0\n"
                        + "0.0                                                        1.0\n";
        wrapTest(CUBIC_STANDARD, -359, 359, 360, exp);
    }

    @Test
    public void testCubicStandard() {
        String exp =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|                                                            "
                    + " ****************** |\n"
                    + "|                                                      *******              "
                    + "     |\n"
                    + "|                                                  ****                     "
                    + "     |\n"
                    + "|                                              ****                         "
                    + "     |\n"
                    + "|                                           ***                             "
                    + "     | 0.828\n"
                    + "|                                        ****                               "
                    + "     |\n"
                    + "|                                       *                                   "
                    + "     |\n"
                    + "|                                    ***                                    "
                    + "     |\n"
                    + "|                                   *                                       "
                    + "     |\n"
                    + "|                                 **                                        "
                    + "     | 0.655\n"
                    + "|                               **                                          "
                    + "     |\n"
                    + "|                               *                                           "
                    + "     |\n"
                    + "|                             **                                            "
                    + "     |\n"
                    + "|                            *                                              "
                    + "     |\n"
                    + "|                           *                                               "
                    + "     | 0.483\n"
                    + "|                          *                                                "
                    + "     |\n"
                    + "|                         *                                                 "
                    + "     |\n"
                    + "|                        *                                                  "
                    + "     |\n"
                    + "|                       *                                                   "
                    + "     |\n"
                    + "|                      *                                                    "
                    + "     | 0.31\n"
                    + "|                     *                                                     "
                    + "     |\n"
                    + "|                   **                                                      "
                    + "     |\n"
                    + "|                   *                                                       "
                    + "     |\n"
                    + "|                 **                                                        "
                    + "     |\n"
                    + "|                *                                                          "
                    + "     | 0.138\n"
                    + "|              **                                                           "
                    + "     |\n"
                    + "|            **                                                             "
                    + "     |\n"
                    + "|         ***                                                               "
                    + "     |\n"
                    + "|*********                                                                  "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        baseTest(CUBIC_STANDARD, exp);
    }

    @Test
    public void testCubicAccelerate() {
        String exp =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|                                                                           "
                    + "  ** |\n"
                    + "|                                                                          "
                    + " **   |\n"
                    + "|                                                                         "
                    + " **    |\n"
                    + "|                                                                       *** "
                    + "     |\n"
                    + "|                                                                      **   "
                    + "     | 0.828\n"
                    + "|                                                                    **     "
                    + "     |\n"
                    + "|                                                                   *       "
                    + "     |\n"
                    + "|                                                                ***        "
                    + "     |\n"
                    + "|                                                               *           "
                    + "     |\n"
                    + "|                                                            ***            "
                    + "     | 0.655\n"
                    + "|                                                           *               "
                    + "     |\n"
                    + "|                                                        ***                "
                    + "     |\n"
                    + "|                                                       *                   "
                    + "     |\n"
                    + "|                                                    ***                    "
                    + "     |\n"
                    + "|                                                   *                       "
                    + "     | 0.483\n"
                    + "|                                                ***                        "
                    + "     |\n"
                    + "|                                              **                           "
                    + "     |\n"
                    + "|                                           ***                             "
                    + "     |\n"
                    + "|                                         ***                               "
                    + "     |\n"
                    + "|                                       **                                  "
                    + "     | 0.31\n"
                    + "|                                    ***                                    "
                    + "     |\n"
                    + "|                                 ***                                       "
                    + "     |\n"
                    + "|                              ***                                          "
                    + "     |\n"
                    + "|                           ***                                             "
                    + "     |\n"
                    + "|                       *****                                               "
                    + "     | 0.138\n"
                    + "|                   *****                                                   "
                    + "     |\n"
                    + "|               *****                                                       "
                    + "     |\n"
                    + "|          ******                                                           "
                    + "     |\n"
                    + "|**********                                                                 "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        baseTest(CUBIC_ACCELERATE, exp);
    }

    @Test
    public void testCubicAnticipate() {
        String exp =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|                                                                           "
                    + "   * |\n"
                    + "|                                                                           "
                    + "  *  |\n"
                    + "|                                                                           "
                    + " *   |\n"
                    + "|                                                                          "
                    + " *    | 0.811\n"
                    + "|                                                                          "
                    + " *    |\n"
                    + "|                                                                          *"
                    + "     |\n"
                    + "|                                                                         * "
                    + "     |\n"
                    + "|                                                                        *  "
                    + "     |\n"
                    + "|                                                                       *   "
                    + "     | 0.621\n"
                    + "|                                                                       *   "
                    + "     |\n"
                    + "|                                                                      *    "
                    + "     |\n"
                    + "|                                                                     *     "
                    + "     |\n"
                    + "|                                                                    *      "
                    + "     |\n"
                    + "|                                                                   *       "
                    + "     | 0.432\n"
                    + "|                                                                  **       "
                    + "     |\n"
                    + "|                                                                 *         "
                    + "     |\n"
                    + "|                                                                *          "
                    + "     |\n"
                    + "|                                                               *           "
                    + "     |\n"
                    + "|                                                             **            "
                    + "     | 0.243\n"
                    + "|                                                            *              "
                    + "     |\n"
                    + "|                                                           *               "
                    + "     |\n"
                    + "|                                                         **                "
                    + "     |\n"
                    + "|                                                       **                  "
                    + "     |\n"
                    + "|                                                     ***                   "
                    + "     | 0.054\n"
                    + "|                                                   **                      "
                    + "     |\n"
                    + "|***********                                     ***                        "
                    + "     |\n"
                    + "|           *********                        ****                           "
                    + "     |\n"
                    + "|                   *************************                               "
                    + "     | -0.097\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        baseTest(CUBIC_ANTICIPATE, exp);
    }

    @Test
    public void testCubicLinear() {
        String exp =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|                                                                           "
                    + " *** |\n"
                    + "|                                                                         "
                    + " **    |\n"
                    + "|                                                                       *** "
                    + "     |\n"
                    + "|                                                                    ***    "
                    + "     |\n"
                    + "|                                                                 ***       "
                    + "     | 0.828\n"
                    + "|                                                               **          "
                    + "     |\n"
                    + "|                                                            ***            "
                    + "     |\n"
                    + "|                                                         ***               "
                    + "     |\n"
                    + "|                                                       **                  "
                    + "     |\n"
                    + "|                                                   ****                    "
                    + "     | 0.655\n"
                    + "|                                                 ***                       "
                    + "     |\n"
                    + "|                                               **                          "
                    + "     |\n"
                    + "|                                           ****                            "
                    + "     |\n"
                    + "|                                         ***                               "
                    + "     |\n"
                    + "|                                      ***                                  "
                    + "     | 0.483\n"
                    + "|                                   ***                                     "
                    + "     |\n"
                    + "|                                ****                                       "
                    + "     |\n"
                    + "|                              **                                           "
                    + "     |\n"
                    + "|                           ***                                             "
                    + "     |\n"
                    + "|                        ****                                               "
                    + "     | 0.31\n"
                    + "|                      **                                                   "
                    + "     |\n"
                    + "|                   ***                                                     "
                    + "     |\n"
                    + "|                ***                                                        "
                    + "     |\n"
                    + "|              **                                                           "
                    + "     |\n"
                    + "|           ***                                                             "
                    + "     | 0.138\n"
                    + "|        ***                                                                "
                    + "     |\n"
                    + "|     ***                                                                   "
                    + "     |\n"
                    + "|   **                                                                      "
                    + "     |\n"
                    + "|***                                                                        "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        baseTest(CUBIC_LINEAR, exp);
    }

    @Test
    public void testCubicDecelerate() {
        String exp =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|                                                              "
                    + " **************** |\n"
                    + "|                                                       ********            "
                    + "     |\n"
                    + "|                                                 *******                   "
                    + "     |\n"
                    + "|                                            *****                          "
                    + "     |\n"
                    + "|                                        ****                               "
                    + "     | 0.828\n"
                    + "|                                     ***                                   "
                    + "     |\n"
                    + "|                                  ***                                      "
                    + "     |\n"
                    + "|                               ***                                         "
                    + "     |\n"
                    + "|                            ***                                            "
                    + "     |\n"
                    + "|                          **                                               "
                    + "     | 0.655\n"
                    + "|                       ***                                                 "
                    + "     |\n"
                    + "|                     ***                                                   "
                    + "     |\n"
                    + "|                   **                                                      "
                    + "     |\n"
                    + "|                  **                                                       "
                    + "     |\n"
                    + "|               ***                                                         "
                    + "     | 0.483\n"
                    + "|              **                                                           "
                    + "     |\n"
                    + "|            **                                                             "
                    + "     |\n"
                    + "|           *                                                               "
                    + "     |\n"
                    + "|          **                                                               "
                    + "     |\n"
                    + "|        **                                                                 "
                    + "     | 0.31\n"
                    + "|       *                                                                   "
                    + "     |\n"
                    + "|      **                                                                   "
                    + "     |\n"
                    + "|     *                                                                     "
                    + "     |\n"
                    + "|    *                                                                      "
                    + "     |\n"
                    + "|   *                                                                       "
                    + "     | 0.138\n"
                    + "|  *                                                                        "
                    + "     |\n"
                    + "| *                                                                         "
                    + "     |\n"
                    + "|*                                                                          "
                    + "     |\n"
                    + "|*                                                                          "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        baseTest(CUBIC_DECELERATE, exp);
    }

    @Test
    public void testCubicOvershoot() {
        String exp =
                "|                                             *                                  |"
                    + " 1.098\n"
                    + "|                                   ********** **************               "
                    + "     |\n"
                    + "|                               ****                        *********       "
                    + "     |\n"
                    + "|                            ***                                    "
                    + " ************|\n"
                    + "|                          **                                               "
                    + "     |\n"
                    + "|                       ***                                                 "
                    + "     | 0.908\n"
                    + "|                      **                                                   "
                    + "     |\n"
                    + "|                    **                                                     "
                    + "     |\n"
                    + "|                   *                                                       "
                    + "     |\n"
                    + "|                  *                                                        "
                    + "     |\n"
                    + "|                **                                                         "
                    + "     | 0.719\n"
                    + "|               *                                                           "
                    + "     |\n"
                    + "|              *                                                            "
                    + "     |\n"
                    + "|             *                                                             "
                    + "     |\n"
                    + "|           **                                                              "
                    + "     |\n"
                    + "|           *                                                               "
                    + "     | 0.53\n"
                    + "|          *                                                                "
                    + "     |\n"
                    + "|         *                                                                 "
                    + "     |\n"
                    + "|        *                                                                  "
                    + "     |\n"
                    + "|       *                                                                   "
                    + "     |\n"
                    + "|       *                                                                   "
                    + "     | 0.341\n"
                    + "|      *                                                                    "
                    + "     |\n"
                    + "|     *                                                                     "
                    + "     |\n"
                    + "|    *                                                                      "
                    + "     |\n"
                    + "|   *                                                                       "
                    + "     |\n"
                    + "|   *                                                                       "
                    + "     | 0.151\n"
                    + "|  *                                                                        "
                    + "     |\n"
                    + "| *                                                                         "
                    + "     |\n"
                    + "|*                                                                          "
                    + "     |\n"
                    + "|*                                                                          "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        baseTest(CUBIC_OVERSHOOT, exp);
    }

    @Test
    public void testCubicCustom() {
        String exp =
                "|                                             *                                  |"
                    + " 1.098\n"
                    + "|                                   ********** **************               "
                    + "     |\n"
                    + "|                               ****                        *********       "
                    + "     |\n"
                    + "|                            ***                                    "
                    + " ************|\n"
                    + "|                          **                                               "
                    + "     |\n"
                    + "|                       ***                                                 "
                    + "     | 0.908\n"
                    + "|                      **                                                   "
                    + "     |\n"
                    + "|                    **                                                     "
                    + "     |\n"
                    + "|                   *                                                       "
                    + "     |\n"
                    + "|                  *                                                        "
                    + "     |\n"
                    + "|                **                                                         "
                    + "     | 0.719\n"
                    + "|               *                                                           "
                    + "     |\n"
                    + "|              *                                                            "
                    + "     |\n"
                    + "|             *                                                             "
                    + "     |\n"
                    + "|           **                                                              "
                    + "     |\n"
                    + "|           *                                                               "
                    + "     | 0.53\n"
                    + "|          *                                                                "
                    + "     |\n"
                    + "|         *                                                                 "
                    + "     |\n"
                    + "|        *                                                                  "
                    + "     |\n"
                    + "|       *                                                                   "
                    + "     |\n"
                    + "|       *                                                                   "
                    + "     | 0.341\n"
                    + "|      *                                                                    "
                    + "     |\n"
                    + "|     *                                                                     "
                    + "     |\n"
                    + "|    *                                                                      "
                    + "     |\n"
                    + "|   *                                                                       "
                    + "     |\n"
                    + "|   *                                                                       "
                    + "     | 0.151\n"
                    + "|  *                                                                        "
                    + "     |\n"
                    + "| *                                                                         "
                    + "     |\n"
                    + "|*                                                                          "
                    + "     |\n"
                    + "|*                                                                          "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        // same parameters as Overshoot
        baseTest(CUBIC_CUSTOM, new float[] {0.34f, 1.56f, 0.64f, 1f}, exp);
        String exp2 =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|                                                                     "
                    + " ********* |\n"
                    + "|                                                                  ****     "
                    + "     |\n"
                    + "|                                                               ***         "
                    + "     |\n"
                    + "|                                                           ****            "
                    + "     |\n"
                    + "|                                                         ***               "
                    + "     | 0.828\n"
                    + "|                                                       **                  "
                    + "     |\n"
                    + "|                                                     **                    "
                    + "     |\n"
                    + "|                                                   **                      "
                    + "     |\n"
                    + "|                                                 **                        "
                    + "     |\n"
                    + "|                                               **                          "
                    + "     | 0.655\n"
                    + "|                                             **                            "
                    + "     |\n"
                    + "|                                           **                              "
                    + "     |\n"
                    + "|                                          **                               "
                    + "     |\n"
                    + "|                                       ***                                 "
                    + "     |\n"
                    + "|                                      **                                   "
                    + "     | 0.483\n"
                    + "|                                    **                                     "
                    + "     |\n"
                    + "|                                   *                                       "
                    + "     |\n"
                    + "|                                ***                                        "
                    + "     |\n"
                    + "|                               *                                           "
                    + "     |\n"
                    + "|                             **                                            "
                    + "     | 0.31\n"
                    + "|                           **                                              "
                    + "     |\n"
                    + "|                         **                                                "
                    + "     |\n"
                    + "|                       **                                                  "
                    + "     |\n"
                    + "|                     **                                                    "
                    + "     |\n"
                    + "|                   **                                                      "
                    + "     | 0.138\n"
                    + "|               ****                                                        "
                    + "     |\n"
                    + "|            ****                                                           "
                    + "     |\n"
                    + "|         ***                                                               "
                    + "     |\n"
                    + "|*********                                                                  "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        // same parameters as Overshoot
        baseTest(CUBIC_CUSTOM, new float[] {0.34f, 0.0f, 0.64f, 1f}, exp2);
    }

    @Test
    public void testEaseOutBounce() {
        String exp =
                "|                            *                                          *       *|"
                    + " 1.0\n"
                    + "|                             *                          ****         "
                    + " ********* |\n"
                    + "|                           *  *                        *   ***********     "
                    + "     |\n"
                    + "|                           *   *                      **                   "
                    + "     |\n"
                    + "|                                *                    *                     "
                    + "     |\n"
                    + "|                          *      **                **                      "
                    + "     | 0.828\n"
                    + "|                         *         **            ***                       "
                    + "     |\n"
                    + "|                        *            ***      ***                          "
                    + "     |\n"
                    + "|                       *                ******                             "
                    + "     |\n"
                    + "|                       *                                                   "
                    + "     |\n"
                    + "|                                                                           "
                    + "     | 0.655\n"
                    + "|                      *                                                    "
                    + "     |\n"
                    + "|                     *                                                     "
                    + "     |\n"
                    + "|                    *                                                      "
                    + "     |\n"
                    + "|                   *                                                       "
                    + "     |\n"
                    + "|                   *                                                       "
                    + "     | 0.483\n"
                    + "|                  *                                                        "
                    + "     |\n"
                    + "|                 *                                                         "
                    + "     |\n"
                    + "|                *                                                          "
                    + "     |\n"
                    + "|               *                                                           "
                    + "     |\n"
                    + "|              *                                                            "
                    + "     | 0.31\n"
                    + "|             *                                                             "
                    + "     |\n"
                    + "|            *                                                              "
                    + "     |\n"
                    + "|           *                                                               "
                    + "     |\n"
                    + "|          *                                                                "
                    + "     |\n"
                    + "|        **                                                                 "
                    + "     | 0.138\n"
                    + "|       *                                                                   "
                    + "     |\n"
                    + "|     **                                                                    "
                    + "     |\n"
                    + "|   **                                                                      "
                    + "     |\n"
                    + "|***                                                                        "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        // same parameters as Overshoot
        baseTest(EASE_OUT_BOUNCE, exp);
    }

    @Test
    public void testEaseOutElastic() {
        String exp =
                "|          *                                                                     |"
                    + " 1.372\n"
                    + "|         * *                                                               "
                    + "     |\n"
                    + "|        *   *                                                              "
                    + "     |\n"
                    + "|       *     *                                                             "
                    + "     |\n"
                    + "|              *                                                            "
                    + "     |\n"
                    + "|       *       *                                                           "
                    + "     | 1.135\n"
                    + "|               *                                                           "
                    + "     |\n"
                    + "|      *         *               ****                                       "
                    + "     |\n"
                    + "|                 *           ***    *******       "
                    + " *****************************|\n"
                    + "|                  *        **              ********                        "
                    + "     |\n"
                    + "|     *             *     ***                                               "
                    + "     | 0.899\n"
                    + "|                    *****                                                  "
                    + "     |\n"
                    + "|    *                                                                      "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|                                                                           "
                    + "     | 0.662\n"
                    + "|   *                                                                       "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|   *                                                                       "
                    + "     |\n"
                    + "|                                                                           "
                    + "     | 0.426\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|  *                                                                        "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "| *                                                                         "
                    + "     | 0.189\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|*                                                                          "
                    + "     |\n"
                    + "|*                                                                          "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        // same parameters as Overshoot
        baseTest(EASE_OUT_ELASTIC, exp);
    }

    @Test
    public void testSplineCustom() {
        String exp =
                "|                          *                                                    *|"
                    + " 1.0\n"
                    + "|                       *** **                                              "
                    + "   * |\n"
                    + "|                      *      **                                            "
                    + "  *  |\n"
                    + "|                    **         *                                           "
                    + " *   |\n"
                    + "|                   *           *                                          "
                    + " *    |\n"
                    + "|                   *            *                                         "
                    + " *    | 0.828\n"
                    + "|                  *              *                                         "
                    + "     |\n"
                    + "|                 *                *                                       *"
                    + "     |\n"
                    + "|                *                  *                                     * "
                    + "     |\n"
                    + "|               *                   *                                    *  "
                    + "     |\n"
                    + "|               *                    *                                  *   "
                    + "     | 0.655\n"
                    + "|              *                                                        *   "
                    + "     |\n"
                    + "|             *                       *                                *    "
                    + "     |\n"
                    + "|            *                         *                              *     "
                    + "     |\n"
                    + "|           *                           *                                   "
                    + "     |\n"
                    + "|                                                                    *      "
                    + "     | 0.483\n"
                    + "|           *                           *                           *       "
                    + "     |\n"
                    + "|          *                             *                          *       "
                    + "     |\n"
                    + "|         *                               *                        *        "
                    + "     |\n"
                    + "|        *                                                        *         "
                    + "     |\n"
                    + "|       *                                  *                     *          "
                    + "     | 0.31\n"
                    + "|       *                                   *                   *           "
                    + "     |\n"
                    + "|                                           *                   *           "
                    + "     |\n"
                    + "|      *                                     *                 *            "
                    + "     |\n"
                    + "|     *                                       *               *             "
                    + "     |\n"
                    + "|    *                                         *             *              "
                    + "     | 0.138\n"
                    + "|   *                                           *           *               "
                    + "     |\n"
                    + "|  **                                           *          **               "
                    + "     |\n"
                    + "| *                                              **      **                 "
                    + "     |\n"
                    + "|*                                                 ******                   "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        // same parameters as Overshoot
        baseTest(SPLINE_CUSTOM, new float[] {0, 1, 0, 1}, exp);

        String exp2 =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|          **                    ****                   ***                 "
                    + "     |\n"
                    + "|         *  *                                          *                   "
                    + "     |\n"
                    + "|                               *   *                  *   *                "
                    + "   * |\n"
                    + "|        *    *                                             *               "
                    + "     |\n"
                    + "|                               *    *                *                     "
                    + "  *  | 0.828\n"
                    + "|       *      *                                                            "
                    + "     |\n"
                    + "|                              *                            *               "
                    + "     |\n"
                    + "|       *       *                     *              *                      "
                    + " *   |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|      *                      *        *                     *             "
                    + " *    | 0.655\n"
                    + "|               *                                   *                       "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|     *                      *          *                     *            "
                    + " *    |\n"
                    + "|                *                                  *                       "
                    + "     |\n"
                    + "|                                                                           "
                    + "     | 0.483\n"
                    + "|    *                      *                                  *           *"
                    + "     |\n"
                    + "|                 *                     *          *                        "
                    + "     |\n"
                    + "|                                                                         * "
                    + "     |\n"
                    + "|   *                       *                                   *           "
                    + "     |\n"
                    + "|                  *                     *        *                         "
                    + "     | 0.31\n"
                    + "|   *                                                                    *  "
                    + "     |\n"
                    + "|                          *              *                     *           "
                    + "     |\n"
                    + "|                   *                            *                      *   "
                    + "     |\n"
                    + "|  *                                                             *          "
                    + "     |\n"
                    + "|                         *                *    *                       *   "
                    + "     | 0.138\n"
                    + "| *                 *                                             *         "
                    + "     |\n"
                    + "|                    *   *                  *   *                      *    "
                    + "     |\n"
                    + "|*                      *                                          *  *     "
                    + "     |\n"
                    + "|*                    ***                   ****                    **      "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   1.0\n";
        // same parameters as Overshoot
        baseTest(SPLINE_CUSTOM, new float[] {0, 1, 0, 1, 0, 1, 0, 1}, exp2);
    }

    @Test
    public void testDuration() {
        String exp =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|                                                            "
                    + " ****************** |\n"
                    + "|                                                      *******              "
                    + "     |\n"
                    + "|                                                  ****                     "
                    + "     |\n"
                    + "|                                              ****                         "
                    + "     |\n"
                    + "|                                           ***                             "
                    + "     | 0.828\n"
                    + "|                                        ****                               "
                    + "     |\n"
                    + "|                                       *                                   "
                    + "     |\n"
                    + "|                                    ***                                    "
                    + "     |\n"
                    + "|                                   *                                       "
                    + "     |\n"
                    + "|                                 **                                        "
                    + "     | 0.655\n"
                    + "|                               **                                          "
                    + "     |\n"
                    + "|                               *                                           "
                    + "     |\n"
                    + "|                             **                                            "
                    + "     |\n"
                    + "|                            *                                              "
                    + "     |\n"
                    + "|                           *                                               "
                    + "     | 0.483\n"
                    + "|                          *                                                "
                    + "     |\n"
                    + "|                         *                                                 "
                    + "     |\n"
                    + "|                        *                                                  "
                    + "     |\n"
                    + "|                       *                                                   "
                    + "     |\n"
                    + "|                      *                                                    "
                    + "     | 0.31\n"
                    + "|                     *                                                     "
                    + "     |\n"
                    + "|                   **                                                      "
                    + "     |\n"
                    + "|                   *                                                       "
                    + "     |\n"
                    + "|                 **                                                        "
                    + "     |\n"
                    + "|                *                                                          "
                    + "     | 0.138\n"
                    + "|              **                                                           "
                    + "     |\n"
                    + "|            **                                                             "
                    + "     |\n"
                    + "|         ***                                                               "
                    + "     |\n"
                    + "|*********                                                                  "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   2.0\n";
        // same parameters as Overshoot
        baseTest(CUBIC_STANDARD, 2, null, exp);

        String exp2 =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|          **                    ****                   ***                 "
                    + "     |\n"
                    + "|         *  *                                          *                   "
                    + "     |\n"
                    + "|                               *   *                  *   *                "
                    + "   * |\n"
                    + "|        *    *                                             *               "
                    + "     |\n"
                    + "|                               *    *                *                     "
                    + "  *  | 0.828\n"
                    + "|       *      *                                                            "
                    + "     |\n"
                    + "|                              *                            *               "
                    + "     |\n"
                    + "|       *       *                     *              *                      "
                    + " *   |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|      *                      *        *                     *             "
                    + " *    | 0.655\n"
                    + "|               *                                   *                       "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|     *                      *          *                     *            "
                    + " *    |\n"
                    + "|                *                                  *                       "
                    + "     |\n"
                    + "|                                                                           "
                    + "     | 0.483\n"
                    + "|    *                      *                                  *           *"
                    + "     |\n"
                    + "|                 *                     *          *                        "
                    + "     |\n"
                    + "|                                                                         * "
                    + "     |\n"
                    + "|   *                       *                                   *           "
                    + "     |\n"
                    + "|                  *                     *        *                         "
                    + "     | 0.31\n"
                    + "|   *                                                                    *  "
                    + "     |\n"
                    + "|                          *              *                     *           "
                    + "     |\n"
                    + "|                   *                            *                      *   "
                    + "     |\n"
                    + "|  *                                                             *          "
                    + "     |\n"
                    + "|                         *                *    *                       *   "
                    + "     | 0.138\n"
                    + "| *                 *                                             *         "
                    + "     |\n"
                    + "|                    *   *                  *   *                      *    "
                    + "     |\n"
                    + "|*                      *                                          *  *     "
                    + "     |\n"
                    + "|*                    ***                   ****                    **      "
                    + "     | 0.0\n"
                    + "0.0                                                                         "
                    + "   0.5\n";
        // same parameters as Overshoot
        baseTest(SPLINE_CUSTOM, 0.5f, new float[] {0, 1, 0, 1, 0, 1, 0, 1}, exp2);
    }

    @Test
    public void testStart() {
        String exp =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|                                                            "
                    + " ****************** |\n"
                    + "|                                                      *******              "
                    + "     |\n"
                    + "|                                                  ****                     "
                    + "     |\n"
                    + "|                                              ****                         "
                    + "     |\n"
                    + "|                                           ***                             "
                    + "     | 0.914\n"
                    + "|                                        ****                               "
                    + "     |\n"
                    + "|                                       *                                   "
                    + "     |\n"
                    + "|                                    ***                                    "
                    + "     |\n"
                    + "|                                   *                                       "
                    + "     |\n"
                    + "|                                 **                                        "
                    + "     | 0.828\n"
                    + "|                               **                                          "
                    + "     |\n"
                    + "|                               *                                           "
                    + "     |\n"
                    + "|                             **                                            "
                    + "     |\n"
                    + "|                            *                                              "
                    + "     |\n"
                    + "|                           *                                               "
                    + "     | 0.741\n"
                    + "|                          *                                                "
                    + "     |\n"
                    + "|                         *                                                 "
                    + "     |\n"
                    + "|                        *                                                  "
                    + "     |\n"
                    + "|                       *                                                   "
                    + "     |\n"
                    + "|                      *                                                    "
                    + "     | 0.655\n"
                    + "|                     *                                                     "
                    + "     |\n"
                    + "|                   **                                                      "
                    + "     |\n"
                    + "|                   *                                                       "
                    + "     |\n"
                    + "|                 **                                                        "
                    + "     |\n"
                    + "|                *                                                          "
                    + "     | 0.569\n"
                    + "|              **                                                           "
                    + "     |\n"
                    + "|            **                                                             "
                    + "     |\n"
                    + "|         ***                                                               "
                    + "     |\n"
                    + "|*********                                                                  "
                    + "     | 0.5\n"
                    + "0.0                                                                         "
                    + "   2.0\n";
        // same parameters as Overshoot
        baseTest(CUBIC_STANDARD, 2, null, 0.5f, Float.NaN, exp);

        String exp2 =
                "|                                                                               *|"
                    + " 1.0\n"
                    + "|          **                    ****                   ***                 "
                    + "     |\n"
                    + "|         *  *                                          *                   "
                    + "     |\n"
                    + "|                               *   *                  *   *                "
                    + "   * |\n"
                    + "|        *    *                                             *               "
                    + "     |\n"
                    + "|                               *    *                *                     "
                    + "  *  | -0.896\n"
                    + "|       *      *                                                            "
                    + "     |\n"
                    + "|                              *                            *               "
                    + "     |\n"
                    + "|       *       *                     *              *                      "
                    + " *   |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|      *                      *        *                     *             "
                    + " *    | -2.792\n"
                    + "|               *                                   *                       "
                    + "     |\n"
                    + "|                                                                           "
                    + "     |\n"
                    + "|     *                      *          *                     *            "
                    + " *    |\n"
                    + "|                *                                  *                       "
                    + "     |\n"
                    + "|                                                                           "
                    + "     | -4.689\n"
                    + "|    *                      *                                  *           *"
                    + "     |\n"
                    + "|                 *                     *          *                        "
                    + "     |\n"
                    + "|                                                                         * "
                    + "     |\n"
                    + "|   *                       *                                   *           "
                    + "     |\n"
                    + "|                  *                     *        *                         "
                    + "     | -6.585\n"
                    + "|   *                                                                    *  "
                    + "     |\n"
                    + "|                          *              *                     *           "
                    + "     |\n"
                    + "|                   *                            *                      *   "
                    + "     |\n"
                    + "|  *                                                             *          "
                    + "     |\n"
                    + "|                         *                *    *                       *   "
                    + "     | -8.482\n"
                    + "| *                 *                                             *         "
                    + "     |\n"
                    + "|                    *   *                  *   *                      *    "
                    + "     |\n"
                    + "|*                      *                                          *  *     "
                    + "     |\n"
                    + "|*                    ***                   ****                    **      "
                    + "     | -9.999\n"
                    + "0.0                                                                         "
                    + "   0.5\n";
        // same parameters as Overshoot
        baseTest(SPLINE_CUSTOM, 0.5f, new float[] {0, 1, 0, 1, 0, 1, 0, 1}, -10f, Float.NaN, exp2);
    }
}
