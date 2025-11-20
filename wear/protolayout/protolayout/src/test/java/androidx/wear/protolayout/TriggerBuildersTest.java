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

import static androidx.wear.protolayout.TriggerBuilders.createOnConditionMetTrigger;
import static androidx.wear.protolayout.TriggerBuilders.createOnLoadTrigger;
import static androidx.wear.protolayout.TriggerBuilders.createOnVisibleOnceTrigger;
import static androidx.wear.protolayout.TriggerBuilders.createOnVisibleTrigger;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.TriggerBuilders.Trigger;
import androidx.wear.protolayout.expression.AppDataKey;
import androidx.wear.protolayout.expression.DynamicBuilders;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TriggerBuildersTest {

    @Test
    public void onLoadTrigger() {
        Trigger onLoadTrigger = createOnLoadTrigger();

        assertThat(onLoadTrigger.toTriggerProto().hasOnLoadTrigger()).isTrue();
    }

    @Test
    public void onConditionTrigger() {
        DynamicBuilders.DynamicBool condition =
                DynamicBuilders.DynamicBool.from(new AppDataKey<>("state"));

        Trigger onConditionMetTrigger = createOnConditionMetTrigger(condition);

        assertThat(onConditionMetTrigger.toTriggerProto().getOnConditionMetTrigger().getCondition())
                .isEqualTo(condition.toDynamicBoolProto());
    }

    @Test
    public void onLoad_onVisibleOne_different() {
        Trigger onLoad = createOnLoadTrigger();
        Trigger onVisibleOnce = createOnVisibleOnceTrigger();

        assertThat(onVisibleOnce).isNotEqualTo(onLoad);
        assertThat(onVisibleOnce.hashCode()).isNotEqualTo(onLoad.hashCode());
        assertThat(Trigger.equal(onVisibleOnce, onLoad)).isFalse();
        assertThat(Trigger.hash(onVisibleOnce)).isNotEqualTo(Trigger.hash(onLoad));
    }

    @Test
    public void onVisible_onVisibleOne_different() {
        Trigger onVisible = createOnVisibleTrigger();
        Trigger onVisibleOnce = createOnVisibleOnceTrigger();

        assertThat(onVisibleOnce).isNotEqualTo(onVisible);
        assertThat(onVisibleOnce.hashCode()).isNotEqualTo(onVisible.hashCode());
        assertThat(Trigger.equal(onVisibleOnce, onVisible)).isFalse();
        assertThat(Trigger.hash(onVisibleOnce)).isNotEqualTo(Trigger.hash(onVisible));
    }

    @Test
    public void onLoad_onVisible_different() {
        Trigger onLoad = createOnLoadTrigger();
        Trigger onVisible = createOnVisibleTrigger();

        assertThat(onLoad).isNotEqualTo(onVisible);
        assertThat(onLoad.hashCode()).isNotEqualTo(onVisible.hashCode());
        assertThat(Trigger.equal(onLoad, onVisible)).isFalse();
        assertThat(Trigger.hash(onLoad)).isNotEqualTo(Trigger.hash(onVisible));
    }

    @Test
    public void onLoad_differentObjects_equal() {
        Trigger onVisible = createOnLoadTrigger();
        Trigger onVisible2 = createOnLoadTrigger();

        assertThat(onVisible2).isEqualTo(onVisible);
        assertThat(onVisible2.hashCode()).isEqualTo(onVisible.hashCode());
    }

    @Test
    public void onVisible_differentObjects_equal() {
        Trigger onVisible = createOnVisibleTrigger();
        Trigger onVisible2 = createOnVisibleTrigger();

        assertThat(onVisible2).isEqualTo(onVisible);
        assertThat(onVisible2.hashCode()).isEqualTo(onVisible.hashCode());
    }

    @Test
    public void onVisibleOnce_differentObjects_equal() {
        Trigger onVisible = createOnVisibleOnceTrigger();
        Trigger onVisible2 = createOnVisibleOnceTrigger();

        assertThat(onVisible2).isEqualTo(onVisible);
        assertThat(onVisible2.hashCode()).isEqualTo(onVisible.hashCode());
    }
}
