package com.yy.askew

import android.os.Debug
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private val processMemoryInfo = Debug.MemoryInfo()
}