package space.ogurecs.framed

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import space.ogurecs.framed.theme.FramedTheme
import space.ogurecs.framed.ui.FramedScreen

class MainActivity : ComponentActivity() {
  private val incomingUris = androidx.compose.runtime.mutableStateOf<List<Uri>>(emptyList())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    incomingUris.value = extractUrisFromIntent(intent)

    setContent {
      FramedTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          FramedScreen(initialUris = incomingUris.value)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val uris = extractUrisFromIntent(intent)
    if (uris.isNotEmpty()) {
      incomingUris.value = uris
    }
  }

  private fun extractUrisFromIntent(intent: Intent?): List<Uri> {
    if (intent == null) return emptyList()
    val list = mutableListOf<Uri>()
    when (intent.action) {
      Intent.ACTION_SEND -> {
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        uri?.let { list.add(it) }
      }
      Intent.ACTION_SEND_MULTIPLE -> {
        val uris: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
        uris?.let { list.addAll(it) }
      }
      Intent.ACTION_VIEW -> {
        intent.data?.let { list.add(it) }
      }
    }
    return list
  }
}
