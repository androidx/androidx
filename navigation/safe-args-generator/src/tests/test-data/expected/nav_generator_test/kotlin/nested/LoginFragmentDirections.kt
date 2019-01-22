package foo.flavor

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

class LoginFragmentDirections private constructor() {
    companion object {
        fun register(): NavDirections = ActionOnlyNavDirections(foo.R.id.register)

        fun actionDone(): NavDirections = LoginDirections.actionDone()
    }
}
