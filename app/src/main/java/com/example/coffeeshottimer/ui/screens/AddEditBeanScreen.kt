package com.example.coffeeshottimer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AddEditBeanScreen(
    beanId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (beanId != null) "Edit Bean Screen\nBean ID: $beanId" else "Add Bean Screen",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}