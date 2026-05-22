package com.gamenuri.yakcho24.defines


interface E_State

enum class E_AppState(val route: String) : E_State {
    CAMERA("camera"),      // 카메라 스캔 화면
    DICTIONARY("dictionary"),
    MAP("map"),
    MY_PAGE("mypage")
}

enum class E_CameraState : E_State {
    INITIALIZING,
    SCANNING,
    DETECTED
}





