/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.net;

import static org.junit.Assert.assertEquals;

import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link UriCompat}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class UriCompatTest {

    @Test
    public void testToSafeString_tel() {
        checkToSafeString("tel:xxxxxx", "tel:Google");
        checkToSafeString("tel:xxxxxxxxxx", "tel:1234567890");
        checkToSafeString("tEl:xxx.xxx-xxxx", "tEl:123.456-7890");
    }

    @Test
    public void testToSafeString_sip() {
        checkToSafeString("sip:xxxxxxx@xxxxxxx.xxxxxxxx", "sip:android@android.com:1234");
        checkToSafeString("sIp:xxxxxxx@xxxxxxx.xxx", "sIp:android@android.com");
    }

    @Test
    public void testToSafeString_sms() {
        checkToSafeString("sms:xxxxxx", "sms:123abc");
        checkToSafeString("smS:xxx.xxx-xxxx", "smS:123.456-7890");
    }

    @Test
    public void testToSafeString_smsto() {
        checkToSafeString("smsto:xxxxxx", "smsto:123abc");
        checkToSafeString("SMSTo:xxx.xxx-xxxx", "SMSTo:123.456-7890");
    }

    @Test
    public void testToSafeString_mailto() {
        checkToSafeString("mailto:xxxxxxx@xxxxxxx.xxx", "mailto:android@android.com");
        checkToSafeString("Mailto:xxxxxxx@xxxxxxx.xxxxxxxxxx",
                "Mailto:android@android.com/secret");
    }

    @Test
    public void testToSafeString_nfc() {
        checkToSafeString("nfc:xxxxxx", "nfc:123abc");
        checkToSafeString("nfc:xxx.xxx-xxxx", "nfc:123.456-7890");
        checkToSafeString("nfc:xxxxxxx@xxxxxxx.xxx", "nfc:android@android.com");
    }

    @Test
    public void testToSafeString_http() {
        checkToSafeString("http://www.android.com/...", "http://www.android.com");
        checkToSafeString("HTTP://www.android.com/...", "HTTP://www.android.com");
        checkToSafeString("http://www.android.com/...", "http://www.android.com/");
        checkToSafeString("http://www.android.com/...", "http://www.android.com/secretUrl?param");
        checkToSafeString("http://www.android.com/...",
                "http://user:pwd@www.android.com/secretUrl?param");
        checkToSafeString("http://www.android.com/...",
                "http://user@www.android.com/secretUrl?param");
        checkToSafeString("http://www.android.com/...", "http://www.android.com/secretUrl?param");
        checkToSafeString("http:///...", "http:///path?param");
        checkToSafeString("http:///...", "http://");
        checkToSafeString("http://:12345/...", "http://:12345/");
    }

    @Test
    public void testToSafeString_https() {
        checkToSafeString("https://www.android.com/...", "https://www.android.com/secretUrl?param");
        checkToSafeString("https://www.android.com:8443/...",
                "https://user:pwd@www.android.com:8443/secretUrl?param");
        checkToSafeString("https://www.android.com/...", "https://user:pwd@www.android.com");
        checkToSafeString("Https://www.android.com/...", "Https://user:pwd@www.android.com");
    }

    @Test
    public void testToSafeString_ftp() {
        checkToSafeString("ftp://ftp.android.com/...", "ftp://ftp.android.com/");
        checkToSafeString("ftP://ftp.android.com/...", "ftP://anonymous@ftp.android.com/");
        checkToSafeString("ftp://ftp.android.com:2121/...",
                "ftp://root:love@ftp.android.com:2121/");
    }

    @Test
    public void testToSafeString_rtsp() {
        checkToSafeString("rtsp://rtsp.android.com/...", "rtsp://rtsp.android.com/");
        checkToSafeString("rtsp://rtsp.android.com/...", "rtsp://rtsp.android.com/video.mov");
        checkToSafeString("rtsp://rtsp.android.com/...", "rtsp://rtsp.android.com/video.mov?param");
        checkToSafeString("RtsP://rtsp.android.com/...", "RtsP://anonymous@rtsp.android.com/");
        checkToSafeString("rtsp://rtsp.android.com:2121/...",
                "rtsp://username:password@rtsp.android.com:2121/");
    }

    @Test
    public void testToSafeString_notSupport() {
        checkToSafeString("unsupported://ajkakjah/askdha/secret?secret",
                "unsupported://ajkakjah/askdha/secret?secret");
        checkToSafeString("unsupported:ajkakjah/askdha/secret?secret",
                "unsupported:ajkakjah/askdha/secret?secret");
    }

    private void checkToSafeString(String expectedSafeString, String original) {
        assertEquals(expectedSafeString, UriCompat.toSafeString(Uri.parse(original)));
    }
}
