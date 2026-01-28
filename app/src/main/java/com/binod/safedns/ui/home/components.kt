import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binod.safedns.R
import com.binod.safedns.domain.model.ProtectionProfile
import com.binod.safedns.domain.model.ProtectionState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSelector(
    profile: ProtectionProfile,
    upstreamLabel: String,
    state: ProtectionState,
    onPickProfile: (ProtectionProfile) -> Unit,
    onOpenCustomProfile: () -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        onClick = { showBottomSheet = true }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛡️", fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.profile,
                        profile.name.lowercase().replaceFirstChar { it.uppercase() }),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = stringResource(R.string.upstream_dns, upstreamLabel),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Select",
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E3A4A),
            contentColor = Color.White
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Text(
                    text = "Select Protection Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                // Profile Options
                ProfileOption(
                    title = stringResource(R.string.balanced_recommended),
                    description = "Blocks ads and trackers while maintaining compatibility",
                    isSelected = profile == ProtectionProfile.BALANCED,
                    onClick = {
                        onPickProfile(ProtectionProfile.BALANCED)
                        showBottomSheet = false
                    }
                )

                ProfileOption(
                    title = stringResource(R.string.strict_may_break_apps),
                    description = "Maximum protection, may affect some app functionality",
                    isSelected = profile == ProtectionProfile.STRICT,
                    onClick = {
                        onPickProfile(ProtectionProfile.STRICT)
                        showBottomSheet = false
                    }
                )

                ProfileOption(
                    title = stringResource(R.string.custom),
                    description = "Configure your own filtering rules",
                    isSelected = profile == ProtectionProfile.CUSTOM,
                    onClick = {
                        showBottomSheet = false
                        onOpenCustomProfile()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF4ECDC4).copy(alpha = 0.15f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color(0xFF4ECDC4) else Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF4ECDC4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}