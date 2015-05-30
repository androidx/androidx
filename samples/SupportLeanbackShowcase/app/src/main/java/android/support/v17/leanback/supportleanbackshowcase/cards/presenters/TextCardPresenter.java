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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.cards.views.BaseCardViewEx;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * The Presenter displays a card consisting of text as a replacement for a big image. The footer is
 * also quite unique since it does contain two images rather than one or non.
 */
public class TextCardPresenter extends AbstractCardPresenter<BaseCardViewEx> {

    private static final String TAG = "AbstractFooterCardPresenter";

    public TextCardPresenter(Context context) {
        super(context);
    }

    @Override protected BaseCardViewEx onCreateView() {
        BaseCardViewEx cardView = new BaseCardViewEx(getContext());
        LayoutInflater.from(getContext()).inflate(R.layout.text_icon_card, cardView);
        LayoutInflater.from(getContext()).inflate(R.layout.text_icon_card_footer, cardView);
        return cardView;
    }

    @Override public void onBindViewHolder(Card card, BaseCardViewEx cardView) {
        TextView extraText = cardView.getViewById(R.id.extra_text);
        TextView primaryText = cardView.getViewById(R.id.primary_text);
        ImageView footerIcon = cardView.getViewById(R.id.footer_icon);
        final ImageView imageView = cardView.getViewById(R.id.main_image);

        extraText.setText(card.getExtraText());
        primaryText.setText(card.getTitle());
        if (card.getLocalImageResourceName() != null) {
            int width = (int) getContext().getResources()
                                          .getDimension(R.dimen.sidetext_image_card_width);
            int height = (int) getContext().getResources()
                                           .getDimension(R.dimen.sidetext_image_card_height);
            int resourceId = card.getLocalImageResourceId(getContext());
            // TODO: hahnr@ load the image without Picasso
            Picasso.with(getContext()).load(resourceId).resize(width, height).centerCrop()
                   .into(new Target() {

                       @Override
                       public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                           RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory
                                   .create(getContext().getResources(), bitmap);
                           drawable.setCornerRadius(
                                   Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);
                           imageView.setImageDrawable(drawable);
                       }

                       @Override public void onBitmapFailed(Drawable errorDrawable) {
                       }

                       @Override public void onPrepareLoad(Drawable placeHolderDrawable) {
                       }
                   });
        }
        footerIcon.setImageResource(R.drawable.stars_white);
    }

}
