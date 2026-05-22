package org.m0skit0.android.emtmadridalarms.di

import org.koin.dsl.module
import org.m0skit0.android.emtmadridalarms.state.AppState
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder

val stateModule = module {
    single { GlobalStateHolder(AppState()) }
}
