@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.studentnotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

private const val BASE_URL =
    "https://studentnotes-backend.onrender.com/"

private const val PREFS_NAME = "blood_donor_session"
private const val TOKEN_KEY = "jwt_token"

// ---------- COLORS ----------

private val BloodRed = Color(0xFFD32F2F)
private val BloodDark = Color(0xFF8E0000)
private val BloodPink = Color(0xFFFF6F61)
private val AvailGreen = Color(0xFF2E9D60)
private val AvailGreenDark = Color(0xFF1B7F4B)
private val AvailGreenSoft = Color(0xFFE3F5EA)
private val LightPink = Color(0xFFFFF1F0)
private val PageBackground = Color(0xFFFFF8F7)
private val TextDark = Color(0xFF2B1B1B)
private val TextMuted = Color(0xFF6B5B5B)
private val ErrorSoft = Color(0xFFFDE7E7)
private val NeutralSoft = Color(0xFFEFEAEA)

private val HeaderBrush = Brush.linearGradient(
    listOf(BloodDark, BloodRed, BloodPink)
)

private val bloodGroups = listOf(
    "A+", "A-", "B+", "B-",
    "O+", "O-", "AB+", "AB-"
)

private val filterGroups = listOf("All") + bloodGroups

// ---------- DATA MODELS ----------

data class UserDto(
    val id: String = "",
    val name: String = "",
    val email: String = ""
)

data class DonorDto(
    val _id: String = "",
    val name: String = "",
    val bloodGroup: String = "O+",
    val phone: String = "",
    val location: String = "",
    val available: Boolean = true
)

data class AuthResponse(
    val message: String = "",
    val token: String = "",
    val user: UserDto? = null,
    val error: String? = null
)

data class ProfileResponse(
    val message: String = "",
    val user: UserDto? = null,
    val donor: DonorDto? = null,
    val error: String? = null
)

data class RegisterBody(
    val name: String,
    val email: String,
    val password: String,
    val bloodGroup: String,
    val phone: String,
    val location: String
)

data class LoginBody(
    val email: String,
    val password: String
)

data class ProfileUpdateBody(
    val name: String,
    val email: String,
    val bloodGroup: String,
    val phone: String,
    val location: String
)

data class AvailabilityBody(
    val available: Boolean
)

// ---------- API SERVICE ----------

interface ApiService {

    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterBody
    ): AuthResponse

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginBody
    ): AuthResponse

    @GET("auth/me")
    suspend fun getMyProfile(
        @Header("Authorization") token: String
    ): ProfileResponse

    @PUT("auth/me")
    suspend fun updateMyProfile(
        @Header("Authorization") token: String,
        @Body body: ProfileUpdateBody
    ): ProfileResponse

    @GET("donors")
    suspend fun getDonors(): List<DonorDto>

    @PATCH("donors/me/availability")
    suspend fun updateAvailability(
        @Header("Authorization") token: String,
        @Body body: AvailabilityBody
    ): Map<String, Any>
}

private object ApiClient {
    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// ---------- ACTIVITY ----------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = BloodRed,
                    onPrimary = Color.White,
                    primaryContainer = LightPink,
                    onPrimaryContainer = BloodDark,
                    secondary = BloodDark,
                    secondaryContainer = Color(0xFFFFD9D6),
                    onSecondaryContainer = BloodDark,
                    background = PageBackground,
                    surface = Color.White,
                    onSurface = TextDark,
                    surfaceVariant = LightPink,
                    onSurfaceVariant = TextMuted,
                    outline = Color(0xFFB9A5A5)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PageBackground
                ) {
                    BloodDonorApp()
                }
            }
        }
    }
}

// ---------- NAVIGATION ----------

private enum class MainTab(
    val label: String,
    val icon: ImageVector
) {
    Home("Home", Icons.Filled.Home),
    Search("Search", Icons.Filled.Search),
    Profile("My Profile", Icons.Filled.Person)
}

// ---------- MAIN APP ----------

