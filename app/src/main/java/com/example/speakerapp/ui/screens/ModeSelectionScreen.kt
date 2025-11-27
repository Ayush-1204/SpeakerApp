package com.example.speakerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModeSelectionScreen(
    onParentClick: () -> Unit,
    onChildClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Mode",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onParentClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Parent Mode")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onChildClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Child Mode")
        }
    }
}
