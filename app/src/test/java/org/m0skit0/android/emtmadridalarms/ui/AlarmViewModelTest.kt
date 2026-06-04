package org.m0skit0.android.emtmadridalarms.ui

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.m0skit0.android.emtmadridalarms.data.AlarmStateReader
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.domain.MAX_ACTIVE_ALARMS
import org.m0skit0.android.emtmadridalarms.domain.PersistedAlarmState

class AlarmViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private lateinit var state: MutableStateFlow<AlarmState>
    private lateinit var vm: AlarmViewModel

    private var loadLinesCallCount = 0
    private var loadAllStopsCallCount = 0
    private var lastLoadStopsArg: BusLine? = null
    private var startAlarmArg: BusAlarmRequest? = null
    private var cancelSingleAlarmArg: BusAlarmRequest? = null
    private var cancelAlarmCalled = false
    private var stopRingingCalled = false
    private var buildRequestResult: Result<BusAlarmRequest> = Result.success(BusAlarmRequest("", "", 0))

    private val loadLines = LineLoader { loadLinesCallCount++ }
    private val loadAllStops = AllStopLoader { loadAllStopsCallCount++ }
    private val loadStops = StopLoader { lastLoadStopsArg = it }
    private val startAlarm = AlarmStarter { startAlarmArg = it }
    private val cancelSingleAlarm = SingleAlarmCanceller { cancelSingleAlarmArg = it }
    private val cancelAlarm = AlarmCanceller { cancelAlarmCalled = true }
    private val stopRinging = RingingStop { stopRingingCalled = true }
    private val buildRequest = AlarmRequestBuilder { buildRequestResult }

    private fun emptyPersisted() = PersistedAlarmState(
        activeAlarm = null, latestEtaSeconds = null,

        latestDestination = "", statusMessage = "", isRinging = false,
    )

    @Before
    fun setup() {
        loadLinesCallCount = 0
        loadAllStopsCallCount = 0
        lastLoadStopsArg = null
        startAlarmArg = null
        cancelSingleAlarmArg = null
        cancelAlarmCalled = false
        stopRingingCalled = false
        buildRequestResult = Result.success(BusAlarmRequest("", "", 0))

        state = MutableStateFlow(AlarmState())

        vm = AlarmViewModel(
            _state = state,
            scope = scope,
            alarmStateReader = AlarmStateReader { flowOf(emptyPersisted()) },
            loadLines = loadLines,
            loadAllStops = loadAllStops,
            loadStops = loadStops,
            buildRequest = buildRequest,
            startAlarm = startAlarm,
            cancelSingleAlarm = cancelSingleAlarm,
            cancelAlarm = cancelAlarm,
            stopRinging = stopRinging,
        )
    }

    @Test
    fun `given the viewmodel is created, when init runs, then loadLines is called once`() {
        loadLinesCallCount shouldBe 1
    }

    @Test
    fun `given a persisted alarm state, when init collects from alarmStateReader, then state is synced`() = scope.runTest {
        val request = BusAlarmRequest("1", "100", 5)
        val persisted = PersistedAlarmState(
            activeAlarm = request, latestEtaSeconds = 120,
            latestDestination = "Centro", statusMessage = "On time", isRinging = true,
            activeAlarms = listOf(request),
        )
        val freshState = MutableStateFlow(AlarmState())
        AlarmViewModel(
            _state = freshState, scope = scope,
            alarmStateReader = AlarmStateReader { flowOf(persisted) },
            loadLines = loadLines, loadAllStops = loadAllStops, loadStops = loadStops, buildRequest = buildRequest,
            startAlarm = startAlarm, cancelSingleAlarm = cancelSingleAlarm, cancelAlarm = cancelAlarm, stopRinging = stopRinging,
        )
        dispatcher.scheduler.advanceUntilIdle()

        freshState.value.activeAlarm shouldBe request
        freshState.value.activeAlarms shouldBe listOf(request)
        freshState.value.latestEtaSeconds shouldBe 120
        freshState.value.latestDestination shouldBe "Centro"
        freshState.value.statusMessage shouldBe "On time"
        freshState.value.isRinging shouldBe true
    }

    @Test
    fun `given a state with a selected stop, when LineChanged is dispatched, then stop selection and error are cleared`() {
        state.value = AlarmState(stopInput = "old", selectedStop = BusStop("1", "X", "Y"), errorMessage = "err")
        vm.dispatch(AlarmIntent.LineChanged("27"))
        state.value.lineInput shouldBe "27"
        state.value.stopInput shouldBe ""
        state.value.selectedLine shouldBe null
        state.value.selectedStop shouldBe null
        state.value.stops shouldBe emptyList()
        state.value.errorMessage shouldBe null
    }

    @Test
    fun `given a bus line, when LineSelected is dispatched, then state is updated and loadStops is called`() {
        val line = BusLine("27", "27", "A", "B")
        vm.dispatch(AlarmIntent.LineSelected(line))
        state.value.selectedLine shouldBe line
        state.value.lineInput shouldBe line.displayName
        state.value.isLoadingStops shouldBe true
        lastLoadStopsArg shouldBe line
    }

    @Test
    fun `given a state with a selected stop, when StopChanged is dispatched, then stop selection and error are cleared`() {
        state.value = AlarmState(selectedStop = BusStop("1", "X", "Y"), errorMessage = "e")
        vm.dispatch(AlarmIntent.StopChanged("Gran"))
        state.value.stopInput shouldBe "Gran"
        state.value.selectedStop shouldBe null
        state.value.errorMessage shouldBe null
    }

    @Test
    fun `given a bus stop, when StopSelected is dispatched, then stop input and selection are updated`() {
        val stop = BusStop("62", "Gran Via", "Calle 1")
        vm.dispatch(AlarmIntent.StopSelected(stop))
        state.value.selectedStop shouldBe stop
        state.value.stopInput shouldBe stop.displayName
        state.value.errorMessage shouldBe null
    }

    @Test
    fun `given a bus stop selected first, when StopSelected is dispatched, then lines are filtered to matching lines`() {
        val line27 = BusLine("027", "27", "A", "B")
        val line34 = BusLine("034", "34", "C", "D")
        val stop = BusStop("62", "Gran Via", "Calle 1", lineLabels = setOf("27"))
        state.value = AlarmState(allLines = listOf(line27, line34), lines = listOf(line27, line34))

        vm.dispatch(AlarmIntent.StopSelected(stop))

        state.value.selectedStop shouldBe stop
        state.value.selectedLine shouldBe null
        state.value.lineInput shouldBe ""
        state.value.lines shouldBe listOf(line27)
    }

    @Test
    fun `given a selected line and compatible stop, when StopSelected is dispatched, then line selection is kept`() {
        val line = BusLine("027", "27", "A", "B")
        val stop = BusStop("62", "Gran Via", "Calle 1", lineLabels = setOf("27"))
        state.value = AlarmState(selectedLine = line, lineInput = line.displayName, allLines = listOf(line), lines = listOf(line))

        vm.dispatch(AlarmIntent.StopSelected(stop))

        state.value.selectedLine shouldBe line
        state.value.lineInput shouldBe line.displayName
        state.value.selectedStop shouldBe stop
    }

    @Test
    fun `given a cached all stops list, when StopPickerOpened is dispatched without a selected line, then stops are shown`() {
        val stop = BusStop("62", "Gran Via", "Calle 1")
        state.value = AlarmState(allStops = listOf(stop))

        vm.dispatch(AlarmIntent.StopPickerOpened)

        state.value.stops shouldBe listOf(stop)
        loadAllStopsCallCount shouldBe 0
    }

    @Test
    fun `given no cached all stops list, when StopPickerOpened is dispatched without a selected line, then all stops are loaded`() {
        vm.dispatch(AlarmIntent.StopPickerOpened)

        loadAllStopsCallCount shouldBe 1
    }

    @Test
    fun `given an input with non-digit characters, when MinutesChanged is dispatched, then only digits are retained`() {
        vm.dispatch(AlarmIntent.MinutesChanged("1a2b3"))
        state.value.minutesInput shouldBe "123"
    }

    @Test
    fun `given the viewmodel is initialised, when RefreshLinesClicked is dispatched, then loadLines is called again`() {
        vm.dispatch(AlarmIntent.RefreshLinesClicked)
        loadLinesCallCount shouldBe 2
    }

    @Test
    fun `given an active alarm, when CancelClicked is dispatched, then cancelAlarm is called`() {
        vm.dispatch(AlarmIntent.CancelClicked)
        cancelAlarmCalled shouldBe true
    }

    @Test
    fun `given an active alarm, when CancelAlarmClicked is dispatched, then cancelSingleAlarm is called`() {
        val request = BusAlarmRequest("1", "100", 5)

        vm.dispatch(AlarmIntent.CancelAlarmClicked(request))

        cancelSingleAlarmArg shouldBe request
    }

    @Test
    fun `given a ringing alarm, when StopRingingClicked is dispatched, then stopRinging is called`() {
        vm.dispatch(AlarmIntent.StopRingingClicked)
        stopRingingCalled shouldBe true
    }

    @Test
    fun `given an error message in state, when ErrorShown is dispatched, then errorMessage is cleared`() {
        state.value = AlarmState(errorMessage = "some error")
        vm.dispatch(AlarmIntent.ErrorShown)
        state.value.errorMessage shouldBe null
    }

    @Test
    fun `given a valid alarm request, when StartClicked is dispatched, then startAlarm is called with the request`() {
        val request = BusAlarmRequest("1", "100", 5)
        buildRequestResult = Result.success(request)
        vm.dispatch(AlarmIntent.StartClicked)
        startAlarmArg shouldBe request
        state.value.errorMessage shouldBe null
    }

    @Test
    fun `given maximum active alarms, when StartClicked is dispatched, then errorMessage is set and startAlarm is not called`() {
        val alarms = (1..MAX_ACTIVE_ALARMS).map { BusAlarmRequest(it.toString(), "100", 5) }
        state.value = AlarmState(activeAlarm = alarms.first(), activeAlarms = alarms)

        vm.dispatch(AlarmIntent.StartClicked)

        state.value.errorMessage shouldBe "You can have up to $MAX_ACTIVE_ALARMS active alarms."
        startAlarmArg shouldBe null
    }

    @Test
    fun `given an invalid alarm request, when StartClicked is dispatched, then errorMessage is set and startAlarm is not called`() {
        buildRequestResult = Result.failure(IllegalArgumentException("bad input"))
        vm.dispatch(AlarmIntent.StartClicked)
        state.value.errorMessage shouldBe "bad input"
        startAlarmArg shouldBe null
    }
}
