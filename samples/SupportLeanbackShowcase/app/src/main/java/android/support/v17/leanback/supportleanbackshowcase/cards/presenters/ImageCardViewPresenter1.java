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
import android.support.v17.leanback.supportleanbackshowcase.Constants;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.cards.views.ImageCardViewReplacement;
import android.support.v17.leanback.widget.ImageCardView;
import android.util.Log;

/**
 * A ImageCardViewPresenter is used to generate Views and bind Objects to them on demand. It
 * contains an {@link ImageCardViewReplacement}.
 */
public class ImageCardViewPresenter1 extends AbstractCardPresenter<ImageCardView> {

    private static final String TAG = "ImageCardViewPresenter";
    private final int mImageWidthInDp;
    private final int mImageHeightDp;

    public ImageCardViewPresenter1(Context context, int imageWidthInDp, int imageHeightInDp) {
        super(context);
        mImageWidthInDp = imageWidthInDp;
        mImageHeightDp = imageHeightInDp;
    }

    @Override protected ImageCardView onCreateView() {
        if (Constants.LOCAL_LOGD) Log.d(TAG, "onCreateView()");

        final ImageCardView cardView = new ImageCardView(getContext(), R.style.ImageCardViewColoredTextStyle);
        cardView.setMainImageDimensions(mImageWidthInDp, mImageHeightDp);
        cardView.setFocusable(true);
        return cardView;
    }

    @Override public void onBindViewHolder(Card card, final ImageCardView cardView) {
        if (Constants.LOCAL_LOGD) Log.d(TAG, "onBindViewHolder(Card,ImageCardViewReplacement)");
        cardView.setTag(card);
        cardView.setTitleText(card.getTitle());
        //cardView.setContentText("Hello");
        if (card.getLocalImageResourceName() != null) {
            int resourceId = getContext().getResources()
                                         .getIdentifier(card.getLocalImageResourceName(),
                                                        "drawable", getContext().getPackageName());
            cardView.getMainImageView().setImageResource(resourceId);
        }

    }

}
