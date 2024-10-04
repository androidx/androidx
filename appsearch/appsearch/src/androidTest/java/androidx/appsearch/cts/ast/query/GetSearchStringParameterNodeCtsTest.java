/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.cts.ast.query;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.ast.query.GetSearchStringParameterNode;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class GetSearchStringParameterNodeCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Test
    public void testConstructor_throwsOnNegativeIndex() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new GetSearchStringParameterNode(-1));
        assertThat(thrown).hasMessageThat().contains("must be non-negative");
    }

    @Test
    public void testGetFunctionName_functionNameCorrect() {
        GetSearchStringParameterNode getSearchStringParameterNode =
                new GetSearchStringParameterNode(1);
        assertThat(getSearchStringParameterNode.getFunctionName())
                .isEqualTo("getSearchStringParameter");
    }

    @Test
    public void testGetChildren_returnsEmptyList() {
        GetSearchStringParameterNode getSearchStringParameterNode =
                new GetSearchStringParameterNode(0);

        assertThat(getSearchStringParameterNode.getChildren().isEmpty()).isTrue();
    }

    @Test
    public void testGetIndex_returnsIndex() {
        GetSearchStringParameterNode getSearchStringParameterNode =
                new GetSearchStringParameterNode(1);
        assertThat(getSearchStringParameterNode.getSearchStringIndex()).isEqualTo(1);
    }

    @Test
    public void testSetSearchStringIndex_throwsOnNegativeIndex() {
        GetSearchStringParameterNode getSearchStringParameterNode =
                new GetSearchStringParameterNode(0);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> getSearchStringParameterNode.setSearchStringIndex(-1));
        assertThat(thrown).hasMessageThat().contains("must be non-negative");
    }

    @Test
    public void testSetSearchStringIndex_correctlySetsIndex() {
        GetSearchStringParameterNode getSearchStringParameterNode =
                new GetSearchStringParameterNode(0);
        getSearchStringParameterNode.setSearchStringIndex(1);

        assertThat(getSearchStringParameterNode.getSearchStringIndex()).isEqualTo(1);
    }
}
