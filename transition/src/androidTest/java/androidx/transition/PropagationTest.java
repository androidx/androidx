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
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.support.test.filters.MediumTest;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.transition.test.R;

import org.junit.Test;

@MediumTest
public class PropagationTest extends BaseTransitionTest {

    @Test
    public void testCircularPropagation() throws Throwable {
        enterScene(R.layout.scene10);
        CircularPropagation propagation = new CircularPropagation();
        mTransition.setPropagation(propagation);
        final TransitionValues redValues = new TransitionValues();
        redValues.view = mRoot.findViewById(R.id.redSquare);
        propagation.captureValues(redValues);

        // Only the reported propagation properties are set
        for (String prop : propagation.getPropagationProperties()) {
            assertTrue(redValues.values.keySet().contains(prop));
        }
        assertEquals(propagation.getPropagationProperties().length, redValues.values.size());

        // check the visibility
        assertEquals(View.VISIBLE, propagation.getViewVisibility(redValues));
        assertEquals(View.GONE, propagation.getViewVisibility(null));

        // Check the positions
        int[] pos = new int[2];
        redValues.view.getLocationOnScreen(pos);
        pos[0] += redValues.view.getWidth() / 2;
        pos[1] += redValues.view.getHeight() / 2;
        assertEquals(pos[0], propagation.getViewX(redValues));
        assertEquals(pos[1], propagation.getViewY(redValues));

        mTransition.setEpicenterCallback(new Transition.EpicenterCallback() {
            @Override
            public Rect onGetEpicenter(@NonNull Transition transition) {
                return new Rect(0, 0, redValues.view.getWidth(), redValues.view.getHeight());
            }
        });

        long redDelay = getDelay(R.id.redSquare);
        // red square's delay should be roughly 0 since it is at the epicenter
        assertEquals(0f, redDelay, 30f);

        // The green square is on the upper-right
        long greenDelay = getDelay(R.id.greenSquare);
        assertTrue(greenDelay < redDelay);

        // The blue square is on the lower-right
        long blueDelay = getDelay(R.id.blueSquare);
        assertTrue(blueDelay < greenDelay);

        // Test propagation speed
        propagation.setPropagationSpeed(1000000000f);
        assertEquals(0, getDelay(R.id.blueSquare));
    }

    private TransitionValues capturePropagationValues(int viewId) {
        TransitionValues transitionValues = new TransitionValues();
        transitionValues.view = mRoot.findViewById(viewId);
        TransitionPropagation propagation = mTransition.getPropagation();
        assertNotNull(propagation);
        propagation.captureValues(transitionValues);
        return transitionValues;
    }

    private long getDelay(int viewId) {
        TransitionValues transitionValues = capturePropagationValues(viewId);
        TransitionPropagation propagation = mTransition.getPropagation();
        assertNotNull(propagation);
        return propagation.getStartDelay(mRoot, mTransition, transitionValues, null);
    }

}
