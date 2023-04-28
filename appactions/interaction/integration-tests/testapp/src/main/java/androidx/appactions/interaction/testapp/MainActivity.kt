package androidx.appactions.interaction.testapp

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
  private var duration = 59
  private var hasRunningTimer = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val hours: TextView = findViewById(R.id.hour)
    val mins: TextView = findViewById(R.id.minute)
    val seconds: TextView = findViewById(R.id.second)
    val startButton: AppCompatButton = findViewById(R.id.startButton)

    startButton.setOnClickListener {
      if (!hasRunningTimer) {
        hasRunningTimer = true
        object : CountDownTimer((duration * 1000).toLong(), 1000) {
          override fun onTick(millisUntilFinished: Long) {
            runOnUiThread {
              val time = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                  TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                ),
                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                  TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                )
              )
              val hourMinSecArray = time.split(":")
              hours.text = hourMinSecArray[0]
              mins.text = hourMinSecArray[1]
              seconds.text = hourMinSecArray[2]
            }
          }

          override fun onFinish() {
            // Reset timer duration
            duration = 59
            hasRunningTimer = false
          }
        }.start()
      } else {
        Toast.makeText(this@MainActivity, "Timer is already running", Toast.LENGTH_SHORT).show()
      }
    }
  }
}