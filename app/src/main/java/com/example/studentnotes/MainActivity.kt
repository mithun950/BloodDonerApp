package com.example.studentnotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// DATA
data class Donor(
    @SerializedName("_id") val id: String? = null,
    val name: String,
    val bloodGroup: String,
    val phone: String,
    val location: String = "",
    val available: Boolean = true
)

//  API
interface ApiService {
    @GET("donors")
    suspend fun getDonors(): List<Donor>

    @POST("donors")
    suspend fun addDonor(@Body d: Donor): Donor

    @PUT("donors/{id}")
    suspend fun updateDonor(@Path("id") id: String, @Body d: Donor): Donor

    @DELETE("donors/{id}")
    suspend fun deleteDonor(@Path("id") id: String)
}

object Api {
    // adb reverse দিলে localhost ; না দিলে "http://10.0.2.2:5000/"
    private const val BASE_URL = "https://studentnotes-backend.onrender.com/"

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// color
val BloodRed = Color(0xFFD32F2F)
val BloodDark = Color(0xFF8E0000)
val BloodPink = Color(0xFFFF6F61)
val AvailGreen = Color(0xFF2ECC71)

val allGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

// ACTIVITY
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DonorApp()
            }
        }
    }
}

// MAIN SCREEN
@Composable
fun DonorApp() {
    val scope = rememberCoroutineScope()
    var donors by remember { mutableStateOf(listOf<Donor>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var groupFilter by remember { mutableStateOf("All") }
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Donor?>(null) }

    suspend fun load() {
        try {
            donors = Api.service.getDonors()
            error = null
        } catch (e: Exception) {
            error = "Server-এর সাথে connect হচ্ছে না: ${e.message}"
        }
    }

    LaunchedEffect(Unit) { load() }

    val shown = donors.filter {
        (groupFilter == "All" || it.bloodGroup == groupFilter) &&
                (it.name.contains(query, ignoreCase = true) ||
                        it.location.contains(query, ignoreCase = true))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = null; showDialog = true },
                containerColor = BloodRed,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            HeaderCard(donors.size, donors.count { it.available })
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("নাম বা এলাকা দিয়ে খোঁজো") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All") + allGroups) { g ->
                    FilterChip(
                        selected = groupFilter == g,
                        onClick = { groupFilter = g },
                        label = { Text(g) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            error?.let {
                Text(it, color = BloodRed, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }

            if (shown.isEmpty() && error == null) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("কোনো donor নেই। + চাপো ", color = Color.Gray)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(shown) { d ->
                    DonorItem(
                        d = d,
                        onToggle = {
                            scope.launch {
                                try {
                                    d.id?.let {
                                        Api.service.updateDonor(it, d.copy(id = null, available = !d.available))
                                    }
                                    load()
                                } catch (e: Exception) {
                                    error = "Update হয়নি: ${e.message}"
                                }
                            }
                        },
                        onEdit = { editing = d; showDialog = true },
                        onDelete = {
                            scope.launch {
                                try {
                                    d.id?.let { Api.service.deleteDonor(it) }
                                    load()
                                } catch (e: Exception) {
                                    error = "Delete হয়নি: ${e.message}"
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        DonorDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { d ->
                showDialog = false
                scope.launch {
                    try {
                        val id = editing?.id
                        if (id == null) Api.service.addDonor(d) else Api.service.updateDonor(id, d)
                        load()
                    } catch (e: Exception) {
                        error = "Save হয়নি: ${e.message}"
                    }
                }
            }
        )
    }
}

//  HEADER
@Composable
fun HeaderCard(total: Int, available: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BloodDark, BloodRed, BloodPink)))
            .padding(22.dp)
    ) {
        Column {
            Text("🩸 Blood Donor Finder", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("একটি ফোন কলে বাঁচতে পারে একটি প্রাণ", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBox("মোট Donor", "$total")
                StatBox("এখন Available", "$available")
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

// DONOR ITEM
@Composable
fun DonorItem(d: Donor, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val statusColor = if (d.available) AvailGreen else Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(BloodRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(d.bloodGroup, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(d.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("📍 ${d.location.ifBlank { "এলাকা দেওয়া নেই" }}", color = Color.Gray, fontSize = 12.sp)
                    Text("📞 ${d.phone}", color = Color.Gray, fontSize = 12.sp)
                }
                IconButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${d.phone}")))
                    }
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = AvailGreen)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = d.available, onCheckedChange = { onToggle() })
                Spacer(Modifier.width(8.dp))
                Text(
                    if (d.available) "Available" else "Not available",
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

//ADD / EDIT DIALOG
@Composable
fun DonorDialog(initial: Donor?, onDismiss: () -> Unit, onSave: (Donor) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var group by remember { mutableStateOf(initial?.bloodGroup ?: "O+") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "নতুন Donor" else "Donor Edit করো") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("নাম") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("ফোন নম্বর") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("এলাকা (যেমন: Dhaka)") },
                    singleLine = true
                )
                Text("রক্তের গ্রুপ", fontSize = 12.sp, color = Color.Gray)
                allGroups.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { g ->
                            FilterChip(
                                selected = group == g,
                                onClick = { group = g },
                                label = { Text(g, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(
                            Donor(
                                id = null,
                                name = name.trim(),
                                bloodGroup = group,
                                phone = phone.trim(),
                                location = location.trim(),
                                available = initial?.available ?: true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BloodRed)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}