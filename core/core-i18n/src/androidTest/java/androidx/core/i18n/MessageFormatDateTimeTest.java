/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.i18n;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.ImmutableMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@RunWith(AndroidJUnit4.class)
public class MessageFormatDateTimeTest {
    private Context mAppContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    private Calendar mTestCalendar = new GregorianCalendar(
            2022, Calendar.SEPTEMBER, 27, // Date
            21, 42, 12 // Time
    );
    private Date mTestDate = mTestCalendar.getTime();
    private long mTestMillis = mTestCalendar.getTimeInMillis();

    @Test @SmallTest
    public void testSimpleStyles() { // date formatting using JDK styles
        String message = "Your card expires on {exp, date, FULL}.";
        String expected = "Your card expires on Tuesday, September 27, 2022.";

        Assert.assertEquals(expected,
                MessageFormat.format(mAppContext, message,
                        ImmutableMap.of("exp", mTestDate)));
        Assert.assertEquals(expected,
                MessageFormat.format(mAppContext, message,
                        ImmutableMap.of("exp", mTestMillis)));
        Assert.assertEquals(expected,
                MessageFormat.format(mAppContext, message,
                        ImmutableMap.of("exp", mTestCalendar)));
    }

    @Test @SmallTest
    public void testSimpleSkeleton() { // date formatting using date-time skeletons
        String message = "Your card expires on {exp, date, ::yMMMdE}.";
        String expected = "Your card expires on Tue, Sep 27, 2022.";

        Assert.assertEquals(expected,
                MessageFormat.format(mAppContext, message,
                        ImmutableMap.of("exp", mTestDate)));
        Assert.assertEquals(expected,
                MessageFormat.format(mAppContext, message,
                        ImmutableMap.of("exp", mTestMillis)));
        Assert.assertEquals(expected,
                MessageFormat.format(mAppContext, message,
                        ImmutableMap.of("exp", mTestCalendar)));
    }

    @Test @SmallTest
    public void testSimplePattern() { // date formatting using date-time patterns. Bad i18n.
        String message = "Your card expires on {exp, date,EEEE, d 'of' MMMM, y}.";
        String expected = "Your card expires on Tuesday, 27 of September, 2022.";

        Assert.assertEquals(expected,
                MessageFormat.format(mAppContext, message,
                        ImmutableMap.of("exp", mTestDate)));
        Assert.assertEquals(expected,
                MessageFormat.format(mAppContext, message,
                        ImmutableMap.of("exp", mTestMillis)));

        // This does not work, see testAndroidCannotFormatCalendar() below.
        try {
            Assert.assertEquals(expected,
                    MessageFormat.format(mAppContext, message,
                            ImmutableMap.of("exp", mTestCalendar)));
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Showing that the Android classes in java.text can't format Calendar objects.
     * For now we do the same (we throw the same exception).
     * TBD if worth the trouble to implement a workaround, since the skeletons and
     * Java date/time styles work (because our compat classes).
     * And using patterns is a bad i18n practice, so we don't want to encourage it / make it easy.
     */
    @Test @SmallTest
    public void testAndroidCannotFormatCalendar() {
        try {
            DateFormat.getDateInstance(DateFormat.LONG).format(mTestCalendar);
        } catch (IllegalArgumentException e) {
        }

        try {
            new SimpleDateFormat("MMMM d, y").format(mTestCalendar);
        } catch (IllegalArgumentException e) {
        }

        try {
            java.text.MessageFormat.format("The date is {0,date,LONG}", mTestCalendar);
        } catch (IllegalArgumentException e) {
        }

        try {
            java.text.MessageFormat.format("The date is {0,date,MMMM d, y}", mTestCalendar);
        } catch (IllegalArgumentException e) {
        }
    }
}
