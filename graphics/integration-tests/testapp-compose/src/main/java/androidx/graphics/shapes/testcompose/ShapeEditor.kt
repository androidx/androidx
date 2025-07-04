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

package androidx.graphics.shapes.testcompose

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.graphics.shapes.Feature
import androidx.graphics.shapes.FeatureSerializer
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.SvgPathParser

class ShapeEditor : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent(parent = null) { MaterialTheme { PolygonEditor(this) } }
    }
}

@Composable
private fun PolygonEditor(activity: FragmentActivity) {
    val shapeParams = remember { materialShapes().map { mutableStateOf(it) }.toMutableStateList() }

    var selectedStartShape by remember { mutableIntStateOf(5) }
    var selectedEndShape by remember { mutableIntStateOf(16) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedShape = if (selectedIndex == 0) selectedStartShape else selectedEndShape
    val currentScreenType: MutableState<ScreenTypes> = remember { mutableStateOf(ScreenTypes.HOME) }

    val changeSelected = { shapeIndex: Int ->
        if (selectedIndex == 0) selectedStartShape = shapeIndex else selectedEndShape = shapeIndex
    }

    val screens: Map<ScreenTypes, @Composable () -> Unit> =
        mapOf(
            ScreenTypes.HOME to
                {
                    HomeScreen(
                        shapeParams,
                        selectedStartShape,
                        selectedEndShape,
                        selectedShape,
                        selectedIndex,
                        onSelectedSwitch = { index -> selectedIndex = index },
                        onShapeSwitch = changeSelected,
                        onEditClick = { currentScreenType.value = ScreenTypes.EDIT },
                        onAboutClick = { currentScreenType.value = ScreenTypes.ABOUT },
                        onExportClick = {
                            exportShape(activity, shapeParams[selectedShape].value.serialized())
                        },
                        onAddShapeClick = {
                            shapeParams.add(mutableStateOf(ShapeParameters("Custom")))
                            changeSelected(shapeParams.lastIndex)
                            // Give this shape a chance to be visualized, as the visualization
                            // will initialize/sync properties like the feature overlay and we do
                            // not
                            // want a prompt for a 'save changes?' when users go back immediately
                            shapeParams.last().value.genShape()
                            currentScreenType.value = ScreenTypes.EDIT
                        },
                        onImportShapeClick = { parseFunction: () -> List<Feature> ->
                            val features: List<Feature> = parseFunction()
                            shapeParams.add(
                                mutableStateOf(
                                    CustomShapeParameters("Custom") {
                                        RoundedPolygon(features).normalized()
                                    }
                                )
                            )
                            changeSelected(shapeParams.lastIndex)
                        },
                    )
                },
            ScreenTypes.EDIT to
                {
                    EditScreen(
                        shapeParams[selectedShape].value,
                        onBackClick = { currentScreenType.value = ScreenTypes.HOME },
                        onSave = { newParams: ShapeParameters ->
                            shapeParams[selectedShape].value = newParams
                            currentScreenType.value = ScreenTypes.HOME
                        },
                    )
                },
            ScreenTypes.ABOUT to
                {
                    AboutScreen(onBackClick = { currentScreenType.value = ScreenTypes.HOME })
                },
        )

    screens[currentScreenType.value]?.let { it() }
}

@Composable
private fun EditScreen(
    parameters: ShapeParameters,
    onBackClick: () -> Unit,
    onSave: (ShapeParameters) -> Unit,
) {
    var showSaveMessage by remember { mutableStateOf(false) }
    val copyToEdit = remember { parameters.copy() }
    var selectedViewModeIndex by remember { mutableIntStateOf(0) }

    if (showSaveMessage) {
        SaveConfirmationDialog(
            onDismissRequest = {
                showSaveMessage = false
                onBackClick()
            },
            onConfirmation = { onSave(copyToEdit) },
        )
    }

    Scaffold(
        topBar = {
            EditScreenHeader(
                selectedViewModeIndex,
                onBackClick = {
                    if (!copyToEdit.equals(parameters)) showSaveMessage = true else onBackClick()
                },
                onModeSwitch = { index -> selectedViewModeIndex = index },
            )
        },
        bottomBar = { EditScreenFooter(onCancel = onBackClick, onSave = { onSave(copyToEdit) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (selectedViewModeIndex == 0) {
                ParametricEditor(copyToEdit)
            } else {
                FeatureEditor(copyToEdit)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreenHeader(
    selectedIndex: Int,
    onBackClick: () -> Unit,
    onModeSwitch: (Int) -> Unit,
) {
    val options = listOf("Parametric", "Features")

    TopAppBar(
        title = { Text("Shape Edit") },
        actions = {
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape =
                            SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onModeSwitch(index) },
                        selected = index == selectedIndex,
                        icon = {},
                    ) {
                        Text(label)
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go back to Home")
            }
        },
    )
}

@Composable
private fun EditScreenFooter(onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Cancel",
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Text("Cancel")
        }
        Spacer(Modifier.width(12.dp))
        Button(onClick = onSave, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Save",
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Text("Save")
        }
    }
}

@Composable
private fun HomeScreen(
    shapeParams: SnapshotStateList<MutableState<ShapeParameters>>,
    selectedStartShape: Int,
    selectedEndShape: Int,
    selectedShape: Int,
    selectedIndex: Int,
    onSelectedSwitch: (Int) -> Unit,
    onShapeSwitch: (Int) -> Unit,
    onEditClick: () -> Unit,
    onAboutClick: () -> Unit,
    onExportClick: () -> Unit,
    onAddShapeClick: () -> Unit,
    onImportShapeClick: (() -> List<Feature>) -> Unit,
) {
    val shapes =
        remember(shapeParams.size) { shapeParams.map { sp -> sp.value.genShape().normalized() } }

    var showImportMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HomeScreenHeader(
                selectedIndex,
                shapes[selectedStartShape],
                shapes[selectedEndShape],
                onSelectedSwitch,
            )
        },
        bottomBar = { HomeScreenFooter(onAboutClick, onExportClick) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (showImportMessage) {
                NewShapeDialog(
                    { showImportMessage = false },
                    {
                        onAddShapeClick()
                        showImportMessage = false
                    },
                    {
                        onImportShapeClick(it)
                        showImportMessage = false
                    },
                )
            }

            ShapesGallery(
                shapes,
                selectedShape,
                if (selectedIndex == 0) selectedEndShape else selectedStartShape,
                Modifier.fillMaxHeight(0.35f).verticalScroll(rememberScrollState()),
                onShapeSwitch,
            )

            EditButtonRow(onEditClick) { showImportMessage = true }

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(thickness = 2.dp)

            AnimatedMorphView(shapes, selectedStartShape, selectedEndShape)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenHeader(
    selectedIndex: Int,
    selectedStartShape: RoundedPolygon,
    selectedEndShape: RoundedPolygon,
    onSwitch: (Int) -> Unit,
) {
    val options = listOf("From", "To")

    TopAppBar(
        title = { Text("Shape Selection") },
        actions = {
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape =
                            SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onSwitch(index) },
                        selected = index == selectedIndex,
                        icon = {},
                        label = {
                            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                                Text(label)
                                Spacer(Modifier.size(10.dp))
                                if (index == 0) PolygonView(selectedStartShape)
                                else PolygonView(selectedEndShape)
                            }
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun HomeScreenFooter(onAboutClick: () -> Unit, onExportClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(onClick = onAboutClick, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Help",
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Text("Usage FAQ")
        }
        Spacer(Modifier.width(12.dp))
        Button(onClick = onExportClick, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.Share,
                contentDescription = "Export",
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Text("Export")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    val padding = 10.dp
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Go back to Home",
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(30.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))

            ExpandableQuestionCard(
                "What are Shape Features and why should I care about them?",
                AnnotatedString(
                    "Features create special target points on a shape that the morph algorithm uses to guide the shape's transformation. The algorithm will match Features of the same type (except 'None') between shapes. You can see and edit these points by going to Edit > Features. By understanding and manipulating Features, you can control how your shapes change during a morph."
                ),
                Modifier.padding(vertical = padding),
            )

            ExpandableQuestionCard(
                "How can I make my Morph symmetrical?",
                SYMMETRIC_SHAPE_ANSWER,
                Modifier.padding(vertical = padding),
            )

            ExpandableQuestionCard(
                "How can I add Shapes from Figma / Google Icons / other ?",
                CUSTOM_SHAPE_ANSWER,
                Modifier.padding(vertical = padding),
            )

            ExpandableQuestionCard(
                "I finished editing my Shape and want to maintain the changes in my code. What do I have to do?",
                AnnotatedString(
                    "To preserve your edited shape for future use, export it by clicking the 'Export' button on the Home Screen. This will generate a code snippet representing your shape. Copy this code and integrate it into your project's relevant file to load and use the shape within your project."
                ),
                Modifier.padding(vertical = padding),
            )
        }
    }
}

@Composable
private fun EditButtonRow(onEditClick: () -> Unit, onImportShapeClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(onClick = onEditClick, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit Shape",
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Text("Edit Shape")
        }
        Spacer(Modifier.width(12.dp))
        Button(onClick = onImportShapeClick, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.Add,
                contentDescription = "New Shape",
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Text("New Shape")
        }
    }
}

@Composable
private fun NewShapeDialog(
    onDismissRequest: () -> Unit,
    onAddShape: () -> Unit,
    onImportShape: (() -> List<Feature>) -> Unit,
) {
    var wantsSvgImport by remember { mutableStateOf(true) }
    val text = remember { mutableStateOf(TextFieldValue("")) }
    var selectedButton by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add Shape") },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text("How do create the new Shape?")

                LabelledRadioButton(selectedButton == 0, "Parametric Shape") { selectedButton = 0 }

                LabelledRadioButton(selectedButton == 1, "SVG Path Import") {
                    selectedButton = 1
                    wantsSvgImport = true
                }

                LabelledRadioButton(selectedButton == 2, "Serialized Shape Features") {
                    selectedButton = 2
                    wantsSvgImport = false
                }

                ImportTextField(text, wantsSvgImport, selectedButton != 0)
            }
        },
        icon = { Icon(Icons.Default.Add, "Add Shape") },
        confirmButton = {
            TextButton(
                onClick = {
                    when (selectedButton) {
                        0 -> onAddShape()
                        1 -> {
                            onImportShape { SvgPathParser.parseFeatures(text.value.text) }
                        }
                        2 -> onImportShape { FeatureSerializer.parse(text.value.text) }
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } },
    )
}

@Composable
private fun SaveConfirmationDialog(onDismissRequest: () -> Unit, onConfirmation: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Discard Changes?") },
        text = { Text("Any changes made to the shape will be lost.") },
        icon = { Icon(Icons.Default.Warning, "Discard Changes?") },
        confirmButton = { TextButton(onClick = onConfirmation) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Discard") } },
    )
}

@Composable
private fun ExpandableQuestionCard(
    title: String,
    content: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Card(
        modifier =
            modifier.fillMaxWidth().animateContentSize(animationSpec = tween(durationMillis = 250)),
        shape = RoundedCornerShape(16.dp),
        onClick = { isExpanded = !isExpanded },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(6f),
                    text = title,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    modifier = Modifier.weight(1f).rotate(rotation),
                    onClick = { isExpanded = !isExpanded },
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Content",
                    )
                }
            }
            if (isExpanded) {
                Text(
                    text = content,
                    fontSize = MaterialTheme.typography.titleSmall.fontSize,
                    fontWeight = FontWeight.Normal,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ImportTextField(text: MutableState<TextFieldValue>, importsSvgPath: Boolean, enabled: Boolean) {
    var hasBeenEdited by remember { mutableStateOf(false) }

    if (!hasBeenEdited) {
        text.value = TextFieldValue(if (importsSvgPath) TRIANGE_SVG_PATH else SERIALIZED_TRIANGLE)
    }

    TextField(
        value = text.value,
        onValueChange = {
            text.value = it
            hasBeenEdited = true
        },
        enabled = enabled,
        minLines = 10,
        maxLines = 20,
        label = {
            Text(
                if (importsSvgPath) "Svg Path ('d' attribute in svg files)"
                else "Serialized Shape Features String"
            )
        },
    )
}

@Composable
fun LabelledRadioButton(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick, enabled = true)
        Text(label)
    }
}

private fun AnnotatedString.Builder.addParagraph(header: String, content: String) {
    appendLine()
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(header) }
    withStyle(ParagraphStyle()) { append(content) }
}

