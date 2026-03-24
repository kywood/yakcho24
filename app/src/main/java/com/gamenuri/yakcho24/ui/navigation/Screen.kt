package com.gamenuri.yakcho24.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Camera : Screen("camera", "카메라", Icons.Default.CameraAlt)
    data object HerbList : Screen("herb_list", "도감", Icons.Default.LocalFlorist)
    data object Map : Screen("map", "지도", Icons.Default.Map)
    data object MyPage : Screen("my_page", "마이페이지", Icons.Default.Person)
}

val bottomNavItems = listOf(
    Screen.Camera,
    Screen.HerbList,
    Screen.Map,
    Screen.MyPage,
)
