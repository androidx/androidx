/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v17.leanback.widget;


import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

public class SearchOrbView extends FrameLayout implements View.OnClickListener {
    private OnClickListener mListener;
    private View mSearchOrbView;
    private final float mFocusedZoom;

    public SearchOrbView(Context context) {
        this(context, null);
    }

    public SearchOrbView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchOrbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inflater.inflate(R.layout.lb_search_orb, this, true);
        mSearchOrbView = root.findViewById(R.id.search_orb);

        // By default we are not visible
        setVisibility(INVISIBLE);
        setFocusable(true);
        setClipChildren(false);

        mFocusedZoom = context.getResources().getFraction(
                R.fraction.lb_search_orb_focused_zoom, 1, 1);

        setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (null != mListener) {
            mListener.onClick(view);
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        final float zoom = gainFocus ? mFocusedZoom : 1f;
        mSearchOrbView.animate().scaleX(zoom).scaleY(zoom).setDuration(200).start();
    }

    /**
     * Set the on click listener for the orb
     * @param listener The listener.
     */
    public void setOnOrbClickedListener(OnClickListener listener) {
        mListener = listener;
        if (null != listener) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.INVISIBLE);
        }
    }

    public void setOrbColor(int color) {
        if (mSearchOrbView.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) mSearchOrbView.getBackground()).setColor(color);
        }
    }
}
