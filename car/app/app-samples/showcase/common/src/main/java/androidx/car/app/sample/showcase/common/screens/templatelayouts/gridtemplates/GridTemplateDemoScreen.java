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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.gridtemplates;

import static androidx.car.app.CarToast.LENGTH_SHORT;
import static androidx.car.app.model.Action.BACK;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Creates a screen that demonstrates usage of the full screen {@link GridTemplate}. */
public final class GridTemplateDemoScreen extends Screen implements DefaultLifecycleObserver {
    private static final int MAX_GRID_ITEMS = 100;
    private static final int LOADING_TIME_MILLIS = 2000;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private @Nullable IconCompat mImage;
    private @Nullable IconCompat mIcon;
    private boolean mIsFourthItemLoading;
    private boolean mThirdItemToggleState;
    private boolean mFourthItemToggleState;
    private boolean mFifthItemToggleState;

    public GridTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
        mIsFourthItemLoading = false;
        mThirdItemToggleState = false;
        mFourthItemToggleState = true;
        mFifthItemToggleState = false;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        Resources resources = getCarContext().getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.test_image_square);
        mImage = IconCompat.createWithBitmap(bitmap);
        mIcon = IconCompat.createWithResource(getCarContext(), R.drawable.ic_fastfood_white_48dp);
    }

    @Override
    @SuppressWarnings({"FutureReturnValueIgnored"})
    public void onStart(@NonNull LifecycleOwner owner) {
        mIsFourthItemLoading = false;

        // Post a message that starts loading the fourth item for some time.
        triggerFourthItemLoading();
    }

    private GridItem createGridItem(int index) {
        switch (index) {
            case 0:
                // Grid item with an icon and a title.
                return createGridItem(createCarIconFromImage(mIcon), GridItem.IMAGE_TYPE_ICON,
                        getTextStringFromId(R.string.non_actionable));
            case 1:
                // Grid item with an icon, a title, onClickListener and no text.
                return createGridItem(createCarIconFromImage(mIcon), GridItem.IMAGE_TYPE_ICON,
                        getTextStringFromId(R.string.second_item), createOnClickListener(
                                (String) getTextStringFromId(R.string.second_item_toast_msg),
                                LENGTH_SHORT));

            case 2:
                // Grid item with an icon marked as icon, a title, a text and a toggle in
                // unchecked state.
                return createGridItem(createCarIconFromImage(mIcon),
                        GridItem.IMAGE_TYPE_LARGE, getTextStringFromId(R.string.third_item),
                        mThirdItemToggleState
                                ? getTextStringFromId(R.string.checked_action_title) :
                                getTextStringFromId(R.string.unchecked_action_title),
                        createOnClickListenerForThirdItem());

            case 3:
                // Grid item with an image, a title, a long text and a toggle that takes some
                // time to
                // update.
                if (mIsFourthItemLoading) {
                    return createGridItem(getTextStringFromId(R.string.fourth_item),
                            mFourthItemToggleState
                                    ? getTextStringFromId(R.string.on_action_title) :
                                    getTextStringFromId(R.string.off_action_title),
                            true);
                } else {
                    return createGridItem(createCarIconFromImage(mImage),
                            getTextStringFromId(R.string.fourth_item),
                            mFourthItemToggleState
                                    ? getTextStringFromId(R.string.on_action_title) :
                                    getTextStringFromId(R.string.off_action_title),
                            this::triggerFourthItemLoading);
                }
            case 4:
                // Grid item with a large image, a long title, no text and a toggle in unchecked
                // state.
                return createGridItem(createCarIconFromImage(mIcon),
                        GridItem.IMAGE_TYPE_LARGE, getTextStringFromId(R.string.fifth_item),
                        createOnClickListenerForFifthItem());
            case 5:
                // Grid item with an image marked as an icon, a long title, a long text and
                // onClickListener.
                return createGridItem(createCarIconFromImage(mIcon),
                        GridItem.IMAGE_TYPE_ICON, getTextStringFromId(R.string.sixth_item),
                        getTextStringFromId(R.string.sixth_item),
                        createOnClickListener(
                                (String) getTextStringFromId(R.string.sixth_item_toast_msg),
                                LENGTH_SHORT));

            default:
                String titleText = (index + 1) + "th item";
                String toastText = "Clicked " + (index + 1) + "th item";

                return createGridItem(createCarIconFromImage(mIcon),
                        GridItem.IMAGE_TYPE_ICON, titleText, createOnClickListener(toastText,
                                LENGTH_SHORT));
        }
    }


    @Override
    public @NonNull Template onGetTemplate() {
        int itemLimit = 6;
        // Adjust the item limit according to the car constrains.
        if (getCarContext().getCarAppApiLevel() > CarAppApiLevels.LEVEL_1) {
            itemLimit =
                    Math.min(MAX_GRID_ITEMS,
                            getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                                    ConstraintManager.CONTENT_LIMIT_TYPE_GRID));
        }

        ItemList.Builder gridItemListBuilder = new ItemList.Builder();
        for (int i = 0; i <= itemLimit; i++) {
            gridItemListBuilder.addItem(createGridItem(i));
        }

        Action settings = new Action.Builder()
                .setTitle(getCarContext().getString(
                        R.string.settings_action_title))
                .setOnClickListener(
                        () -> CarToast.makeText(
                                        getCarContext(),
                                        getCarContext().getString(R.string.settings_toast_msg),
                                        LENGTH_SHORT)
                                .show())
                .build();
        return new GridTemplate.Builder()
                .setHeader(new Header.Builder()
                        .setStartHeaderAction(BACK)
                        .setTitle(getCarContext().getString(R.string.grid_template_demo_title))
                        .addEndHeaderAction(settings)
                        .build())
                .setSingleList(gridItemListBuilder.build())
                .build();
    }

    /**
     * Changes the fourth item to a loading state for some time and changes it back to the loaded
     * state.
     */
    private void triggerFourthItemLoading() {
        mHandler.post(
                () -> {
                    mIsFourthItemLoading = true;
                    invalidate();

                    mHandler.postDelayed(
                            () -> {
                                mIsFourthItemLoading = false;
                                mFourthItemToggleState = !mFourthItemToggleState;
                                invalidate();
                            },
                            LOADING_TIME_MILLIS);
                });
    }

    /**
     * Custom listener for third grid item
     */
    private OnClickListener createOnClickListenerForThirdItem() {
        return () -> {
            mThirdItemToggleState = !mThirdItemToggleState;
            CarToast.makeText(
                            getCarContext(),
                            getCarContext().getString(
                                    R.string.third_item_checked_toast_msg)
                                    + ": " + mThirdItemToggleState,
                            LENGTH_SHORT)
                    .show();
            invalidate();
        };
    }

    /**
     * Custom listener for fifth grid item
     */
    private OnClickListener createOnClickListenerForFifthItem() {
        return () -> {
            mFifthItemToggleState = !mFifthItemToggleState;
            CarToast.makeText(
                            getCarContext(),
                            getCarContext().getString(
                                    R.string.fifth_item_checked_toast_msg)
                                    + ": "
                                    + mFifthItemToggleState,
                            LENGTH_SHORT)
                    .show();
            invalidate();
        };
    }

    /**
     * Returns CharSequence text for given id
     */
    private CharSequence getTextStringFromId(int id) {
        return getCarContext().getString(id);
    }

    /**
     * Create an onClickListener using a Car Toast
     */
    private OnClickListener createOnClickListener(String toastText, int toastLength) {
        return () ->
                CarToast.makeText(
                                getCarContext(),
                                toastText,
                                toastLength)
                        .show();
    }

    /**
     * Creates CarIcon from given IconCompat image
     */
    private CarIcon createCarIconFromImage(IconCompat image) {
        return new CarIcon.Builder(image).build();
    }

    /**
     * A number of overloaded constructors with different parameters to build grid items
     */
    private GridItem createGridItem(CharSequence title, CharSequence text,
            boolean isLoading) {
        return new GridItem.Builder()
                .setTitle(title)
                .setText(text)
                .setLoading(isLoading)
                .build();
    }

    private GridItem createGridItem(CarIcon carIcon,
            int imageType, CharSequence title, CharSequence text,
            OnClickListener onClickListener) {
        return new GridItem.Builder()
                .setImage(carIcon, imageType)
                .setTitle(title)
                .setText(text)
                .setOnClickListener(onClickListener)
                .build();
    }

    private GridItem createGridItem(CarIcon carIcon,
            CharSequence title, CharSequence text,
            OnClickListener onClickListener) {
        return new GridItem.Builder()
                .setImage(carIcon)
                .setTitle(title)
                .setText(text)
                .setOnClickListener(onClickListener)
                .build();
    }

    private GridItem createGridItem(CarIcon carIcon,
            int imageType, CharSequence title,
            OnClickListener onClickListener) {
        return new GridItem.Builder()
                .setImage(carIcon, imageType)
                .setTitle(title)
                .setOnClickListener(onClickListener)
                .build();
    }

    private GridItem createGridItem(CarIcon carIcon,
            int imageType, CharSequence title) {
        return new GridItem.Builder()
                .setImage(carIcon, imageType)
                .setTitle(title)
                .build();
    }
}
