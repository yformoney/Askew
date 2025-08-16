package com.yy.askew.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yy.askew.map.data.PlaceSearchRepository
import com.yy.askew.map.data.PlaceSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaceSearchViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PlaceSearchRepository(application)
    
    private val _searchResults = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    val searchResults: StateFlow<List<PlaceSuggestion>> = _searchResults.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun searchPlaces(query: String, city: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = repository.searchPlaces(query, city)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearResults() {
        _searchResults.value = emptyList()
    }
    
    fun getPopularPlaces(): List<PlaceSuggestion> {
        return repository.getPopularPlaces()
    }
    
    fun getSearchHistory(): List<PlaceSuggestion> {
        return repository.getSearchHistory()
    }
    
    fun saveSearchHistory(suggestion: PlaceSuggestion) {
        repository.saveSearchHistory(suggestion)
    }
}