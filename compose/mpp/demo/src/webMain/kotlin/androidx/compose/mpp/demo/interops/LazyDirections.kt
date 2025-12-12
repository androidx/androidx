import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LazyDirections() {
    val colors = listOf(
        Color.Black,
        Color.LightGray,
        Color.DarkGray,
        Color.Gray
    )

    val rows = 20
    val columns = 20

    val rowHeight = 200.dp

    var layoutDirection by remember { mutableStateOf(LayoutDirection.Ltr) }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                layoutDirection = when (layoutDirection) {
                    LayoutDirection.Ltr -> LayoutDirection.Rtl
                    LayoutDirection.Rtl -> LayoutDirection.Ltr
                }
            }) {
                Text("Toggle layout direction")
            }
            LazyColumn(
                Modifier.fillMaxSize().padding(all = 20.dp),
            ) {
                items(rows) { row ->
                    LazyRow(Modifier.height(rowHeight).fillMaxSize()) {
                        items(columns) { col ->
                            val itemColor = colors[(row + col) % colors.size]
                            val elementId = "$row:$col"

                            Box(
                                Modifier
                                    .size(rowHeight, rowHeight)
                                    .background(itemColor)
                                ,
                                contentAlignment = Alignment.Center
                            ) {
                                HtmlElementView(
                                    modifier = Modifier.size(50.dp).background(Color.Yellow).padding(5.dp),
                                    factory = {
                                        (document.createElement("div") as HTMLDivElement).apply {
                                            innerText = elementId
                                            id = elementId
                                            style.apply {
                                                textAlign = "center"
                                                border = "2px solid white"
                                                boxSizing = "border-box"
                                                background = "white"
                                                color = "black"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
