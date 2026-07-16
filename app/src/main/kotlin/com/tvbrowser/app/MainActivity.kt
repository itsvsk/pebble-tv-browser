package com.tvbrowser.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvbrowser.app.navigation.AppNavHost
import com.tvbrowser.core.design.PebbleTheme
import com.tvbrowser.core.cursor.CursorController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var cursorController: CursorController
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cursorState by cursorController.state.collectAsState()
            PebbleTheme {
                AppNavHost(cursorState = cursorState)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (cursorController.handleKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }
}

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _ready = MutableStateFlow(true)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()
}
