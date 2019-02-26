package foo

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

class SettingsDirections private constructor() {
    private data class Exit(val exitReason: String = "DEFAULT") : NavDirections {
        override fun getActionId(): Int = R.id.exit

        override fun getArguments(): Bundle {
            val result = Bundle()
            result.putString("exitReason", this.exitReason)
            return result
        }
    }

    companion object {
        fun exit(exitReason: String = "DEFAULT"): NavDirections = Exit(exitReason)
    }
}
