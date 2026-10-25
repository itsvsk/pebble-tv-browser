package com.tvbrowser.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvbrowser.core.db.repo.TabRepository
import com.tvbrowser.core.webview.TabManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val tabs: TabRepository,
    private val tabManager: TabManager,
) : ViewModel() {
    fun openBrowser(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val existing = tabs.persisted().first().maxByOrNull { it.lastActiveAt }
            onReady(existing?.id ?: tabManager.newTab("https://www.google.com"))
        }
    }
}
