package com.example.spacebridgeapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.spacebridgeapp.ui.theme.SpaceBridgeAppTheme

@Preview(showBackground = true)
@Composable
fun HalamanCounterPreview() {
    SpaceBridgeAppTheme {
        HalamanCounter()
    }
}

@Composable
fun HalamanCounter() {
    var angka by remember { mutableIntStateOf(0) }

    Column(
        modifier =
            Modifier
                .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$angka",
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(modifier = Modifier.height(36.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = { if (angka > 0) angka-- },
            ) {
                Text("Kurang")
            }
            Button(
                onClick = { angka = 0 },
            ) {
                Text("Reset")
            }
            Button(
                onClick = { angka++ },
            ) {
                Text("Tambah")
            }
        }
    }
}
