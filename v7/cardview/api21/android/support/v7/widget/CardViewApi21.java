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
package android.support.v7.widget;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.support.v7.cardview.R;

class CardViewApi21 implements CardViewImpl {

    @Override
    public void initialize(CardViewDelegate cardView, Context context, int backgroundColor,
            float radius, float elevation, float maxElevation/*ignored*/) {
        cardView.setBackgroundDrawable(new RoundRectDrawable(backgroundColor, radius));
        View view = (View) cardView;
        view.setClipToOutline(true);
        view.setElevation(elevation);
    }

    @Override
    public void setRadius(CardViewDelegate cardView, float radius) {
        ((RoundRectDrawable) (cardView.getBackground())).setRadius(radius);
    }

    @Override
    public void initStatic() {
    }

    @Override
    public void setMaxElevation(CardViewDelegate cardView, float maxElevation) {
        // no op
    }

    @Override
    public float getMaxElevation(CardViewDelegate cardView) {
        return 0;
    }

    @Override
    public float getMinWidth(CardViewDelegate cardView) {
        return getRadius(cardView) * 2;
    }

    @Override
    public float getMinHeight(CardViewDelegate cardView) {
        return getRadius(cardView) * 2;
    }

    @Override
    public float getRadius(CardViewDelegate cardView) {
        return ((RoundRectDrawable) (cardView.getBackground())).getRadius();
    }

    @Override
    public void setElevation(CardViewDelegate cardView, float elevation) {
        ((View) cardView).setElevation(elevation);
    }

    @Override
    public float getElevation(CardViewDelegate cardView) {
        return ((View) cardView).getElevation();
    }

}