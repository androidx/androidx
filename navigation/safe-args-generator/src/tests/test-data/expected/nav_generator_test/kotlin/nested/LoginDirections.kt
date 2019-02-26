package foo

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

class LoginDirections private constructor() {
    companion object {
        fun actionDone(): NavDirections = ActionOnlyNavDirections(R.id.action_done)
    }
}
