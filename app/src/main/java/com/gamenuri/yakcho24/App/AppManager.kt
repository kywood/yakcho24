package com.gamenuri.yakcho24.App

import android.content.Context
import com.gamenuri.yakcho24.defines.E_AppState
import com.gamenuri.yakcho24.defines.E_State
import com.gamenuri.yakcho24.modules.state.CameraState
import com.gamenuri.yakcho24.modules.state.DictionaryState
import com.gamenuri.yakcho24.modules.state.MapState
import com.gamenuri.yakcho24.modules.state.MyPageState
import com.gamenuri.yakcho24.modules.state.StateManager

class AppManager(val context: Context){

    val stateManager = StateManager()

    init {
        // 2. 초기화 시점에 각 상태들을 등록 (Factory 역할 대행 가능)
        setupStates()
    }

    private fun setupStates() {
        stateManager.registerState(E_AppState.CAMERA, CameraState())
        stateManager.registerState(E_AppState.MAP, MapState())
        stateManager.registerState(E_AppState.DICTIONARY, DictionaryState())
        stateManager.registerState(E_AppState.MY_PAGE, MyPageState())

        // 초기 상태 설정
        stateManager.changeState(E_AppState.CAMERA)
    }

}