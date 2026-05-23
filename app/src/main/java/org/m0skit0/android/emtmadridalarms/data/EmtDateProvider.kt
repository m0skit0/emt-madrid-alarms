package org.m0skit0.android.emtmadridalarms.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun interface EmtDateProvider : () -> String

internal fun emtDateProvider(): EmtDateProvider = EmtDateProvider { todayDateRef() }

internal fun todayDateRef(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