private fun exportShape(activity: FragmentActivity, serialization: String) {
    println(serialization)
    val sendIntent: Intent =
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, serialization)
            type = "text/plain"
        }
    val shareIntent = Intent.createChooser(sendIntent, null)
    activity.startActivity(shareIntent)
}

private enum class ScreenTypes {
    HOME,
    EDIT,
    ABOUT,
}

private val CUSTOM_SHAPE_ANSWER = buildAnnotatedString {
    append("Here's a general approach to import shapes from various sources into your design tool:")
    appendLine()
    addParagraph(
        "1. Export the Shape",
        "Figma: Export the shape as an SVG file.\nGoogle Icons: Download the icon in SVG format.\nOther Sources: Export the shape in a vector format like SVG or PDF.",
    )
    addParagraph(
        "2. Obtain the Path Data",
        "Open the SVG file in a text editor and locate the '<path>' element. Copy the value of the 'd' attribute. This attribute contains the path data, which is a sequence of commands that define the shape's geometry.",
    )
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Example (triangle)") }
    withStyle(ParagraphStyle()) { append(TRIANGE_SVG_PATH) }

    addParagraph(
        "3. Import the Path Data",
        "In the app, click the 'Import' button. It will open an field that you can paste the path data into. Finally, your shape will be added to the gallery and is ready for editing.",
    )
}

