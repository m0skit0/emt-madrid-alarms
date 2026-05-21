package org.m0skit0.android.emtmadridalarms.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmtDateProvider {
    fun todayDateRef(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
}
