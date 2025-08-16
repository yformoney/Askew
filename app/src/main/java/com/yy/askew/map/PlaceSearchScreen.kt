package com.yy.askew.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.yy.askew.map.data.PlaceSuggestion

@Composable
fun PlaceSearchScreen(
    onBackClick: () -> Unit,
    onPlaceSelected: (PlaceSuggestion) -> Unit,
    searchViewModel: PlaceSearchViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    
    // 搜索防抖
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            delay(500) // 500ms防抖
            searchViewModel.searchPlaces(searchQuery)
        } else {
            searchViewModel.clearResults()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部搜索栏
        TopSearchHeader(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onBackClick = onBackClick,
            onClearClick = { searchQuery = "" }
        )
        
        // 搜索结果或建议列表
        if (searchQuery.isEmpty()) {
            // 显示历史记录和热门地点
            DefaultSuggestions(
                onPlaceSelected = onPlaceSelected,
                searchViewModel = searchViewModel
            )
        } else {
            // 显示搜索结果
            SearchResults(
                results = searchResults,
                isLoading = isLoading,
                onPlaceSelected = { suggestion ->
                    searchViewModel.saveSearchHistory(suggestion)
                    onPlaceSelected(suggestion)
                }
            )
        }
    }
}

@Composable
private fun TopSearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索地点，如：广州南、天河城") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearClick) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除"
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
        }
    }
}

@Composable
private fun DefaultSuggestions(
    onPlaceSelected: (PlaceSuggestion) -> Unit,
    searchViewModel: PlaceSearchViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        // 历史搜索
        val history = searchViewModel.getSearchHistory()
        if (history.isNotEmpty()) {
            item {
                Text(
                    text = "历史搜索",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(history) { suggestion ->
                PlaceItem(
                    suggestion = suggestion,
                    icon = Icons.Default.Star,
                    onClick = { onPlaceSelected(suggestion) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // 热门地点
        item {
            Text(
                text = "热门地点",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(searchViewModel.getPopularPlaces()) { suggestion ->
            PlaceItem(
                suggestion = suggestion,
                icon = Icons.Default.LocationOn,
                onClick = { onPlaceSelected(suggestion) }
            )
        }
    }
}

@Composable
private fun SearchResults(
    results: List<PlaceSuggestion>,
    isLoading: Boolean,
    onPlaceSelected: (PlaceSuggestion) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("搜索中...")
                }
            }
        } else if (results.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "未找到相关地点",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(results) { suggestion ->
                PlaceItem(
                    suggestion = suggestion,
                    icon = Icons.Default.LocationOn,
                    onClick = { onPlaceSelected(suggestion) }
                )
            }
        }
    }
}

@Composable
private fun PlaceItem(
    suggestion: PlaceSuggestion,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = suggestion.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (suggestion.address.isNotEmpty()) {
                Text(
                    text = suggestion.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (suggestion.district.isNotEmpty()) {
                Text(
                    text = suggestion.district,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}