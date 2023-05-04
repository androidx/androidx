/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.webkit.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class WebViewFeatureInternalTest {

    private static class MockFeature implements ConditionallySupportedFeature {
        private final String mPublicFeatureValue;
        private final boolean mIsSupported;

        MockFeature(String publicFeatureName, boolean isSupported) {
            mPublicFeatureValue = publicFeatureName;
            mIsSupported = isSupported;
        }

        @Override
        public boolean isSupported() {
            return mIsSupported;
        }

        @Override
        @NonNull
        public String getPublicFeatureName() {
            return mPublicFeatureValue;
        }
    }

    @Test
    public void testOneMatchingFeature_supported() throws Throwable {
        List<ConditionallySupportedFeature> features = new ArrayList<>();
        features.add(new MockFeature("MY_FEATURE", true));
        features.add(new MockFeature("FEATURE_DOES_NOT_MATCH", true));
        assertTrue("Feature should be supported by the matching feature",
                WebViewFeatureInternal.isSupported("MY_FEATURE", features));
    }

    @Test
    public void testOneMatchingFeature_notSupported() throws Throwable {
        List<ConditionallySupportedFeature> features = new ArrayList<>();
        features.add(new MockFeature("MY_FEATURE", false));
        features.add(new MockFeature("FEATURE_DOES_NOT_MATCH", true));
        assertFalse("Feature should not be supported by the matching feature",
                WebViewFeatureInternal.isSupported("MY_FEATURE", features));
    }

    @Test
    public void testMultipleMatchingFeatures_supportedByFirst() throws Throwable {
        List<ConditionallySupportedFeature> features = new ArrayList<>();
        features.add(new MockFeature("MY_FEATURE", true));
        features.add(new MockFeature("MY_FEATURE", false));
        assertTrue("Feature should be supported by first internal feature",
                WebViewFeatureInternal.isSupported("MY_FEATURE", features));
    }

    @Test
    public void testMultipleMatchingFeatures_supportedBySecond() throws Throwable {
        // This test verifies we don't return early for the first matching feature.
        List<ConditionallySupportedFeature> features = new ArrayList<>();
        features.add(new MockFeature("MY_FEATURE", false));
        features.add(new MockFeature("MY_FEATURE", true));
        assertTrue("Feature should be supported by second internal feature",
                WebViewFeatureInternal.isSupported("MY_FEATURE", features));
    }

    @Test
    public void testMultipleMatchingFeatures_supportedByBoth() throws Throwable {
        List<ConditionallySupportedFeature> features = new ArrayList<>();
        features.add(new MockFeature("MY_FEATURE", true));
        features.add(new MockFeature("MY_FEATURE", true));
        assertTrue("Feature should be supported by both internal features",
                WebViewFeatureInternal.isSupported("MY_FEATURE", features));
    }

    @Test
    public void testMultipleMatchingFeatures_notSupported() throws Throwable {
        List<ConditionallySupportedFeature> features = new ArrayList<>();
        features.add(new MockFeature("MY_FEATURE", false));
        features.add(new MockFeature("MY_FEATURE", false));
        assertFalse("Neither internal feature should support this feature",
                WebViewFeatureInternal.isSupported("MY_FEATURE", features));
    }

    @Test(expected = RuntimeException.class)
    public void testNoMatchingFeatures() throws Throwable {
        List<ConditionallySupportedFeature> features = new ArrayList<>();
        features.add(new MockFeature("FEATURE_DOES_NOT_MATCH", true));

        // Should throw RuntimeException:
        WebViewFeatureInternal.isSupported("MY_FEATURE", features);
    }
}
