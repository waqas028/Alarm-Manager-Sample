package com.waqas028.alarm_manager_sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.waqas028.alarm_manager_sample.ui.theme.AlarmManagerSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlarmManagerSampleTheme {
                AlarmSchedulerScreen()
            }
        }
    }
}