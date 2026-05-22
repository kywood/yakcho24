package com.gamenuri.yakcho24.modules.state

import com.gamenuri.yakcho24.defines.E_AppState


class MyPageState : abState(E_AppState.MY_PAGE) {

    override fun enter() {
        super.enter() // 부모의 파이프라인 가동 로직 실행
    }

    override fun leave() {
        super.enter() // 부모의 파이프라인 가동 로직 실행
    }
}