@Composable
fun BloodDonorApp() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, 0)
    }
    val scope = rememberCoroutineScope()

    var token by remember {
        mutableStateOf(prefs.getString(TOKEN_KEY, null))
    }

    var isRegister by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var donors by remember { mutableStateOf(listOf<DonorDto>()) }
    var myUser by remember { mutableStateOf(UserDto()) }
    var myDonor by remember { mutableStateOf<DonorDto?>(null) }

    var search by rememberSaveable { mutableStateOf("") }
    var groupFilter by rememberSaveable { mutableStateOf("All") }

    fun saveToken(value: String) {
        prefs.edit().putString(TOKEN_KEY, value).apply()
        token = value
    }

    fun logout() {
        prefs.edit().remove(TOKEN_KEY).apply()
        token = null
        myUser = UserDto()
        myDonor = null
        selectedTab = 0
        search = ""
        groupFilter = "All"
        message = "You have logged out."
    }

    suspend fun loadDonors() {
        try {
            donors = ApiClient.service.getDonors()
        } catch (e: Exception) {
            message = "Donor list load হয়নি: ${e.message}"
        }
    }

    suspend fun loadProfile() {
        val currentToken = token ?: return

        try {
            val result = ApiClient.service.getMyProfile(
                "Bearer $currentToken"
            )

            myUser = result.user ?: UserDto()
            myDonor = result.donor

            if (result.error != null) {
                message = result.error
            }
        } catch (e: Exception) {
            message = "Profile load হয়নি: ${e.message}"
        }
    }

    LaunchedEffect(token) {
        loading = true
        try {
            if (token != null) {
                loadProfile()
            }
            loadDonors()
        } finally {
            loading = false
        }
    }

    val filteredDonors = donors.filter { donor ->
        (groupFilter == "All" ||
                donor.bloodGroup == groupFilter) &&
                (
                        donor.name.contains(search, ignoreCase = true) ||
                                donor.location.contains(search, ignoreCase = true) ||
                                donor.bloodGroup.contains(search, ignoreCase = true)
                        )
    }

    if (token == null) {
        AuthScreen(
            isRegister = isRegister,
            loading = loading,
            message = message,
            onModeChange = {
                isRegister = it
                message = ""
            },
            onSubmit = { name, email, password, group, phone, location ->
                scope.launch {
                    loading = true
                    message = ""

                    try {
                        if (isRegister) {
                            val result = ApiClient.service.register(
                                RegisterBody(
                                    name = name.trim(),
                                    email = email.trim(),
                                    password = password,
                                    bloodGroup = group,
                                    phone = phone.trim(),
                                    location = location.trim()
                                )
                            )

                            if (result.token.isNotBlank()) {
                                saveToken(result.token)
                                message = "Registration successful!"
                            } else {
                                message = result.error
                                    ?: "Registration failed."
                            }
                        } else {
                            val result = ApiClient.service.login(
                                LoginBody(
                                    email = email.trim(),
                                    password = password
                                )
                            )

                            if (result.token.isNotBlank()) {
                                saveToken(result.token)
                                message = "Login successful!"
                            } else {
                                message = result.error ?: "Login failed."
                            }
                        }
                    } catch (e: Exception) {
                        message = "Request failed: ${e.message}"
                    } finally {
                        loading = false
                    }
                }
            }
        )
    } else {
        Scaffold(
            containerColor = PageBackground,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    MainTab.values().forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                if (selectedTab != index) {
                                    message = ""
                                    selectedTab = index
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BloodDark,
                                selectedTextColor = BloodDark,
                                indicatorColor = Color(0xFFFFD9D6),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        donors = filteredDonors,
                        totalDonors = donors.size,
                        availableDonors = donors.count { it.available },
                        userName = myUser.name,
                        groupFilter = groupFilter,
                        filtersActive = search.isNotBlank() ||
                                groupFilter != "All",
                        message = message,
                        loading = loading,
                        onGroupChange = { groupFilter = it },
                        onClearFilters = {
                            search = ""
                            groupFilter = "All"
                        },
                        onRefresh = {
                            scope.launch {
                                loading = true
                                loadDonors()
                                loading = false
                            }
                        },
                        onLogout = { logout() }
                    )

                    1 -> SearchScreen(
                        donors = filteredDonors,
                        totalDonors = donors.size,
                        search = search,
                        groupFilter = groupFilter,
                        message = message,
                        loading = loading,
                        onSearchChange = { search = it },
                        onGroupChange = { groupFilter = it },
                        onClearFilters = {
                            search = ""
                            groupFilter = "All"
                        }
                    )

                    else -> ProfileScreen(
                        user = myUser,
                        donor = myDonor,
                        loading = loading,
                        message = message,
                        onRefresh = {
                            scope.launch {
                                loading = true
                                loadProfile()
                                loading = false
                            }
                        },
                        onSave = { name, email, group, phone, location ->
                            scope.launch {
                                loading = true
                                message = ""

                                try {
                                    val result = ApiClient.service.updateMyProfile(
                                        "Bearer $token",
                                        ProfileUpdateBody(
                                            name = name.trim(),
                                            email = email.trim(),
                                            bloodGroup = group,
                                            phone = phone.trim(),
                                            location = location.trim()
                                        )
                                    )

                                    myUser = result.user ?: myUser
                                    myDonor = result.donor ?: myDonor
                                    message = result.message.ifBlank {
                                        result.error ?: "Profile updated."
                                    }

                                    loadDonors()
                                } catch (e: Exception) {
                                    message = "Profile update failed: ${e.message}"
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        onAvailabilityChange = { available ->
                            scope.launch {
                                loading = true
                                message = ""

                                try {
                                    ApiClient.service.updateAvailability(
                                        "Bearer $token",
                                        AvailabilityBody(available)
                                    )

                                    myDonor = myDonor?.copy(
                                        available = available
                                    )
                                    loadDonors()
                                    message = "Availability updated."
                                } catch (e: Exception) {
                                    message = "Availability update failed: ${e.message}"
                                    loadProfile()
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        onLogout = { logout() }
                    )
                }
            }
        }
    }
}

// ---------- SHARED UI HELPERS ----------

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BloodRed,
    focusedLabelColor = BloodRed,
    cursorColor = BloodRed,
    focusedLeadingIconColor = BloodRed,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null) }
        } else null,
        trailingIcon = if (isPassword) {
            {
                TextButton(onClick = { visible = !visible }) {
                    Text(
                        if (visible) "Hide" else "Show",
                        color = BloodRed,
                        fontSize = 13.sp
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !visible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction
        ),
        colors = fieldColors()
    )
}

@Composable
private fun BloodGroupSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bloodGroups.chunked(4).forEach { rowGroups ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowGroups.forEach { g ->
                    val isSelected = selected == g
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BloodRed else Color.White)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isSelected) BloodRed else Color(0xFFD9C7C7)
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(g) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            g,
                            color = if (isSelected) Color.White else TextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    if (message.isBlank()) return

    val isError = listOf(
        "fail", "হয়নি", "error", "invalid", "incorrect",
        "not found", "required", "wrong"
    ).any { message.contains(it, ignoreCase = true) }

    val isInfo = message.contains("logged out", ignoreCase = true)

    val background = when {
        isError -> ErrorSoft
        isInfo -> NeutralSoft
        else -> AvailGreenSoft
    }
    val contentColor = when {
        isError -> BloodDark
        isInfo -> TextDark
        else -> AvailGreenDark
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isError) Icons.Filled.Warning else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            message,
            color = contentColor,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LoadingBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        color = BloodRed,
        trackColor = Color(0xFFFFD9D6)
    )
}

