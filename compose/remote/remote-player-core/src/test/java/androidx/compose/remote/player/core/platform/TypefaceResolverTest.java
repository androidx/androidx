/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.core.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.graphics.Typeface;

import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TypefaceResolverTest {

    private Context mContext;
    private RemoteContext mRemoteContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mRemoteContext = new AndroidRemoteContext();
    }

    @Test
    public void testDefaultTypefaceResolver_StandardTypes() {
        DefaultTypefaceResolver resolver = new DefaultTypefaceResolver(mRemoteContext);

        FontInstance instance =
                resolver.resolve(PaintBundle.FONT_TYPE_DEFAULT, 400, false, null, 400, false);
        assertNotNull(instance);
        assertEquals(Typeface.DEFAULT, instance.getTypeface());

        instance = resolver.resolve(PaintBundle.FONT_TYPE_SERIF, 400, false, null, 400, false);
        assertNotNull(instance);
        assertEquals(Typeface.SERIF, instance.getTypeface());

        instance = resolver.resolve(PaintBundle.FONT_TYPE_SANS_SERIF, 400, false, null, 400, false);
        assertNotNull(instance);
        assertEquals(Typeface.SANS_SERIF, instance.getTypeface());

        instance = resolver.resolve(PaintBundle.FONT_TYPE_MONOSPACE, 400, false, null, 400, false);
        assertNotNull(instance);
        assertEquals(Typeface.MONOSPACE, instance.getTypeface());
    }
}
