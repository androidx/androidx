/*
 * Copyright (c) 2015 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package android.support.v17.leanback.supportleanbackshowcase.cards.views;

import android.content.Context;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.view.LayoutInflater;
import android.widget.ImageView;

/**
 * The FooterLayoutCardView creates a card view consisting of a big image followed by a footer
 * passed as a layout resource.
 */
public class FooterLayoutCardView extends BaseCardViewEx {

    private final ImageView mImageView;

    /**
     * The footer passed as a layout resource id will be inflated and added automatically to this
     * view.
     * <p/>
     * <u>Note:</u> If you want your footer to expand/collapse when its parent row is
     * activated/deactivated, you have to add the <code>layout_viewType="info"</code> property to
     * your footers root view.
     * <p/>
     * <u>Example footer layout:</u>
     * <pre>{@code <?xml version="1.0" encoding="utf-8"?>
     * <FrameLayout
     *     xmlns:android="http://schemas.android.com/apk/res/android"
     *     xmlns:lb="http://schemas.android.com/apk/res-auto"
     *     lb:layout_viewType="info"
     *     android:layout_width="match_parent"
     *     android:layout_height="match_parent">
     *     <TextView
     *         android:id="@+id/primary_text"
     *         android:layout_width="match_parent"
     *         android:layout_height="wrap_content"/>
     * </FrameLayout>}</pre>
     *
     * @param context The current context.
     * @param layoutId The footers layout resource id.
     * @param imageWidthInDp The width of the ImageView used in this card. The card's width always
     * equals the image's width.
     * @param imageHeightInDp The height of the ImageView used in this card.
     * @see android.support.v17.leanback.widget.BaseCardView.LayoutParams
     */
    public FooterLayoutCardView(Context context, int layoutId, int imageWidthInDp,
                                int imageHeightInDp) {
        super(context);
        setCardType(CARD_TYPE_INFO_UNDER);
        setBackgroundColor(context.getResources().getColor(R.color.default_card_background_color));

        LayoutInflater.from(context).inflate(R.layout.image_card, this);
        mImageView = getViewById(R.id.image_card_view_main_image);
        LayoutInflater.from(context).inflate(layoutId, this);
    }

    public ImageView getImageView() {
        return mImageView;
    }

}
