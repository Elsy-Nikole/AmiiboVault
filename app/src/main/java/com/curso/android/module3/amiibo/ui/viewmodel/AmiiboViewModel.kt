package com.curso.android.module3.amiibo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curso.android.module3.amiibo.data.local.entity.AmiiboEntity
import com.curso.android.module3.amiibo.domain.error.ErrorType
import com.curso.android.module3.amiibo.repository.AmiiboRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AmiiboUiState {
    data object Loading : AmiiboUiState

    data class Success(
        val amiibos: List<AmiiboEntity>,
        val hasMorePages: Boolean = true,
        val isLoadingMore: Boolean = false,
        val paginationError: String? = null,
        val isRefreshing: Boolean = false
    ) : AmiiboUiState

    data class Error(
        val message: String,
        val cachedData: List<AmiiboEntity> = emptyList(),
        val hasMorePages: Boolean = false,
        val errorType: ErrorType = ErrorType.UNKNOWN,
        val isRetryable: Boolean = true
    ) : AmiiboUiState
}

class AmiiboViewModel(
    private val repository: AmiiboRepository
) : ViewModel() {

    // --- ESTADO ---
    private val _uiState = MutableStateFlow<AmiiboUiState>(AmiiboUiState.Loading)
    val uiState: StateFlow<AmiiboUiState> = _uiState.asStateFlow()

    // --- BÚSQUEDA ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- PAGINACIÓN ---
    private val _currentPage = MutableStateFlow(0)
    private val _pageSize = MutableStateFlow(AmiiboRepository.DEFAULT_PAGE_SIZE)
    private val _loadedAmiibos = MutableStateFlow<List<AmiiboEntity>>(emptyList())
    private val _hasMorePages = MutableStateFlow(true)

    init {
        // Al iniciar, combinamos la búsqueda con los datos
        observeAmiibos()
        // Carga inicial desde la API
        refreshAmiibos()
    }

    /**
     * Combina la búsqueda y la lista paginada.
     * Si hay búsqueda, muestra resultados de búsqueda.
     * Si no, muestra la lista cargada por paginación.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAmiibos() {
        _searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    // Si no hay búsqueda, usamos los que hemos cargado con scroll
                    _loadedAmiibos
                } else {
                    // Si hay búsqueda, preguntamos al repo (vía Room Flow)
                    repository.getAmiibos(query)
                }
            }
            .onEach { list ->
                // Actualizamos la UI con lo que venga (búsqueda o lista normal)
                if (list.isNotEmpty() || _uiState.value !is AmiiboUiState.Loading) {
                    _uiState.value = AmiiboUiState.Success(
                        amiibos = list,
                        hasMorePages = if (_searchQuery.value.isBlank()) _hasMorePages.value else false,
                        isRefreshing = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun loadNextPage() {
        // Solo permitimos cargar más si NO estamos buscando
        if (_searchQuery.value.isNotBlank()) return

        val currentState = _uiState.value
        if (currentState is AmiiboUiState.Success && !currentState.isLoadingMore && currentState.hasMorePages) {
            viewModelScope.launch {
                _uiState.value = currentState.copy(isLoadingMore = true, paginationError = null)
                try {
                    val nextPage = _currentPage.value + 1
                    val newItems = repository.getAmiibosPage(nextPage, _pageSize.value)

                    if (newItems.isNotEmpty()) {
                        _currentPage.value = nextPage
                        _loadedAmiibos.value = _loadedAmiibos.value + newItems
                        _hasMorePages.value = repository.hasMorePages(nextPage, _pageSize.value)
                        // El observer (observeAmiibos) se encarga de actualizar la UI
                    } else {
                        _hasMorePages.value = false
                        _uiState.value = currentState.copy(isLoadingMore = false, hasMorePages = false)
                    }
                } catch (e: Exception) {
                    _uiState.value = currentState.copy(
                        isLoadingMore = false,
                        paginationError = e.message ?: "Error al cargar más"
                    )
                }
            }
        }
    }

    fun refreshAmiibos() {
        viewModelScope.launch {
            try {
                // Si la lista está vacía, ponemos Loading
                if (_loadedAmiibos.value.isEmpty()) _uiState.value = AmiiboUiState.Loading

                repository.refreshAmiibos()

                // Reiniciamos paginación tras el refresh
                _currentPage.value = 0
                val firstPage = repository.getAmiibosPage(0, _pageSize.value)
                _loadedAmiibos.value = firstPage
                _hasMorePages.value = repository.hasMorePages(0, _pageSize.value)

            } catch (e: Exception) {
                _uiState.value = AmiiboUiState.Error(
                    message = e.message ?: "Error desconocido",
                    cachedData = _loadedAmiibos.value
                )
            }
        }
    }
}