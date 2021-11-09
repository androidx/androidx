/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.core.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the various Evaluator classes in androidx.core.animation.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class EvaluatorTest {
    private static final float EPSILON = 0.001f;

    @Test
    public void testFloatEvaluator() {
        float start = 0.0f;
        float end = 1.0f;
        float fraction = 0.5f;
        FloatEvaluator floatEvaluator = FloatEvaluator.getInstance();

        float result = floatEvaluator.evaluate(0, start, end);
        assertEquals(start, result, EPSILON);

        result = floatEvaluator.evaluate(fraction, start, end);
        assertEquals(.5f, result, EPSILON);

        result = floatEvaluator.evaluate(1, start, end);
        assertEquals(end, result, EPSILON);
    }

    @Test
    public void testFloatArrayEvaluator() {
        FloatArrayEvaluator evaluator = new FloatArrayEvaluator();
        floatArrayEvaluatorTestImpl(evaluator, null);

        float[] reusableArray = new float[2];
        FloatArrayEvaluator evaluator2 = new FloatArrayEvaluator(reusableArray);
        floatArrayEvaluatorTestImpl(evaluator2, reusableArray);
    }

    private void floatArrayEvaluatorTestImpl(FloatArrayEvaluator evaluator, float[] reusedArray) {
        float[] start = {0f, 0f};
        float[] end = {.8f, 1.0f};
        float fraction = 0.5f;

        float[] result = evaluator.evaluate(0, start, end);
        assertEquals(start[0], result[0], EPSILON);
        assertEquals(start[1], result[1], EPSILON);

        result = evaluator.evaluate(fraction, start, end);
        assertEquals(.4f, result[0], EPSILON);
        assertEquals(.5f, result[1], EPSILON);

        result = evaluator.evaluate(1, start, end);
        assertEquals(end[0], result[0], EPSILON);
        assertEquals(end[1], result[1], EPSILON);

        if (reusedArray != null) {
            assertEquals(reusedArray, result);
        }
    }

    @Test
    public void testArgbEvaluator() {
        final int Start =  0xffFF8080;
        final int end = 0xff8080FF;
        int aStart = Color.alpha(Start);
        int rStart = Color.red(Start);
        int gStart = Color.green(Start);
        int bStart = Color.blue(Start);
        int aEnd = Color.alpha(end);
        int rEnd = Color.red(end);
        int gEnd = Color.green(end);
        int bEnd = Color.blue(end);

        final ArgbEvaluator evaluator = ArgbEvaluator.getInstance();

        int result = (Integer) evaluator.evaluate(0, Start, end);
        int aResult = Color.alpha(result);
        int rResult = Color.red(result);
        int gResult = Color.green(result);
        int bResult = Color.blue(result);
        assertEquals(aStart, aResult);
        assertEquals(rStart, rResult);
        assertEquals(gStart, gResult);
        assertEquals(bStart, bResult);

        result = (Integer) evaluator.evaluate(.5f, Start, end);
        aResult = Color.alpha(result);
        rResult = Color.red(result);
        gResult = Color.green(result);
        bResult = Color.blue(result);
        assertEquals(0xff, aResult);
        assertEquals(0x80, gResult);
        if (rStart < rEnd) {
            assertTrue(rResult > rStart && rResult < rEnd);
        } else {
            assertTrue(rResult < rStart && rResult > rEnd);
        }
        if (bStart < bEnd) {
            assertTrue(bResult > bStart && bResult < bEnd);
        } else {
            assertTrue(bResult < bStart && bResult > bEnd);
        }

        result = (Integer) evaluator.evaluate(1, Start, end);
        aResult = Color.alpha(result);
        rResult = Color.red(result);
        gResult = Color.green(result);
        bResult = Color.blue(result);
        assertEquals(aEnd, aResult);
        assertEquals(rEnd, rResult);
        assertEquals(gEnd, gResult);
        assertEquals(bEnd, bResult);
    }

    @Test
    public void testIntEvaluator() {
        final int start = 0;
        final int end = 100;
        final float fraction = 0.5f;
        final IntEvaluator intEvaluator = IntEvaluator.getInstance();

        int result = intEvaluator.evaluate(0, start, end);
        assertEquals(start, result);

        result = intEvaluator.evaluate(fraction, start, end);
        assertEquals(50, result);

        result = intEvaluator.evaluate(1, start, end);
        assertEquals(end, result);
    }

    @Test
    public void testIntArrayEvaluator() {
        IntArrayEvaluator evaluator = new IntArrayEvaluator();
        intArrayEvaluatorTestImpl(evaluator, null);

        int[] reusableArray = new int[2];
        IntArrayEvaluator evaluator2 = new IntArrayEvaluator(reusableArray);
        intArrayEvaluatorTestImpl(evaluator2, reusableArray);
    }

    private void intArrayEvaluatorTestImpl(IntArrayEvaluator evaluator, int[] reusedArray) {
        int[] start = {0, 0};
        int[] end = {80, 100};
        float fraction = 0.5f;

        int[] result = evaluator.evaluate(0, start, end);
        assertEquals(start[0], result[0]);
        assertEquals(start[1], result[1]);

        result = evaluator.evaluate(fraction, start, end);
        assertEquals(40, result[0]);
        assertEquals(50, result[1]);

        result = evaluator.evaluate(1, start, end);
        assertEquals(end[0], result[0]);
        assertEquals(end[1], result[1]);

        if (reusedArray != null) {
            assertEquals(reusedArray, result);
        }
    }

    @Test
    public void testRectEvaluator() throws Throwable {
        final RectEvaluator evaluator = new RectEvaluator();
        rectEvaluatorTestImpl(evaluator, null);

        Rect reusableRect = new Rect();
        final RectEvaluator evaluator2 = new RectEvaluator(reusableRect);
        rectEvaluatorTestImpl(evaluator2, reusableRect);
    }

    private void rectEvaluatorTestImpl(RectEvaluator evaluator, Rect reusedRect) {
        final Rect start = new Rect(0, 0, 0, 0);
        final Rect end = new Rect(100, 200, 300, 400);
        final float fraction = 0.5f;

        Rect result = evaluator.evaluate(0, start, end);
        assertEquals(start.left, result.left, EPSILON);
        assertEquals(start.top, result.top, EPSILON);
        assertEquals(start.right, result.right, EPSILON);
        assertEquals(start.bottom, result.bottom, 001f);

        result = evaluator.evaluate(fraction, start, end);
        assertEquals(50, result.left, EPSILON);
        assertEquals(100, result.top, EPSILON);
        assertEquals(150, result.right, EPSILON);
        assertEquals(200, result.bottom, EPSILON);

        result = evaluator.evaluate(1, start, end);
        assertEquals(end.left, result.left, EPSILON);
        assertEquals(end.top, result.top, EPSILON);
        assertEquals(end.right, result.right, EPSILON);
        assertEquals(end.bottom, result.bottom, EPSILON);

        if (reusedRect != null) {
            assertEquals(reusedRect, result);
        }
    }

    @Test
    public void testPointFEvaluator() throws Throwable {
        final PointFEvaluator evaluator = new PointFEvaluator();
        pointFEvaluatorTestImpl(evaluator, null);

        PointF reusablePoint = new PointF();
        final PointFEvaluator evaluator2 = new PointFEvaluator(reusablePoint);
        pointFEvaluatorTestImpl(evaluator2, reusablePoint);
    }

    private void pointFEvaluatorTestImpl(PointFEvaluator evaluator, PointF reusedPoint) {
        final PointF start = new PointF(0, 0);
        final PointF end = new PointF(100, 200);
        final float fraction = 0.5f;

        PointF result = evaluator.evaluate(0, start, end);
        assertEquals(start.x, result.x, EPSILON);
        assertEquals(start.y, result.y, EPSILON);

        result = evaluator.evaluate(fraction, start, end);
        assertEquals(50, result.x, EPSILON);
        assertEquals(100, result.y, EPSILON);

        result = evaluator.evaluate(1, start, end);
        assertEquals(end.x, result.x, EPSILON);
        assertEquals(end.y, result.y, EPSILON);

        if (reusedPoint != null) {
            assertEquals(reusedPoint, result);
        }
    }
}
