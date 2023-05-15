/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.core.text.method;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.os.BuildCompat;

/**
 * Backwards compatible version of {@link LinkMovementMethod} which fixes the issue that links can
 * be triggered for touches outside of line bounds before Android V.
 */
public class LinkMovementMethodCompat extends LinkMovementMethod {
    private static LinkMovementMethodCompat sInstance;

    private LinkMovementMethodCompat() {}

    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @Override
    public boolean onTouchEvent(@Nullable TextView widget, @Nullable Spannable buffer,
            @Nullable MotionEvent event) {
        if (!BuildCompat.isAtLeastV()) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                boolean isOutOfLineBounds;
                if (y < 0 || y > layout.getHeight()) {
                    isOutOfLineBounds = true;
                } else {
                    int line = layout.getLineForVertical(y);
                    isOutOfLineBounds = x < layout.getLineLeft(line)
                            || x > layout.getLineRight(line);
                }

                if (isOutOfLineBounds) {
                    Selection.removeSelection(buffer);

                    // The same as super.onTouchEvent() in LinkMovementMethod.onTouchEvent(), i.e.
                    // ScrollingMovementMethod.onTouchEvent().
                    return Touch.onTouchEvent(widget, buffer, event);
                }
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    /**
     * Retrieves the singleton instance of {@link LinkMovementMethodCompat}.
     *
     * @return the singleton instance of {@link LinkMovementMethodCompat}
     */
    @NonNull
    public static LinkMovementMethodCompat getInstance() {
        if (sInstance == null) {
            sInstance = new LinkMovementMethodCompat();
        }

        return sInstance;
    }
}
