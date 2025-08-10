package com.yy.askew.http.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.askew.http.HttpManager
import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.TokenResponse
import com.yy.askew.http.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val authRepository = HttpManager.getAuthRepository()
    
    private val _loginState = MutableStateFlow<ApiResult<TokenResponse>?>(null)
    val loginState: StateFlow<ApiResult<TokenResponse>?> = _loginState
    
    private val _userInfo = MutableStateFlow<ApiResult<UserInfo>?>(null)
    val userInfo: StateFlow<ApiResult<UserInfo>?> = _userInfo
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = ApiResult.Loading()
            val result = authRepository.login(username, password)
            _loginState.value = result
            
            if (result is ApiResult.Success) {
                fetchUserInfo()
            }
        }
    }
    
    fun fetchUserInfo() {
        viewModelScope.launch {
            _userInfo.value = ApiResult.Loading()
            val result = authRepository.getUserInfo()
            _userInfo.value = result
        }
    }
    
    fun logout() {
        authRepository.clearTokens()
        _loginState.value = null
        _userInfo.value = null
    }
    
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
}