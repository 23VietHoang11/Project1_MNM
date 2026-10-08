package com.example.vigil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vigil.data.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(vm: GameViewModel) {
    var isRegisterTab by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("🧑‍🎤") }

    val avatars = listOf("🧑‍🎤", "🥷", "👑", "🗿", "🐉", "🔥", "🐺", "⚡")

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0C0A1D),
                        Color(0xFF140F2D),
                        Color(0xFF1D143D),
                        Color(0xFF0F0D22)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xE61B1638))
                .border(1.5.dp, Color(0xFF3B336A), RoundedCornerShape(26.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo
            Box(
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, Color(0xFFFFC94D), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.vigil.R.drawable.app_avatar),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "VIGIL RPG",
                color = C.Text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                if (isRegisterTab) "Gia nhập hàng ngũ chiến binh huyền thoại" else "Tiếp tục hành trình chinh phục thử thách",
                color = C.Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Tab Selector
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF120E29))
                    .padding(4.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isRegisterTab) C.Orange else Color.Transparent)
                        .clickable {
                            isRegisterTab = false
                            vm.authErrorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🔑 ĐĂNG NHẬP",
                        color = if (!isRegisterTab) Color.White else C.Muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isRegisterTab) C.Orange else Color.Transparent)
                        .clickable {
                            isRegisterTab = true
                            vm.authErrorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "📝 ĐĂNG KÝ",
                        color = if (isRegisterTab) Color.White else C.Muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Chọn Avatar khi Đăng ký
            if (isRegisterTab) {
                Text("Chọn Linh Thú / Avatar của bạn:", color = C.Muted, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    avatars.forEach { avt ->
                        val isSelected = selectedAvatar == avt
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x66FF8A3D) else Color(0xFF140F2E))
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) C.Orange else Color(0xFF332959),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedAvatar = avt },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(avt, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Input: Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Tên tài khoản (Username)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Orange,
                    unfocusedBorderColor = Color(0xFF3D346E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = C.Orange,
                    unfocusedLabelColor = C.Muted
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Input: Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Orange,
                    unfocusedBorderColor = Color(0xFF3D346E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = C.Orange,
                    unfocusedLabelColor = C.Muted
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Hiển thị thông báo lỗi
            if (vm.authErrorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    vm.authErrorMessage!!,
                    color = Color(0xFFFF5A7A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            // Nút bấm Xác nhận
            BigButton(
                if (isRegisterTab) "✨ TẠO TÀI KHOẢN VÀ BẮT ĐẦU" else "⚔️ TIẾN VÀO THẾ GIỚI VIGIL",
                C.Orange,
                Color.White
            ) {
                if (isRegisterTab) {
                    vm.register(username, password, selectedAvatar)
                } else {
                    vm.login(username, password)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Nút đăng nhập nhanh Demo (Dành cho thử nghiệm ngay)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF261F48))
                    .clickable {
                        username = "Hachimi"
                        password = "123"
                        vm.loginAsDemo()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚡ Chơi Thử Nhanh (Acc: Hachimi)",
                    color = C.Yellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
