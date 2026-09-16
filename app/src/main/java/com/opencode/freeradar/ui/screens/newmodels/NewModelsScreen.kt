/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.screens.newmodels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencode.freeradar.R
import com.opencode.freeradar.domain.model.FreeStatus
import com.opencode.freeradar.ui.components.OfferCard
import com.opencode.freeradar.ui.model.OfferUi
import com.opencode.freeradar.ui.navigation.NewModels
import com.opencode.freeradar.ui.theme.AppThemePreview
import com.opencode.freeradar.ui.viewmodel.NewModelsUiState
import com.opencode.freeradar.ui.viewmodel.NewModelsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NewModelsRoot(
    key: NewModels,
    onOpenDetails: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: NewModelsViewModel = koinViewModel { parametersOf(key) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NewModelsScreen(
        state = state,
        onOpenDetails = onOpenDetails,
        onBack = onBack,
        onToggleFavorite = { remoteId, favorite -> viewModel.toggleFavorite(remoteId, favorite) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewModelsScreen(
    state: NewModelsUiState,
    onOpenDetails: (String) -> Unit,
    onBack: () -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit = { _, _ -> }
) {
    Scaffold(
        modifier = Modifier.testTag("new_models_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_models_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.newModels.isNotEmpty()) {
                item(key = "header_new") {
                    SectionHeader(text = stringResource(R.string.new_models_new_section))
                }
                items(state.newModels, key = { it.remoteId }) { offer ->
                    OfferCard(
                        offer = offer,
                        onClick = { onOpenDetails(offer.remoteId) },
                        onToggleFavorite = { onToggleFavorite(offer.remoteId, !offer.favorite) }
                    )
                }
            }
            if (state.expired.isNotEmpty()) {
                item(key = "header_expired") {
                    SectionHeader(text = stringResource(R.string.new_models_expired_section))
                }
                items(state.expired, key = { it.remoteId }) { offer ->
                    OfferCard(
                        offer = offer,
                        onClick = { onOpenDetails(offer.remoteId) },
                        onToggleFavorite = { onToggleFavorite(offer.remoteId, !offer.favorite) }
                    )
                }
            }
            if (state.newModels.isEmpty() && state.expired.isEmpty()) {
                item(key = "gone") {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        text = stringResource(R.string.new_models_gone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Preview(showBackground = true)
@Composable
private fun NewModelsPreview() {
    AppThemePreview {
        NewModelsScreen(
            state = NewModelsUiState(
                isLoading = false,
                newModels = listOf(
                    OfferUi("p/a", "Model A", "p", FreeStatus.FREE, null, 1_000L, "opencode-data")
                ),
                expired = listOf(
                    OfferUi("p/b", "Model B", "p", FreeStatus.PAID, null, 1_000L, "opencode-data")
                )
            ),
            onOpenDetails = {},
            onBack = {}
        )
    }
}
