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

package androidx.ui.material.studies.rally

import android.app.Activity
import android.os.Bundle
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Column
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.material.studies.Orientation
import androidx.ui.material.studies.Scaffold
import androidx.ui.material.studies.Spacer
import androidx.ui.material.themeTextStyle
import com.google.r4a.Composable
import com.google.r4a.composer
import com.google.r4a.setContent
import com.google.r4a.unaryPlus

/**
 * This Activity recreates the Rally Material Study from
 * https://material.io/design/material-studies/rally.html
 */
class RallyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            <CraneWrapper >
                <RallyApp />
            </CraneWrapper>
        }
    }

    @Composable
    fun RallyApp() {
        <RallyTheme>
            <Scaffold appBar={<RallyAppBar />}>
                <RallyBody />
            </Scaffold>
        </RallyTheme>
    }

    @Composable
    fun RallyAppBar() {
        // TODO: Transform to tabs
        <Row>
            // <Icon />
            <Text text="Overview" style=+themeTextStyle { h4 } />
            // TODO: Other items
        </Row>
    }
}

@Composable
fun RallyBody() {
    <Padding padding=EdgeInsets(16.dp)>
        <Column>
            // TODO: scrolling container
            <RallyAlertCard />
            <Spacer size=10.dp orientation=Orientation.Vertical/>
            <RallyAccountsCard />
            <Spacer size=10.dp orientation=Orientation.Vertical />
            <RallyBillsCard />
        </Column>
    </Padding>
}