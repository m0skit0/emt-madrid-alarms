package org.m0skit0.android.emtmadridalarms.utils

inline fun <T> T?.orDefault(block: () -> T): T = this ?: block()