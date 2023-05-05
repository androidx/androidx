package androidx.appactions.interaction.testapp

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class MainActivity : AppCompatActivity() {
  private var duration = 120
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
              val timeArray = millisecondsToTimeString(millisUntilFinished).split(":")
              hours.text = timeArray[0]
              mins.text = timeArray[1]
              seconds.text = timeArray[2]
            }
          }

          override fun onFinish() {
            // Reset timer duration
            duration = 120
            hasRunningTimer = false
          }
        }.start()
      } else {
        Toast.makeText(this@MainActivity, "Timer is already running", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun millisecondsToTimeString(milliseconds: Long): String {
    val hours = milliseconds / 3600000
    val minutes = (milliseconds % 3600000) / 60000
    val seconds = (milliseconds % 60000) / 1000
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
  }
}