package com.yy.askew

import TopArcRoundedRectDemo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yy.askew.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                TopArcRoundedRectDemo()
                Spacer(modifier = Modifier.height(100.dp))
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