package org.m0skit0.android.emtmadridalarms.data

import org.m0skit0.android.emtmadridalarms.BuildConfig

data class EmtCredentials(
    val email: String,
    val password: String,
    val clientId: String,
    val passKey: String,
) {
    val hasUsableCredentials: Boolean =
        (email.isNotBlank() && password.isNotBlank()) || (clientId.isNotBlank() && passKey.isNotBlank())

    companion object {
        fun fromBuildConfig(): EmtCredentials = EmtCredentials(
            email = BuildConfig.EMT_EMAIL,
            password = BuildConfig.EMT_PASSWORD,
            clientId = BuildConfig.EMT_CLIENT_ID,
            passKey = BuildConfig.EMT_PASS_KEY,
        )
    }
}
