package space.ogurecs.framed.ui

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.ogurecs.framed.model.CameraBrand
import space.ogurecs.framed.model.CanvasRatio
import space.ogurecs.framed.model.CustomFontWeight
import space.ogurecs.framed.model.ExifData
import space.ogurecs.framed.model.FontOption
import space.ogurecs.framed.model.FrameConfig
import space.ogurecs.framed.model.LogoColorMode
import space.ogurecs.framed.parser.ExifParser
import space.ogurecs.framed.render.FrameCompositor
import kotlin.math.max

data class SavedFramedItem(
    val uri: Uri,
    val name: String,
    val dateAdded: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FramedScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var currentUriIndex by remember { mutableIntStateOf(0) }

    var exifData by remember { mutableStateOf(ExifData()) }
    var config by remember { mutableStateOf(FrameConfig()) }

    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showGalleryModal by remember { mutableStateOf(false) }
    var fullScreenPreviewUri by remember { mutableStateOf<Uri?>(null) }

    fun loadUri(uri: Uri) {
        scope.launch {
            isProcessing = true
            withContext(Dispatchers.IO) {
                val parsedExif = ExifParser.parse(context, uri)
                exifData = parsedExif

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

    val multiplePhotosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            currentUriIndex = 0
            loadUri(uris[0])
        }
    }

    LaunchedEffect(currentUriIndex, selectedUris) {
        if (selectedUris.isNotEmpty() && currentUriIndex in selectedUris.indices) {
            loadUri(selectedUris[currentUriIndex])
        }
    }

    LaunchedEffect(fullBitmap, config, exifData) {
        val src = fullBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
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
                    IconButton(onClick = { showGalleryModal = true }) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Галерея работ",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                    IconButton(onClick = {
                        multiplePhotosPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Выбрать фото",
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        },
        floatingActionButton = {
            if (fullBitmap != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedUris.size > 1) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    try {
                                        withContext(Dispatchers.Default) {
                                            selectedUris.forEachIndexed { idx, u ->
                                                batchProgress = Pair(idx + 1, selectedUris.size)
                                                val parsed = ExifParser.parse(context, u)
                                                context.contentResolver.openInputStream(u)?.use { st ->
                                                    val raw = BitmapFactory.decodeStream(st)
                                                    if (raw != null) {
                                                        val rendered = FrameCompositor.render(context, raw, parsed, config)
                                                        FrameCompositor.saveToGallery(context, rendered, "framed_${parsed.model.ifBlank { "photo" }}")
                                                        rendered.recycle()
                                                        raw.recycle()
                                                    }
                                                }
                                            }
                                        }
                                        Toast.makeText(context, "Все ${selectedUris.size} фото сохранены в Галерею!", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Ошибка пакета: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isExporting = false
                                        batchProgress = null
                                    }
                                }
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF1E222D),
                                contentColor = Color(0xFFE2E8F0)
                            )
                        ) {
                            Text("Экспорт всех (${selectedUris.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

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
                                        Toast.makeText(context, "Сохранено в 100% качестве!", Toast.LENGTH_SHORT).show()
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
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null)
                            }
                        },
                        text = {
                            val label = if (isExporting) {
                                batchProgress?.let { "Кадр ${it.first}/${it.second}..." } ?: "Экспорт..."
                            } else {
                                "Сохранить (100%)"
                            }
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Multi-photo strip
            if (selectedUris.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF11141A))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(selectedUris) { idx, u ->
                        val isSelected = idx == currentUriIndex
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accentColor else Color(0xFF2D3342),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { currentUriIndex = idx }
                        ) {
                            AsyncImage(
                                model = u,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Viewport area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (fullBitmap == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF232733)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Выберите фото с камеры",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Поддерживается пакетный выбор: один стиль рамки применится ко всем кадрам без потери качества",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    multiplePhotosPicker.launch(
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
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp)
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
                                text = { Text("Формат", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                icon = { Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(15.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Стиль", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Параметры", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp)) }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp)
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

    // Saved Works Gallery Sheet
    if (showGalleryModal) {
        SavedGalleryModal(
            context = context,
            onDismiss = { showGalleryModal = false },
            onOpenItem = { fullScreenPreviewUri = it }
        )
    }

    // Full screen image preview dialog
    if (fullScreenPreviewUri != null) {
        Dialog(
            onDismissRequest = { fullScreenPreviewUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = fullScreenPreviewUri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { fullScreenPreviewUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
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
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CanvasRatio.values().forEach { ratio ->
            val isSelected = config.ratio == ratio
            FilterChip(
                selected = isSelected,
                onClick = { onConfigChange(config.copy(ratio = ratio)) },
                label = { Text(ratio.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4F46E5),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222631),
                    labelColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val accent = Color(0xFF818CF8)
    SettingSlider(
        label = "Масштаб кадра в рамке",
        valueText = "${(config.photoScale * 100).toInt()}%",
        value = config.photoScale,
        range = 0.70f..0.95f,
        accent = accent,
        onValueChange = { onConfigChange(config.copy(photoScale = it)) }
    )
}

@Composable
private fun StyleSettings(
    config: FrameConfig,
    onConfigChange: (FrameConfig) -> Unit
) {
    val accent = Color(0xFF818CF8)

    // Typography: Font Family Selection
    Text(
        text = "Шрифт надписи",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF94A3B8)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FontOption.values().forEach { font ->
            val isSelected = config.fontOption == font
            FilterChip(
                selected = isSelected,
                onClick = { onConfigChange(config.copy(fontOption = font)) },
                label = { Text(font.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4F46E5),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222631),
                    labelColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Typography: Font Weight Selection
    Text(
        text = "Начертание (жирность)",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF94A3B8)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CustomFontWeight.values().forEach { weight ->
            val isSelected = config.fontWeight == weight
            FilterChip(
                selected = isSelected,
                onClick = { onConfigChange(config.copy(fontWeight = weight)) },
                label = { Text(weight.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4F46E5),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222631),
                    labelColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Logo Color Mode
    Text(
        text = "Цвет логотипа камеры",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF94A3B8)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LogoColorMode.values().forEach { mode ->
            val isSelected = config.logoColorMode == mode
            FilterChip(
                selected = isSelected,
                onClick = { onConfigChange(config.copy(logoColorMode = mode)) },
                label = { Text(mode.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4F46E5),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222631),
                    labelColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Sliders
    SettingSlider(
        label = "Скругление углов",
        valueText = "${config.cornerRadius.toInt()} dp",
        value = config.cornerRadius,
        range = 0f..60f,
        accent = accent,
        onValueChange = { onConfigChange(config.copy(cornerRadius = it)) }
    )

    SettingSlider(
        label = "Размытие фона",
        valueText = "${config.blurRadius.toInt()}%",
        value = config.blurRadius,
        range = 10f..60f,
        accent = accent,
        onValueChange = { onConfigChange(config.copy(blurRadius = it)) }
    )

    SettingSlider(
        label = "Глубина тени",
        valueText = "${(config.shadowAlpha * 100).toInt()}%",
        value = config.shadowAlpha,
        range = 0f..0.6f,
        accent = accent,
        onValueChange = { onConfigChange(config.copy(shadowAlpha = it)) }
    )

    SettingSlider(
        label = "Радиус рассеивания тени",
        valueText = "${config.shadowRadius.toInt()} px",
        value = config.shadowRadius,
        range = 10f..70f,
        accent = accent,
        onValueChange = { onConfigChange(config.copy(shadowRadius = it)) }
    )
}

@Composable
private fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE2E8F0))
        Surface(
            color = Color(0xFF222733),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = valueText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
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
    Spacer(modifier = Modifier.height(10.dp))
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
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CameraBrand.values().forEach { brand ->
            val isSelected = exif.brand == brand
            FilterChip(
                selected = isSelected,
                onClick = { onExifChange(exif.copy(brand = brand)) },
                label = { Text(brand.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4F46E5),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222631),
                    labelColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

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

    Spacer(modifier = Modifier.height(12.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedGalleryModal(
    context: Context,
    onDismiss: () -> Unit,
    onOpenItem: (Uri) -> Unit
) {
    var savedItems by remember { mutableStateOf<List<SavedFramedItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<SavedFramedItem>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("framed_%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val date = cursor.getLong(dateCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(SavedFramedItem(contentUri, name, date))
                }
            }
            savedItems = list
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF14171F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Галерея работ (${savedItems.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF818CF8))
                }
            } else if (savedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Пока нет сохранённых работ в галерее",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(savedItems) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C202B)),
                            modifier = Modifier.clickable { onOpenItem(item.uri) }
                        ) {
                            Column {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.name.removePrefix("framed_").take(14),
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1
                                    )
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/jpeg"
                                                putExtra(Intent.EXTRA_STREAM, item.uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Поделиться кадром"))
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Поделиться",
                                            tint = Color(0xFF818CF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

