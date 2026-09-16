/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.di

import com.opencode.freeradar.ui.viewmodel.DashboardViewModel
import com.opencode.freeradar.ui.viewmodel.DetailsViewModel
import com.opencode.freeradar.ui.viewmodel.NewModelsViewModel
import com.opencode.freeradar.ui.navigation.NewModels
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::DashboardViewModel)
    viewModel { (offerId: String) -> DetailsViewModel(offerId, get()) }
    viewModel { (key: NewModels) -> NewModelsViewModel(key, get()) }
}
