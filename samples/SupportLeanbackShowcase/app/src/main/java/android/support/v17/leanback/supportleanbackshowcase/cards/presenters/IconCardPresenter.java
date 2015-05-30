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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * This Presenter will display cards which consists of a single icon which will be highlighted by a
 * surrounding circle when the card is focused. AndroidTV uses these cards for entering settings
 * menu.
 */
public class IconCardPresenter extends AbstractCardPresenter<BaseCardViewEx> {

    private static final String TAG = "IconCardPresenter";

    public IconCardPresenter(Context context) {
        super(context);
    }

    @Override protected BaseCardViewEx onCreateView() {
        final BaseCardViewEx cardView = new BaseCardViewEx(getContext());
        LayoutInflater.from(getContext()).inflate(R.layout.icon_card, cardView);
        LayoutInflater.from(getContext()).inflate(R.layout.icon_card_footer, cardView);
        cardView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) cardView.getViewById(R.id.container)
                                      .setBackgroundResource(R.drawable.icon_focused);
                else cardView.getViewById(R.id.container).setBackground(null);
            }
        });
        return cardView;
    }

    @Override public void onBindViewHolder(Card card, BaseCardViewEx cardView) {
        TextView primaryText = cardView.getViewById(R.id.primary_text);
        ImageView imageView = cardView.getViewById(R.id.main_image);

        primaryText.setText(card.getTitle());
        if (card.getLocalImageResourceName() != null) {
            int resourceId = card.getLocalImageResourceId(getContext());
            Picasso.with(getContext()).load(resourceId).into(imageView);
        }
    }

}
