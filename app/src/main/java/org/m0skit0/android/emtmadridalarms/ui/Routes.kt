package org.m0skit0.android.emtmadridalarms.ui

internal object Routes {
    const val SETUP = "setup"
    const val SELECT_LINE = "select-line"
    const val SELECT_STOP = "select-stop"
    const val MONITORING = "monitoring"
    const val RINGING = "ringing"

    val ALARM_ROUTES = setOf(SETUP, MONITORING, RINGING)
}
