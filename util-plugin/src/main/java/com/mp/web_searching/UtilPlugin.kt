package com.mp.web_searching

import android.content.Context
import androidx.annotation.Keep
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dark.plugins.api.PluginApi
import org.json.JSONObject

class UtilPlugin(appContext: Context) : PluginApi(appContext) {

    @Composable
    override fun ToolPreviewContent(data: String) {

    }

    @Composable
    override fun AppContent() {
        val neuralState = rememberNeuralNetworkState()

        var currentConfig by remember { mutableStateOf(NeuralThemes.MidnightMoss) }


        Card(modifier = Modifier.size(350.dp, 550.dp)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Email Writer",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium
                    )
                )

                Spacer(Modifier.height(24.dp))

                Card(Modifier.size(200.dp), shape = CircleShape) {
                    FuturisticNeuralAnimation(
                        modifier = Modifier.fillMaxSize(),
                        config = currentConfig,
                        state = neuralState
                    )
                }
            }
        }
    }

    @Keep
    override fun runTool(
        context: Context, toolName: String, args: JSONObject, callback: (result: Any) -> Unit
    ) {

    }

}
