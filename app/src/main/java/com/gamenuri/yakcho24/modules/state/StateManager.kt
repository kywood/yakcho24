package com.gamenuri.yakcho24.modules.state

import android.util.Log
import com.gamenuri.yakcho24.defines.E_State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StateManager {

    private val stateContainer : MutableMap<E_State, IState> = mutableMapOf()

    // 만약 외부로 노출하는 변수와 내부 변수를 구분해야 한다면 (Backing Property)
    private val _currentState = MutableStateFlow<IState?>(null) // 내부용 (언더바)
    val currentState = _currentState.asStateFlow()            // 외부용 (공개)

    fun registerState(eState : E_State, state : IState) {
        stateContainer[eState] = state
    }


    fun changeState( eNextState : E_State) {

        if (_currentState.value is abState && (_currentState.value as abState).stateType == eNextState) {
            Log.d("StateManager", "이미 $eNextState 상태입니다.")
            return
        }


        val newState = stateContainer[eNextState]

        if (newState == null) {
            Log.e("StateManager", "등록되지 않은 상태 타입입니다: $eNextState")
            return
        }

        _currentState.value?.leave()
        _currentState.value = newState
        newState.enter()

    }

}