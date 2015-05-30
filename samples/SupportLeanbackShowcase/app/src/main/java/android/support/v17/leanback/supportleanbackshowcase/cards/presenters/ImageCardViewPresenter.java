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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.supportleanbackshowcase.Constants;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.Utils;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.cards.views.ImageCardViewReplacement;
import android.util.Log;
import android.view.View;

import com.squareup.picasso.Picasso;

import java.util.HashMap;

/**
 * A ImageCardViewPresenter is used to generate Views and bind Objects to them on demand. It
 * contains an {@link ImageCardViewReplacement}.
 */
public class ImageCardViewPresenter extends AbstractCardPresenter<ImageCardViewReplacement> {

    private static final String TAG = "ImageCardViewPresenter";
    private final int mImageWidthInDp;
    private final int mImageHeightDp;
    private final Drawable mLoadingErrorDrawable;
    private final HashMap<Object, Integer> mSelectedColors = new HashMap<Object, Integer>();
    private int mDefaultFooterColor;

    public ImageCardViewPresenter(Context context, int imageWidthInDp, int imageHeightInDp) {
        super(context);
        int color = context.getResources().getColor(R.color.loading_error_card_background);
        mLoadingErrorDrawable = new ColorDrawable(color);

        mDefaultFooterColor = context.getResources()
                                     .getColor(R.color.default_card_footer_background_color);
        mImageWidthInDp = imageWidthInDp;
        mImageHeightDp = imageHeightInDp;
    }

    @Override protected ImageCardViewReplacement onCreateView() {
        if (Constants.LOCAL_LOGD) Log.d(TAG, "onCreateView()");
        final ImageCardViewReplacement cardView = new ImageCardViewReplacement(getContext(),
                                                                               mImageWidthInDp,
                                                                               mImageHeightDp);
        cardView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                udateCardUi(cardView);
            }
        });
        return cardView;
    }

    @Override public void onBindViewHolder(Card card, ImageCardViewReplacement cardView) {
        if (Constants.LOCAL_LOGD) Log.d(TAG, "onBindViewHolder(Card,ImageCardViewReplacement)");
        cardView.setTag(card);

        // Display description iff there is one.
        if (card.getDescription() == null || card.getDescription().length() == 0) {
            cardView.getSecondaryTextView().setVisibility(View.GONE);
            cardView.getPrimaryTextView().setLines(2);
            cardView.getPrimaryTextView().setMaxLines(2);
        } else {
            cardView.getPrimaryTextView().setLines(1);
            cardView.getPrimaryTextView().setMaxLines(1);
            cardView.getSecondaryTextView().setText(card.getDescription());
            cardView.getSecondaryTextView().setVisibility(View.VISIBLE);
        }

        // Display title iff there is one.
        if (card.getTitle() == null || card.getTitle().length() == 0) {
            cardView.getPrimaryTextView().setVisibility(View.GONE);
            cardView.getSecondaryTextView().setLines(2);
            cardView.getSecondaryTextView().setMaxLines(2);
        } else {
            cardView.getSecondaryTextView().setLines(1);
            cardView.getSecondaryTextView().setMaxLines(1);
            cardView.getPrimaryTextView().setText(card.getTitle());
            cardView.getPrimaryTextView().setVisibility(View.VISIBLE);
        }
        // Load main image from an URI or a local resource.
        if (card.getImageURI() != null) {
            Utils.loadImageFromUri(getContext(), card.getImageURI(), cardView.getImageView(),
                                   mImageWidthInDp, mImageHeightDp, true, mLoadingErrorDrawable);
        } else if (card.getLocalImageResourceName() != null) {
            int resourceId = getContext().getResources()
                                         .getIdentifier(card.getLocalImageResourceName(),
                                                        "drawable", getContext().getPackageName());
            Picasso.with(getContext()).load(resourceId).resize(mImageWidthInDp, mImageHeightDp)
                   .centerCrop().into(cardView.getImageView());
        }

        // Load footer icon from a local resource or hide it.
        cardView.getViewById(R.id.container).setVisibility(View.VISIBLE);
        if (card.getFooterLocalImageResourceName() != null) {
            int resourceId = getContext().getResources()
                                         .getIdentifier(card.getFooterLocalImageResourceName(),
                                                        "drawable", getContext().getPackageName());
            Picasso.with(getContext()).load(resourceId).into(cardView.getIconView());
            cardView.getIconView().setVisibility(View.VISIBLE);
        } else {
            if (card.getDescription() == null || card.getDescription().isEmpty()) {
                cardView.getViewById(R.id.container).setVisibility(View.GONE);
            }
            cardView.getIconView().setVisibility(View.GONE);
        }

        // Update background color depending on the card's focused state.
        udateCardUi(cardView);
    }

    private void udateCardUi(ImageCardViewReplacement view) {
        int color = mDefaultFooterColor;

        if (view.getTag() != null) {
            Card card = (Card) view.getTag();
            if (card.getSelectedColor() != -1 && view.isSelected()) {
                color = card.getSelectedColor();
            }
            if (card.getFooterColor() != -1) {
                color = card.getFooterColor();
            }
        }
        view.getInfoBoxView().setBackgroundColor(color);
    }
}
