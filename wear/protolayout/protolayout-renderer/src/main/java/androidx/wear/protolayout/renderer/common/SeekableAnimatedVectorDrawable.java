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

package androidx.wear.protolayout.renderer.common;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.res.TypedArrayUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Placeholder SeekableAnimatedVectorDrawable class for temporarily replacing
 * androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SeekableAnimatedVectorDrawable extends Drawable {
    private static final String ANIMATED_VECTOR = "animated-vector";
    static final int[] STYLEABLE_ANIMATED_VECTOR_DRAWABLE = {
            android.R.attr.drawable
    };
    static final int STYLEABLE_ANIMATED_VECTOR_DRAWABLE_DRAWABLE = 0;

    private final Drawable mDrawable;
    private long mTotalDuration = 0;
    private long mCurrentPlayTime = 0;

    SeekableAnimatedVectorDrawable(@NonNull Drawable drawable) {
       mDrawable = drawable;
    }

    @NonNull
    public static SeekableAnimatedVectorDrawable createFromXmlInner(
            @NonNull Resources res,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs,
            @Nullable Resources.Theme theme
    ) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        final int innerDepth = parser.getDepth() + 1;

        // Parse everything until the end of the animated-vector element.
        while (eventType != XmlPullParser.END_DOCUMENT
                && (parser.getDepth() >= innerDepth || eventType != XmlPullParser.END_TAG)) {
            if (eventType == XmlPullParser.START_TAG) {
                final String tagName = parser.getName();
                 if (ANIMATED_VECTOR.equals(tagName)) {
                    final TypedArray a =
                            TypedArrayUtils.obtainAttributes(res, theme, attrs,
                                    STYLEABLE_ANIMATED_VECTOR_DRAWABLE);

                    int drawableRes = a.getResourceId(
                            STYLEABLE_ANIMATED_VECTOR_DRAWABLE_DRAWABLE, 0);
                    Drawable drawable = res.getDrawable(drawableRes, null);
                    a.recycle();

                    return new SeekableAnimatedVectorDrawable(drawable);

                }
            }
            eventType = parser.next();
        }

        throw new XmlPullParserException("no animated-vector tag in the resource");
    }

    public long getTotalDuration() {
        return mTotalDuration;
    }

    public void setCurrentPlayTime(long playTime) {
        mCurrentPlayTime = playTime;
    }

    public long getCurrentPlayTime() {
        return mCurrentPlayTime;
    }

    public void start() {}

    @Override
    public void draw(@NonNull Canvas canvas) {
        mDrawable.draw(canvas);
    }

    @Override
    public void setAlpha(int i) {
        mDrawable.setAlpha(i);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mDrawable.setColorFilter(colorFilter);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getOpacity() {
        return mDrawable.getOpacity();
    }
}
