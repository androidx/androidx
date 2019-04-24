/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

class MusicView extends ViewGroup {
    private MusicViewType mType = MusicViewType.WITHOUT_TITLE;
    private View mWithTitleLandscape;
    private View mWithTitlePortrait;
    private View mWithoutTitle;

    MusicView(@NonNull Context context) {
        super(context);

        inflateLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)) {
            throw new AssertionError("MusicView should be measured in MeasureSpec.EXACTLY");
        }

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        if (width > height) {
            mType = MusicViewType.WITH_TITLE_LANDSCAPE;
            mWithTitleLandscape.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
            if (hasTooSmallMeasuredState(mWithTitleLandscape)
                    || mWithTitleLandscape.getMeasuredWidth() > width) {
                mType = MusicViewType.WITHOUT_TITLE;
            }
        } else {
            mType = MusicViewType.WITH_TITLE_PORTRAIT;
            mWithTitlePortrait.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
            if (hasTooSmallMeasuredState(mWithTitlePortrait)
                    || mWithTitlePortrait.getMeasuredHeight() > height) {
                mType = MusicViewType.WITHOUT_TITLE;
            }
        }

        if (mType == MusicViewType.WITHOUT_TITLE) {
            mWithoutTitle.measure(MeasureSpec.makeMeasureSpec(width / 2, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height / 2, MeasureSpec.EXACTLY));
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View view;
        if (mType == MusicViewType.WITH_TITLE_LANDSCAPE) {
            view = mWithTitleLandscape;
        } else if (mType == MusicViewType.WITH_TITLE_PORTRAIT) {
            view = mWithTitlePortrait;
        } else {
            view = mWithoutTitle;
        }

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child == view) {
                child.setVisibility(View.VISIBLE);
            } else {
                child.setVisibility(View.INVISIBLE);
            }
        }

        final int parentWidth = right - left;
        final int parentHeight = bottom - top;

        final int width = view.getMeasuredWidth();
        final int height = view.getMeasuredHeight();

        final int childLeft = (parentWidth - width) / 2;
        final int childTop = (parentHeight - height) / 2;

        view.layout(childLeft, childTop, childLeft + width, childTop + height);
    }

    void setAlbumDrawable(Drawable album) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            ImageView albumView = getChildAt(i).findViewById(R.id.album);
            if (albumView != null) {
                albumView.setImageDrawable(album);
            }
        }
    }

    void setTitleText(String title) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            TextView titleView = getChildAt(i).findViewById(R.id.title);
            if (titleView != null) {
                titleView.setText(title);
            }
        }
    }

    void setArtistText(String artist) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            TextView artistView = getChildAt(i).findViewById(R.id.artist);
            if (artistView != null) {
                artistView.setText(artist);
            }
        }
    }

    private void inflateLayout() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mWithTitleLandscape = inflater.inflate(R.layout.music_with_title_landscape, null);
        mWithTitlePortrait = inflater.inflate(R.layout.music_with_title_portrait, null);
        mWithoutTitle = inflater.inflate(R.layout.music_without_title, null);

        addView(mWithTitleLandscape);
        addView(mWithTitlePortrait);
        addView(mWithoutTitle);
    }

    private static boolean hasTooSmallMeasuredState(@NonNull View view) {
        return ((view.getMeasuredWidthAndState() & MEASURED_STATE_TOO_SMALL)
                | (view.getMeasuredHeightAndState() & MEASURED_STATE_TOO_SMALL)) != 0;
    }

    private enum MusicViewType { WITH_TITLE_LANDSCAPE, WITH_TITLE_PORTRAIT, WITHOUT_TITLE }
}
