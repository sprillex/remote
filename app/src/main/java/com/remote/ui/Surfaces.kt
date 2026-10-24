package com.remote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remote.db.ProfileType

@Composable
fun DeviceSurface(profileType: ProfileType) {
    when (profileType) {
        ProfileType.PC_KEYBOARD -> FullKeyboardSurface()
        ProfileType.TV_REMOTE -> DPadTvRemoteSurface()
        ProfileType.MEDIA_CONTROLLER -> MediaBarSurface()
        ProfileType.PRESENTATION_CLICKER -> PresentationClickerSurface()
        ProfileType.TERMINAL_MACRO -> MacroPadSurface()
        ProfileType.SSH_SESSION -> MacroPadSurface() // Or a custom one
    }
}

@Composable
fun FullKeyboardSurface() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Z", "X", "C", "V", "B", "N", "M")
        )
        for (row in rows) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (key in row) {
                    Button(onClick = { /* TODO */ }) {
                        Text(key)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) {
                Text("SPACE")
            }
        }
    }
}

@Composable
fun DPadTvRemoteSurface() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { /* TODO */ }) { Text("Power") }
            Button(onClick = { /* TODO */ }) { Text("Home") }
            Button(onClick = { /* TODO */ }) { Text("Back") }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { /* TODO */ }) { Text("Up") }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { /* TODO */ }) { Text("Left") }
            Button(onClick = { /* TODO */ }) { Text("OK") }
            Button(onClick = { /* TODO */ }) { Text("Right") }
        }
        Button(onClick = { /* TODO */ }) { Text("Down") }
    }
}

@Composable
fun MediaBarSurface() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { /* TODO */ }) { Text("Prev") }
        Button(onClick = { /* TODO */ }) { Text("Play/Pause") }
        Button(onClick = { /* TODO */ }) { Text("Next") }
        Column {
            Button(onClick = { /* TODO */ }) { Text("Vol +") }
            Button(onClick = { /* TODO */ }) { Text("Vol -") }
        }
    }
}

@Composable
fun MacroPadSurface() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(9) { index ->
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.aspectRatio(1f)
            ) {
                Text("M${index + 1}")
            }
        }
    }
}

@Composable
fun PresentationClickerSurface() {
     Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
         Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth().height(100.dp)) { Text("Next Slide") }
         Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth().height(100.dp)) { Text("Prev Slide") }
     }
}