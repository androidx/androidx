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
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.transition.test.R;

import org.junit.Test;

@MediumTest
public class ChangeClipBoundsTest extends BaseTransitionTest {

    @Override
    Transition createTransition() {
        return new ChangeClipBounds();
    }

    @SdkSuppress(minSdkVersion = 18)
    @Test
    public void testChangeClipBounds() throws Throwable {
        enterScene(R.layout.scene1);

        final View redSquare = rule.getActivity().findViewById(R.id.redSquare);
        final Rect newClip = new Rect(redSquare.getLeft() + 10, redSquare.getTop() + 10,
                redSquare.getRight() - 10, redSquare.getBottom() - 10);

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertNull(ViewCompat.getClipBounds(redSquare));
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                ViewCompat.setClipBounds(redSquare, newClip);
            }
        });
        waitForStart();
        Thread.sleep(150);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Rect midClip = ViewCompat.getClipBounds(redSquare);
                assertNotNull(midClip);
                assertTrue(midClip.left > 0 && midClip.left < newClip.left);
                assertTrue(midClip.top > 0 && midClip.top < newClip.top);
                assertTrue(midClip.right < redSquare.getRight() && midClip.right > newClip.right);
                assertTrue(midClip.bottom < redSquare.getBottom()
                        && midClip.bottom > newClip.bottom);
            }
        });
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
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                ViewCompat.setClipBounds(redSquare, null);
            }
        });
        waitForStart();
        Thread.sleep(150);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Rect midClip = ViewCompat.getClipBounds(redSquare);
                assertNotNull(midClip);
                assertTrue(midClip.left > 0 && midClip.left < newClip.left);
                assertTrue(midClip.top > 0 && midClip.top < newClip.top);
                assertTrue(midClip.right < redSquare.getRight() && midClip.right > newClip.right);
                assertTrue(midClip.bottom < redSquare.getBottom()
                        && midClip.bottom > newClip.bottom);
            }
        });
        waitForEnd();

        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertNull(ViewCompat.getClipBounds(redSquare));
            }
        });

    }

    @Test
    public void dummy() {
        // Avoid "No tests found" on older devices
    }

}
