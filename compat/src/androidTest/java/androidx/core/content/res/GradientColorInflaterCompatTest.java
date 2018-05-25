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

package androidx.core.content.res;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link GradientColorInflaterCompat}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class GradientColorInflaterCompatTest {

    private Context mContext;
    private Resources mResources;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        mResources = mContext.getResources();
    }

    @Test
    public void testGetLinearGradient() throws Exception {
        @SuppressLint("ResourceType")
        final Shader result = GradientColorInflaterCompat.createFromXml(mResources,
                mResources.getXml(R.color.gradient_linear),
                mContext.getTheme());
        assertNotNull(result);
        assertTrue(result instanceof LinearGradient);
    }

    @Test
    public void testGetLinearGradientColorItems() throws Exception {
        @SuppressLint("ResourceType")
        final Shader result = GradientColorInflaterCompat.createFromXml(mResources,
                mResources.getXml(R.color.gradient_linear_item),
                mContext.getTheme());
        assertNotNull(result);
        assertTrue(result instanceof LinearGradient);
    }

    @Test
    public void testGetRadialGradient() throws Exception {
        @SuppressLint("ResourceType")
        final Shader result = GradientColorInflaterCompat.createFromXml(mResources,
                mResources.getXml(R.color.gradient_radial),
                mContext.getTheme());
        assertNotNull(result);
        assertTrue(result instanceof RadialGradient);
    }

    @Test
    public void testGetRadialGradientColorItems() throws Exception {
        @SuppressLint("ResourceType")
        final Shader result = GradientColorInflaterCompat.createFromXml(mResources,
                mResources.getXml(R.color.gradient_radial_item),
                mContext.getTheme());
        assertNotNull(result);
        assertTrue(result instanceof RadialGradient);
    }

    @Test
    public void testGetSweepGradient() throws Exception {
        @SuppressLint("ResourceType")
        final Shader result = GradientColorInflaterCompat.createFromXml(mResources,
                mResources.getXml(R.color.gradient_sweep),
                mContext.getTheme());
        assertNotNull(result);
        assertTrue(result instanceof SweepGradient);
    }

    @Test
    public void testGetSweepGradientColorItems() throws Exception {
        @SuppressLint("ResourceType")
        final Shader result = GradientColorInflaterCompat.createFromXml(mResources,
                mResources.getXml(R.color.gradient_sweep_item),
                mContext.getTheme());
        assertNotNull(result);
        assertTrue(result instanceof SweepGradient);
    }

    @Test
    public void testGetTypelessLinearGradient() throws Exception {
        @SuppressLint("ResourceType")
        final Shader result = GradientColorInflaterCompat.createFromXml(mResources,
                mResources.getXml(R.color.gradient_no_type),
                mContext.getTheme());
        assertNotNull(result);
        assertTrue(result instanceof LinearGradient);
    }
}
