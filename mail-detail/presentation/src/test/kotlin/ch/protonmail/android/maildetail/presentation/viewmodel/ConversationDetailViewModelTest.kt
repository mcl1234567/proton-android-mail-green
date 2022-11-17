/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.maildetail.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversationMessages
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMetadataUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.contact.domain.entity.Contact
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

class ConversationDetailViewModelTest {

    private val actionUiModelMapper = ActionUiModelMapper()
    private val conversationMetadataMapper: ConversationDetailMetadataUiModelMapper = mockk {
        every { toUiModel(ConversationSample.WeatherForecast) } returns
            ConversationDetailMetadataUiModelSample.WeatherForecast
    }
    private val conversationMessageMapper: ConversationDetailMessageUiModelMapper = mockk {
        every { toUiModel(message = MessageSample.AugWeatherForecast, contacts = any()) } returns
            ConversationDetailMessageUiModelSample.AugWeatherForecast
        every { toUiModel(message = MessageSample.SepWeatherForecast, contacts = any()) } returns
            ConversationDetailMessageUiModelSample.SepWeatherForecast
    }
    private val observeContacts: ObserveContacts = mockk {
        every { this@mockk(userId = UserIdSample.Primary) } returns flowOf(emptyList<Contact>().right())
    }
    private val observeConversation: ObserveConversation = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns
            flowOf(ConversationSample.WeatherForecast.right())
    }
    private val observeConversationMessages: ObserveConversationMessages = mockk {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns
            flowOf(listOf(MessageSample.AugWeatherForecast, MessageSample.SepWeatherForecast))
    }
    private val observeConversationDetailActions = mockk<ObserveConversationDetailActions> {
        every { this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns flowOf(
            listOf(Action.Reply, Action.Archive, Action.MarkUnread).right()
        )
    }
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns flowOf(UserIdSample.Primary)
    }
    private val reducer: ConversationDetailReducer = mockk {
        every { newStateFrom(currentState = any(), operation = any()) } returns ConversationDetailState.Loading
    }
    private val savedStateHandle: SavedStateHandle = mockk {
        every { get<String>(ConversationDetailScreen.ConversationIdKey) } returns
            ConversationIdSample.WeatherForecast.id
    }

    private val viewModel by lazy {
        ConversationDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            actionUiModelMapper = actionUiModelMapper,
            conversationMessageMapper = conversationMessageMapper,
            conversationMetadataMapper = conversationMetadataMapper,
            observeContacts = observeContacts,
            observeConversation = observeConversation,
            observeConversationMessages = observeConversationMessages,
            observeDetailActions = observeConversationDetailActions,
            reducer = reducer,
            savedStateHandle = savedStateHandle
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @Test
    fun `initial state is loading`() = runTest {
        // When
        viewModel.state.test {
            // Then
            assertEquals(ConversationDetailState.Loading, awaitItem())
        }
    }

    @Test
    fun `throws exception when conversation id parameter was not provided as input`() = runTest {
        // Given
        every { savedStateHandle.get<String>(ConversationDetailScreen.ConversationIdKey) } returns null
        // When
        val thrown = assertFailsWith<IllegalStateException> { viewModel.state }
        // Then
        assertEquals("No Conversation id given", thrown.message)
    }

    @Test
    fun `does handle user not logged in`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
        val expectedState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Error(TextUiModel(commonString.x_error_not_logged_in))
        )
        every { observePrimaryUserId() } returns flowOf(null)
        every { reducer.newStateFrom(initialState, ConversationDetailEvent.NoPrimaryUser) } returns expectedState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does handle conversation data`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
        val conversationUiModel = ConversationDetailMetadataUiModelSample.WeatherForecast
        val expectedState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Data(conversationUiModel)
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ConversationDetailEvent.ConversationData(conversationUiModel)
            )
        } returns expectedState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does handle conversation error`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
        val expectedState = initialState.copy(
            conversationState = ConversationDetailMetadataState.Error(
                message = TextUiModel(string.detail_error_loading_conversation)
            )
        )
        every { observeConversation(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns
            flowOf(DataError.Local.NoDataCached.left())
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ConversationDetailEvent.ErrorLoadingConversation
            )
        } returns expectedState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does handle conversation messages data`() = runTest {
        // given
        val initialState = ConversationDetailState.Loading
        val messagesUiModels = listOf(
            ConversationDetailMessageUiModelSample.AugWeatherForecast,
            ConversationDetailMessageUiModelSample.SepWeatherForecast
        )
        val expectedState = initialState.copy(
            messagesState = ConversationDetailsMessagesState.Data(messagesUiModels)
        )
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ConversationDetailEvent.MessagesData(messagesUiModels)
            )
        } returns expectedState

        // when
        viewModel.state.test {
            initialStateEmitted()

            // then
            assertEquals(expectedState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bottomBar state is data when use case returns actions`() = runTest {
        // Given
        val initialState = ConversationDetailState.Loading
        val actions = listOf(Action.Reply, Action.Archive)
        val actionUiModels = listOf(ActionUiModelTestData.reply, ActionUiModelTestData.archive)
        val expected = initialState.copy(bottomBarState = BottomBarState.Data(actionUiModels))
        every {
            observeConversationDetailActions(UserIdSample.Primary, ConversationIdSample.WeatherForecast)
        } returns flowOf(actions.right())
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ConversationDetailEvent.ConversationBottomBarEvent(
                    bottomBarEvent = BottomBarEvent.ActionsData(actionUiModels)
                )
            )
        } returns expected

        // when
        viewModel.state.test {
            initialStateEmitted()

            // Then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `bottomBar state is failed loading actions when use case returns error`() = runTest {
        // Given
        val initialState = ConversationDetailState.Loading
        val expected = initialState.copy(bottomBarState = BottomBarState.Error.FailedLoadingActions)
        every {
            observeConversationDetailActions(UserIdSample.Primary, ConversationIdSample.WeatherForecast)
        } returns flowOf(DataError.Local.NoDataCached.left())
        every {
            reducer.newStateFrom(
                currentState = initialState,
                operation = ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.ErrorLoadingActions)
            )
        } returns expected


        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            assertEquals(expected, awaitItem())
        }

    }

    private suspend fun FlowTurbine<ConversationDetailState>.initialStateEmitted() {
        assertEquals(ConversationDetailState.Loading, awaitItem())
    }
}
