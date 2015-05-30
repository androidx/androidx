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
import android.graphics.BitmapFactory;
import android.support.v17.leanback.supportleanbackshowcase.Constants;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.cards.views.BaseCardViewEx;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This Presenter is used to display the characters card row in the DetailView examples.
 */
public class CharacterCardPresenter extends AbstractCardPresenter<BaseCardViewEx> {

    private static final String TAG = "CharacterCardPresenter";
    private final SparseArray<RoundedBitmapDrawable> mImageCache = new SparseArray<RoundedBitmapDrawable>();

    public CharacterCardPresenter(Context context) {
        super(context);
    }

    @Override protected BaseCardViewEx onCreateView() {
        final BaseCardViewEx cardView = new BaseCardViewEx(getContext());
        LayoutInflater.from(getContext()).inflate(R.layout.character_card, cardView);
        cardView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (Constants.LOCAL_LOGD) Log.d(TAG, "onFocusChanged(" + hasFocus + ")");
                ImageView mainImage = cardView.getViewById(R.id.main_image);
                View container = cardView.getViewById(R.id.container);
                if (hasFocus) {
                    container.setBackgroundResource(R.drawable.character_focused);
                    mainImage.setBackgroundResource(R.drawable.character_focused);
                } else {
                    container.setBackgroundResource(R.drawable.character_not_focused_padding);
                    mainImage.setBackgroundResource(R.drawable.character_not_focused);
                }
            }
        });
        return cardView;
    }

    @Override public void onBindViewHolder(Card card, BaseCardViewEx cardView) {
        if (Constants.LOCAL_LOGD) Log.d(TAG, "onBindViewHolder");
        TextView primaryText = cardView.getViewById(R.id.primary_text);
        final ImageView imageView = cardView.getViewById(R.id.main_image);

        primaryText.setText(card.getTitle());
        if (card.getLocalImageResourceName() != null) {
            int resourceId = card.getLocalImageResourceId(getContext());
            RoundedBitmapDrawable drawable = mImageCache.get(resourceId, null);
            if (drawable == null) {
                Bitmap bitmap = BitmapFactory
                        .decodeResource(getContext().getResources(), resourceId);
                drawable = RoundedBitmapDrawableFactory.create(getContext().getResources(), bitmap);
                drawable.setAntiAlias(true);
                drawable.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);
                mImageCache.put(resourceId, drawable);
            }
            imageView.setImageDrawable(drawable);
            if (Constants.LOCAL_LOGD) Log.d(TAG, "Round image created and set.");
        }
    }

}
