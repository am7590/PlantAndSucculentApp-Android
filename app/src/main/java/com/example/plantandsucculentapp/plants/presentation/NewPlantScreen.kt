package com.example.plantandsucculentapp.plants.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.LocalImageLoader
import plant.PlantOuterClass
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlantScreen(
    onCreatePlant: (String, PlantOuterClass.PlantInformation) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lastWatered by remember { mutableStateOf("") }
    var wateringSchedule by remember { mutableStateOf("As needed") }
    var expanded by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val context = LocalContext.current
    val imageLoader = LocalImageLoader.current

    // this disables the 'save' button if all fields aren't filled out
    val isFormComplete = name.isNotEmpty() && lastWatered.isNotEmpty() &&
            (photoUri.isNotEmpty() || bitmap != null)

    // date picker for watering reminders
    val calendar = Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            calendar.set(year, month, day)
            lastWatered = sdf.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Add function to copy image to internal storage
    fun copyImageToInternalStorage(uri: Uri): String {
        return try {
            val fileName = "plant_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            Log.e("NewPlantScreen", "Failed to copy image", e)
            uri.toString()
        }
    }

    // Modify the photo selection launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> 
            uri?.let {
                photoUri = copyImageToInternalStorage(it)
            }
        }
    )
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmapResult -> if (bitmapResult != null) bitmap = bitmapResult }
    )

    // Add this function to handle saving the photo
    fun savePhotoAndCreatePlant() {
        val photoUrlToSave = when {
            photoUri.isNotEmpty() -> photoUri
            bitmap != null -> {
                val fileName = "plant_${UUID.randomUUID()}.jpg"
                val file = File(context.filesDir, fileName)
                file.outputStream().use { stream ->
                    bitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                }
                "file://${file.absolutePath}"
            }
            else -> ""
        }

        // Create a PhotoEntry for the initial photo if we have one
        val initialPhoto = if (photoUrlToSave.isNotEmpty()) {
            PlantOuterClass.PhotoEntry.newBuilder()
                .setUrl(photoUrlToSave)
                .setTimestamp(System.currentTimeMillis())
                .setNote("Initial photo")
                .build()
        } else null

        onCreatePlant(
            UUID.randomUUID().toString(),
            PlantOuterClass.PlantInformation.newBuilder()
                .setName(name)
                .setLastWatered(calendar.timeInMillis)
                .apply {
                    // Add the photo only if we have one
                    initialPhoto?.let { addPhotos(it) }
                }
                .build()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (name.isEmpty()) "New Plant" else name) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { savePhotoAndCreatePlant() },
                        enabled = isFormComplete
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Create",
                            modifier = Modifier.alpha(if (isFormComplete) 1f else 0.4f)
                        )
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Spacer(modifier = Modifier.height(72.dp))
            // Plant Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("New Plant") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Last Watered
            OutlinedTextField(
                value = lastWatered,
                onValueChange = {},
                label = { Text("Last Watered") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pick Date")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Watering Schedule
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = wateringSchedule,
                    onValueChange = {},
                    label = { Text("Watering Schedule") },
                    modifier = Modifier.fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    val schedules = listOf("As needed") + (1..14).map { "Every $it days" }
                    schedules.forEach { schedule ->
                        DropdownMenuItem(
                            text = { Text(schedule) },
                            onClick = {
                                wateringSchedule = schedule
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Photo Pickers
            Text("Photo", modifier = Modifier.padding(bottom = 8.dp))
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose from Gallery")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Take a Photo")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Display selected image
            if (photoUri.isNotEmpty()) {
                AsyncImage(
                    model = photoUri,
                    imageLoader = imageLoader,
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
