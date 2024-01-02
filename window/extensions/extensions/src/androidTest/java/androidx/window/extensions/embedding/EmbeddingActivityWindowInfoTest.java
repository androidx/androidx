/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.extensions.embedding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests for {@link EmbeddingActivityWindowInfo} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmbeddingActivityWindowInfoTest {

    @Mock
    private Activity mActivity;
    @Mock
    private Activity mActivity2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetter() {
        final Rect activityBounds = new Rect(0, 0, 500, 1000);
        final Rect taskBounds = new Rect(0, 0, 1000, 2000);
        final Rect activityStackBounds = new Rect(0, 0, 1000, 1000);
        final EmbeddingActivityWindowInfo info = new EmbeddingActivityWindowInfo(mActivity,
                true /* isEmbedded */, activityBounds, taskBounds, activityStackBounds);

        assertEquals(mActivity, info.getActivity());
        assertTrue(info.isEmbedded());
        assertEquals(activityBounds, info.getActivityBounds());
        assertEquals(taskBounds, info.getTaskBounds());
        assertEquals(activityStackBounds, info.getActivityStackBounds());
    }

    @Test
    public void testEqualsAndHashCode() {
        final EmbeddingActivityWindowInfo info1 = new EmbeddingActivityWindowInfo(mActivity,
                true /* isEmbedded */,
                new Rect(0, 0, 500, 1000),
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));
        final EmbeddingActivityWindowInfo info2 = new EmbeddingActivityWindowInfo(mActivity2,
                true /* isEmbedded */,
                new Rect(0, 0, 500, 1000),
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));
        final EmbeddingActivityWindowInfo info3 = new EmbeddingActivityWindowInfo(mActivity,
                false /* isEmbedded */,
                new Rect(0, 0, 500, 1000),
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));
        final EmbeddingActivityWindowInfo info4 = new EmbeddingActivityWindowInfo(mActivity,
                true /* isEmbedded */,
                new Rect(0, 0, 1000, 1000),
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));
        final EmbeddingActivityWindowInfo info5 = new EmbeddingActivityWindowInfo(mActivity,
                true /* isEmbedded */,
                new Rect(0, 0, 500, 1000),
                new Rect(0, 0, 1000, 1000),
                new Rect(0, 0, 1000, 1000));
        final EmbeddingActivityWindowInfo info6 = new EmbeddingActivityWindowInfo(mActivity,
                true /* isEmbedded */,
                new Rect(0, 0, 500, 1000),
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1500));
        final EmbeddingActivityWindowInfo info7 = new EmbeddingActivityWindowInfo(mActivity,
                true /* isEmbedded */,
                new Rect(0, 0, 500, 1000),
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));

        assertNotEquals(info1, info2);
        assertNotEquals(info1, info3);
        assertNotEquals(info1, info4);
        assertNotEquals(info1, info5);
        assertNotEquals(info1, info6);
        assertEquals(info1, info7);

        assertNotEquals(info1.hashCode(), info2.hashCode());
        assertNotEquals(info1.hashCode(), info3.hashCode());
        assertNotEquals(info1.hashCode(), info4.hashCode());
        assertNotEquals(info1.hashCode(), info5.hashCode());
        assertNotEquals(info1.hashCode(), info6.hashCode());
        assertEquals(info1.hashCode(), info7.hashCode());
    }
}
