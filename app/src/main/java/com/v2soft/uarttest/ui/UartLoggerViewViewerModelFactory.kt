package com.v2soft.uarttest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.v2soft.uarttest.domain.AddControllerUseCase

class UartLoggerViewViewerModelFactory(
    private val addControllerUseCase: AddControllerUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        UartLoggerViewViewerModel(
            addControllerUseCase = addControllerUseCase,
        ) as T
}
