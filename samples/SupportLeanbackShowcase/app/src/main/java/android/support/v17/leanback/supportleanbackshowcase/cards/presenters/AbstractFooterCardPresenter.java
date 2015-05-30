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
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.Utils;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.cards.views.FooterLayoutCardView;
import android.support.v17.leanback.widget.Presenter;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

/**
 * A custom and abstract card {@link Presenter} which allows to create a card consisting of an image
 * and a custom footer. The footer is passed as a layout and will be inflated automatically.
 */
public abstract class AbstractFooterCardPresenter extends
        AbstractCardPresenter<FooterLayoutCardView> {

    private static final String TAG = "AbstractFooterCardPresenter";
    private final Drawable mLoadingErrorDrawable;
    private final int mFooterLayoutd;
    private final int mImageWidth;
    private final int mImageHeight;

    /**
     * @param context Used to retrieve default values such as error drawables.
     * @param footerLayoutId The layout which represents the footer.
     * @param imageWidth The width of the card's main image. This width defines the card's width
     * too.
     * @param imageHeight The height of the card's main image. The card's height will be
     * <code>imageHeight + footerHeight</code>
     */
    public AbstractFooterCardPresenter(Context context, int footerLayoutId, int imageWidth,
                                       int imageHeight) {
        super(context);
        mFooterLayoutd = footerLayoutId;
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;

        // In case the image could not be fetched from the server we want to make sure something is show. In this case, a solid color.
        int color = context.getResources().getColor(R.color.loading_error_card_background);
        mLoadingErrorDrawable = new ColorDrawable(color);
    }

    @Override public final FooterLayoutCardView onCreateView() {
        FooterLayoutCardView cardView = new FooterLayoutCardView(getContext(), mFooterLayoutd,
                                                                 mImageWidth, mImageHeight);
        onViewCreated(cardView);
        return cardView;
    }

    @Override public void onBindViewHolder(Card card, FooterLayoutCardView cardView) {
        // Load the card's image. This can be either an image on a remote server or a local one stored in the resources.
        ImageView imageView = cardView.getImageView();
        if (card.getImageURI() != null) {
            Utils.loadImageFromUri(getContext(), card.getImageURI(), imageView, mImageWidth,
                                   mImageHeight, true, mLoadingErrorDrawable);
        } else if (card.getLocalImageResourceName() != null) {
            int resourceId = getContext().getResources()
                                         .getIdentifier(card.getLocalImageResourceName(),
                                                        "drawable", getContext().getPackageName());
            Picasso.with(getContext()).load(resourceId).resize(mImageWidth, mImageHeight)
                   .centerCrop().into(imageView);
        }
    }

    /**
     * Override this method to react to creations of new card views.
     *
     * @param cardView The view which has been created.
     * @see Presenter#onCreateViewHolder(ViewGroup)
     */
    public void onViewCreated(FooterLayoutCardView cardView) {
        // Nothing to clean up. Override if necessary.
    }

}
