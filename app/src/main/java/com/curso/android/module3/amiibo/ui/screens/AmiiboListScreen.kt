package com.curso.android.module3.amiibo.ui.screens

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
// Ensure you have these for the ViewModel and Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import coil3.compose.AsyncImage
import com.curso.android.module3.amiibo.R
import com.curso.android.module3.amiibo.data.local.entity.AmiiboEntity
import com.curso.android.module3.amiibo.domain.error.ErrorType
import com.curso.android.module3.amiibo.ui.viewmodel.AmiiboUiState
import com.curso.android.module3.amiibo.ui.viewmodel.AmiiboViewModel

/**
 * ============================================================================
 * AMIIBO LIST SCREEN - Pantalla Principal (Jetpack Compose)
 * ============================================================================
 *
 * Esta pantalla muestra la colección de Amiibos en un grid de 2 columnas.
 * Implementa el patrón de UI reactiva con:
 * - StateFlow para el estado
 * - when exhaustivo para manejar todos los estados
 * - Coil para carga asíncrona de imágenes
 *
 * ESTRUCTURA DE LA UI:
 * --------------------
 *
 *   ┌─────────────────────────────────────────┐
 *   │           TOP APP BAR                   │
 *   │  [Amiibo Vault]              [Refresh]  │
 *   ├─────────────────────────────────────────┤
 *   │                                         │
 *   │   ┌─────────┐    ┌─────────┐           │
 *   │   │  IMG    │    │  IMG    │           │
 *   │   │         │    │         │           │
 *   │   │  Name   │    │  Name   │           │
 *   │   │  Series │    │  Series │           │
 *   │   └─────────┘    └─────────┘           │
 *   │                                         │
 *   │   ┌─────────┐    ┌─────────┐           │
 *   │   │  IMG    │    │  IMG    │           │
 *   │   │  ...    │    │  ...    │           │
 *   │                                         │
 *   └─────────────────────────────────────────┘
 *
 * ============================================================================
 */

/**
 * Pantalla principal que muestra la lista de Amiibos.
 *
 * @OptIn(ExperimentalMaterial3Api::class):
 * - TopAppBar es experimental en Material3
 * - Requerido por las especificaciones del proyecto
 *
 * @param onAmiiboClick Callback para cuando se hace click en un Amiibo
 * @param viewModel ViewModel inyectado por Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmiiboListScreen(
    onAmiiboClick: (String) -> Unit,
    viewModel: AmiiboViewModel = koinViewModel() // O hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle() // Observamos el texto del buscador

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.primary)) {
                    TopAppBar(
                        title = { Text("Amiibo Collection", color = Color.White) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // --- BARRA DE BÚSQUEDA ---
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        placeholder = { Text("Buscar por nombre...", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is AmiiboUiState.Loading -> {
                    LoadingContent()
                }
                is AmiiboUiState.Success -> {
                    AmiiboGrid(
                        amiibos = state.amiibos,
                        onAmiiboClick = onAmiiboClick,
                        onLoadMore = { viewModel.loadNextPage() },
                        isLoadingMore = state.isLoadingMore,
                        hasMorePages = state.hasMorePages,
                        paginationError = state.paginationError,
                        onRetryLoadMore = { viewModel.loadNextPage() }
                    )
                }
                is AmiiboUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        errorType = state.errorType,
                        isRetryable = state.isRetryable,
                        onRetry = { viewModel.refreshAmiibos() }
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message)
        Button(onClick = onRetry) { Text("Retry") }
    }
}
/**
 * ============================================================================
 * COMPONENTES DE UI REUTILIZABLES
 * ============================================================================
 */

/**
 * Contenido mostrado durante la carga inicial.
 */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.loading_amiibos),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Contenido mostrado cuando hay error y no hay cache.
 */
@Composable
private fun ErrorContent(
    message: String,
    errorType: ErrorType,
    isRetryable: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Icono según el tipo de error
            Icon(
                imageVector = errorType.toIcon(),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = stringResource(R.string.error_loading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Solo mostrar botón si el error es recuperable
            if (isRetryable) {
                Button(onClick = onRetry) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        }
    }
}

/**
 * Función de extensión para obtener el icono según el tipo de error.
 */
private fun ErrorType.toIcon(): ImageVector = when (this) {
    ErrorType.NETWORK -> Icons.Default.CloudOff   // Sin conexión
    ErrorType.PARSE -> Icons.Default.Warning      // Error de datos
    ErrorType.DATABASE -> Icons.Default.Storage   // Error de BD
    ErrorType.UNKNOWN -> Icons.Default.Error      // Error genérico
}

/**
 * Banner de error mostrado sobre contenido existente.
 */
@Composable
private fun ErrorBanner(
    message: String,
    errorType: ErrorType,
    isRetryable: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono pequeño según el tipo de error
            Icon(
                imageVector = errorType.toIcon(),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Solo mostrar botón si el error es recuperable
            if (isRetryable) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        }
    }
}

/**
 * Grid de Amiibos con soporte para paginación infinita.
 */
@Composable
private fun AmiiboGrid(
    amiibos: List<AmiiboEntity>,
    onAmiiboClick: (String) -> Unit,
    hasMorePages: Boolean,
    isLoadingMore: Boolean,
    paginationError: String?,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            // Condición: estamos a 6 items del final Y hay más páginas
            // Y no estamos cargando Y no hay error pendiente
            lastVisibleItem >= totalItems - 6 &&
                    hasMorePages &&
                    !isLoadingMore &&
                    paginationError == null &&
                    totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        state = gridState,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Items de Amiibos
        items(
            items = amiibos,
            key = { it.id }
        ) { amiibo ->
            AmiiboCard(
                amiibo = amiibo,
                onClick = { onAmiiboClick(amiibo.name) }
            )
        }

        // Indicador de carga al final (ocupa 2 columnas)
        if (isLoadingMore) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        if (paginationError != null) {
            item(span = { GridItemSpan(2) }) {
                PaginationErrorItem(
                    errorMessage = paginationError,
                    onRetry = onRetryLoadMore
                )
            }
        }

        // Mensaje cuando no hay más páginas
        if (!hasMorePages && amiibos.isNotEmpty() && paginationError == null) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "— Fin de la lista (${amiibos.size} amiibos) —",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * COMPONENTE: Error de Paginación Inline
 */
@Composable
private fun PaginationErrorItem(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono y mensaje
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón de reintentar
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reintentar",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * Card individual para mostrar un Amiibo.
 */
@Composable
private fun AmiiboCard(
    amiibo: AmiiboEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Imagen con fondo degradado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = amiibo.imageUrl,
                        contentDescription = stringResource(
                            R.string.amiibo_image_description,
                            amiibo.name
                        ),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }

                // Información del Amiibo
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Nombre del Amiibo
                        Text(
                            text = amiibo.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Chip con la serie del juego
                        Surface(
                            modifier = Modifier.padding(top = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = amiibo.gameSeries,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
