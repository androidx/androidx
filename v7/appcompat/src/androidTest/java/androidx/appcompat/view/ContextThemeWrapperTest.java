/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.appcompat.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.appcompat.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ContextThemeWrapperTest {
    private static final int SYSTEM_DEFAULT_THEME = 0;

    private Context mContext;

    private static class MockContextThemeWrapper extends ContextThemeWrapper {
        boolean mIsOnApplyThemeResourceCalled;
        MockContextThemeWrapper(Context base, int themeres) {
            super(base, themeres);
        }

        @Override
        protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
            mIsOnApplyThemeResourceCalled = true;
            super.onApplyThemeResource(theme, resid, first);
        }
    }

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testConstructor() {
        new ContextThemeWrapper();

        new ContextThemeWrapper(mContext, R.style.TextAppearance);

        new ContextThemeWrapper(mContext, mContext.getTheme());
    }

    @Test
    public void testAccessTheme() {
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(
                mContext, SYSTEM_DEFAULT_THEME);
        // set Theme to TextAppearance
        contextThemeWrapper.setTheme(R.style.TextAppearance);
        TypedArray ta = contextThemeWrapper.getTheme().obtainStyledAttributes(
                R.styleable.TextAppearance);

        // assert theme style of TextAppearance
        verifyIdenticalTextAppearanceStyle(ta);
    }

    @Test
    public void testGetSystemService() {
        // Note that we can't use Mockito since ContextThemeWrapper.onApplyThemeResource is
        // protected
        final MockContextThemeWrapper contextThemeWrapper =
                new MockContextThemeWrapper(mContext, R.style.TextAppearance);
        contextThemeWrapper.getTheme();
        assertTrue(contextThemeWrapper.mIsOnApplyThemeResourceCalled);

        // All service get from contextThemeWrapper just the same as this context get,
        // except Context.LAYOUT_INFLATER_SERVICE.
        assertEquals(mContext.getSystemService(Context.ACTIVITY_SERVICE),
                contextThemeWrapper.getSystemService(Context.ACTIVITY_SERVICE));
        assertNotSame(mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE),
                contextThemeWrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    }

    @Test
    public void testAttachBaseContext() {
        assertTrue((new ContextThemeWrapper() {
            public boolean test() {
                // Set two different context to ContextThemeWrapper
                // it should throw a exception when set it at second time.
                // As ContextThemeWrapper is a context, we will attachBaseContext to
                // two different ContextThemeWrapper instances.
                try {
                    attachBaseContext(new ContextThemeWrapper(mContext,
                            R.style.TextAppearance));
                } catch (IllegalStateException e) {
                    fail("test attachBaseContext fail");
                }

                try {
                    attachBaseContext(new ContextThemeWrapper());
                    fail("test attachBaseContext fail");
                } catch (IllegalStateException e) {
                    // expected
                }
                return true;
            }
        }).test());
    }

    @Test
    public void testApplyOverrideConfiguration() {
        // Configuration.densityApi is only available on API 17 and above
        if (Build.VERSION.SDK_INT >= 17) {
            final int realDensity = mContext.getResources().getConfiguration().densityDpi;
            final int expectedDensity = realDensity + 1;

            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(
                    mContext, SYSTEM_DEFAULT_THEME);

            Configuration overrideConfig = new Configuration();
            overrideConfig.densityDpi = expectedDensity;
            contextThemeWrapper.applyOverrideConfiguration(overrideConfig);

            Configuration actualConfiguration =
                    contextThemeWrapper.getResources().getConfiguration();
            assertEquals(expectedDensity, actualConfiguration.densityDpi);
        }
    }

    private void verifyIdenticalTextAppearanceStyle(TypedArray ta) {
        final int defValue = -1;
        // get Theme and assert
        Resources.Theme expected = mContext.getResources().newTheme();
        expected.setTo(mContext.getTheme());
        expected.applyStyle(R.style.TextAppearance, true);
        TypedArray expectedTa = expected.obtainStyledAttributes(R.styleable.TextAppearance);
        assertEquals(expectedTa.getIndexCount(), ta.getIndexCount());
        assertEquals(expectedTa.getColor(
                androidx.appcompat.R.styleable.TextAppearance_android_textColor,
                defValue),
                ta.getColor(
                        androidx.appcompat.R.styleable.TextAppearance_android_textColor,
                        defValue));
        assertEquals(expectedTa.getColor(
                androidx.appcompat.R.styleable.TextAppearance_android_textColorHint,
                defValue),
                ta.getColor(
                        androidx.appcompat.R.styleable
                                .TextAppearance_android_textColorHint,
                        defValue));
        assertEquals(expectedTa.getColor(
                androidx.appcompat.R.styleable.TextAppearance_android_textColorLink,
                defValue),
                ta.getColor(
                        androidx.appcompat.R.styleable
                                .TextAppearance_android_textColorLink,
                        defValue));
        assertEquals(expectedTa.getDimension(
                androidx.appcompat.R.styleable.TextAppearance_android_textSize,
                defValue),
                ta.getDimension(
                        androidx.appcompat.R.styleable.TextAppearance_android_textSize,
                        defValue), 0.0f);
        assertEquals(expectedTa.getInt(
                androidx.appcompat.R.styleable.TextAppearance_android_textStyle,
                defValue),
                ta.getInt(androidx.appcompat.R.styleable
                                .TextAppearance_android_textStyle,
                        defValue));
    }
}
