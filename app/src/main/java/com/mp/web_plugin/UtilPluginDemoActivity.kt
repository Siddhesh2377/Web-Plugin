package com.mp.web_plugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mp.web_plugin.ui.theme.WebPluginTheme
import com.mp.web_searching.UtilPlugin
import org.json.JSONObject

class UtilPluginDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WebPluginTheme {
                Scaffold {
                    Box(Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center){
                        val plugin = UtilPlugin(this@UtilPluginDemoActivity)
                        plugin.AppContent()
                    }
                }
            }
        }
    }
}