package org.m0skit0.android.emtmadridalarms.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.state.AppState
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder
import java.io.IOException

class EmtAuthTokenProviderTest {

    private lateinit var api: EmtApi
    private lateinit var globalState: GlobalStateHolder

    private val validCredentials = EmtCredentials(
        email = "user@test.com", password = "pass", clientId = "", passKey = ""
    )

    @Before
    fun setup() {
        api = mockk()
        globalState = GlobalStateHolder(AppState())
    }

    @Test
    fun `given a valid cached token, when provider is invoked, then the cached token is returned without a network call`() = runTest {
        val futureExpiry = System.currentTimeMillis() + 120_000L
        globalState.update { it.copy(emtAuthToken = EmtAuthTokenState(token = "cached", expiresAtMillis = futureExpiry)) }
        val provider = provideToken(api, validCredentials, globalState)

        val token = provider()

        token shouldBe "cached"
    }

    @Test
    fun `given no cached token, when provider is invoked, then a new token is fetched and stored`() = runTest {
        coEvery { api.login(any(), any(), any(), any()) } returns EmtLoginResponse(
            code = "00",
            data = listOf(EmtLoginData(accessToken = "newtoken", tokenSecExpiration = 900))
        )
        val provider = provideToken(api, validCredentials, globalState)

        val token = provider()

        token shouldBe "newtoken"
        globalState.state.emtAuthToken.token shouldBe "newtoken"
        globalState.state.emtAuthToken.expiresAtMillis shouldNotBe 0L
    }

    @Test
    fun `given an expired cached token, when provider is invoked, then a fresh token is fetched`() = runTest {
        val pastExpiry = System.currentTimeMillis() - 1_000L
        globalState.update { it.copy(emtAuthToken = EmtAuthTokenState(token = "old", expiresAtMillis = pastExpiry)) }
        coEvery { api.login(any(), any(), any(), any()) } returns EmtLoginResponse(
            code = "00",
            data = listOf(EmtLoginData(accessToken = "fresh", tokenSecExpiration = 900))
        )
        val provider = provideToken(api, validCredentials, globalState)

        val token = provider()

        token shouldBe "fresh"
    }

    @Test
    fun `given a token expiring within 60 seconds, when provider is invoked, then a renewed token is fetched`() = runTest {
        val soonExpiry = System.currentTimeMillis() + 30_000L
        globalState.update { it.copy(emtAuthToken = EmtAuthTokenState(token = "expiring", expiresAtMillis = soonExpiry)) }
        coEvery { api.login(any(), any(), any(), any()) } returns EmtLoginResponse(
            code = "00",
            data = listOf(EmtLoginData(accessToken = "renewed", tokenSecExpiration = 900))
        )
        val provider = provideToken(api, validCredentials, globalState)

        val token = provider()

        token shouldBe "renewed"
    }

    @Test
    fun `given an API response with an empty token, when provider is invoked, then an IOException with the description is thrown`() = runTest {
        coEvery { api.login(any(), any(), any(), any()) } returns EmtLoginResponse(
            code = "01",
            description = "Unauthorised",
            data = listOf(EmtLoginData(accessToken = null))
        )
        val provider = provideToken(api, validCredentials, globalState)

        val ex = shouldThrow<IOException> { provider() }
        ex.message shouldBe "Unauthorised"
    }

    @Test
    fun `given an API response with a null description, when provider is invoked, then an IOException with a fallback message is thrown`() = runTest {
        coEvery { api.login(any(), any(), any(), any()) } returns EmtLoginResponse(
            code = "01",
            description = null,
            data = listOf(EmtLoginData(accessToken = ""))
        )
        val provider = provideToken(api, validCredentials, globalState)

        val ex = shouldThrow<IOException> { provider() }
        ex.message shouldBe "EMT login failed"
    }

    @Test
    fun `given passKey credentials, when provider is invoked, then passKey auth headers are sent`() = runTest {
        val passKeyCredentials = EmtCredentials(email = "", password = "", clientId = "cid", passKey = "pkey")
        coEvery { api.login(null, null, "cid", "pkey") } returns EmtLoginResponse(
            code = "00",
            data = listOf(EmtLoginData(accessToken = "pktoken", tokenSecExpiration = 900))
        )
        val provider = provideToken(api, passKeyCredentials, globalState)

        val token = provider()

        token shouldBe "pktoken"
    }
}
