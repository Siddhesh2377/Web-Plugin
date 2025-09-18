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
    private lateinit var viewModel: ToolScreenViewModel

    @Composable
    override fun ToolPreviewContent() {
        viewModel = viewModel()
        ToolingScreen(scope = ioScope)
    }

    @Composable
    override fun AppContent() {
        ToolPreviewContent()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Log.d(TAG, "onDestroy: scope cancelled")
    }

    @Keep
    override fun runTool(
        context: Context, toolName: String, args: JSONObject, callback: (result: Any) -> Unit
    ) {
        super.runTool(context, toolName, args, callback)
        viewModel.runTool(ioScope, toolName, args, callback)
    }
}