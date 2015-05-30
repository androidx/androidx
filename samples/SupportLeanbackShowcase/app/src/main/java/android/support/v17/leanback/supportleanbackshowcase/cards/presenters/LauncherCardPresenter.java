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

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.Utils;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.cards.views.ImageCardViewReplacement;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

/**
 * THis Presenter displays an expandable card which is used e.g. in the AndroidTV launcher. Once
 * such a card gets focused it expands and will show more details of the image.
 */
public class LauncherCardPresenter extends ImageCardViewPresenter {

    private static final String TAG = "LauncherCardPresenter";

    public LauncherCardPresenter(Context context) {
        super(context, 1 /* val > 0 required by Picasso */,
              (int) context.getResources().getDimension(R.dimen.default_image_card_height));
    }

    @Override protected ImageCardViewReplacement onCreateView() {
        ImageCardViewReplacement cardView = super.onCreateView();
        final ImageView imageView = cardView.getImageView();
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cardView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, final boolean hasFocus) {
                int expandedWidth = (int) getContext().getResources().getDimension(
                        R.dimen.default_image_card_width);
                int collapsedWidth = (int) getContext().getResources().getDimension(
                        R.dimen.default_image_card_height);

                expandedWidth = collapsedWidth;

                ValueAnimator animator = new ValueAnimator();
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setIntValues(hasFocus ? collapsedWidth : expandedWidth,
                                      hasFocus ? expandedWidth : collapsedWidth);
                animator.setDuration(500);
                animator.setStartDelay(0);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override public void onAnimationUpdate(ValueAnimator animation) {
                        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                        layoutParams.width = (Integer) animation.getAnimatedValue();
                        imageView.setLayoutParams(layoutParams);
                    }
                });
                animator.start();
            }
        });
        return cardView;
    }

    @Override public void onBindViewHolder(Card card, ImageCardViewReplacement cardView) {
        super.onBindViewHolder(card, cardView);

        ImageView imageView = cardView.getImageView();
        cardView.setTag(card);
        int width = (int) getContext().getResources()
                                      .getDimension(R.dimen.default_image_card_height);
        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        layoutParams.width = Utils.convertDpToPixel(getContext(), width);
        imageView.setLayoutParams(layoutParams);


        if (card.getLocalImageResourceName() != null) {
            int height = (int) getContext().getResources()
                                           .getDimension(R.dimen.sidetext_image_card_height);
            int resourceId = getContext().getResources()
                                         .getIdentifier(card.getLocalImageResourceName(),
                                                        "drawable", getContext().getPackageName());
            Picasso.with(getContext()).load(resourceId).into(imageView);
        }
    }

}
