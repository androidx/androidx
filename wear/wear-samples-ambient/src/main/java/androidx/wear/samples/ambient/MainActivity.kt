package androidx.wear.samples.ambient

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import java.text.SimpleDateFormat
import java.util.Date

/** Sample activity that provides an ambient experience. */
class MainActivity :
    FragmentActivity(R.layout.activity_main),
    AmbientModeSupport.AmbientCallbackProvider {

    /** Used to dispatch periodic updates when the activity is in active mode. */
    private val activeUpdatesHandler = Handler(Looper.getMainLooper())

    /** The view model for this activity. */
    private val model = MainViewModel()

    /** The controller for ambient mode, initialized when the activity is created. */
    private lateinit var ambientController: AmbientModeSupport.AmbientController

    // The views that are part of the activity.
    private val timerTextView by lazy { findViewById<TextView>(R.id.timer) }
    private val statusTextView by lazy { findViewById<TextView>(R.id.status) }
    private val timestampTextView by lazy { findViewById<TextView>(R.id.timestamp) }
    private val updatesTextView by lazy { findViewById<TextView>(R.id.updates) }

    /** Invoked on [activeUpdatesHandler], posts an update when the activity is in active mode. */
    private val mActiveUpdatesRunnable: Runnable =
        Runnable {
            // If invoked in ambient mode, do nothing.
            if (ambientController.isAmbient) {
                return@Runnable
            }
            model.publishUpdate()
            // Schedule the next update.
            schedule()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        observeModel()
        ambientController = AmbientModeSupport.attach(this)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        // Update the last resume time and post the first update in this mode.
        model.updateResumeTime()
        model.publishUpdate()
        schedule()
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun getAmbientCallback() = object : AmbientModeSupport.AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle) {
            super.onEnterAmbient(ambientDetails)
            Log.d(TAG, "onEnterAmbient()")
            model.setStatus(Status.AMBIENT)
            model.publishUpdate()
        }

        override fun onUpdateAmbient() {
            super.onUpdateAmbient()
            Log.d(TAG, "onUpdateAmbient()")
            model.publishUpdate()
        }

        override fun onExitAmbient() {
            super.onExitAmbient()
            Log.d(TAG, "onExitAmbient()")
            model.setStatus(Status.ACTIVE)
            model.publishUpdate()
            schedule()
        }
    }

    private fun observeModel() {
        model.observeStartTime(this) {
            timerTextView.text = formatTimer(model.getTimer())
        }
        model.observeStatus(this) { status ->
            statusTextView.text = "Status: $status"
        }
        model.observeUpdates(this) { updates ->
            updatesTextView.text = formatUpdates(updates)
        }
        model.observeUpdateTimestamp(this) { timestamp ->
            timestampTextView.text = formatTimestamp(timestamp)
            timerTextView.text = formatTimer(model.getTimer())
        }
    }

    private fun schedule() {
        val now = System.currentTimeMillis()
        activeUpdatesHandler.postDelayed(mActiveUpdatesRunnable, 100 - now % 100)
    }

    private fun formatTimer(timer: Long): String {
        val minutes = timer / 60 / 1000
        val seconds = String.format("%02d", timer / 1000 % 60)
        val tenths = timer / 100 % 10
        return "Since resume: $minutes:$seconds.$tenths"
    }

    private fun formatUpdates(updates: Number): String = "#Updates: $updates"

    private fun formatTimestamp(timestamp: Long): String =
        "At ${SimpleDateFormat("HH:mm:ss.SSS").format(Date(timestamp))}"

    private companion object {
        private const val TAG = "AmbientSample"
    }
}