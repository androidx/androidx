/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.provider.ui;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import android.app.PendingIntent;
import android.app.slice.Slice;
import android.content.Context;
import android.content.Intent;

import androidx.credentials.provider.Action;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ActionJavaTest {
    private static final CharSequence TITLE = "title";
    private static final CharSequence SUBTITLE = "subtitle";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = new Intent();
    private final PendingIntent mPendingIntent =
            PendingIntent.getActivity(mContext, 0, mIntent,
                    PendingIntent.FLAG_IMMUTABLE);


    @Test
    public void constructor_success() {
        Action action = new Action(TITLE, mPendingIntent, SUBTITLE);

        assertNotNull(action);
        assertThat(TITLE.equals(action.getTitle()));
        assertThat(SUBTITLE.equals(action.getSubtitle()));
        assertThat(mPendingIntent == action.getPendingIntent());
    }

    @Test
    public void constructor_nullTitle_throwsNPE() {
        assertThrows("Expected null title to throw NPE",
                NullPointerException.class,
                () -> new Action(null, mPendingIntent, SUBTITLE));
    }

    @Test
    public void constructor_nullPendingIntent_throwsNPE() {
        assertThrows("Expected null title to throw NPE",
                NullPointerException.class,
                () -> new Action(TITLE, null, SUBTITLE));
    }

    @Test
    public void constructor_emptyTitle_throwsIllegalArgumentException() {
        assertThrows("Expected empty title to throw IllegalArgumentException",
                IllegalArgumentException.class,
                () -> new Action("", mPendingIntent, SUBTITLE));
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void fromSlice_success() {
        Action originalAction = new Action(TITLE, mPendingIntent, SUBTITLE);
        Slice slice = Action.toSlice(originalAction);

        Action fromSlice = Action.fromSlice(slice);

        assertNotNull(fromSlice);
        assertThat(fromSlice.getTitle()).isEqualTo(TITLE);
        assertThat(fromSlice.getSubtitle()).isEqualTo(SUBTITLE);
        assertThat(fromSlice.getPendingIntent()).isEqualTo(mPendingIntent);
    }
}
