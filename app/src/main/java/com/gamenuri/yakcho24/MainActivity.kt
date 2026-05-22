package com.gamenuri.yakcho24

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.gamenuri.yakcho24.defines.E_AppState
import com.gamenuri.yakcho24.ui.component.BottomNavBar
import com.gamenuri.yakcho24.ui.navigation.NavGraph
import com.gamenuri.yakcho24.ui.theme.Yakcho24Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            Yakcho24Theme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val stateManager = Yakcho24App.appManager.stateManager

    // 2. [핵심] Navigation의 목적지 변화를 감시하여 StateManager와 동기화
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val route = backStackEntry.destination.route
            // route 이름을 기반으로 State 전환 (E_AppState 정의에 맞춰 매핑)
            when (route) {
                E_AppState.CAMERA.route -> stateManager.changeState(E_AppState.CAMERA)
                E_AppState.DICTIONARY.route -> stateManager.changeState(E_AppState.DICTIONARY)
                E_AppState.MAP.route -> stateManager.changeState(E_AppState.MAP)
                E_AppState.MY_PAGE.route -> stateManager.changeState(E_AppState.MY_PAGE)
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
