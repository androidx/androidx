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

package androidx.ui.layout

import androidx.ui.core.Constraints
import androidx.ui.core.Dp
import androidx.ui.core.withDensity
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer
import com.google.r4a.unaryPlus

/**
 * A convenience widget that combines common layout and painting widgets for one child:
 * - padding: the padding to be applied to the child
 * - alignment: how to position the padded child if the [Container] is larger than the child
 * - constraints: additional Constraints to be enforced when measuring the Container
 * - width: the width to be used for the Container
 * - height: the height to be used for the Container
 *
 * When constraints, width and/or height are provided, these will be applied to the constraints
 * incoming from the [Container]'s parent, and might not always be satisfied if this is impossible.
 *
 * By default, the [Container] will try to be the size of its child (including padding), or as
 * small as possible within the incoming constraints if that is not possible. If expanded is
 * [true], the [Container] will be as large as possible for bounded incoming constraints.
 * If the padded child is smaller, regardless of the value of expanded, the provided alignment
 * will be used to position it. For unbounded incoming constraints, the [Container] will wrap
 * its child (same behavior as if expanded was [false]). Also, note that the measurement
 * information passed for the [Container] (constraints, width and height) will not be satisfied
 * if the incoming [Constraints] do not allow it.
 */
@Composable
fun Container(
    padding: EdgeInsets? = null,
    alignment: Alignment = Alignment.Center,
    expanded: Boolean = false,
    constraints: DpConstraints? = null,
    width: Dp? = null,
    height: Dp? = null,
    @Children() children: () -> Unit = {}
) {
    var container = children

    if (padding != null) {
        val childContainer = container
        container = @Composable {
            <Padding padding>
                <childContainer />
            </Padding>
        }
    }

    run {
        // Center child in Container.
        val childContainer = container
        container = if (!expanded) {
            @Composable {
                <Wrap alignment>
                    <childContainer />
                </Wrap>
            }
        } else {
            @Composable {
                <Align alignment>
                    <childContainer />
                </Align>
            }
        }
    }

    if (constraints != null || width != null || height != null) {
        val containerConstraints = +withDensity {
            (constraints ?: DpConstraints()).withTight(width, height)
        }
        val childContainer = container
        container = @Composable {
            +withDensity {
                <ConstrainedBox constraints=containerConstraints>
                    <childContainer />
                </ConstrainedBox>
            }
        }
    }

    <container />
}
