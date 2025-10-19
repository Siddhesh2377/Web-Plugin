package com.mp.web_plugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.mp.web_plugin.ui.theme.WebPluginTheme
import com.mp.web_searching.WebSearchPlugin
import com.mp.web_searching.screens.ToolingScreen
import org.json.JSONObject

class ToolsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WebPluginTheme {
                Scaffold {
                    Column(Modifier.fillMaxSize().padding(it)) {
                        ToolingScreen(data = JSONObject().apply {
                            put("query", "collage")
                        }.toString())
                    }
                }
            }
        }
    }
}