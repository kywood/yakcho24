package com.gamenuri.yakcho24.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gamenuri.yakcho24.ui.screen.camera.CameraScreen
import com.gamenuri.yakcho24.ui.screen.herb.HerbDetailScreen
import com.gamenuri.yakcho24.ui.screen.herb.HerbListScreen
import com.gamenuri.yakcho24.ui.screen.map.HerbMapScreen
import com.gamenuri.yakcho24.ui.screen.auth.MyPageScreen

const val ROUTE_HERB_DETAIL = "herb_detail/{classId}"
fun herbDetailRoute(classId: Int) = "herb_detail/$classId"

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Camera.route,
        modifier = modifier,
    ) {
        composable(Screen.Camera.route) { CameraScreen() }
        composable(Screen.HerbList.route) { HerbListScreen(navController) }
        composable(Screen.Map.route) { HerbMapScreen() }
        composable(Screen.MyPage.route) { MyPageScreen() }
        composable(ROUTE_HERB_DETAIL) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId")?.toIntOrNull() ?: 0
            HerbDetailScreen(classId = classId, onBack = { navController.popBackStack() })
        }
    }
}
