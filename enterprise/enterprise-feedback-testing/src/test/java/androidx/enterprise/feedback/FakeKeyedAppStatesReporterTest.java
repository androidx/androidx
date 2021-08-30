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

package androidx.enterprise.feedback;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Map;

/** Tests {@link FakeKeyedAppStatesReporter}. */
@RunWith(JUnit4.class)
public class FakeKeyedAppStatesReporterTest {

    private final FakeKeyedAppStatesReporter mReporter = new FakeKeyedAppStatesReporter();

    private static final KeyedAppState KEYED_APP_STATE = KeyedAppState.builder()
            .setKey("key")
            .setSeverity(KeyedAppState.SEVERITY_INFO)
            .build();

    private static final KeyedAppState KEYED_APP_STATE_DIFFERENT_KEY = KeyedAppState.builder()
            .setKey("key2")
            .setSeverity(KeyedAppState.SEVERITY_INFO)
            .build();

    private static final KeyedAppState KEYED_APP_STATE_DIFFERENT_MESSAGE = KeyedAppState.builder()
            .setKey("key")
            .setSeverity(KeyedAppState.SEVERITY_INFO)
            .setMessage("different-message")
            .build();

    private final TestKeyedAppStatesCallback mCallback = new TestKeyedAppStatesCallback();

    @Test
    public void beginsEmpty() {
        FakeKeyedAppStatesReporter reporter = new FakeKeyedAppStatesReporter();

        assertReporterIsEmpty(reporter);
    }

