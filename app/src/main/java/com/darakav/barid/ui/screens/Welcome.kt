package com.darakav.barid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import com.darakav.barid.R
import com.darakav.barid.ui.components.BaridIcons
import com.darakav.barid.ui.components.BaridMark
import com.darakav.barid.ui.components.BrandTile
import com.darakav.barid.ui.components.DarakavMark
import com.darakav.barid.ui.components.Divider
import com.darakav.barid.ui.components.GlassCard
import com.darakav.barid.ui.components.IconTile
import com.darakav.barid.ui.components.PrimaryButton
import com.darakav.barid.ui.theme.Barid
import com.darakav.barid.ui.theme.BaridColors

/** First run: what Barid does and what never leaves the phone, before any permission is asked. */
@Composable
fun WelcomeScreen(onConnect: () -> Unit) {
    val c = Barid.colors
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(40.dp))
            BrandTile(
                BaridMark, 84.dp,
                Modifier.shadow(28.dp, RoundedCornerShape(24.dp), ambientColor = c.tones.glowA, spotColor = c.tones.glowA),
                corner = 24.dp,
            )
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = c.text)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.welcome_tagline), style = MaterialTheme.typography.bodyLarge, color = c.muted,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Promise(BaridIcons.Bank, stringResource(R.string.promise_bank_title), stringResource(R.string.promise_bank_body))
            Divider()
            Promise(BaridIcons.Lock, stringResource(R.string.promise_otp_title), stringResource(R.string.promise_otp_body))
            Divider()
            Promise(BaridIcons.Refresh, stringResource(R.string.promise_queue_title), stringResource(R.string.promise_queue_body))
        }
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(stringResource(R.string.action_connect), onConnect, Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                BrandTile(DarakavMark, 16.dp, corner = 4.dp, markScale = .9f)
                Text(
                    stringResource(R.string.made_by), style = MaterialTheme.typography.labelSmall, color = c.muted,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Promise(icon: ImageVector, title: String, body: String) {
    val c = Barid.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, c.accent, c.accentSoft)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            Text(body, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

