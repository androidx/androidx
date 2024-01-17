package androidx.appactions.interaction.testapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

const val KEY_IS_TIMER_PAUSED = "timer_paused"
const val KEY_IS_TIMER_RUNNING = "timer_running"
const val DEFAULT_DURATION = 120 * 1000L

class MainActivity : AppCompatActivity() {
    private var remainingDuration = 0L
    private var hasRunningTimer = false
    private var startButton: AppCompatButton? = null
    private lateinit var timer: CountDownTimer
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var remainingTimeLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = applicationContext
            .getSharedPreferences(KEY_TIMER_SHARED_PREFERENCES, Context.MODE_PRIVATE)
        startButton = findViewById(R.id.startButton)
        remainingTimeLabel = findViewById(R.id.remainingTimeLabel)
        hasRunningTimer = sharedPreferences.getBoolean(KEY_IS_TIMER_RUNNING, false)

        if (hasRunningTimer) {
            remainingDuration = sharedPreferences.getLong(KEY_TIMER_DURATION, 0L)
            if (!sharedPreferences.getBoolean(KEY_IS_TIMER_PAUSED, false)) {
                val startedTime = sharedPreferences.getLong(KEY_TIMER_STARTED_AT, 0L)
                val elapsedTime = System.currentTimeMillis() - startedTime
                remainingDuration = remainingDuration - elapsedTime
            }
            startTimer(remainingDuration)
        }

        startButton?.apply {
            if (!hasRunningTimer) {
                text = "Start"
            } else if (sharedPreferences.getBoolean(KEY_IS_TIMER_PAUSED, false)) {
                text = "Resume"
            } else {
                text = "Pause"
            }
            setOnClickListener {
                setTimer(DEFAULT_DURATION)
            }
        }
    }
    private fun setTimer(duration: Long) {
        hasRunningTimer = sharedPreferences.getBoolean(KEY_IS_TIMER_RUNNING, false)
        if (!hasRunningTimer) {
            startButton?.text = "Pause"
            startTimer(duration)
        } else if (sharedPreferences.getBoolean(KEY_IS_TIMER_PAUSED, false)) {
            startButton?.text = "Pause"
            resumeTimer()
        } else {
            startButton?.text = "Resume"
            pauseTimer()
        }
    }
    private fun startTimer(duration: Long) {
        sharedPreferences.edit().putBoolean(KEY_IS_TIMER_RUNNING, true).apply()
        timer = object : CountDownTimer(duration, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread {
                    this@MainActivity.remainingDuration = millisUntilFinished
                    val timeArray = millisecondsToTimeString(millisUntilFinished).split(":")
                    remainingTimeLabel.text = "$timeArray[0]:$timeArray[1]:$timeArray[2]"
                }
            }
            override fun onFinish() {
                // Reset timer duration
                this@MainActivity.remainingDuration = 0
                sharedPreferences.edit().putBoolean(KEY_IS_TIMER_RUNNING, false).apply()
            }
        }
        timer.start()
    }

    private fun pauseTimer() {
        timer.cancel()
        sharedPreferences.edit().putBoolean(KEY_IS_TIMER_PAUSED, true).apply()
        sharedPreferences.edit().putLong(KEY_TIMER_DURATION, this.remainingDuration).apply()
    }

    private fun resumeTimer() {
        val duration = sharedPreferences.getLong(KEY_TIMER_DURATION, DEFAULT_DURATION)
        sharedPreferences.edit().putBoolean(KEY_IS_TIMER_PAUSED, false).apply()
        sharedPreferences.edit().putLong(KEY_TIMER_STARTED_AT, System.currentTimeMillis()).apply()
        startTimer(duration)
    }

    private fun stopTimer() {
        sharedPreferences.edit().putBoolean(KEY_IS_TIMER_RUNNING, false).apply()
        sharedPreferences.edit().putBoolean(KEY_IS_TIMER_PAUSED, false).apply()
    }

    private fun millisecondsToTimeString(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        val seconds = (milliseconds % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