@Composable
private fun GroupFilterRow(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(items = filterGroups, key = { it }) { g ->
            FilterChip(
                selected = selected == g,
                onClick = { onSelect(g) },
                label = {
                    Text(
                        g,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BloodRed,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = TextDark
                )
            )
        }
    }
}

@Composable
private fun EmptyDonorState(
    filtersActive: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD9D6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = BloodDark,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "No donors found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(Modifier.height(6.dp))

        Text(
            if (filtersActive) {
                "Nothing matches your current search or blood group filter. Try a different keyword or clear the filters."
            } else {
                "There are no donors to show yet. Pull the latest list using the refresh button."
            },
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        if (filtersActive) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClearFilters,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BloodRed)
            ) {
                Text("Clear search & filters", color = BloodRed)
            }
        }
    }
}

// ---------- AUTH SCREEN ----------

@Composable
fun AuthScreen(
    isRegister: Boolean,
    loading: Boolean,
    message: String,
    onModeChange: (Boolean) -> Unit,
    onSubmit: (
        String, String, String, String, String, String
    ) -> Unit
) {
    var name by remember(isRegister) { mutableStateOf("") }
    var email by remember(isRegister) { mutableStateOf("") }
    var password by remember(isRegister) { mutableStateOf("") }
    var phone by remember(isRegister) { mutableStateOf("") }
    var location by remember(isRegister) { mutableStateOf("") }
    var group by remember(isRegister) { mutableStateOf("O+") }
    var localError by remember(isRegister) { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(LightPink, PageBackground))
            )
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Branded header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 32.dp,
                            bottomEnd = 32.dp
                        )
                    )
                    .background(HeaderBrush)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🩸", fontSize = 36.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Blood Donor Finder",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        "একটি ফোন কলে বাঁচতে পারে একটি প্রাণ",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (isRegister) "Create your account" else "Welcome back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BloodDark
                    )

                    Text(
                        if (isRegister) {
                            "Join as a donor so people in need can reach you quickly."
                        } else {
                            "Log in to find donors and manage your donor profile."
                        },
                        color = TextMuted,
                        fontSize = 14.sp
                    )

                    if (isRegister) {
                        AppTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Full name",
                            leadingIcon = Icons.Filled.Person
                        )
                    }

                    AppTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        leadingIcon = Icons.Filled.Email,
                        keyboardType = KeyboardType.Email
                    )

                    AppTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = Icons.Filled.Lock,
                        isPassword = true,
                        imeAction = if (isRegister) ImeAction.Next else ImeAction.Done
                    )

                    if (isRegister) {
                        AppTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "Phone number",
                            leadingIcon = Icons.Filled.Phone,
                            keyboardType = KeyboardType.Phone
                        )

                        AppTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = "Location / Area",
                            leadingIcon = Icons.Filled.LocationOn,
                            imeAction = ImeAction.Done
                        )

                        Text(
                            "Select blood group",
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        BloodGroupSelector(
                            selected = group,
                            onSelect = { group = it }
                        )
                    }

                    StatusBanner(localError)
                    StatusBanner(message)

                    if (loading) {
                        LoadingBar()
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank() ||
                                (isRegister && (
                                        name.isBlank() || phone.isBlank()
                                        ))
                            ) {
                                localError = if (isRegister) {
                                    "Name, email, password and phone are required."
                                } else {
                                    "Email and password are required."
                                }
                                return@Button
                            }

                            localError = ""

                            onSubmit(
                                name, email, password, group, phone, location
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !loading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BloodRed,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (loading) "Please wait..."
                            else if (isRegister) "Create Account" else "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = { onModeChange(!isRegister) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isRegister) {
                                "Already registered? Login"
                            } else {
                                "New user? Create account"
                            },
                            color = BloodRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ---------- HOME SCREEN ----------

