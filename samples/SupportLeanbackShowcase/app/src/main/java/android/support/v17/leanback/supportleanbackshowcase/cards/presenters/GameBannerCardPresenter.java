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

package android.support.v17.leanback.supportleanbackshowcase.cards.presenters;

import android.content.Context;
import android.graphics.Color;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.cards.views.ImageCardViewReplacement;
import android.widget.TextView;

/**
 * This presenter will display the cards representing a gamer banners. It inherits from {@link
 * ImageCardViewPresenter} and will set a footer icon as well as a secondary text color.
 */
public class GameBannerCardPresenter extends ImageCardViewPresenter {

    private static final String TAG = "GameBannerCardPresenter";

    public GameBannerCardPresenter(Context context) {
        super(context,
              (int) context.getResources().getDimension(R.dimen.wide_short_image_card_width),
              (int) context.getResources().getDimension(R.dimen.wide_short_image_card_height));
    }

    @Override protected ImageCardViewReplacement onCreateView() {
        ImageCardViewReplacement cardView = super.onCreateView();
        TextView secondaryText = cardView.getSecondaryTextView();
        secondaryText.setTextColor(Color.parseColor("#80c349"));
        return cardView;
    }

}
