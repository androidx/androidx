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
 *
 */

package android.support.v17.leanback.supportleanbackshowcase.cards.views;

import android.content.Context;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.widget.ImageCardView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Leanback's {@link ImageCardView} will need some layout changes to fit all design requirements
 * from the specs. This class is a temporary implementation of the "new" {@link ImageCardView} and
 * will replace the existing one at some point in the development process. The original
 * implementation also requires some refactoring to be more flexible in order to be used for various
 * card types.
 * <p/>
 * Besides from the refactoring of the ImageCardView I suggest to change not only the BaseCardView
 * but also to add at least one additional CardView, the "FooterLayoutCardView". More about this
 * topic can be discussed later in the development process.
 */
public class ImageCardViewReplacement extends FooterLayoutCardView {

    public static final int PRIMARY_TEXTVIEW_ID = R.id.primary_text;
    public static final int SECONDARY_TEXTVIEW_ID = R.id.secondary_text;
    public static final int FOOTER_ICON_ID = R.id.footer_icon;
    public static final int INFO_BOX_ID = R.id.info_field;

    public ImageCardViewReplacement(Context context, int widthInDp, int heightInDp) {
        super(context, R.layout.image_card_footer, widthInDp, heightInDp);
        setBackgroundColor(context.getResources().getColor(R.color.default_card_background_color));
    }

    public TextView getPrimaryTextView() {
        return getViewById(PRIMARY_TEXTVIEW_ID);
    }

    public TextView getSecondaryTextView() {
        return getViewById(SECONDARY_TEXTVIEW_ID);
    }

    public ImageView getIconView() {
        return getViewById(FOOTER_ICON_ID);
    }

    public View getInfoBoxView() {
        return getViewById(INFO_BOX_ID);
    }
}
