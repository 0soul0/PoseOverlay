package com.example.poseoverlay.common.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.poseoverlay.data.ImageEntity

@Composable
fun DeleteImageDialog(
    selectedImage: ImageEntity,
    onDismiss: () -> Unit,
    onConfirm: (ImageEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
        title = { Text("Delete Image") },
        text = { Text("Remove this image from library?") },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedImage) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text("Cancel") }
        }
    )
}


@Composable
fun InfoImageDialog(
    selectedImage: ImageEntity,
    onDismiss: () -> Unit, // 修改點：改為傳入一個 function
) {
    AlertDialog(
        onDismissRequest = { onDismiss() }, // 呼叫傳進來的關閉邏輯
        title = { Text("Image Info") },
        text = {
            Column {
                Text("Category: ${selectedImage.category}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(selectedImage.description.ifBlank { "No description provided." })
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) { Text("OK") }
        }
    )
}