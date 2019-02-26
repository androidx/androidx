package foo

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

class InnerSettingsDirections private constructor() {
    private data class Exit(val exitReason: Int) : NavDirections {
        override fun getActionId(): Int = R.id.exit

        override fun getArguments(): Bundle {
            val result = Bundle()
            result.putInt("exitReason", this.exitReason)
            return result
        }
    }

    companion object {
        fun exit(exitReason: Int): NavDirections = Exit(exitReason)

        fun main(enterReason: String = "DEFAULT"): NavDirections =
                SettingsDirections.main(enterReason)
    }
}
