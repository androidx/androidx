package foo.flavor

import androidx.navigation.NavDirections

class RegisterFragmentDirections private constructor() {
    companion object {
        fun actionDone(): NavDirections = LoginDirections.actionDone()
    }
}
