package com.example.meshsosrelay.ui.fake

import androidx.lifecycle.ViewModel

object FakeDependencyInjection {
    val controller = FakeSosController()
}

class FakeSosViewModel : ViewModel() {
    val controller: FakeSosController = FakeDependencyInjection.controller
}
