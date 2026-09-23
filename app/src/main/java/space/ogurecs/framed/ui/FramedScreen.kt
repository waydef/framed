package space.ogurecs.framed.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.ogurecs.framed.model.CameraBrand
import space.ogurecs.framed.model.CanvasRatio
import space.ogurecs.framed.model.ExifData
import space.ogurecs.framed.model.FrameConfig
import space.ogurecs.framed.parser.ExifParser
import space.ogurecs.framed.render.FrameCompositor
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FramedScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var exifData by remember { mutableStateOf(ExifData()) }
    var config by remember { mutableStateOf(FrameConfig()) }

    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            scope.launch {
                isProcessing = true
                withContext(Dispatchers.IO) {
                    val parsedExif = ExifParser.parse(context, uri)
                    exifData = parsedExif

                    // Load full bitmap safely
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        fullBitmap = BitmapFactory.decodeStream(stream, null, options)
                    }
                }
                isProcessing = false
            }
        }
    }

    // Update preview when config or image changes
    LaunchedEffect(fullBitmap, config, exifData) {
        val src = fullBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            // Generate low-res preview bitmap to keep UI ultra responsive
            val maxPreviewDim = 1200
            val scale = minOf(1f, maxPreviewDim.toFloat() / max(src.width, src.height))
            val previewSrc = if (scale < 1f) {
                Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true)
            } else {
                src
            }
            val rendered = FrameCompositor.render(context, previewSrc, exifData, config)
            previewBitmap = rendered
        }
    }

    val darkBg = Color(0xFF0D0F12)
    val cardSurface = Color(0xFF161920)
    val accentColor = Color(0xFF818CF8)

    Scaffold(
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Framed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                actions = {
                    if (imageUri != null) {
                        IconButton(onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Заменить фото",
                                tint = Color(0xFFCBD5E1)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        },
        floatingActionButton = {
            if (fullBitmap != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val src = fullBitmap ?: return@ExtendedFloatingActionButton
                        scope.launch {
                            isExporting = true
                            try {
                                val savedUri = withContext(Dispatchers.Default) {
                                    val renderedHighRes = FrameCompositor.render(context, src, exifData, config)
                                    val uri = FrameCompositor.saveToGallery(context, renderedHighRes, "framed_${exifData.model.ifBlank { "photo" }}")
                                    renderedHighRes.recycle()
                                    uri
                                }
                                if (savedUri != null) {
                                    Toast.makeText(context, "Сохранено в Галерею в 100% качестве!", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    containerColor = accentColor,
                    contentColor = Color.White,
                    icon = {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                        }
                    },
                    text = {
                        Text(if (isExporting) "Экспорт в 100%..." else "Сохранить (100%)", fontWeight = FontWeight.SemiBold)
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Viewport area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (fullBitmap == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF232733)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Выберите фото с камеры",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Framed автоматически прочитает EXIF, подтянет логотип бренда и создаст кинематографичную карточку",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Открыть галерею", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else if (isProcessing) {
                    CircularProgressIndicator(color = accentColor)
                } else {
                    previewBitmap?.let { bmp ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Bottom Settings Panel
            if (fullBitmap != null) {
                Surface(
                    color = cardSurface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 80.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = cardSurface,
                            contentColor = accentColor,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = accentColor
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Формат", fontSize = 14.sp) },
                                icon = { Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Стиль", fontSize = 14.sp) },
                                icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Параметры", fontSize = 14.sp) },
                                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            when (selectedTab) {
                                0 -> FormatSettings(config = config, onConfigChange = { config = it })
                                1 -> StyleSettings(config = config, onConfigChange = { config = it })
                                2 -> MetaSettings(
                                    exif = exifData,
                                    config = config,
                                    onExifChange = { exifData = it },
                                    onConfigChange = { config = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatSettings(
    config: FrameConfig,
    onConfigChange: (FrameConfig) -> Unit
) {
    Text(
        text = "Соотношение сторон холста",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF94A3B8)
    )
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CanvasRatio.values().forEach { ratio ->
            val selected = config.ratio == ratio
            FilterChip(
                selected = selected,
                onClick = { onConfigChange(config.copy(ratio = ratio)) },
                label = { Text(ratio.label, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4F46E5),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222631),
                    labelColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
}

@Composable
private fun StyleSettings(
    config: FrameConfig,
    onConfigChange: (FrameConfig) -> Unit
) {
    val accent = Color(0xFF818CF8)

    // Corner radius
    SettingSlider(
        label = "Скругление углов: ${config.cornerRadius.toInt()}px",
        value = config.cornerRadius,
        range = 0f..60f,
        accent = accent,
        onValueChange = { onConfigChange(config.copy(cornerRadius = it)) }
    )

    // Blur
    SettingSlider(
        label = "Размытие фона: ${config.blurRadius.toInt()}%",
        value = config.blurRadius,
        range = 10f..60f,
        accent = accent,
        onValueChange = { onConfigChange(config.copy(blurRadius = it)) }
    )

    // Shadow
    SettingSlider(
        label = "Глубина тени: ${(config.shadowAlpha * 100).toInt()}%",
        value = config.shadowAlpha,
        range = 0f..0.6f,
        accent = accent,
        onValueChange = { onConfigChange(config.copy(shadowAlpha = it)) }
    )

    // Photo Scale
    SettingSlider(
        label = "Размер карточки: ${(config.photoScale * 100).toInt()}%",
        value = config.photoScale,
        range = 0.70f..0.95f,
        accent = accent,
        onValueChange = { onConfigChange(config.copy(photoScale = it)) }
    )
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFFCBD5E1))
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = Color(0xFF2D3342)
        )
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun MetaSettings(
    exif: ExifData,
    config: FrameConfig,
    onExifChange: (ExifData) -> Unit,
    onConfigChange: (FrameConfig) -> Unit
) {
    Text(
        text = "Бренд камеры (официальный логотип)",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF94A3B8)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CameraBrand.values().forEach { brand ->
            val selected = exif.brand == brand
            FilterChip(
                selected = selected,
                onClick = { onExifChange(exif.copy(brand = brand)) },
                label = { Text(brand.displayName, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4F46E5),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222631),
                    labelColor = Color(0xFFCBD5E1)
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Model name editable
    OutlinedTextField(
        value = exif.model,
        onValueChange = { onExifChange(exif.copy(model = it)) },
        label = { Text("Модель камеры", fontSize = 12.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF818CF8),
            unfocusedBorderColor = Color(0xFF334155),
            focusedLabelColor = Color(0xFF818CF8),
            unfocusedLabelColor = Color(0xFF94A3B8)
        )
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Toggles
    MetaToggle("Показывать логотип", config.showLogo) { onConfigChange(config.copy(showLogo = it)) }
    MetaToggle("Показывать модель камеры", config.showModel) { onConfigChange(config.copy(showModel = it)) }
    MetaToggle("Показывать параметры (ISO, выдержка, f-stop)", config.showParams) { onConfigChange(config.copy(showParams = it)) }
    MetaToggle("Показывать объектив", config.showLens) { onConfigChange(config.copy(showLens = it)) }
    MetaToggle("Показывать дату съёмки", config.showDate) { onConfigChange(config.copy(showDate = it)) }
}

@Composable
private fun MetaToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFFE2E8F0))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4F46E5),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF262C3A)
            )
        )
    }
}
