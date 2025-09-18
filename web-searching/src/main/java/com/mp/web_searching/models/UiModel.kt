package com.mp.web_searching.models

sealed interface UiState {
    data object Idle : UiState
    data class Loading(val label: String) : UiState
    data class SearchSuccess(val data: SearchResponse) : UiState
    data class FetchSuccess(val data: PageSummary) : UiState
    data class Error(val message: String) : UiState
}