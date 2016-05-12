/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.support.v17.leanback.supportleanbackshowcase.app.page;

import android.content.Context;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.cards.presenters.ImageCardViewPresenter;
import android.support.v17.leanback.widget.ImageCardView;
import android.view.View;
import android.widget.ImageView;

/**
 * Simple presenter implementation to represent settings icon as cards.
 */
public class SettingsIconPresenter extends ImageCardViewPresenter {

    public SettingsIconPresenter(Context context) {
        super(context, R.style.IconCardTheme);
    }

    @Override
    protected ImageCardView onCreateView() {
        final ImageCardView imageCardView = super.onCreateView();
        imageCardView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setImageBackground(imageCardView, R.color.settings_card_background_focussed);
                } else {
                    setImageBackground(imageCardView, R.color.settings_card_background);
                }
            }
        });
        setImageBackground(imageCardView, R.color.settings_card_background);
        return imageCardView;
    }

    private void setImageBackground(ImageCardView imageCardView, int colorId) {
        imageCardView.setBackgroundColor(getContext().getResources().getColor(colorId));
    }
}