@Composable
fun HomeScreen(
    donors: List<DonorDto>,
    totalDonors: Int,
    availableDonors: Int,
    userName: String,
    groupFilter: String,
    filtersActive: Boolean,
    message: String,
    loading: Boolean,
    onGroupChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp
                        )
                    )
                    .background(HeaderBrush)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🩸 Blood Donor Finder",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (userName.isBlank()) "Welcome!"
                                else "Welcome, $userName",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = onRefresh, enabled = !loading) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh donors",
                                tint = Color.White
                            )
                        }

                        TextButton(onClick = onLogout) {
                            Text(
                                "Logout",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        "একটি ফোন কলে বাঁচতে পারে একটি প্রাণ",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatTile(
                            label = "Total donors",
                            value = totalDonors,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Available now",
                            value = availableDonors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            item(key = "message") {
                StatusBanner(
                    message,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (loading) {
            item(key = "loading") {
                LoadingBar(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        item(key = "section_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Donors",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BloodDark,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${donors.size} shown",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        item(key = "filters") {
            GroupFilterRow(
                selected = groupFilter,
                onSelect = onGroupChange
            )
        }

        if (donors.isEmpty()) {
            item(key = "empty") {
                EmptyDonorState(
                    filtersActive = filtersActive,
                    onClearFilters = onClearFilters
                )
            }
        } else {
            items(items = donors, key = { it._id }) { donor ->
                DonorCard(
                    donor = donor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 13.sp
        )
        Text(
            "$value",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------- SEARCH SCREEN ----------

@Composable
fun SearchScreen(
    donors: List<DonorDto>,
    totalDonors: Int,
    search: String,
    groupFilter: String,
    message: String,
    loading: Boolean,
    onSearchChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    val filtersActive = search.isNotBlank() || groupFilter != "All"

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    )
                )
                .background(HeaderBrush)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Column {
                Text(
                    "Find a donor",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Search by name, location or blood group",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search name, location or group") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = if (search.isNotEmpty()) {
                        {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = fieldColors()
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        GroupFilterRow(
            selected = groupFilter,
            onSelect = onGroupChange
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${donors.size} of $totalDonors donors match",
                color = TextDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            if (filtersActive) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear", color = BloodRed)
                }
            }
        }

        if (message.isNotBlank()) {
            StatusBanner(
                message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (loading) {
            LoadingBar(modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (donors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                EmptyDonorState(
                    filtersActive = filtersActive,
                    onClearFilters = onClearFilters
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 20.dp
                )
            ) {
                items(items = donors, key = { it._id }) { donor ->
                    DonorCard(donor)
                }
            }
        }
    }
}

// ---------- DONOR CARD ----------

@Composable
fun DonorCard(
    donor: DonorDto,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(BloodDark, BloodRed))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        donor.bloodGroup,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        donor.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            donor.location.ifBlank { "Location not provided" },
                            color = TextMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            donor.phone,
                            color = TextMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (donor.available) AvailGreenSoft else NeutralSoft
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (donor.available) "● Available" else "● Not available",
                        color = if (donor.available) AvailGreenDark else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse("tel:${donor.phone}")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BloodRed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Call", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---------- MY PROFILE ----------

@Composable
fun ProfileScreen(
    user: UserDto,
    donor: DonorDto?,
    loading: Boolean,
    message: String,
    onRefresh: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
    onAvailabilityChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember(user.name) {
        mutableStateOf(user.name)
    }
    var email by remember(user.email) {
        mutableStateOf(user.email)
    }
    var group by remember(donor?.bloodGroup) {
        mutableStateOf(donor?.bloodGroup ?: "O+")
    }
    var phone by remember(donor?.phone) {
        mutableStateOf(donor?.phone ?: "")
    }
    var location by remember(donor?.location) {
        mutableStateOf(donor?.location ?: "")
    }
    var localError by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Profile header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp
                        )
                    )
                    .background(HeaderBrush)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.name.trim().take(1).uppercase().ifBlank { "?" },
                            color = BloodDark,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            user.name.ifBlank { "My Profile" },
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            user.email.ifBlank { "Manage your donor information" },
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Blood group: $group",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Personal information card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Personal information",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BloodDark
                        )

                        AppTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Full name",
                            leadingIcon = Icons.Filled.Person
                        )

                        AppTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email",
                            leadingIcon = Icons.Filled.Email,
                            keyboardType = KeyboardType.Email
                        )

                        AppTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "Phone number",
                            leadingIcon = Icons.Filled.Phone,
                            keyboardType = KeyboardType.Phone
                        )

                        AppTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = "Location",
                            leadingIcon = Icons.Filled.LocationOn,
                            imeAction = ImeAction.Done
                        )

                        Text(
                            "Blood group",
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )

                        BloodGroupSelector(
                            selected = group,
                            onSelect = { group = it }
                        )
                    }
                }

                // Availability card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (donor?.available == true) {
                            AvailGreenSoft
                        } else {
                            Color.White
                        }
                    ),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Available for blood donation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (donor?.available == true) {
                                    "People can see you as available"
                                } else {
                                    "You are currently unavailable"
                                },
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }

                        Switch(
                            checked = donor?.available ?: false,
                            onCheckedChange = onAvailabilityChange,
                            enabled = donor != null && !loading,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AvailGreen
                            )
                        )
                    }
                }

                StatusBanner(localError)
                StatusBanner(message)

                if (loading) {
                    LoadingBar()
                }

                Button(
                    onClick = {
                        if (
                            name.isNotBlank() &&
                            email.isNotBlank() &&
                            phone.isNotBlank()
                        ) {
                            localError = ""
                            onSave(name, email, group, phone, location)
                        } else {
                            localError = "Name, email and phone are required."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BloodRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Save Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !loading,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BloodRed)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = BloodRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Refresh Profile",
                        color = BloodRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        "Logout",
                        color = BloodDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}