package com.mp.web_searching

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dark.plugins.api.PluginApi
import com.mp.web_searching.Common.TAG
import com.mp.web_searching.screens.ToolingScreen
import com.mp.web_searching.viewModels.ToolScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject

class WebSearchPlugin(appContext: Context) : PluginApi(appContext) {
    private val job = SupervisorJob()
    private val ioScope = CoroutineScope(job + Dispatchers.IO)

    // create once outside Composable
    private val viewModel: ToolScreenViewModel by lazy {
        ToolScreenViewModel()
    }

    @Composable
    override fun ToolPreviewContent(data: String) {
        ToolingScreen(data = data, viewModel = viewModel)
    }

    @Composable
    override fun AppContent() {
        ToolingScreen(data = null, viewModel = viewModel)
    }

    @Keep
    override fun runTool(
        context: Context, toolName: String, args: JSONObject, callback: (result: Any) -> Unit
    ) {
        viewModel.runTool(ioScope, toolName, args, callback)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Log.d(TAG, "onDestroy: scope cancelled")
    }
}
