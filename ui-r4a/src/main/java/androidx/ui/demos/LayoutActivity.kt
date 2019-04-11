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

package androidx.ui.demos

import android.app.Activity
import android.os.Bundle
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Row
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import com.google.r4a.Composable
import com.google.r4a.composer
import com.google.r4a.setContent

class LayoutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            <CraneWrapper>
                <LayoutDemo />
            </CraneWrapper>
        }
    }
}

@Composable
fun LayoutDemo() {
    val lightGrey = Color(0xFFCFD8DC.toInt())
    <Column mainAxisAlignment=MainAxisAlignment.Start crossAxisAlignment=CrossAxisAlignment.Start>
        <Text text=TextSpan(text="Row", style = TextStyle(fontSize = 48f)) />
        <Container width=140.dp color=lightGrey>
            <Row>
                <PurpleSquare />
                <CyanSquare />
            </Row>
        </Container>
        <ColumnSpacer />
        <Container width=140.dp color=lightGrey>
            <Row mainAxisAlignment=MainAxisAlignment.Center>
                <PurpleSquare />
                <CyanSquare />
            </Row>
        </Container>
        <ColumnSpacer />
        <Container width=140.dp color=lightGrey>
            <Row mainAxisAlignment=MainAxisAlignment.End>
                <PurpleSquare />
                <CyanSquare />
            </Row>
        </Container>
        <ColumnSpacer />
        <Container width=140.dp color=lightGrey>
            <Row crossAxisAlignment=CrossAxisAlignment.Start>
                <PurpleSquare />
                <CyanSquare />
            </Row>
        </Container>
        <ColumnSpacer />
        <Container width=140.dp color=lightGrey>
            <Row crossAxisAlignment=CrossAxisAlignment.End>
                <PurpleSquare />
                <CyanSquare />
            </Row>
        </Container>
        <Text text=TextSpan(text="Column", style = TextStyle(fontSize = 48f)) />
        <Row>
            <Container height=140.dp color=lightGrey>
                <Column>
                    <PurpleSquare />
                    <CyanSquare />
                </Column>
            </Container>
            <RowSpacer />
            <Container height=140.dp color=lightGrey>
                <Column mainAxisAlignment=MainAxisAlignment.Center>
                    <PurpleSquare />
                    <CyanSquare />
                </Column>
            </Container>
            <RowSpacer />
            <Container height=140.dp color=lightGrey>
                <Column mainAxisAlignment=MainAxisAlignment.End>
                    <PurpleSquare />
                    <CyanSquare />
                </Column>
            </Container>
            <RowSpacer />
            <Container height=140.dp color=lightGrey>
                <Column crossAxisAlignment=CrossAxisAlignment.Start>
                    <PurpleSquare />
                    <CyanSquare />
                </Column>
            </Container>
            <RowSpacer />
            <Container height=140.dp color=lightGrey>
                <Column crossAxisAlignment=CrossAxisAlignment.End>
                    <PurpleSquare />
                    <CyanSquare />
                </Column>
            </Container>
        </Row>
    </Column>
}

@Composable
fun ColumnSpacer() {
    <Container height=24.dp />
}

@Composable
fun RowSpacer() {
    <Container width=24.dp />
}

@Composable
fun PurpleSquare() {
    <Container width=48.dp height=48.dp color=Color(0xFF6200EE.toInt()) />
}

@Composable
fun CyanSquare() {
    <Container width=24.dp height=24.dp color=Color(0xFF03DAC6.toInt()) />
}