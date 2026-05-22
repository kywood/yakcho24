package com.gamenuri.yakcho24.modules.state

import com.gamenuri.yakcho24.defines.E_State


interface IState {
    fun enter()
    fun leave()

}

abstract class abState(val stateType: E_State): IState
{
    override fun enter() {

    }

    override fun leave() {

    }

}