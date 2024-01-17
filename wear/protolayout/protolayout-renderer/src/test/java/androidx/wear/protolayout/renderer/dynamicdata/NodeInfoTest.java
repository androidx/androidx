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

package androidx.wear.protolayout.renderer.dynamicdata;

import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.FIRST_CHILD_INDEX;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.ROOT_NODE_ID;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.createNodePosId;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.pipeline.QuotaManager;
import androidx.wear.protolayout.proto.TriggerProto.OnLoadTrigger;
import androidx.wear.protolayout.proto.TriggerProto.Trigger;
import androidx.wear.protolayout.proto.TriggerProto.Trigger.InnerCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class NodeInfoTest {
    private static final String POS_ID = createNodePosId(ROOT_NODE_ID, FIRST_CHILD_INDEX);
    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Mock private QuotaManager mMockQuotaManager;
    private NodeInfo mNodeInfoUnderTest;

    @Before
    public void setUp() {
        mNodeInfoUnderTest = new NodeInfo(POS_ID, mMockQuotaManager);
    }

    @Test
    public void destroy_releasesAvdQuota() {
        when(mMockQuotaManager.tryAcquireQuota(anyInt())).thenReturn(true);
        TestAnimatedVectorDrawable drawableAvd = new TestAnimatedVectorDrawable();
        mNodeInfoUnderTest.addResolvedAvd(
                drawableAvd,
                Trigger.newBuilder().setOnLoadTrigger(OnLoadTrigger.getDefaultInstance()).build());
        mNodeInfoUnderTest.playAvdAnimations(InnerCase.ON_LOAD_TRIGGER);
        verify(mMockQuotaManager).tryAcquireQuota(eq(1));

        mNodeInfoUnderTest.destroy();

        verify(mMockQuotaManager).releaseQuota(1);
    }
}
