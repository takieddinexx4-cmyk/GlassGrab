package com.glassgrab.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glassgrab.core.domain.model.DownloadTask
import com.glassgrab.core.domain.model.LibrarySort
import com.glassgrab.core.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val query: String = "",
    val sort: LibrarySort = LibrarySort.NEWEST,
    val items: List<DownloadTask> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: DownloadRepository,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val sortFlow = MutableStateFlow(LibrarySort.NEWEST)

    val state: StateFlow<LibraryUiState> = combine(queryFlow, sortFlow) { q, s -> q to s }
        .flatMapLatest { (q, s) ->
            repository.observeLibrary(q, s).combine(MutableStateFlow(q to s)) { items, (query, sort) ->
                LibraryUiState(query = query, sort = sort, items = items)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onQueryChanged(query: String) {
        queryFlow.value = query
    }

    fun onSortChanged(sort: LibrarySort) {
        sortFlow.value = sort
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteFromLibrary(id, deleteFile = true)
        }
    }
}
