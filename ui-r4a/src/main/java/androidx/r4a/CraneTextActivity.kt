package androidx.r4a

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.TextOverflow
import com.google.r4a.setContent

class CraneTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { <TextDemo /> }
    }
}