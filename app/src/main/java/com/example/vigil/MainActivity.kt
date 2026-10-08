package com.example.vigil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import com.example.vigil.data.GameViewModel
import com.example.vigil.pose.Exercise
import com.example.vigil.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VigilTheme { AppRoot() } }
    }
}

@Composable
fun AppRoot(vm: GameViewModel = viewModel()) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val showBar = route in listOf("today", "adventure", "shop", "guild", "hero")

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = C.Bg,
            bottomBar = {
                if (showBar) BottomBar(route) { dest ->
                    nav.navigate(dest) {
                        popUpTo("today")
                        launchSingleTop = true
                    }
                }
            }
        ) { pad ->
            NavHost(nav, "today", Modifier.padding(bottom = pad.calculateBottomPadding())) {
                composable("today") { TodayScreen(vm, nav) }
                composable("adventure") { AdventureScreen(vm, nav) }
                composable("shop") { ShopScreen(vm) }
                composable("guild") { GuildScreen() }
                composable("hero") { HeroScreen(vm, nav) }
                composable("challenges") { ChallengesScreen(vm) { nav.popBackStack() } }
                composable("workout/{ex}") { backStack ->
                    val exName = backStack.arguments?.getString("ex") ?: Exercise.PUSHUP.name
                    val ex = try { Exercise.valueOf(exName) } catch (e: Exception) { Exercise.PUSHUP }
                    WorkoutScreen(ex, vm, isBoss = false) { nav.popBackStack() }
                }
                composable("workout/{ex}/{isBoss}") { backStack ->
                    val exName = backStack.arguments?.getString("ex") ?: Exercise.PUSHUP.name
                    val isBoss = backStack.arguments?.getString("isBoss")?.toBooleanStrictOrNull() ?: false
                    val ex = try { Exercise.valueOf(exName) } catch (e: Exception) { Exercise.PUSHUP }
                    WorkoutScreen(ex, vm, isBoss = isBoss) { nav.popBackStack() }
                }
                composable("workout/{ex}/{isBoss}/{stageId}") { backStack ->
                    val exName = backStack.arguments?.getString("ex") ?: Exercise.PUSHUP.name
                    val isBoss = backStack.arguments?.getString("isBoss")?.toBooleanStrictOrNull() ?: false
                    val stageId = backStack.arguments?.getString("stageId")?.toIntOrNull()
                    val ex = try { Exercise.valueOf(exName) } catch (e: Exception) { Exercise.PUSHUP }
                    WorkoutScreen(ex, vm, stageId = stageId, isBoss = isBoss) { nav.popBackStack() }
                }
            }
        }

        // Modal ăn mừng và hiển thị vật phẩm rơi ra khi kết thúc bài tập
        val reward = vm.lastReward
        if (reward != null) {
            LootDropDialog(reward, vm) {
                vm.dismissReward()
            }
        }

        // Thông báo Toast nhỏ ở đầu màn hình
        val toast = vm.toastMessage
        if (toast != null) {
            LaunchedEffect(toast) {
                kotlinx.coroutines.delay(2500)
                vm.clearToast()
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = toast,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xE62A2650))
                        .border(1.dp, C.Orange, RoundedCornerShape(14.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomBar(current: String?, go: (String) -> Unit) {
    val items = listOf(
        Triple("today", "🏠", "Hôm nay"),
        Triple("adventure", "🧭", "Phiêu lưu"),
        Triple("shop", "🛒", "Cửa hàng"), // Ở CHÍNH GIỮA THEO YÊU CẦU
        Triple("guild", "👥", "Sảnh"),
        Triple("hero", "🛡", "Anh hùng")
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(C.Card)
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (r, icon, label) ->
            val sel = r == current
            Column(
                Modifier
                    .clickable { go(r) }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (r == "shop") {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (sel) C.Yellow else C.Orange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 26.sp)
                    }
                } else {
                    Text(icon, fontSize = 24.sp)
                }
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sel || r == "shop") C.Orange else C.Muted
                )
            }
        }
    }
}