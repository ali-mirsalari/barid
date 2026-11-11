package com.darakav.barid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.darakav.barid.Forwarder
import com.darakav.barid.Pairing
import com.darakav.barid.Payload
import com.darakav.barid.Queued
import com.darakav.barid.R
import com.darakav.barid.ui.components.BaridIcons
import com.darakav.barid.ui.components.Divider
import com.darakav.barid.ui.components.GlassCard
import com.darakav.barid.ui.components.IconTile
import com.darakav.barid.ui.components.ListRow
import com.darakav.barid.ui.components.PrimaryButton
import com.darakav.barid.ui.components.QuietButton
import com.darakav.barid.ui.components.Segmented
import com.darakav.barid.ui.components.SecondaryButton
import com.darakav.barid.ui.theme.Barid
import com.darakav.barid.ui.theme.Vazirmatn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SERVERS_DOC_URL = "https://github.com/ali-mirsalari/barid/blob/main/docs/SERVERS.md"

/** The message a template server receives from "Send test" and the check after connecting. */
fun sampleMessage(text: String) = Queued("test", "Barid", text, System.currentTimeMillis())

/**
 * "Use another server": any HTTP(S) endpoint, an optional key and the header that carries it, and either
 * Barid's JSON batch or a template filled per message. [initial] prefills the form from a custom pairing.
 */
