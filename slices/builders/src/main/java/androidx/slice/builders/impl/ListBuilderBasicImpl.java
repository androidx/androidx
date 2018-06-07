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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.core.SliceHints.HINT_KEYWORDS;

import android.app.PendingIntent;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;
import androidx.slice.builders.GridRowBuilder;
import androidx.slice.builders.ListBuilder.HeaderBuilder;
import androidx.slice.builders.ListBuilder.InputRangeBuilder;
import androidx.slice.builders.ListBuilder.RangeBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import java.time.Duration;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public class ListBuilderBasicImpl extends TemplateBuilderImpl implements ListBuilder {
    private boolean mIsError;
    private Set<String> mKeywords;

    /**
     */
    public ListBuilderBasicImpl(Slice.Builder b, SliceSpec spec) {
        super(b, spec);
    }

    /**
     */
    @Override
    public void addRow(RowBuilder impl) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void addGridRow(GridRowBuilder impl) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void addAction(SliceAction impl) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void setHeader(HeaderBuilder impl) {
        // Do nothing.
    }

    @Override
    public void addInputRange(InputRangeBuilder builder) {
        // Do nothing.
    }

    @Override
    public void addRange(RangeBuilder builder) {
        // Do nothing.
    }

    /**
     */
    @Override
    public void setSeeMoreRow(RowBuilder builder) {
    }

    /**
     */
    @Override
    public void setSeeMoreAction(PendingIntent intent) {
    }

    /**
     */
    @Override
    public void setColor(@ColorInt int color) {
    }

    /**
     */
    @Override
    public void setKeywords(Set<String> keywords) {
        mKeywords = keywords;
    }

    /**
     */
    @Override
    public void setTtl(long ttl) {
    }

    @Override
    @RequiresApi(26)
    public void setTtl(@Nullable Duration ttl) {
    }

    @Override
    public void setIsError(boolean isError) {
        mIsError = isError;
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
    }

    /**
     */
    @Override
    public void apply(Slice.Builder builder) {
        if (mIsError) {
            builder.addHints(HINT_ERROR);
        }
        if (mKeywords != null) {
            Slice.Builder sb = new Slice.Builder(getBuilder());
            for (String keyword : mKeywords) {
                sb.addText(keyword, null);
            }
            getBuilder().addSubSlice(sb.addHints(HINT_KEYWORDS).build());
        }
    }
}
