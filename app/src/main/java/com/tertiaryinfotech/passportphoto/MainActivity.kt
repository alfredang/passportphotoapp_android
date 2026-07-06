package com.tertiaryinfotech.passportphoto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tertiaryinfotech.passportphoto.ui.PassportPhotoTheme
import com.tertiaryinfotech.passportphoto.ui.RootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PassportPhotoTheme {
                RootScreen()
            }
        }
    }
}