@Composable
fun ServerScreen(initial: Pairing?, onBack: () -> Unit, onConnect: (Pairing) -> Unit) {
    val c = Barid.colors
    val uri = LocalUriHandler.current
    val start = initial?.takeUnless { it.darakav }
    var url by rememberSaveable { mutableStateOf(start?.url ?: "https://") }
    var label by rememberSaveable { mutableStateOf(start?.label.orEmpty()) }
    var key by rememberSaveable { mutableStateOf(start?.key.orEmpty()) }
    var header by rememberSaveable { mutableStateOf(start?.header.orEmpty()) }
    var templated by rememberSaveable { mutableStateOf(start?.template?.isNotEmpty() == true) }
    var template by rememberSaveable { mutableStateOf(start?.template?.ifEmpty { null } ?: Payload.DEFAULT_TEMPLATE) }
    var showErrors by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Forwarder.Result?>(null) }
    val scope = rememberCoroutineScope()
    val sample = stringResource(R.string.server_test_message)

    val bodyTemplate = if (templated) template else ""
    val problem = Pairing.problem(url, key, header, bodyTemplate)
    fun draft() = Pairing.custom(url, key, label, header, bodyTemplate)
    fun edited() { testResult = null }

    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.server_title), onBack) }
        item {
            Text(
                stringResource(R.string.server_intro), style = MaterialTheme.typography.bodySmall, color = c.muted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.server_section_address))
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        stringResource(R.string.server_url), url, { url = it.trim(); edited() },
                        placeholder = "https://example.com/hooks/sms", ltr = true, keyboard = KeyboardType.Uri,
                        error = stringResource(R.string.server_url_error).takeIf { showErrors && problem == Pairing.Field.URL },
                    )
                    if (url.startsWith("http://", ignoreCase = true)) {
                        Text(stringResource(R.string.unencrypted_warning), style = MaterialTheme.typography.bodySmall, color = c.warning)
                    }
                    Field(stringResource(R.string.server_label), label, { label = it.take(64) }, placeholder = stringResource(R.string.server_label_hint))
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.server_section_key))
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        stringResource(R.string.server_key), key, { key = it.trim(); edited() },
                        placeholder = stringResource(R.string.optional), ltr = true, secret = true,
                        error = stringResource(R.string.server_key_error).takeIf { showErrors && problem == Pairing.Field.KEY },
                    )
                    Field(
                        stringResource(R.string.server_header), header, { header = it.trim(); edited() },
                        placeholder = "Authorization: Bearer", ltr = true,
                        error = stringResource(R.string.server_header_error).takeIf { showErrors && problem == Pairing.Field.HEADER },
                    )
                    Text(stringResource(R.string.server_header_help), style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.server_section_format))
                ListRow(stringResource(R.string.server_format), trailing = {
                    Segmented(
                        listOf(false to stringResource(R.string.server_format_batch), true to stringResource(R.string.server_format_template)),
                        templated, { templated = it; edited() },
                    )
                })
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!templated) {
                        Text(stringResource(R.string.server_format_batch_help), style = MaterialTheme.typography.bodySmall, color = c.muted)
                        Code("{\"messages\":[{\"from\":\"…\",\"text\":\"…\",\"receivedStamp\":…}]}")
                    } else {
                        Field(
                            stringResource(R.string.server_template), template, { template = it.take(Pairing.MAX_TEMPLATE); edited() },
                            ltr = true, singleLine = false, mono = true,
                            error = stringResource(R.string.server_template_error).takeIf { showErrors && problem == Pairing.Field.TEMPLATE },
                        )
                        Text(stringResource(R.string.server_template_help), style = MaterialTheme.typography.bodySmall, color = c.muted)
                        Code("{{from}}  {{text}}  {{receivedAt}}  {{receivedStamp}}")
                    }
                }
                Divider()
                ListRow(
                    stringResource(R.string.server_spec), subtitle = stringResource(R.string.server_spec_body),
                    onClick = { uri.openUri(SERVERS_DOC_URL) },
                    leading = { IconTile(BaridIcons.Code, c.accent, c.accentSoft, Modifier.size(32.dp)) },
                    trailing = { Icon(BaridIcons.Forward, null, Modifier.size(16.dp), tint = c.muted) },
                )
            }
        }
        item {
            Column(Modifier.navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                testResult?.let { result ->
                    val (text, color) = when (result) {
                        Forwarder.Result.Ok -> stringResource(R.string.server_test_ok) to c.success
                        Forwarder.Result.Rejected -> stringResource(R.string.server_test_rejected) to c.danger
                        Forwarder.Result.Invalid -> stringResource(R.string.server_test_invalid) to c.danger
                        is Forwarder.Result.Retry -> stringResource(R.string.server_test_failed, result.reason) to c.warning
                    }
                    Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.padding(horizontal = 4.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        stringResource(if (templated) R.string.server_send_test else R.string.action_test),
                        {
                            val pairing = draft()
                            if (pairing == null) { showErrors = true; return@SecondaryButton }
                            testing = true
                            scope.launch {
                                testResult = withContext(Dispatchers.IO) { Forwarder.check(pairing, sampleMessage(sample)) }
                                testing = false
                            }
                        },
                        Modifier.weight(1f), busy = testing,
                    )
                    PrimaryButton(
                        stringResource(R.string.action_connect),
                        { draft()?.let(onConnect) ?: run { showErrors = true } },
                        Modifier.weight(1f),
                    )
                }
                if (start != null) {
                    QuietButton(stringResource(R.string.action_cancel), onBack, Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun Code(text: String) {
    val c = Barid.colors
    Text(
        text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, textDirection = TextDirection.Ltr),
        color = c.text,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.line).padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

/** A labelled text field in the glass style: recessed fill, hairline border, red when invalid. */
@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    error: String? = null,
    ltr: Boolean = false,
    secret: Boolean = false,
    singleLine: Boolean = true,
    mono: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    val c = Barid.colors
    var reveal by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.text)
        Row(
            Modifier.fillMaxWidth().clip(shape).background(c.line.copy(alpha = c.line.alpha * .6f))
                .border(1.dp, if (error != null) c.danger else c.line, shape)
                .padding(start = 12.dp, end = if (secret) 2.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).padding(vertical = 12.dp).heightIn(min = 20.dp)) {
                val style = TextStyle(
                    fontFamily = if (mono) androidx.compose.ui.text.font.FontFamily.Monospace else Vazirmatn,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = c.text,
                    textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content,
                )
                if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = style.copy(color = c.muted))
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = singleLine,
                    minLines = if (singleLine) 1 else 3,
                    textStyle = style,
                    cursorBrush = SolidColor(c.accent),
                    visualTransformation = if (secret && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (secret) KeyboardType.Password else keyboard,
                        imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (secret && value.isNotEmpty()) {
                QuietButton(stringResource(if (reveal) R.string.action_hide else R.string.action_show), { reveal = !reveal })
            }
        }
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = c.danger)
    }
}

/** Asked before Barid starts sending to a server that is not Darakav's, whichever way the code arrived. */
@Composable
fun ConfirmServer(pairing: Pairing, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val c = Barid.colors
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(BaridIcons.Shield, null, Modifier.size(24.dp), tint = c.accent) },
        title = { Text(stringResource(R.string.confirm_server_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    pairing.host, style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr), color = c.text,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.line).padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(stringResource(R.string.confirm_server_body), style = MaterialTheme.typography.bodyMedium, color = c.text)
                if (!pairing.encrypted) Text(stringResource(R.string.unencrypted_warning), style = MaterialTheme.typography.bodySmall, color = c.warning)
            }
        },
        confirmButton = { QuietButton(stringResource(R.string.action_connect), onConfirm) },
        dismissButton = { QuietButton(stringResource(R.string.action_cancel), onCancel, color = c.muted) },
        containerColor = c.surfaceSolid,
        shape = RoundedCornerShape(22.dp),
    )
}
