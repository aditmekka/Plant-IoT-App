package id.mekka.iot.plant

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.slider.Slider
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var progressBarPlantA: ProgressBar
    private lateinit var progressBarPlantB: ProgressBar
    private lateinit var textPlantAHumidity: TextView
    private lateinit var textPlantBHumidity: TextView
    private lateinit var textMoistureThreshold: TextView
    private lateinit var sliderMoistureThreshold: Slider
    private lateinit var switchPump: Switch
    private lateinit var buttonServoPlantA: Button
    private lateinit var buttonServoPlantB: Button
    private lateinit var deviceStatus: TextView

    private val handler = Handler()
    private val updateRunnable: Runnable = object : Runnable {
        override fun run() {
            readData()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBarPlantA = findViewById(R.id.progressBarPlantA)
        progressBarPlantB = findViewById(R.id.progressBarPlantB)
        textPlantAHumidity = findViewById(R.id.textPlantAHumidity)
        textPlantBHumidity = findViewById(R.id.textPlantBHumidity)
        textMoistureThreshold = findViewById(R.id.textMoistureTreshold)
        sliderMoistureThreshold = findViewById(R.id.sliderMoistureThreshold)
        switchPump = findViewById(R.id.switchPump)
        buttonServoPlantA = findViewById(R.id.buttonServoPlantA)
        buttonServoPlantB = findViewById(R.id.buttonServoPlantB)
        deviceStatus = findViewById(R.id.deviceStatus)

        database = FirebaseDatabase.getInstance().getReference("Sensor")

        sliderMoistureThreshold.addOnChangeListener { slider, value, fromUser ->
            textMoistureThreshold.text = "Moisture Threshold: ${value.toInt()}%"
            updateMoistureThreshold(value.toInt())
        }

        switchPump.setOnCheckedChangeListener { _, isChecked ->
            updatePumpState(isChecked)
        }

        buttonServoPlantA.setOnClickListener {
            updateServoPosition(0)
        }

        buttonServoPlantB.setOnClickListener {
            updateServoPosition(1)
        }

        checkDeviceAlive()

        handler.post(updateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable) // Stop updates when activity is destroyed
    }

    private fun updateMoistureThreshold(value: Int) {
        database = FirebaseDatabase.getInstance().getReference("userInput")
        database.child("moistureTreshold").setValue(value)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update Moisture Threshold", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePumpState(isOn: Boolean) {
        database = FirebaseDatabase.getInstance().getReference("userInput")
        database.child("pumpState").setValue(isOn)
            .addOnSuccessListener {
                Toast.makeText(this, "Pump state updated to $isOn", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update Pump state", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateServoPosition(position: Int) {
        database = FirebaseDatabase.getInstance().getReference("userInput")
        database.child("servoPos").setValue(position)
            .addOnSuccessListener {
                Toast.makeText(this, "Servo position updated to $position", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update Servo position", Toast.LENGTH_SHORT).show()
            }
    }

    private fun readData() {
        database = FirebaseDatabase.getInstance().getReference("Sensor")
        database.child("SoilMoisture-A").get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val moistureValA: Int = dataSnapshot.value.toString().toInt()
                progressBarPlantA.progress = moistureValA
                textPlantAHumidity.text = "Plant A Humidity: $moistureValA%"
                Toast.makeText(this, "Successfully read sensor A", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Path does not exist.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "FAILED to read data for Plant A", Toast.LENGTH_SHORT).show()
        }

        database = FirebaseDatabase.getInstance().getReference("Sensor")
        database.child("SoilMoisture-B").get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val moistureValB: Int = dataSnapshot.value.toString().toInt()
                progressBarPlantB.progress = moistureValB
                textPlantBHumidity.text = "Plant B Humidity: $moistureValB%"
                Toast.makeText(this, "Successfully read sensor B", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Path does not exist.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "FAILED to read data for Plant B", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkDeviceAlive() {
        val handler = Handler()
        val runnableCode = object : Runnable {
            override fun run() {
                database = FirebaseDatabase.getInstance().getReference("thingStat")
                database.child("lastSeen").get().addOnSuccessListener { dataSnapshot ->
                    val lastSeenTimestamp = dataSnapshot.getValue(Long::class.java)

                    val currentTime = Calendar.getInstance().timeInMillis / 1000

                    if (lastSeenTimestamp != null) {
                        val timeDifference = currentTime - lastSeenTimestamp
                        Log.d("DeviceStatus", "Current time: $currentTime, Last seen: $lastSeenTimestamp, Difference: $timeDifference")

                        if (timeDifference <= 10) {
                            deviceStatus.text = "Device is: ON"
                        } else {
                            deviceStatus.text = "Device is: OFF"
                        }
                    } else {
                        deviceStatus.text = "Device is: OFF"
                    }
                }.addOnFailureListener {
                    deviceStatus.text = "Device is: OFF"
                    Log.e("DeviceStatus", "Failed to read lastSeen")
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(runnableCode)
    }
}