private val SYMMETRIC_SHAPE_ANSWER = buildAnnotatedString {
    append(
        "To achieve symmetry in your Morph, focus on the Features of your shapes. These are the special points that guide the morphing process."
    )
    appendLine()
    addParagraph(
        "1. Identify Symmetrical Areas",
        "Determine the parts of your shapes that you want to be symmetrical.",
    )
    addParagraph(
        "2. Match Feature Types",
        "Ensure that the Features corresponding to these symmetrical areas have the same type (e.g., both inward or outward indentation).",
    )
    addParagraph(
        "3. Align Feature Points",
        "Check if the anchor points of these Features align. If not, you may need to split your shapes to create additional Features and fine-tune the alignment.",
    )
    appendLine()
    append(
        "By carefully aligning and matching Features, you can create smooth and symmetrical Morphs."
    )
}

private const val TRIANGE_SVG_PATH = "M 0 0 0.5 1 1 0 Z"
private const val SERIALIZED_TRIANGLE =
    "V1n0.5,1.5,0.167,0.83,-0.167,0.167,-0.5,-0.5x-0.5,-0.5,-0.5,-0.5,-0.5,-0.5,-0.5,-0.5n-0.5,-0.5,0.167,-0.5,0.83,-0.5,1.5,-0.5x1.5,-0.5,1.5,-0.5,1.5,-0.5,1.5,-0.5n1.5,-0.5,1.167,0.167,0.83,0.83,0.5,1.5x0.5,1.5,0.5,1.5,0.5,1.5,0.5,1.5"
