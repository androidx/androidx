/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.core.telephony.mbms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.telephony.mbms.ServiceInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
@MediumTest
public class MbmsHelperTest{
    @Test
    public void testNameMatchFound() {
        if (!shouldTest()) {
            return;
        }
        LocaleList userPreferredLocales = new LocaleList(Locale.CANADA_FRENCH, Locale.PRC,
                Locale.forLanguageTag("es-us"));
        ServiceInfo sampleService = makeServiceInfoWithLocales(
                Locale.forLanguageTag("en-us"), Locale.forLanguageTag("es-us"));
        Context context = ApplicationProvider.getApplicationContext();
        context.getResources().getConfiguration().setLocales(userPreferredLocales);
        assertEquals(getServiceNameForLocale(Locale.forLanguageTag("es-us")),
                MbmsHelper.getBestNameForService(context, sampleService));
    }

    @Test
    public void testNameMatchNotFound() {
        if (!shouldTest()) {
            return;
        }
        LocaleList userPreferredLocales = new LocaleList(Locale.CANADA_FRENCH, Locale.PRC,
                Locale.forLanguageTag("es-us"));
        ServiceInfo sampleService = makeServiceInfoWithLocales(
                Locale.forLanguageTag("en-us"), Locale.forLanguageTag("en-uk"));
        Context context = ApplicationProvider.getApplicationContext();
        context.getResources().getConfiguration().setLocales(userPreferredLocales);

        assertNull(MbmsHelper.getBestNameForService(context, sampleService));
    }

    private static ServiceInfo makeServiceInfoWithLocales(Locale... locales) {
        final Map<Locale, String> nameDict = new HashMap<>();
        for (Locale l : locales) {
            nameDict.put(l, getServiceNameForLocale(l));
        }
        ServiceInfo serviceInfo = mock(ServiceInfo.class);
        when(serviceInfo.getNamedContentLocales()).thenReturn(nameDict.keySet());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Locale l = invocation.getArgument(0);
                return nameDict.get(l);
            }
        }).when(serviceInfo).getNameForLocale(any(Locale.class));
        return serviceInfo;
    }

    private static boolean shouldTest() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    private static String getServiceNameForLocale(Locale l) {
        return "name for " + l.toLanguageTag();
    }
}
