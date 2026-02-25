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
package androidx.compose.remote.integration.view.demos.examples.old;

import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;

import android.annotation.SuppressLint;

import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.modifiers.RecordingModifier;

import org.jspecify.annotations.NonNull;

/**
 * A custom scroller element for RecordingModifier.
 */
@SuppressLint("RestrictedApiAndroidX")
public class CustomScroller implements RecordingModifier.Element {

    /** Vertical scroll direction. */
    public static final int VERTICAL = 0;
    /** Horizontal scroll direction. */
    public static final int HORIZONTAL = 1;
    private final float mScrollPosition;
    private final float mTouchPosition;

    int mDirection;
    int mNotches;
    float mPositionId;
    CustomTouch mCustom;
    float mMax;

    /**
     * Interface for custom touch logic.
     */
    public interface CustomTouch {
        /**
         * Calculates the touch value.
         * @param max the maximum scroll value.
         * @param notchMax the maximum notch value.
         * @return the touch value.
         */
        float touch(float max, float notchMax);
    }

    /**
     * Creates a new CustomScroller.
     * @param direction the scroll direction (VERTICAL or HORIZONTAL).
     * @param touchPosition the touch position variable ID.
     * @param scrollPosition the scroll position variable ID.
     * @param notches the number of notches.
     * @param max the maximum scroll value.
     */
    public CustomScroller(int direction, float touchPosition, float scrollPosition, int notches,
            float max) {
        this.mDirection = direction;
        mMax = max;
        mNotches = notches;
        mScrollPosition = scrollPosition;
        mTouchPosition = touchPosition;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        addModifierCustomScroll(writer, mDirection, mScrollPosition, mTouchPosition, mNotches,
                mMax);
    }

    /**
     * Adds a custom scroll modifier to the writer.
     * @param writer the writer.
     * @param direction the scroll direction.
     * @param scrollPosition the scroll position variable ID.
     * @param touchPosition the touch position variable ID.
     * @param notches the number of notches.
     * @param max the maximum scroll value.
     */
    public void addModifierCustomScroll(@NonNull RemoteComposeWriter writer,
            int direction, float scrollPosition, float touchPosition, int notches, float max) {
        //float max = this.reserveFloatVariable();
        float notchMax = writer.reserveFloatVariable();
        float touchExpressionDirection =
                direction != 0 ? RemoteContext.FLOAT_TOUCH_POS_X : RemoteContext.FLOAT_TOUCH_POS_Y;

        ScrollModifierOperation.apply(writer.getBuffer().getBuffer(), direction, scrollPosition,
                max, notchMax);

        writer.getBuffer().addTouchExpression(
                Utils.idFromNan(touchPosition),
                0f,
                Float.NaN,
                notches + 1,
                0f,
                3,
                new float[]{
                        touchExpressionDirection, max, DIV, notches + 1, MUL, -1, MUL
                },
                TouchExpression.STOP_NOTCHES_EVEN,
                new float[]{notches + 1},
                writer.easing(0.5f, 10f, 0.1f));
        writer.getBuffer().addContainerEnd();
    }
}
