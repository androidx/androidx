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
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.cards.views.ImageCardViewReplacement;
import android.view.Gravity;

/**
 * This Presenter inherits from ImageCardViewPresenter and will set the secondary text alignment as
 * well as a footer icon to display the movie's rating.
 */
public class MovieRatingCardPresenter extends ImageCardViewPresenter {

    private static final String TAG = "MovieRatingCardPresenter";

    public MovieRatingCardPresenter(Context context) {
        super(context, (int) context.getResources().getDimension(R.dimen.thin_image_card_width),
              (int) context.getResources().getDimension(R.dimen.thin_image_card_height));
    }

    @Override public void onBindViewHolder(Card card, ImageCardViewReplacement cardView) {
        super.onBindViewHolder(card, cardView);
        cardView.getPrimaryTextView().setLines(2);
        cardView.getPrimaryTextView().setMaxLines(2);
        cardView.getSecondaryTextView().setGravity(Gravity.RIGHT);
    }
}
