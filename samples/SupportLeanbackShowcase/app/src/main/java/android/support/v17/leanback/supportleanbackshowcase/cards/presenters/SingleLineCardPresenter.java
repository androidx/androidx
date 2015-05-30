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
import android.support.v17.leanback.widget.BaseCardView;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This Presenter will display a card which consists of a big image followed by a colored footer.
 * Not only the colored footer is unique to this card, but also it's footer (info) will be visible
 * even when its parent row is inactive.
 */
public class SingleLineCardPresenter extends AbstractCardPresenter<BaseCardViewEx> {

    private static final String TAG = "SingleLineCardPresenter";

    public SingleLineCardPresenter(Context context) {
        super(context);
    }

    @Override protected BaseCardViewEx onCreateView() {
        BaseCardViewEx cardView = new BaseCardViewEx(getContext());
        cardView.setCardType(BaseCardView.CARD_TYPE_MAIN_ONLY);
        cardView.addView(
                LayoutInflater.from(getContext()).inflate(R.layout.single_line_card_footer, null));
        return cardView;
    }

    @Override public void onBindViewHolder(Card card, BaseCardViewEx cardView) {
        TextView primaryText = cardView.getViewById(R.id.primary_text);
        primaryText.setText(card.getTitle());

        int resourceId = getContext().getResources()
                                     .getIdentifier(card.getLocalImageResourceName(), "drawable",
                                                    getContext().getPackageName());
        ImageView mainImage = cardView.getViewById(R.id.main_image);
        mainImage.setImageResource(resourceId);

        cardView.getViewById(R.id.container).setBackgroundColor(card.getFooterColor());
    }

}
