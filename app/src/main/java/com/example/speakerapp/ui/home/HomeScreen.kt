package com.example.speakerapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    goToParent: () -> Unit,
    goToChild: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(
                text = "Speaker Security",
                fontSize = 28.sp,
                modifier = Modifier.padding(20.dp)
            )

            Button(
                onClick = goToParent,
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Text("Parent Mode")
            }

            Button(
                onClick = goToChild,
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Text("Child Mode")
            }
        }
    }
}
