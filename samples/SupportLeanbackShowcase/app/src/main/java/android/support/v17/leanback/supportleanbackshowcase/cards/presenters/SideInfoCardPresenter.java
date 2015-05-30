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
import android.support.v17.leanback.supportleanbackshowcase.cards.views.BaseCardViewEx;
import android.support.v17.leanback.supportleanbackshowcase.cards.views.OnActivateStateChangeHandler;
import android.support.v17.leanback.widget.BaseCardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * This Presenter will display a card consisting of an image on the left side of the card followed
 * by text on the right side. The image and text have equal width. The text will work like a info
 * box, thus it will be hidden if the parent row is inactive. This behavior is unique to this card
 * and requires a special focus handler.
 */
public class SideInfoCardPresenter extends AbstractCardPresenter<BaseCardViewEx> implements
        OnActivateStateChangeHandler {

    private static final String TAG = "SideInfoCardPresenter";

    public SideInfoCardPresenter(Context context) {
        super(context);
    }

    @Override protected BaseCardViewEx onCreateView() {
        BaseCardViewEx cardView = new BaseCardViewEx(getContext());
        cardView.setCardType(BaseCardView.CARD_TYPE_MAIN_ONLY);
        cardView.addView(LayoutInflater.from(getContext()).inflate(R.layout.side_info_card, null));
        cardView.setOnActivateStateChangeHandler(this);
        onActivateStateChanged(cardView, cardView.isActivated());
        return cardView;
    }

    @Override public void onBindViewHolder(Card card, BaseCardViewEx cardView) {
        ImageView imageView = cardView.getViewById(R.id.main_image);
        if (card.getLocalImageResourceName() != null) {
            int width = (int) getContext().getResources()
                                          .getDimension(R.dimen.sidetext_image_card_width);
            int height = (int) getContext().getResources()
                                           .getDimension(R.dimen.sidetext_image_card_height);
            int resourceId = getContext().getResources()
                                         .getIdentifier(card.getLocalImageResourceName(),
                                                        "drawable", getContext().getPackageName());
            Picasso.with(getContext()).load(resourceId).resize(width, height).centerCrop()
                   .into(imageView);
        }

        TextView primaryText = cardView.getViewById(R.id.primary_text);
        primaryText.setText(card.getTitle());

        TextView secondaryText = cardView.getViewById(R.id.secondary_text);
        secondaryText.setText(card.getDescription());

        TextView extraText = cardView.getViewById(R.id.extra_text);
        extraText.setText(card.getExtraText());
    }

    @Override public void onActivateStateChanged(final BaseCardViewEx cardView, boolean activated) {
        cardView.getViewById(R.id.info).setVisibility(activated ? View.VISIBLE : View.GONE);
    }
}
