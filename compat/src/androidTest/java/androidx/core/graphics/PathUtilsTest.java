/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.graphics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import android.graphics.Path;
import android.graphics.PointF;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;

import org.junit.Test;

@SmallTest
public final class PathUtilsTest {
    @SdkSuppress(minSdkVersion = 26)
    @Test public void flattenEmptyPath() {
        for (PathSegment segment : PathUtils.flatten(new Path())) {
            fail("An empty path should not have segments: " + segment);
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test public void flatten() {
        Path p = new Path();

        // Single line
        p.lineTo(10.0f, 10.0f);
        assertEquals(
                new PathSegment(new PointF(), 0.0f, new PointF(10.0f, 10.0f), 1.0f),
                PathUtils.flatten(p).iterator().next());

        // Only moves
        p.reset();
        p.moveTo(10.0f, 10.0f);
        p.moveTo(20.0f, 20.0f);
        for (PathSegment segment : PathUtils.flatten(p)) {
            fail("A path with only moves should not have segments: " + segment);
        }

        // Mix of moves/lines
        p.reset();
        p.moveTo(10.0f, 10.0f);
        p.lineTo(20.0f, 20.0f);
        p.lineTo(60.0f, 20.0f);

        int count = 0;
        for (PathSegment segment : PathUtils.flatten(p)) {
            count++;
            assertNotEquals(segment.getStartFraction(), segment.getEndFraction());
        }
        assertEquals(2, count);

        // Mix of moves/lines, starts with moves, ends with moves
        p.reset();
        // Start with several moves
        p.moveTo(5.0f, 5.0f);
        p.moveTo(10.0f, 10.0f);
        p.lineTo(20.0f, 20.0f);
        p.lineTo(30.0f, 10.0f);
        // Several moves in the middle
        p.moveTo(40.0f, 10.0f);
        p.moveTo(50.0f, 10.0f);
        p.lineTo(60.0f, 20.0f);
        // End with several moves
        p.moveTo(10.0f, 10.0f);
        p.moveTo(30.0f, 30.0f);

        count = 0;
        for (PathSegment segment : PathUtils.flatten(p)) {
            count++;
            assertNotEquals(segment.getStartFraction(), segment.getEndFraction());
        }
        assertEquals(3, count);
    }
}

