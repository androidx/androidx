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

/** Tests for {@link EmbeddedActivityWindowInfo} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmbeddedActivityWindowInfoTest {

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
        final Rect taskBounds = new Rect(0, 0, 1000, 2000);
        final Rect activityStackBounds = new Rect(0, 0, 1000, 1000);
        final EmbeddedActivityWindowInfo info = new EmbeddedActivityWindowInfo(mActivity,
                true /* isEmbedded */, taskBounds, activityStackBounds);

        assertEquals(mActivity, info.getActivity());
        assertTrue(info.isEmbedded());
        assertEquals(taskBounds, info.getTaskBounds());
        assertEquals(activityStackBounds, info.getActivityStackBounds());
    }

    @Test
    public void testEqualsAndHashCode() {
        final EmbeddedActivityWindowInfo info1 = new EmbeddedActivityWindowInfo(mActivity,
                true /* isEmbedded */,
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));
        final EmbeddedActivityWindowInfo info2 = new EmbeddedActivityWindowInfo(mActivity2,
                true /* isEmbedded */,
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));
        final EmbeddedActivityWindowInfo info3 = new EmbeddedActivityWindowInfo(mActivity,
                false /* isEmbedded */,
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));
        final EmbeddedActivityWindowInfo info4 = new EmbeddedActivityWindowInfo(mActivity,
                true /* isEmbedded */,
                new Rect(0, 0, 1000, 1000),
                new Rect(0, 0, 1000, 1000));
        final EmbeddedActivityWindowInfo info5 = new EmbeddedActivityWindowInfo(mActivity,
                true /* isEmbedded */,
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1500));
        final EmbeddedActivityWindowInfo info6 = new EmbeddedActivityWindowInfo(mActivity,
                true /* isEmbedded */,
                new Rect(0, 0, 1000, 2000),
                new Rect(0, 0, 1000, 1000));

        assertNotEquals(info1, info2);
        assertNotEquals(info1, info3);
        assertNotEquals(info1, info4);
        assertNotEquals(info1, info5);
        assertEquals(info1, info6);

        assertNotEquals(info1.hashCode(), info2.hashCode());
        assertNotEquals(info1.hashCode(), info3.hashCode());
        assertNotEquals(info1.hashCode(), info4.hashCode());
        assertNotEquals(info1.hashCode(), info5.hashCode());
        assertEquals(info1.hashCode(), info6.hashCode());
    }
}
