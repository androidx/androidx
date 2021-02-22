/*
 * Copyright 2018 The Android Open Source Project
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

import static android.app.slice.Slice.HINT_ERROR;
import static android.app.slice.Slice.HINT_KEYWORDS;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.HINT_TTL;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;
import static android.app.slice.SliceItem.FORMAT_BUNDLE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.builders.ListBuilder.INFINITY;
import static androidx.slice.core.SliceHints.SUBTYPE_HOST_EXTRAS;
import static androidx.slice.core.SliceHints.SUBTYPE_MILLIS;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceSpec;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.GridRowBuilder.CellBuilder;
import androidx.slice.builders.ListBuilder.HeaderBuilder;
import androidx.slice.builders.ListBuilder.InputRangeBuilder;
import androidx.slice.builders.ListBuilder.RangeBuilder;
import androidx.slice.builders.ListBuilder.RatingBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SelectionBuilder;
import androidx.slice.builders.SliceAction;

import java.time.Duration;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public class ListBuilderBasicImpl extends TemplateBuilderImpl implements ListBuilder {
    boolean mIsError;
    private Set<String> mKeywords;

    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private SliceAction mSliceAction;
    private IconCompat mIconCompat;
    private Bundle mHostExtras;

    /**
     */
    public ListBuilderBasicImpl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
    }

    /**
     */
    @Override
    public void addRow(@NonNull RowBuilder builder) {
        if (mTitle == null && builder.getTitle() != null) {
            mTitle = builder.getTitle();
        }
        if (mSubtitle == null && builder.getSubtitle() != null) {
            mSubtitle = builder.getSubtitle();
        }
        if (mSliceAction == null && builder.getPrimaryAction() != null) {
            mSliceAction = builder.getPrimaryAction();
        }
        if (mSliceAction == null && builder.getTitleAction() != null) {
            mSliceAction = builder.getTitleAction();
        }
        if (mIconCompat == null && builder.getTitleIcon() != null) {
            mIconCompat = builder.getTitleIcon();
        }
    }

    /**
     */
    @Override
    public void addGridRow(@NonNull GridRowBuilder builder) {
        for (CellBuilder cell : builder.getCells()) {
            if (mTitle == null) {
                if (cell.getTitle() != null) {
                    mTitle = cell.getTitle();
                } else if (cell.getSubtitle() != null) {
                    mTitle = cell.getSubtitle();
                } else if (cell.getCellDescription() != null) {
                    mTitle = cell.getCellDescription();
                }
            }
            if (mSubtitle == null && cell.getSubtitle() != null) {
                mSubtitle = cell.getSubtitle();
            }
            if (mTitle != null && mSubtitle != null) {
                break;
            }
        }

        if (mSliceAction == null && builder.getPrimaryAction() != null) {
            mSliceAction = builder.getPrimaryAction();
            if (mTitle == null && mSliceAction.getTitle() != null) {
                mTitle = mSliceAction.getTitle();
            }
        }
    }

    /**
     */
    @Override
    public void addAction(@NonNull SliceAction action) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void setHeader(@NonNull HeaderBuilder builder) {
        if (builder.getTitle() != null) {
            mTitle = builder.getTitle();
        }
        if (builder.getSubtitle() != null) {
            mSubtitle = builder.getSubtitle();
        }
        if (builder.getPrimaryAction() != null) {
            mSliceAction = builder.getPrimaryAction();
        }
    }

    @Override
    public void addRating(@NonNull RatingBuilder builder) {
        if (mTitle == null && builder.getTitle() != null) {
            mTitle = builder.getTitle();
        }
        if (mSubtitle == null && builder.getSubtitle() != null) {
            mSubtitle = builder.getSubtitle();
        }
        if (mSliceAction == null && builder.getPrimaryAction() != null) {
            mSliceAction = builder.getPrimaryAction();
        }
    }

    @Override
    public void addInputRange(@NonNull InputRangeBuilder builder) {
        if (mTitle == null && builder.getTitle() != null) {
            mTitle = builder.getTitle();
        }
        if (mSubtitle == null && builder.getSubtitle() != null) {
            mSubtitle = builder.getSubtitle();
        }
        if (mSliceAction == null && builder.getPrimaryAction() != null) {
            mSliceAction = builder.getPrimaryAction();
        }
        if (mIconCompat == null && builder.getThumb() != null) {
            mIconCompat = builder.getThumb();
        }
    }

    @Override
    public void addRange(@NonNull RangeBuilder builder) {
        if (mTitle == null && builder.getTitle() != null) {
            mTitle = builder.getTitle();
        }
        if (mSubtitle == null && builder.getSubtitle() != null) {
            mSubtitle = builder.getSubtitle();
        }
        if (mSliceAction == null && builder.getPrimaryAction() != null) {
            mSliceAction = builder.getPrimaryAction();
        }
    }

    @Override
    public void addSelection(@NonNull SelectionBuilder builder) {
        if (mTitle == null && builder.getTitle() != null) {
            mTitle = builder.getTitle();
        }
        if (mSubtitle == null && builder.getSubtitle() != null) {
            mSubtitle = builder.getSubtitle();
        }
        if (mSliceAction == null && builder.getPrimaryAction() != null) {
            mSliceAction = builder.getPrimaryAction();
        }
    }

    /**
     */
    @Override
    public void setSeeMoreRow(@NonNull RowBuilder builder) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void setSeeMoreAction(@NonNull PendingIntent intent) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void setColor(@ColorInt int color) {
        getBuilder().addInt(color, SUBTYPE_COLOR);
    }

    /**
     */
    @Override
    public void setKeywords(@NonNull Set<String> keywords) {
        mKeywords = keywords;
    }

    /**
     */
    @Override
    public void setTtl(long ttl) {
        long expiry = ttl == INFINITY ? INFINITY : getClock().currentTimeMillis() + ttl;
        getBuilder().addTimestamp(expiry, SUBTYPE_MILLIS, HINT_TTL);
    }

    @Override
    @RequiresApi(26)
    public void setTtl(@Nullable Duration ttl) {
        setTtl(ttl == null ? INFINITY : ttl.toMillis());
    }

    @Override
    public void setIsError(boolean isError) {
        mIsError = isError;
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        getBuilder().addInt(layoutDirection, SUBTYPE_LAYOUT_DIRECTION);
    }

    @Override
    @RequiresApi(21)
    public void setHostExtras(@NonNull PersistableBundle extras) {
        mHostExtras = ConvertPersistableBundleApi21Impl.toBundle(extras);
    }

    @RequiresApi(21)
    private static class ConvertPersistableBundleApi21Impl {
        private ConvertPersistableBundleApi21Impl() {}
        static Bundle toBundle(@NonNull PersistableBundle extras) {
            return new Bundle(extras);
        }
    }

    /**
     */
    @Override
    public void apply(@NonNull Slice.Builder builder) {
        if (mIsError) {
            builder.addHints(HINT_ERROR);
        }
        if (mKeywords != null) {
            Slice.Builder sb = new Slice.Builder(getBuilder());
            for (String keyword : mKeywords) {
                sb.addText(keyword, null);
            }
            builder.addSubSlice(sb.addHints(HINT_KEYWORDS).build());
        }
        Slice.Builder slice = new Slice.Builder(getBuilder());
        if (mSliceAction != null) {
            if (mTitle == null && mSliceAction.getTitle() != null) {
                mTitle = mSliceAction.getTitle();
            }
            if (mIconCompat == null && mSliceAction.getIcon() != null) {
                mIconCompat = mSliceAction.getIcon();
            }
            mSliceAction.setPrimaryAction(slice);
        }
        if (mTitle != null) {
            slice.addItem(new SliceItem(mTitle, FORMAT_TEXT, null, new String[] { HINT_TITLE }));
        }
        if (mSubtitle != null) {
            slice.addItem(new SliceItem(mSubtitle, FORMAT_TEXT, null, new String[0]));
        }

        if (mIconCompat != null) {
            builder.addIcon(mIconCompat, null, HINT_TITLE);
        }

        if (mHostExtras != null) {
            slice.addItem(
                    new SliceItem(mHostExtras, FORMAT_BUNDLE, SUBTYPE_HOST_EXTRAS, new String[0]));
        }
        builder.addSubSlice(slice.build());
    }
}
