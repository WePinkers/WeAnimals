package com.example.weanimals.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.weanimals.components.WeAnimalsBottomBar
import com.example.weanimals.navigation.BottomNavItem

@Composable
fun MainScreen(
    onNavigateToReport: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Home) }

    Scaffold(
        bottomBar = {
            WeAnimalsBottomBar(
                currentRoute = currentTab.route,
                onItemSelected = { selectedItem ->
                    currentTab = selectedItem
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                BottomNavItem.Home -> HomeScreen(
                    onNavigateToMap = { currentTab = BottomNavItem.Map },
                    onReportClick = onNavigateToReport
                )
                BottomNavItem.Map -> MapScreen()
                BottomNavItem.Community -> CommunityScreen()
                BottomNavItem.Adoption -> AdoptionScreen()
                BottomNavItem.Profile -> ProfileScreen()
            }
        }
    }
}
