package app.sclviewer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Auto timeout, legacy mode + SuperKey and the module's state. Changes are saved as they are made. */
@Composable
internal fun SettingsScreen(
    bottomPadding: Dp,
    onModuleSettingsChanged: () -> Unit,
    onUserScroll: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var autoTimeout by remember { mutableStateOf(Prefs.autoTimeout(ctx)) }
    var legacy by remember { mutableStateOf(Prefs.legacyMode(ctx)) }
    var key by remember { mutableStateOf(Prefs.superKey(ctx)) }
    var moduleState by remember { mutableStateOf("Checking module...") }
    val scrollHook = rememberScrollHider { onUserScroll() }

    fun refreshModule() {
        scope.launch {
            val r = withContext(Dispatchers.IO) { ModuleCtl.send(ctx, "list") }
            moduleState = if (r.ok) r.out else (r.error ?: "failed")
        }
    }
    LaunchedEffect(legacy) { refreshModule() }

    Column(
        Modifier
            .fillMaxSize()
            .nestedScroll(scrollHook)
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = bottomPadding)
    ) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            SwitchPreference(
                title = "Auto timeout",
                summary = "The module forgets its targets a few seconds after this app is closed",
                checked = autoTimeout,
                onCheckedChange = {
                    autoTimeout = it
                    Prefs.setAutoTimeout(ctx, it)
                    onModuleSettingsChanged()
                }
            )
        }

        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            SwitchPreference(
                title = "Legacy mode",
                summary = "Old APatch: control the module with the SuperKey instead of the root channel",
                checked = legacy,
                onCheckedChange = {
                    legacy = it
                    Prefs.setLegacyMode(ctx, it)
                }
            )
            if (legacy) {
                TextField(
                    value = key,
                    onValueChange = {
                        key = it
                        Prefs.setSuperKey(ctx, it.trim())
                    },
                    label = "SuperKey",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "Module: $moduleState",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(16.dp)
            )
            TextButton(
                text = "Refresh",
                onClick = { refreshModule() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
