package foo.flavor

import androidx.navigation.NavDirections
import kotlin.String

class SettingsFragmentDirections {
    companion object {
        fun exit(exitReason: String = "DEFAULT"): NavDirections =
                SettingsDirections.exit(exitReason)
    }
}
