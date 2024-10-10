/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import static androidx.car.app.model.Action.BACK;
import static androidx.car.app.model.CarColor.GREEN;
import static androidx.car.app.model.CarColor.RED;
import static androidx.car.app.model.CarColor.YELLOW;

import android.graphics.BitmapFactory;
import android.text.SpannableStringBuilder;

import androidx.annotation.StringRes;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.common.SpannableStringBuilderAnnotationExtensions;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** Creates a screen that shows different types of texts and icons. */
public final class TextAndIconsDemosScreen extends Screen {

    private static final String FULL_STAR = "★";
    private static final String HALF_STAR = "½";

    public TextAndIconsDemosScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(buildRowForTemplate(R.string.title_with_app_icon_row_title,
                CarIcon.APP_ICON));

        listBuilder.addItem(buildRowForTemplate(R.string.png_bitmap_title,
                buildCarIconWithBitmap(R.drawable.banana)));

        listBuilder.addItem(buildRowForTemplate(R.string.png_res_title,
                buildCarIconWithResource(R.drawable.banana)));

        listBuilder.addItem(buildRowForTemplate(R.string.title_with_res_id_image_row_title,
                buildSecondaryText(R.string.example_1_text),
                buildCarIconWithResource(R.drawable.ic_fastfood_white_48dp, RED)));

        listBuilder.addItem(buildRowForTemplate(R.string.title_with_svg_image_row_title,
                buildSecondaryText(R.string.example_2_text),
                buildCarIconWithResource(R.drawable.ic_emoji_food_beverage_white_48dp, GREEN)));

        listBuilder.addItem(buildRowForTemplate(R.string.colored_secondary_row_title,
                getRatingsString(3.5)));

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.text_icons_demo_title))
                        .setStartHeaderAction(BACK)
                        .build())
                .build();

    }

    private CarIcon buildCarIconWithResource(int imageId) {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                        getCarContext(),
                        imageId))
                .build();
    }

    private CarIcon buildCarIconWithResource(int imageId, CarColor color) {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                        getCarContext(),
                        imageId))
                .setTint(color)
                .build();
    }

    private CarIcon buildCarIconWithBitmap(int imageId) {
        return new CarIcon.Builder(
                IconCompat.createWithBitmap(
                        BitmapFactory.decodeResource(
                                getCarContext().getResources(),
                                imageId)))
                .build();
    }

    /**
     * build a colored line of secondary text using a specific CarColor and some custom text
     */
    private CharSequence buildSecondaryText(@StringRes int textId) {
        SpannableStringBuilder ssb =
                SpannableStringBuilderAnnotationExtensions.getSpannableStringBuilder(
                        getCarContext(), textId);
        SpannableStringBuilderAnnotationExtensions.addSpanToAnnotatedPosition(ssb, "color",
                "red", CarColor.RED);
        SpannableStringBuilderAnnotationExtensions.addSpanToAnnotatedPosition(ssb, "color",
                "green", CarColor.GREEN);
        SpannableStringBuilderAnnotationExtensions.addSpanToAnnotatedPosition(ssb, "color",
                "blue", CarColor.BLUE);
        SpannableStringBuilderAnnotationExtensions.addSpanToAnnotatedPosition(ssb, "color",
                "yellow", CarColor.YELLOW);
        return ssb;
    }

    private Row buildRowForTemplate(int title, CharSequence text) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .addText(text)
                .build();
    }

    private Row buildRowForTemplate(int title, CarIcon image) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setImage(image)
                .build();
    }

    private Row buildRowForTemplate(int title, CharSequence text, CarIcon image) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .addText(text)
                .setImage(image)
                .build();
    }

    private static CharSequence getRatingsString(Double ratings) {
        String s;
        double r;
        for (s = "", r = ratings; r > 0; --r) {
            s += r < 1 ? HALF_STAR : FULL_STAR;
        }
        return new SpannableStringBuilder()
                .append(s, ForegroundCarColorSpan.create(YELLOW), SPAN_EXCLUSIVE_EXCLUSIVE)
                .append(" ratings: ")
                .append(ratings.toString());
    }
}
