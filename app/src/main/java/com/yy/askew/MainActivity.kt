package com.yy.askew

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yy.askew.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                PerformanceDashboard()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPerformanceDashboard() {
    AppTheme {
        PerformanceDashboard()
    }
}