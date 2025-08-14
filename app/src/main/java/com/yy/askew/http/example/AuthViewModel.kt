package com.yy.askew.http.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.askew.http.HttpManager
import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.LoginResponse
import com.yy.askew.http.model.UserInfo
import com.yy.askew.http.model.UserProfileResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val authRepository = HttpManager.getAuthRepository()
    
    private val _loginState = MutableStateFlow<ApiResult<LoginResponse>?>(null)
    val loginState: StateFlow<ApiResult<LoginResponse>?> = _loginState
    
    private val _userProfile = MutableStateFlow<ApiResult<UserProfileResponse>?>(null)
    val userProfile: StateFlow<ApiResult<UserProfileResponse>?> = _userProfile
    
    private val _logoutState = MutableStateFlow<ApiResult<*>?>(null)
    val logoutState: StateFlow<ApiResult<*>?> = _logoutState
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = ApiResult.Loading()
            val result = authRepository.login(username, password)
            _loginState.value = result
        }
    }
    
    fun fetchUserProfile() {
        viewModelScope.launch {
            _userProfile.value = ApiResult.Loading()
            val result = authRepository.getUserProfile()
            _userProfile.value = result
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            _logoutState.value = ApiResult.Loading()
            val result = authRepository.logout()
            _logoutState.value = result
            
            _loginState.value = null
            _userProfile.value = null
        }
    }
    
    fun getCurrentUser(): UserInfo? {
        return authRepository.getCurrentUser()
    }
    
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
}