    @Test
    public void setStates_reportsSuccess() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), mCallback);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
        assertThat(mCallback.mLatestState).isEqualTo(KeyedAppStatesCallback.STATUS_SUCCESS);
    }

    @Test
    public void setStatesImmediate_reportsSuccess() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), mCallback);

        assertThat(mCallback.mTotalResults).isEqualTo(1);
        assertThat(mCallback.mLatestState).isEqualTo(KeyedAppStatesCallback.STATUS_SUCCESS);
    }

    @Test
    public void setStates_single_isRecordedInOnDeviceKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStates()).containsExactly(KEYED_APP_STATE);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void setStates_deprecated_isRecordedInOnDeviceKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE));

        assertThat(mReporter.getOnDeviceKeyedAppStates()).containsExactly(KEYED_APP_STATE);
    }

    @Test
    public void setStates_single_isRecordedInOnDeviceKeyedAppStatesByKey() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStatesByKey().values())
                .containsExactly(KEYED_APP_STATE);
        assertThat(mReporter.getOnDeviceKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE);
    }

    @Test
    public void setStates_multiple_isRecordedInOnDeviceKeyedAppStates() {
        mReporter.setStates(
                asList(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStates_multiple_isRecordedInOnDeviceKeyedAppStatesByKey() {
        mReporter.setStates(
                asList(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStatesByKey().values())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
        assertThat(mReporter.getOnDeviceKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE);
        assertThat(
                mReporter.getOnDeviceKeyedAppStatesByKey()
                        .get(KEYED_APP_STATE_DIFFERENT_KEY.getKey()))
                .isEqualTo(KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStates_alreadyPopulated_addsToOnDeviceKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStates_alreadyPopulated_addsToOnDeviceKeyedAppStatesByKey() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStatesByKey().values())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
        assertThat(mReporter.getOnDeviceKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE);
        assertThat(
                mReporter.getOnDeviceKeyedAppStatesByKey()
                        .get(KEYED_APP_STATE_DIFFERENT_KEY.getKey()))
                .isEqualTo(KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStates_sameKeyAsPrevious_addsToOnDeviceKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_MESSAGE), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_MESSAGE);
    }

    @Test
    public void setStates_sameKeyAsPrevious_replacesOnDeviceKeyedAppStatesByKey() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_MESSAGE), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE_DIFFERENT_MESSAGE);
    }

    @Test
    public void setStatesImmediate_clearsOnDeviceKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStates()).isEmpty();
    }

    @Test
    public void setStatesImmediate_clearsOnDeviceKeyedAppStatesByKey() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getOnDeviceKeyedAppStatesByKey().keySet()).isEmpty();
    }

    @Test
    public void setStates_isNotRecordedInUploadedKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStates()).isEmpty();
    }

    @Test
    public void setStates_isNotRecordedInUploadedKeyedAppStatesByKey() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStatesByKey()).isEmpty();
    }

    @Test
    public void setStatesImmediate_single_isRecordedInUploadedKeyedAppStates() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStates()).containsExactly(KEYED_APP_STATE);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void setStatesImmediate_deprecated_isRecordedInUploadedKeyedAppStates() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE));

        assertThat(mReporter.getUploadedKeyedAppStates()).containsExactly(KEYED_APP_STATE);
    }

    @Test
    public void setStatesImmediate_single_isRecordedInUploadedKeyedAppStatesByKey() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStatesByKey().values())
                .containsExactly(KEYED_APP_STATE);
        assertThat(mReporter.getUploadedKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE);
    }

    @Test
    public void setStatesImmediate_multiple_isRecordedInUploadedKeyedAppStates() {
        mReporter.setStatesImmediate(
                asList(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStatesImmediate_multiple_isRecordedInUploadedKeyedAppStatesByKey() {
        mReporter.setStatesImmediate(
                asList(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStatesByKey().values())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
        assertThat(mReporter.getUploadedKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE);
        assertThat(
                mReporter.getUploadedKeyedAppStatesByKey()
                        .get(KEYED_APP_STATE_DIFFERENT_KEY.getKey()))
                .isEqualTo(KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStatesImmediate_alreadyPopulated_addsToUploadedKeyedAppStates() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStatesImmediate_alreadyPopulated_addsToUploadedKeyedAppStatesByKey() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStatesByKey().values())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
        assertThat(mReporter.getUploadedKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE);
        assertThat(
                mReporter.getUploadedKeyedAppStatesByKey()
                        .get(KEYED_APP_STATE_DIFFERENT_KEY.getKey()))
                .isEqualTo(KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStatesImmediate_sameKeyAsPrevious_addsToUploadedKeyedAppStates() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_MESSAGE), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_MESSAGE);
    }

    @Test
    public void setStatesImmediate_sameKeyAsPrevious_replacesUploadedKeyedAppStatesByKey() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_MESSAGE), /* callback= */ null);

        assertThat(
                mReporter.getUploadedKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE_DIFFERENT_MESSAGE);
    }

    @Test
    public void setStatesImmediate_uploadsPreviouslySetStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getUploadedKeyedAppStatesByKey())
                .containsKey(KEYED_APP_STATE.getKey());
    }

    @Test
    public void setStatesImmediate_incrementsNumberOfUploads() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), /* callback= */ null);

        assertThat(mReporter.getNumberOfUploads()).isEqualTo(1);
    }

    @Test
    public void setStates_single_isRecordedInKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        assertThat(mReporter.getKeyedAppStates()).containsExactly(KEYED_APP_STATE);
    }

    @Test
    public void setStates_single_isRecordedInKeyedAppStatesByKey() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        assertThat(mReporter.getKeyedAppStatesByKey().values())
                .containsExactly(KEYED_APP_STATE);
        assertThat(mReporter.getKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE);
    }

    @Test
    public void setStates_multiple_isRecordedInKeyedAppStates() {
        mReporter.setStates(
                asList(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStates_multiple_isRecordedInKeyedAppStatesByKey() {
        mReporter.setStates(
                asList(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getKeyedAppStatesByKey().values())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
        assertThat(mReporter.getKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE);
        assertThat(mReporter.getKeyedAppStatesByKey().get(KEYED_APP_STATE_DIFFERENT_KEY.getKey()))
                .isEqualTo(KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStates_alreadyPopulated_addsToKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStates_alreadyPopulated_addsToKeyedAppStatesByKey() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getKeyedAppStatesByKey().values())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
        assertThat(mReporter.getKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE);
        assertThat(mReporter.getKeyedAppStatesByKey().get(KEYED_APP_STATE_DIFFERENT_KEY.getKey()))
                .isEqualTo(KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStates_sameKeyAsPrevious_addsToKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_MESSAGE), /* callback= */ null);

        assertThat(mReporter.getKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_MESSAGE);
    }

    @Test
    public void setStates_sameKeyAsPrevious_replacesKeyedAppStatesByKey() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_MESSAGE), /* callback= */ null);

        assertThat(
                mReporter.getKeyedAppStatesByKey().get(KEYED_APP_STATE.getKey()))
                .isEqualTo(KEYED_APP_STATE_DIFFERENT_MESSAGE);
    }

    @Test
    public void setStatesImmediate_doesNotClearKeyedAppStates() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getKeyedAppStates())
                .containsExactly(KEYED_APP_STATE, KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void setStatesImmediate_doesNotClearKeyedAppStatesByKey() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(mReporter.getKeyedAppStatesByKey().keySet())
                .containsExactly(KEYED_APP_STATE.getKey(), KEYED_APP_STATE_DIFFERENT_KEY.getKey());
    }

    @Test
    public void getOnDeviceKeyedAppStates_returnsCopy() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);
        List<KeyedAppState> beforeOnDeviceKeyedAppStates = mReporter.getOnDeviceKeyedAppStates();

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(beforeOnDeviceKeyedAppStates).doesNotContain(KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void getOnDeviceKeyedAppStatesByKey_returnsCopy() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);
        Map<String, KeyedAppState> beforeOnDeviceKeyedAppStates =
                mReporter.getOnDeviceKeyedAppStatesByKey();

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(beforeOnDeviceKeyedAppStates)
                .doesNotContainKey(KEYED_APP_STATE_DIFFERENT_KEY.getKey());
    }

    @Test
    public void getKeyedAppStates_returnsCopy() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);
        List<KeyedAppState> beforeKeyedAppStates = mReporter.getKeyedAppStates();

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(beforeKeyedAppStates).doesNotContain(KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void getKeyedAppStatesByKey_returnsCopy() {
        mReporter.setStates(singletonList(KEYED_APP_STATE), /* callback= */ null);
        Map<String, KeyedAppState> beforeKeyedAppStates =
                mReporter.getKeyedAppStatesByKey();

        mReporter.setStates(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(beforeKeyedAppStates)
                .doesNotContainKey(KEYED_APP_STATE_DIFFERENT_KEY.getKey());
    }

    @Test
    public void getUploadedKeyedAppStates_returnsCopy() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), /* callback= */ null);
        List<KeyedAppState> beforeUploadedKeyedAppStates = mReporter.getUploadedKeyedAppStates();

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(beforeUploadedKeyedAppStates).doesNotContain(KEYED_APP_STATE_DIFFERENT_KEY);
    }

    @Test
    public void getUploadedKeyedAppStatesByKey_returnsCopy() {
        mReporter.setStatesImmediate(singletonList(KEYED_APP_STATE), /* callback= */ null);
        Map<String, KeyedAppState> beforeUploadedKeyedAppStates =
                mReporter.getUploadedKeyedAppStatesByKey();

        mReporter.setStatesImmediate(
                singletonList(KEYED_APP_STATE_DIFFERENT_KEY), /* callback= */ null);

        assertThat(beforeUploadedKeyedAppStates)
                .doesNotContainKey(KEYED_APP_STATE_DIFFERENT_KEY.getKey());
    }

    private void assertReporterIsEmpty(FakeKeyedAppStatesReporter reporter) {
        assertThat(reporter.getKeyedAppStates()).isEmpty();
        assertThat(reporter.getOnDeviceKeyedAppStates()).isEmpty();
        assertThat(reporter.getUploadedKeyedAppStates()).isEmpty();
        assertThat(reporter.getKeyedAppStatesByKey()).isEmpty();
        assertThat(reporter.getOnDeviceKeyedAppStatesByKey().keySet()).isEmpty();
        assertThat(reporter.getUploadedKeyedAppStatesByKey().keySet()).isEmpty();
        assertThat(reporter.getNumberOfUploads()).isEqualTo(0);
    }
}
