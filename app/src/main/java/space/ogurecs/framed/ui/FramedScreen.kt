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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import space.ogurecs.framed.R
import space.ogurecs.framed.model.CameraBrand
import space.ogurecs.framed.model.CanvasRatio
import space.ogurecs.framed.model.CustomFontWeight
import space.ogurecs.framed.model.ExifData
import space.ogurecs.framed.model.ExportQuality
import space.ogurecs.framed.model.FontOption
import space.ogurecs.framed.model.FrameConfig
import space.ogurecs.framed.model.LogoColorMode
import space.ogurecs.framed.parser.ExifParser
import space.ogurecs.framed.render.FrameCompositor
import space.ogurecs.framed.util.ConfigStorage
import space.ogurecs.framed.util.HapticFeedback
import kotlin.math.max

data class SavedFramedItem(
    val uri: Uri,
    val name: String,
    val dateAdded: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FramedScreen(initialUris: List<Uri> = emptyList()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUris by remember(initialUris) { mutableStateOf<List<Uri>>(initialUris) }
    var currentUriIndex by remember { mutableIntStateOf(0) }

    var exifData by remember { mutableStateOf(ExifData()) }
    var config by remember { mutableStateOf(ConfigStorage.loadConfig(context)) }

    // Auto-persist config changes to SharedPreferences
    LaunchedEffect(config) {
        ConfigStorage.saveConfig(context, config)
    }

    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var activeExportQuality by remember { mutableStateOf<ExportQuality?>(null) }
    var batchProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showGalleryModal by remember { mutableStateOf(false) }
    var fullScreenPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var isSettingsVisible by remember { mutableStateOf(true) }

    var showExitDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Back handling: double press to exit without saving, single press shows confirmation dialog
    BackHandler(enabled = fullBitmap != null) {
        HapticFeedback.click(context)
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 1500L) {
            fullBitmap = null
            selectedUris = emptyList()
            previewBitmap = null
            showExitDialog = false
        } else {
            lastBackPressTime = now
            showExitDialog = true
        }
    }

    fun loadUri(uri: Uri) {
        scope.launch {
            isProcessing = true
            withContext(Dispatchers.IO) {
                try {
                    val parsedExif = ExifParser.parse(context, uri)
                    exifData = parsedExif

                    val stream = try {
                        context.contentResolver.openInputStream(uri)
                    } catch (e: Exception) {
                        if (uri.path != null) java.io.FileInputStream(java.io.File(uri.path!!)) else null
                    }
                    stream?.use { st ->
                        val options = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        fullBitmap = BitmapFactory.decodeStream(st, null, options)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isProcessing = false
        }
    }

    val multiplePhotosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            HapticFeedback.click(context)
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

    // Material You Dynamic Colors
    val accentColor = MaterialTheme.colorScheme.primary
    val onAccentColor = if (accentColor.luminance() > 0.55f) Color(0xFF0F172A) else Color.White
    val darkBg = Color(0xFF0D0F12)
    val cardSurface = Color(0xFF161920)

    val runExport: (ExportQuality) -> Unit = { quality ->
        val src = fullBitmap
        if (src != null) {
            scope.launch {
                activeExportQuality = quality
                try {
                    withContext(Dispatchers.Default) {
                        if (selectedUris.size > 1) {
                            selectedUris.forEachIndexed { idx, u ->
                                batchProgress = Pair(idx + 1, selectedUris.size)
                                val parsed = ExifParser.parse(context, u)
                                context.contentResolver.openInputStream(u)?.use { st ->
                                    val raw = BitmapFactory.decodeStream(st)
                                    if (raw != null) {
                                        val rendered = FrameCompositor.render(context, raw, parsed, config)
                                        FrameCompositor.saveToGallery(context, rendered, parsed.model.ifBlank { "photo" }, quality)
                                        rendered.recycle()
                                        raw.recycle()
                                    }
                                }
                            }
                        } else {
                            batchProgress = Pair(1, 1)
                            val renderedHighRes = FrameCompositor.render(context, src, exifData, config)
                            FrameCompositor.saveToGallery(context, renderedHighRes, exifData.model.ifBlank { "photo" }, quality)
                            renderedHighRes.recycle()
                        }
                    }
                    HapticFeedback.success(context)
                    val msg = if (quality == ExportQuality.TIKTOK_OPTIMIZED) {
                        if (selectedUris.size > 1) context.getString(R.string.toast_saved_social_all) else context.getString(R.string.toast_saved_social)
                    } else {
                        if (selectedUris.size > 1) context.getString(R.string.toast_saved_original_all) else context.getString(R.string.toast_saved_original)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "${context.getString(R.string.toast_export_error)}: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    activeExportQuality = null
                    batchProgress = null
                }
            }
        }
    }

    Scaffold(
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_framed_logo),
                                contentDescription = null,
                                tint = onAccentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                actions = {
                    if (fullBitmap != null) {
                        IconButton(onClick = {
                            HapticFeedback.click(context)
                            isSettingsVisible = !isSettingsVisible
                        }) {
                            Icon(
                                imageVector = if (isSettingsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "toggle controls",
                                tint = if (isSettingsVisible) accentColor else Color(0xFF94A3B8)
                            )
                        }
                    }
                    IconButton(onClick = {
                        HapticFeedback.click(context)
                        showGalleryModal = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.gallery_title),
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                    IconButton(onClick = {
                        HapticFeedback.click(context)
                        multiplePhotosPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.welcome_title),
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Multi-photo strip (compact)
            if (selectedUris.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF11141A))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(selectedUris) { idx, u ->
                        val isSelected = idx == currentUriIndex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accentColor else Color(0xFF2D3342),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    HapticFeedback.tick(context)
                                    currentUriIndex = idx
                                }
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

            // Viewport area (maximized edge-to-edge space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(2.dp)
                    .then(
                        if (fullBitmap != null) {
                            Modifier.clickable {
                                HapticFeedback.tick(context)
                                isSettingsVisible = !isSettingsVisible
                            }
                        } else Modifier
                    ),
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
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF232733)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_framed_logo),
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = stringResource(R.string.welcome_title),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.welcome_desc),
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    HapticFeedback.click(context)
                                    multiplePhotosPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = onAccentColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = onAccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.gallery_open),
                                    fontWeight = FontWeight.Medium,
                                    color = onAccentColor
                                )
                            }
                        }
                    }
                } else if (isProcessing) {
                    CircularProgressIndicator(color = accentColor)
                } else {
                    previewBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            // Bottom Settings Panel with Animated Visibility for fullscreen inspection
            if (fullBitmap != null) {
                AnimatedVisibility(
                    visible = isSettingsVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        color = cardSurface,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
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
                                    onClick = {
                                        HapticFeedback.tick(context)
                                        selectedTab = 0
                                    },
                                    text = { Text(stringResource(R.string.tab_format), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                    icon = { Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = {
                                        HapticFeedback.tick(context)
                                        selectedTab = 1
                                    },
                                    text = { Text(stringResource(R.string.tab_style), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                Tab(
                                    selected = selectedTab == 2,
                                    onClick = {
                                        HapticFeedback.tick(context)
                                        selectedTab = 2
                                    },
                                    text = { Text(stringResource(R.string.tab_spacing), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                    icon = { Icon(Icons.Default.LinearScale, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                Tab(
                                    selected = selectedTab == 3,
                                    onClick = {
                                        HapticFeedback.tick(context)
                                        selectedTab = 3
                                    },
                                    text = { Text(stringResource(R.string.tab_info), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                when (selectedTab) {
                                    0 -> FormatSettings(config = config, accent = accentColor, onConfigChange = { config = it })
                                    1 -> StyleSettings(config = config, accent = accentColor, onConfigChange = { config = it })
                                    2 -> SpacingSettings(config = config, accent = accentColor, onConfigChange = { config = it })
                                    3 -> MetaSettings(
                                        exif = exifData,
                                        config = config,
                                        accent = accentColor,
                                        onExifChange = { exifData = it },
                                        onConfigChange = { config = it }
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Persistent Action Bar (Pinned cleanly below settings without any overlap)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF12141A))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Export in Full Quality (100% Native)
                                Button(
                                    onClick = {
                                        HapticFeedback.click(context)
                                        runExport(ExportQuality.ORIGINAL_100)
                                    },
                                    enabled = activeExportQuality == null && fullBitmap != null,
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF272C38),
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (activeExportQuality == ExportQuality.ORIGINAL_100) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(batchProgress?.let { "${it.first}/${it.second}" } ?: "...", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFCBD5E1))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.btn_export_original), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }

                                // 2. Export for Social Media / TikTok (1080p, smart unsharp sharpening, optimal 96% JPEG)
                                Button(
                                    onClick = {
                                        HapticFeedback.click(context)
                                        runExport(ExportQuality.TIKTOK_OPTIMIZED)
                                    },
                                    enabled = activeExportQuality == null && fullBitmap != null,
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = onAccentColor
                                    )
                                ) {
                                    if (activeExportQuality == ExportQuality.TIKTOK_OPTIMIZED) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = onAccentColor, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(batchProgress?.let { "${it.first}/${it.second}" } ?: "...", fontSize = 11.sp, color = onAccentColor)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = onAccentColor)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.btn_export_social), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = onAccentColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = Color(0xFF1E222D),
            title = {
                Text(
                    text = stringResource(R.string.back_dialog_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.back_dialog_desc),
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            HapticFeedback.click(context)
                            showExitDialog = false
                            runExport(ExportQuality.ORIGINAL_100)
                            fullBitmap = null
                            selectedUris = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = onAccentColor
                        )
                    ) {
                        Text(stringResource(R.string.back_dialog_save_original), color = onAccentColor)
                    }
                    Button(
                        onClick = {
                            HapticFeedback.click(context)
                            showExitDialog = false
                            runExport(ExportQuality.TIKTOK_OPTIMIZED)
                            fullBitmap = null
                            selectedUris = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF262C3A),
                            contentColor = Color(0xFFE2E8F0)
                        )
                    ) {
                        Text(stringResource(R.string.back_dialog_save_social))
                    }
                    OutlinedButton(
                        onClick = {
                            HapticFeedback.click(context)
                            showExitDialog = false
                            fullBitmap = null
                            selectedUris = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.back_dialog_discard), color = Color(0xFFEF4444))
                    }
                    TextButton(
                        onClick = {
                            HapticFeedback.click(context)
                            showExitDialog = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.back_dialog_cancel), color = Color(0xFF94A3B8))
                    }
                }
            },
            dismissButton = {}
        )
    }

    // Saved Works Gallery Sheet
    if (showGalleryModal) {
        SavedGalleryModal(
            context = context,
            accent = accentColor,
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
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FormatSettings(
    config: FrameConfig,
    accent: Color,
    onConfigChange: (FrameConfig) -> Unit
) {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.ratio_header),
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
        CanvasRatio.values().forEach { ratio ->
            val isSelected = config.ratio == ratio
            FilterChip(
                selected = isSelected,
                onClick = {
                    HapticFeedback.tick(context)
                    onConfigChange(config.copy(ratio = ratio))
                },
                label = { Text(ratio.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222631),
                    labelColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Text Alignment
    Text(
        text = stringResource(R.string.align_header),
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
        space.ogurecs.framed.model.TextAlignment.values().forEach { align ->
            val isSelected = config.textAlignment == align
            val label = when (align) {
                space.ogurecs.framed.model.TextAlignment.CENTER -> stringResource(R.string.align_center)
                space.ogurecs.framed.model.TextAlignment.LEFT -> stringResource(R.string.align_left)
                space.ogurecs.framed.model.TextAlignment.SPLIT -> stringResource(R.string.align_split)
            }
            FilterChip(
                selected = isSelected,
                onClick = {
                    HapticFeedback.tick(context)
                    onConfigChange(config.copy(textAlignment = align))
                },
                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF222631),
                    labelColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    SettingSlider(
        label = stringResource(R.string.scale_photo),
        valueText = "${(config.photoScale * 100).toInt()}%",
        value = config.photoScale,
        range = 0.70f..0.98f,
        accent = accent,
        defaultValue = 0.95f,
        step = 0.01f,
        onValueChange = { onConfigChange(config.copy(photoScale = it)) }
    )
}

@Composable
private fun StyleSettings(
    config: FrameConfig,
    accent: Color,
    onConfigChange: (FrameConfig) -> Unit
) {
    val context = LocalContext.current

    // Typography: Font Family Selection
    Text(
        text = stringResource(R.string.font_header),
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
                onClick = {
                    HapticFeedback.tick(context)
                    onConfigChange(config.copy(fontOption = font))
                },
                label = { Text(font.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
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
        text = stringResource(R.string.weight_header),
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
                onClick = {
                    HapticFeedback.tick(context)
                    onConfigChange(config.copy(fontWeight = weight))
                },
                label = { Text(weight.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
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
        text = stringResource(R.string.logo_color_header),
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
            val label = when (mode) {
                LogoColorMode.WHITE -> stringResource(R.string.color_white)
                LogoColorMode.BLACK -> stringResource(R.string.color_black)
                LogoColorMode.BRAND -> stringResource(R.string.color_brand)
                LogoColorMode.MATCH_TEXT -> stringResource(R.string.color_text)
            }
            FilterChip(
                selected = isSelected,
                onClick = {
                    HapticFeedback.tick(context)
                    onConfigChange(config.copy(logoColorMode = mode))
                },
                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
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
        label = stringResource(R.string.corner_radius),
        valueText = "${config.cornerRadius.toInt()} dp",
        value = config.cornerRadius,
        range = 0f..120f,
        accent = accent,
        defaultValue = 60f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(cornerRadius = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.blur_radius),
        valueText = "${config.blurRadius.toInt()}%",
        value = config.blurRadius,
        range = 10f..60f,
        accent = accent,
        defaultValue = 40f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(blurRadius = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.shadow_opacity),
        valueText = "${(config.shadowAlpha * 100).toInt()}%",
        value = config.shadowAlpha,
        range = 0f..0.8f,
        accent = accent,
        defaultValue = 0.38f,
        step = 0.01f,
        onValueChange = { onConfigChange(config.copy(shadowAlpha = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.shadow_blur),
        valueText = "${config.shadowRadius.toInt()} px",
        value = config.shadowRadius,
        range = 0f..90f,
        accent = accent,
        defaultValue = 36f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(shadowRadius = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.shadow_spread),
        valueText = "${config.shadowSpread.toInt()} px",
        value = config.shadowSpread,
        range = 0f..60f,
        accent = accent,
        defaultValue = 8f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(shadowSpread = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.shadow_offset),
        valueText = if (config.shadowOffsetY == 0f) "0%" else "${config.shadowOffsetY.toInt()}%",
        value = config.shadowOffsetY,
        range = 0f..80f,
        accent = accent,
        defaultValue = 0f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(shadowOffsetY = it)) }
    )
}

@Composable
private fun SpacingSettings(
    config: FrameConfig,
    accent: Color,
    onConfigChange: (FrameConfig) -> Unit
) {
    SettingSlider(
        label = stringResource(R.string.text_master_scale),
        valueText = String.format(java.util.Locale.US, "%.1fx", config.textMasterScale),
        value = config.textMasterScale,
        range = 0.5f..2.5f,
        accent = accent,
        defaultValue = 1.5f,
        step = 0.05f,
        onValueChange = { onConfigChange(config.copy(textMasterScale = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.logo_scale),
        valueText = String.format(java.util.Locale.US, "%.1fx", config.logoScale),
        value = config.logoScale,
        range = 0.5f..2.5f,
        accent = accent,
        defaultValue = 1.0f,
        step = 0.05f,
        onValueChange = { onConfigChange(config.copy(logoScale = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.font_size_line1),
        valueText = "${config.fontSizeLine1.toInt()} pt",
        value = config.fontSizeLine1,
        range = 16f..64f,
        accent = accent,
        defaultValue = 34f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(fontSizeLine1 = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.font_size_line2),
        valueText = "${config.fontSizeLine2.toInt()} pt",
        value = config.fontSizeLine2,
        range = 12f..48f,
        accent = accent,
        defaultValue = 22f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(fontSizeLine2 = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.logo_offset_y),
        valueText = if (config.logoOffsetY == 0f) "0" else "${config.logoOffsetY.toInt()} dp",
        value = config.logoOffsetY,
        range = -25f..25f,
        accent = accent,
        defaultValue = 0f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(logoOffsetY = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.logo_gap),
        valueText = "${config.logoGap.toInt()} dp",
        value = config.logoGap,
        range = 4f..60f,
        accent = accent,
        defaultValue = 20f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(logoGap = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.line_spacing),
        valueText = "${config.lineSpacing.toInt()} dp",
        value = config.lineSpacing,
        range = 10f..60f,
        accent = accent,
        defaultValue = 32f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(lineSpacing = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.text_vertical_offset),
        valueText = if (config.footerVerticalOffset > 0) "+${config.footerVerticalOffset.toInt()}%" else "${config.footerVerticalOffset.toInt()}%",
        value = config.footerVerticalOffset,
        range = -35f..35f,
        accent = accent,
        defaultValue = 0f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(footerVerticalOffset = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.text_horizontal_offset),
        valueText = if (config.textHorizontalOffset == 0f) "0" else "${config.textHorizontalOffset.toInt()} dp",
        value = config.textHorizontalOffset,
        range = -40f..40f,
        accent = accent,
        defaultValue = 0f,
        step = 1f,
        onValueChange = { onConfigChange(config.copy(textHorizontalOffset = it)) }
    )

    SettingSlider(
        label = stringResource(R.string.letter_spacing),
        valueText = String.format("%.2f", config.letterSpacing),
        value = config.letterSpacing,
        range = 0f..0.15f,
        accent = accent,
        defaultValue = 0.02f,
        step = 0.005f,
        onValueChange = { onConfigChange(config.copy(letterSpacing = it)) }
    )
}

@Composable
private fun SettingSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    defaultValue: Float? = null,
    step: Float = 0f,
    onValueChange: (Float) -> Unit
) {
    val context = LocalContext.current
    var lastTickValue by remember { mutableFloatStateOf(value) }
    val isModified = defaultValue != null && kotlin.math.abs(value - defaultValue) > 0.005f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE2E8F0))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimatedVisibility(
                visible = isModified,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                IconButton(
                    onClick = {
                        HapticFeedback.click(context)
                        defaultValue?.let { onValueChange(it) }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                        contentDescription = stringResource(R.string.reset_default),
                        tint = accent.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
    }
    Spacer(modifier = Modifier.height(6.dp))
    Slider(
        value = value,
        onValueChange = { rawVal ->
            val snappedValue = if (step > 0f) {
                val steps = kotlin.math.round((rawVal - range.start) / step)
                (range.start + steps * step).coerceIn(range.start, range.endInclusive)
            } else {
                rawVal
            }
            val tickThreshold = if (step > 0f) step else (range.endInclusive - range.start) * 0.06f
            if (kotlin.math.abs(snappedValue - lastTickValue) >= tickThreshold) {
                HapticFeedback.tick(context)
                lastTickValue = snappedValue
            }
            onValueChange(snappedValue)
        },
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
    accent: Color,
    onExifChange: (ExifData) -> Unit,
    onConfigChange: (FrameConfig) -> Unit
) {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.camera_brand_header),
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
                onClick = {
                    HapticFeedback.tick(context)
                    onExifChange(exif.copy(brand = brand))
                },
                label = { Text(brand.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
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
        label = { Text(stringResource(R.string.camera_model_label), fontSize = 12.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = accent,
            unfocusedBorderColor = Color(0xFF334155),
            focusedLabelColor = accent,
            unfocusedLabelColor = Color(0xFF94A3B8)
        )
    )

    Spacer(modifier = Modifier.height(12.dp))

    MetaToggle(stringResource(R.string.toggle_logo), config.showLogo) { onConfigChange(config.copy(showLogo = it)) }
    MetaToggle(stringResource(R.string.toggle_model), config.showModel) { onConfigChange(config.copy(showModel = it)) }
    MetaToggle(stringResource(R.string.toggle_params), config.showParams) { onConfigChange(config.copy(showParams = it)) }
    MetaToggle(stringResource(R.string.toggle_lens), config.showLens) { onConfigChange(config.copy(showLens = it)) }
    MetaToggle(stringResource(R.string.toggle_date), config.showDate) { onConfigChange(config.copy(showDate = it)) }
    MetaToggle(stringResource(R.string.toggle_separate_line), config.separateExtraLine) { onConfigChange(config.copy(separateExtraLine = it)) }
}

@Composable
private fun MetaToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                HapticFeedback.click(context)
                onCheckedChange(!checked)
            }
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFFE2E8F0),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                HapticFeedback.click(context)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
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
    accent: Color,
    onDismiss: () -> Unit,
    onOpenItem: (Uri) -> Unit
) {
    var savedItems by remember { mutableStateOf<List<SavedFramedItem>>(emptyList()) }
    var itemToDelete by remember { mutableStateOf<SavedFramedItem?>(null) }
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

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color(0xFF1E222D),
            title = {
                Text(
                    text = stringResource(R.string.delete_confirm_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_confirm_desc),
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = itemToDelete
                        itemToDelete = null
                        target?.let { toDelete ->
                            try {
                                context.contentResolver.delete(toDelete.uri, null, null)
                                HapticFeedback.success(context)
                                savedItems = savedItems.filter { it.uri != toDelete.uri }
                                Toast.makeText(context, context.getString(R.string.toast_deleted), Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = Color(0xFF94A3B8))
                }
            }
        )
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
                    text = "${stringResource(R.string.gallery_title)} (${savedItems.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            } else if (savedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.gallery_empty),
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
                                        text = item.name.removePrefix("framed_").take(12),
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/jpeg"
                                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = stringResource(R.string.share),
                                                tint = accent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                HapticFeedback.click(context)
                                                itemToDelete = item
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.delete),
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(15.dp)
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
}
