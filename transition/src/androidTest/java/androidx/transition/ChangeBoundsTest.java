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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import android.support.test.filters.MediumTest;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.transition.test.R;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

@MediumTest
public class ChangeBoundsTest extends BaseTransitionTest {

    @Override
    Transition createTransition() {
        final ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(400);
        changeBounds.setInterpolator(new LinearInterpolator());
        return changeBounds;
    }

    @Test
    public void testResizeClip() {
        ChangeBounds changeBounds = (ChangeBounds) mTransition;
        assertThat(changeBounds.getResizeClip(), is(false));
        changeBounds.setResizeClip(true);
        assertThat(changeBounds.getResizeClip(), is(true));
    }

    @Test
    public void testBasic() throws Throwable {
        enterScene(R.layout.scene1);
        final ViewHolder startHolder = new ViewHolder(rule.getActivity());
        assertThat(startHolder.red, is(atTop()));
        assertThat(startHolder.green, is(below(startHolder.red)));
        startTransition(R.layout.scene6);
        waitForEnd();
        final ViewHolder endHolder = new ViewHolder(rule.getActivity());
        assertThat(endHolder.green, is(atTop()));
        assertThat(endHolder.red, is(below(endHolder.green)));
    }

    private static TypeSafeMatcher<View> atTop() {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                return view.getTop() == 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is placed at the top of its parent");
            }
        };
    }

    private static TypeSafeMatcher<View> below(final View other) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return other.getBottom() == item.getTop();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is placed below the specified view");
            }
        };
    }

    private static class ViewHolder {

        public final View red;
        public final View green;

        ViewHolder(TransitionActivity activity) {
            red = activity.findViewById(R.id.redSquare);
            green = activity.findViewById(R.id.greenSquare);
        }
    }

}
