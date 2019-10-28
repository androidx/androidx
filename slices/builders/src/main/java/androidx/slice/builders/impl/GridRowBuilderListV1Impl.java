/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice.builders.impl;

import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.builders.ListBuilder.ICON_IMAGE;
import static androidx.slice.builders.ListBuilder.LARGE_IMAGE;

import android.app.PendingIntent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Pair;
import androidx.slice.Slice;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.GridRowBuilder.CellBuilder;
import androidx.slice.builders.SliceAction;

import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public class GridRowBuilderListV1Impl extends TemplateBuilderImpl {

    private SliceAction mPrimaryAction;

    /**
     */
    public GridRowBuilderListV1Impl(@NonNull ListBuilderImpl parent, GridRowBuilder builder) {
        super(parent.createChildBuilder(), null);
        if (builder.getLayoutDirection() != -1) {
            setLayoutDirection(builder.getLayoutDirection());
        }
        if (builder.getDescription() != null) {
            setContentDescription(builder.getDescription());
        }
        if (builder.getSeeMoreIntent() != null) {
            setSeeMoreAction(builder.getSeeMoreIntent());
        } else if (builder.getSeeMoreCell() != null) {
            setSeeMoreCell(builder.getSeeMoreCell());
        }
        if (builder.getPrimaryAction() != null) {
            setPrimaryAction(builder.getPrimaryAction());
        }
        for (CellBuilder b : builder.getCells()) {
            addCell(b);
        }
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {
        builder.addHints(HINT_HORIZONTAL);
        if (mPrimaryAction != null) {
            mPrimaryAction.setPrimaryAction(builder);
        }
    }

    /**
     */
    public void addCell(CellBuilder builder) {
        CellBuilderImpl impl = new CellBuilderImpl(this);
        impl.fillFrom(builder);
        impl.apply(getBuilder());
    }

    /**
     */
    public void setSeeMoreCell(@NonNull CellBuilder builder) {
        CellBuilderImpl impl = new CellBuilderImpl(this);
        impl.fillFrom(builder);
        impl.getBuilder().addHints(HINT_SEE_MORE);
        impl.apply(getBuilder());
    }

    /**
     */
    public void setSeeMoreAction(PendingIntent intent) {
        getBuilder().addSubSlice(new Slice.Builder(getBuilder()).addHints(HINT_SEE_MORE)
                .addAction(intent, new Slice.Builder(getBuilder()).build(), null).build());
    }

    /**
     */
    public void setPrimaryAction(SliceAction action) {
        mPrimaryAction = action;
    }

    /**
     */
    public void setContentDescription(CharSequence description) {
        getBuilder().addText(description, SUBTYPE_CONTENT_DESCRIPTION);
    }

    /**
     */
    public void setLayoutDirection(int layoutDirection) {
        getBuilder().addInt(layoutDirection, SUBTYPE_LAYOUT_DIRECTION);
    }

    /**
     */
    public static final class CellBuilderImpl extends TemplateBuilderImpl {

        private PendingIntent mContentIntent;

        /**
         */
        CellBuilderImpl(@NonNull GridRowBuilderListV1Impl parent) {
            super(parent.createChildBuilder(), null);
        }

        /**
         */
        @SuppressWarnings("unchecked")
        public void fillFrom(CellBuilder builder) {
            if (builder.getCellDescription() != null) {
                setContentDescription(builder.getCellDescription());
            }
            if (builder.getContentIntent() != null) {
                setContentIntent(builder.getContentIntent());
            }
            List<Object> objs = builder.getObjects();
            List<Integer> types = builder.getTypes();
            List<Boolean> loadings = builder.getLoadings();
            for (int i = 0; i < objs.size(); i++) {
                switch (types.get(i)) {
                    case CellBuilder.TYPE_TEXT:
                        addText((CharSequence) objs.get(i), loadings.get(i));
                        break;
                    case CellBuilder.TYPE_TITLE:
                        addTitleText((CharSequence) objs.get(i), loadings.get(i));
                        break;
                    case CellBuilder.TYPE_IMAGE:
                        Pair<IconCompat, Integer> pair = (Pair<IconCompat, Integer>) objs.get(i);
                        addImage(pair.first, pair.second, loadings.get(i));
                        break;
                }
            }
        }

        /**
         */
        private CellBuilderImpl(@NonNull Uri uri) {
            super(new Slice.Builder(uri), null);
        }

        /**
         */
        @NonNull
        private void addText(@NonNull CharSequence text) {
            addText(text, false /* isLoading */);
        }

        /**
         */
        private void addText(@Nullable CharSequence text, boolean isLoading) {
            @Slice.SliceHint String[] hints = isLoading
                    ? new String[] {HINT_PARTIAL}
                    : new String[0];
            getBuilder().addText(text, null, hints);
        }

        /**
         */
        @NonNull
        private void addTitleText(@NonNull CharSequence text) {
            addTitleText(text, false /* isLoading */);
        }

        /**
         */
        @NonNull
        private void addTitleText(@Nullable CharSequence text, boolean isLoading) {
            @Slice.SliceHint String[] hints = isLoading
                    ? new String[] {HINT_PARTIAL, HINT_TITLE}
                    : new String[] {HINT_TITLE};
            getBuilder().addText(text, null, hints);
        }

        /**
         */
        @NonNull
        private void addImage(@NonNull IconCompat image, int imageMode) {
            addImage(image, imageMode, false /* isLoading */);
        }

        /**
         */
        @NonNull
        private void addImage(@Nullable IconCompat image, int imageMode, boolean isLoading) {
            ArrayList<String> hints = new ArrayList<>();
            if (imageMode != ICON_IMAGE) {
                hints.add(HINT_NO_TINT);
            }
            if (imageMode == LARGE_IMAGE) {
                hints.add(HINT_LARGE);
            }
            if (isLoading) {
                hints.add(HINT_PARTIAL);
            }
            getBuilder().addIcon(image, null, hints);
        }

        /**
         */
        @NonNull
        private void setContentIntent(@NonNull PendingIntent intent) {
            mContentIntent = intent;
        }

        /**
         */
        private void setContentDescription(CharSequence description) {
            getBuilder().addText(description, SUBTYPE_CONTENT_DESCRIPTION);
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Override
        public void apply(Slice.Builder b) {
            getBuilder().addHints(HINT_HORIZONTAL);
            if (mContentIntent != null) {
                b.addAction(mContentIntent, getBuilder().build(), null);
            } else {
                b.addSubSlice(getBuilder().build());
            }
        }
    }
}
