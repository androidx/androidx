import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Map() {
    val ttCoordinates = "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d243.2605247413949!2d4.892652290246295!3d52.337941149334604!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x47c60b47904314db%3A0xf4b8123a04612968!2sJetBrains%20N.V.%20(Main%20Building)!5e0!3m2!1sen!2snl!4v1748355529708!5m2!1sen!2snl"

    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    ) {
        HtmlElementView(
            factory = {
                (document.createElement("iframe") as HTMLIFrameElement).apply {
                    src = ttCoordinates
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { iframe -> iframe.src = iframe.src }
        )
    }
}