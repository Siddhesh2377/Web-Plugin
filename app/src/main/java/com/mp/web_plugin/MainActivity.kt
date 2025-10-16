package com.mp.web_plugin

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mp.web_plugin.ui.theme.WebPluginTheme
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retrieve URL and form data from Intent extras
        val url = intent.getStringExtra("url")
        val nameData = intent.getStringExtra("name") // Example: "John Doe"
        val emailData = intent.getStringExtra("email") // Example: "john.doe@example.com"

        // Basic validation
        if (url == null) {
            Log.e("MainActivity", "URL not provided in Intent extras")
            // Optionally, show an error message to the user or finish the activity
            finish()
            return
        }

        setContent {
            WebPluginTheme {
                // We'll use AndroidView to embed the WebView in Compose
                AutomatedWebView(
                    url = url,
                    formData = mapOf("name" to nameData, "email" to emailData)
                )
            }
        }
    }
}

@Composable
fun AutomatedWebView(url: String, formData: Map<String, String?>) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // Enable JavaScript
                settings.javaScriptEnabled = true

                // Set a custom WebViewClient to inject JavaScript after page load
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, currentUrl: String?) {
                        super.onPageFinished(view, currentUrl)

                        // Ensure JavaScript is injected only on the target page
                        if (currentUrl == url) {
                            val name = formData["name"]
                            val email = formData["email"]

                            // Construct the JavaScript payload
                            // IMPORTANT: Replace the selectors below with the actual selectors
                            // for your form's input fields and submit button.
                            // You can find these using your browser's developer tools.
                            val script = """
                                (function() {
                                    // Example: Fill the 'name' field (by name attribute)
                                    // var nameField = document.getElementsByName('your_name_field_name_attribute')[0];
                                    // if (nameField) {
                                    //     nameField.value = '${name ?: ""}';
                                    //     console.log('Name field filled with: ${name ?: ""}');
                                    // } else {
                                    //     console.log('Name field not found');
                                    // }

                                    // Example: Fill the 'name' field (by ID)
                                    var nameFieldById = document.getElementById('textInputId_name'); // Replace 'textInputId_name'
                                    if (nameFieldById) {
                                        nameFieldById.value = '${name ?: ""}';
                                        console.log('Name field (ID) filled with: ${name ?: ""}');
                                    } else {
                                        console.log('Name field (ID) not found by ID: textInputId_name');
                                    }

                                    // Example: Fill the 'email' field (by name attribute)
                                    // var emailField = document.getElementsByName('your_email_field_name_attribute')[0];
                                    // if (emailField) {
                                    //    emailField.value = '${email ?: ""}';
                                    //    console.log('Email field filled with: ${email ?: ""}');
                                    // } else {
                                    //    console.log('Email field not found');
                                    // }

                                    // Example: Fill the 'email' field (by ID)
                                     var emailFieldById = document.getElementById('textInputId_email'); // Replace 'textInputId_email'
                                     if (emailFieldById) {
                                         emailFieldById.value = '${email ?: ""}';
                                         console.log('Email field (ID) filled with: ${email ?: ""}');
                                     } else {
                                         console.log('Email field (ID) not found by ID: textInputId_email');
                                     }
                                    
                                    // Example: Click the submit button (by type and class, or ID)
                                    // var submitButton = document.querySelector('button[type="submit"]'); // General submit button
                                    // var submitButton = document.getElementById('submit_button_id'); // Replace 'submit_button_id'
                                    var submitButton = document.querySelector('.quantumWizButtonPaperbuttonContent.exportButtonContent'); // Example for a Google Form submit button
                                    if (submitButton) {
                                        console.log('Submit button found, clicking...');
                                        submitButton.click();
                                    } else {
                                        console.log('Submit button not found with selector.');
                                    }
                                })();
                            """.trimIndent()

                            // Execute the JavaScript
                            // The result of the JavaScript execution (if any) can be obtained in the callback
                            view?.evaluateJavascript(script) { result ->
                                Log.d("WebViewAutomation", "JavaScript execution result: $result")
                                // You can add further logic here based on the JS execution result
                            }
                        }
                    }
                }
                // Load the initial URL
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize() // Make the WebView fill the available space
    )
}
