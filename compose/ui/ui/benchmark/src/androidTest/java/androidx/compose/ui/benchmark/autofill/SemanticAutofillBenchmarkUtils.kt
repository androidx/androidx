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

package androidx.compose.ui.benchmark.autofill

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun RemovableAutofillText(state: MutableState<Boolean>) {
    Column {
        Row {
            if (state.value) {
                TextField(
                    value = data.title,
                    onValueChange = {},
                    label = { Text("Enter prefix here: ") },
                    modifier =
                        Modifier.semantics {
                            contentType = ContentType.PersonNamePrefix
                            contentDataType = ContentDataType.Text
                        },
                )
            }
            TextField(
                value = data.firstName,
                onValueChange = {},
                label = { Text("Enter first name here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonFirstName
                        contentDataType = ContentDataType.Text
                    },
            )
            TextField(
                value = data.lastName,
                onValueChange = {},
                label = { Text("Enter last name here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonLastName
                        contentDataType = ContentDataType.Text
                    },
            )
            if (state.value) {
                TextField(
                    value = data.middleName,
                    onValueChange = {},
                    label = { Text("Enter middle name here: ") },
                    modifier =
                        Modifier.semantics {
                            contentType = ContentType.PersonMiddleName
                            contentDataType = ContentDataType.Text
                        },
                )
            }
        }
    }
}

@Composable
internal fun ChangingAutofillText(state: MutableState<Boolean>) {
    Column {
        Row {
            TextField(
                value = if (state.value) data.title else dataFlipped.title,
                onValueChange = {},
                label = { Text("Enter prefix here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonNamePrefix
                        contentDataType = ContentDataType.Text
                    },
            )
            TextField(
                value = if (state.value) data.firstName else dataFlipped.firstName,
                onValueChange = {},
                label = { Text("Enter first name here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonFirstName
                        contentDataType = ContentDataType.Text
                    },
            )
            TextField(
                value = if (state.value) data.lastName else dataFlipped.lastName,
                onValueChange = {},
                label = { Text("Enter last name here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonLastName
                        contentDataType = ContentDataType.Text
                    },
            )
            TextField(
                value = if (state.value) data.middleName else dataFlipped.middleName,
                onValueChange = {},
                label = { Text("Enter middle name here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonMiddleName
                        contentDataType = ContentDataType.Text
                    },
            )
        }
    }
}

@Composable
internal fun ChangingAutofillFocus(state: MutableState<Boolean>) {
    Column {
        Row {
            TextField(
                value = if (state.value) data.title else dataFlipped.title,
                onValueChange = {},
                label = { Text("Enter prefix here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonNamePrefix
                        contentDataType = ContentDataType.Text
                        focused = state.value
                    },
            )
            TextField(
                value = if (state.value) data.firstName else dataFlipped.firstName,
                onValueChange = {},
                label = { Text("Enter first name here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonFirstName
                        contentDataType = ContentDataType.Text
                    },
            )
            TextField(
                value = if (state.value) data.lastName else dataFlipped.lastName,
                onValueChange = {},
                label = { Text("Enter last name here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonLastName
                        contentDataType = ContentDataType.Text
                        focused = state.value
                    },
            )
            TextField(
                value = if (state.value) data.middleName else dataFlipped.middleName,
                onValueChange = {},
                label = { Text("Enter middle name here: ") },
                modifier =
                    Modifier.semantics {
                        contentType = ContentType.PersonMiddleName
                        contentDataType = ContentDataType.Text
                    },
            )
        }
    }
}

@Composable
internal fun AutofillTextScreen() {
    Column {
        TextField(
            value = data.firstName,
            onValueChange = {},
            label = { Text("Enter first name here: ") },
            modifier =
                Modifier.semantics {
                    contentType = ContentType.PersonFirstName
                    contentDataType = ContentDataType.Text
                },
        )
        TextField(
            value = data.lastName,
            onValueChange = {},
            label = { Text("Enter last name here: ") },
            modifier =
                Modifier.semantics {
                    contentType = ContentType.PersonLastName
                    contentDataType = ContentDataType.Text
                },
        )
        TextField(
            value = data.firstName,
            onValueChange = {},
            label = { Text("Enter first name here: ") },
            modifier =
                Modifier.semantics {
                    contentType = ContentType.PersonFirstName
                    contentDataType = ContentDataType.Text
                },
        )
        TextField(
            value = data.lastName,
            onValueChange = {},
            label = { Text("Enter last name here: ") },
            modifier =
                Modifier.semantics {
                    contentType = ContentType.PersonLastName
                    contentDataType = ContentDataType.Text
                },
        )
    }
}

@Composable
internal fun AutofillScreen() {
    Scaffold(
        content = { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Navigation Button backwards
                Button(onClick = {}, modifier = Modifier.align(Alignment.Start)) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to route.")
                }

                // Navigation Button forwards
                Button(onClick = {}, modifier = Modifier.align(Alignment.Start)) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to next screen")
                }

                Spacer(modifier = Modifier.height(16.dp))
                AutofillTextScreen()
            }
        }
    )
}

internal data class PersonData(
    var title: String = "",
    var firstName: String = "",
    var middleName: String = "",
    var lastName: String = "",
    var age: Int = 0,
)

internal val data =
    PersonData(title = "Mr ", firstName = "John ", middleName = "Ace ", lastName = "Doe, ", age = 1)

internal val dataFlipped =
    PersonData(
        title = "Ms ",
        firstName = "Jane ",
        middleName = "Art ",
        lastName = "Deer, ",
        age = 2,
    )
