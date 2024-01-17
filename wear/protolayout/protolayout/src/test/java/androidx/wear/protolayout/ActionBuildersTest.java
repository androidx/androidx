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

package androidx.wear.protolayout;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;

import androidx.wear.protolayout.proto.ActionProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ActionBuildersTest {
    private static final ComponentName LAUNCH_COMPONENT =
            new ComponentName("com.package", "launchClass");

    @Test
    public void launchAction() {
        ActionBuilders.LaunchAction launchAction = ActionBuilders.launchAction(LAUNCH_COMPONENT);

        ActionProto.LaunchAction launchActionProto = launchAction.toActionProto().getLaunchAction();
        assertThat(launchActionProto.getAndroidActivity().getPackageName())
                .isEqualTo(LAUNCH_COMPONENT.getPackageName());
        assertThat(launchActionProto.getAndroidActivity().getClassName())
                .isEqualTo(LAUNCH_COMPONENT.getClassName());
    }

    @Test
    public void launchActionWithExtras() {
        String keyString = "keyString";
        ActionBuilders.AndroidStringExtra stringExtra =
                new ActionBuilders.AndroidStringExtra.Builder().setValue("string-extra").build();
        String keyInt = "keyInt";
        ActionBuilders.AndroidIntExtra intExtra =
                new ActionBuilders.AndroidIntExtra.Builder().setValue(42).build();

        ActionBuilders.LaunchAction launchAction =
                ActionBuilders.launchAction(
                        LAUNCH_COMPONENT, Map.of(keyInt, intExtra, keyString, stringExtra));

        ActionProto.LaunchAction launchActionProto = launchAction.toActionProto().getLaunchAction();
        assertThat(launchActionProto.getAndroidActivity().getPackageName())
                .isEqualTo(LAUNCH_COMPONENT.getPackageName());
        assertThat(launchActionProto.getAndroidActivity().getClassName())
                .isEqualTo(LAUNCH_COMPONENT.getClassName());
        Map<String, ActionProto.AndroidExtra> keyToExtraMap =
                launchActionProto.getAndroidActivity().getKeyToExtraMap();
        assertThat(keyToExtraMap).hasSize(2);
        assertThat(keyToExtraMap.get(keyString).getStringVal().getValue())
                .isEqualTo(stringExtra.getValue());
        assertThat(keyToExtraMap.get(keyInt).getIntVal().getValue()).isEqualTo(intExtra.getValue());
    }
}
