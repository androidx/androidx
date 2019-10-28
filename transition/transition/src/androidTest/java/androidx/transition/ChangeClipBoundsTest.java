/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

@LargeTest
public class ChangeClipBoundsTest extends BaseTransitionTest {

    @Override
    Transition createTransition() {
        return new ChangeClipBounds();
    }

    @SdkSuppress(minSdkVersion = 18)
    @Test
    public void testChangeClipBounds() throws Throwable {
        final View redSquare = spy(new View(rule.getActivity()));
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                redSquare.setBackgroundColor(Color.RED);
                mRoot.addView(redSquare, 100, 100);
            }
        });

        final Rect newClip = new Rect(40, 40, 60, 60);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertNull(ViewCompat.getClipBounds(redSquare));
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                ViewCompat.setClipBounds(redSquare, newClip);
            }
        });
        waitForStart();
        verify(redSquare, timeout(1000).atLeastOnce())
                .setClipBounds(argThat(isRectContaining(newClip)));
        waitForEnd();

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Rect endRect = ViewCompat.getClipBounds(redSquare);
                assertNotNull(endRect);
                assertEquals(newClip, endRect);
            }
        });

        resetListener();
        reset(redSquare);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                ViewCompat.setClipBounds(redSquare, null);
            }
        });
        waitForStart();
        verify(redSquare, timeout(1000).atLeastOnce())
                .setClipBounds(argThat(isRectContainedIn(newClip)));
        waitForEnd();

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertNull(ViewCompat.getClipBounds(redSquare));
            }
        });

    }

    private ArgumentMatcher<Rect> isRectContaining(final Rect rect) {
        return new ArgumentMatcher<Rect>() {
            @Override
            public boolean matches(Rect self) {
                return rect != null && self != null && self.contains(rect);
            }
        };
    }

    private ArgumentMatcher<Rect> isRectContainedIn(final Rect rect) {
        return new ArgumentMatcher<Rect>() {
            @Override
            public boolean matches(Rect self) {
                return rect != null && self != null && rect.contains(self);
            }
        };
    }

    @Test
    public void dummy() {
        // Avoid "No tests found" on older devices
    }

}
