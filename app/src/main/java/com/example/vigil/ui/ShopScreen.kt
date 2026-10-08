package com.example.vigil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vigil.data.GameViewModel
import com.example.vigil.model.Item
import com.example.vigil.model.ItemSlot
import com.example.vigil.model.Rarity

@Composable
fun ShopScreen(vm: GameViewModel) {
    var selectedSlot by remember { mutableStateOf<ItemSlot?>(null) }
    var selectedItemForBuy by remember { mutableStateOf<Item?>(null) }

    val ownedItemIds = remember(vm.inventory) {
        vm.inventory.map { it.item.id }.toSet()
    }

    val filteredItems = remember(selectedSlot, vm.shopItems) {
        if (selectedSlot == null) vm.shopItems
        else vm.shopItems.filter { it.slot == selectedSlot }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TIỆM RÈN & MA PHÁP", color = C.Muted, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Cửa Hàng", color = C.Text, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("🪙 ${vm.gold}", C.Yellow)
                Chip("◆ ${vm.gems}", C.Blue)
            }
        }


        Spacer(Modifier.height(14.dp))

        // Category Filter Chips
        LazyRow(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryChip("Tất cả", selectedSlot == null) { selectedSlot = null }
            }
            items(ItemSlot.values()) { slot ->
                CategoryChip("${slot.icon} ${slot.label}", selectedSlot == slot) { selectedSlot = slot }
            }
        }

        // Danh sách vật phẩm trong Cửa Hàng
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(filteredItems) { item ->
                val isOwned = ownedItemIds.contains(item.id)
                ShopItemCard(item = item, isOwned = isOwned, onBuyClick = { selectedItemForBuy = item })
            }
        }
    }

    // Modal xác nhận mua
    if (selectedItemForBuy != null) {
        val item = selectedItemForBuy!!
        BuyConfirmDialog(
            item = item,
            userGold = vm.gold,
            userGems = vm.gems,
            onDismiss = { selectedItemForBuy = null },
            onConfirm = {
                vm.buyItem(item)
                selectedItemForBuy = null
            }
        )
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Color.White else C.Muted,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) C.Orange else C.Card)
            .border(1.dp, if (selected) C.Orange else C.Border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun ShopItemCard(item: Item, isOwned: Boolean, onBuyClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val rarityColor = item.rarity.color

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(C.Card)
            .border(1.5.dp, Color(item.rarity.borderHex), shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon hộp vuông với viền phát sáng theo độ hiếm
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D0B1E))
                    .border(1.dp, rarityColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.icon, fontSize = 30.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item.rarity.label,
                        color = rarityColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(rarityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(item.description, color = C.Muted, fontSize = 12.sp, maxLines = 2)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Hiển thị chỉ số cộng thêm
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.bonusStr > 0) StatBadge("+${item.bonusStr} Sức Mạnh", C.Orange)
            if (item.bonusEnd > 0) StatBadge("+${item.bonusEnd} Sức Bền", C.Green)
            if (item.bonusPre > 0) StatBadge("+${item.bonusPre} Chuẩn Xác", C.Blue)
            if (item.bonusLuck > 0) StatBadge("+${item.bonusLuck} Vận May", C.Yellow)
        }

        Spacer(Modifier.height(10.dp))

        // Giá và Nút mua
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.priceGold > 0) Text("🪙 ${item.priceGold}", color = C.Yellow, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (item.priceGems > 0) Text("◆ ${item.priceGems}", color = C.Blue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (isOwned) {
                Text(
                    "✓ ĐÃ SỞ HỮU",
                    color = C.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0D0B1E))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            } else {
                Text(
                    "Mua",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.Orange)
                        .clickable { onBuyClick() }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun BuyConfirmDialog(
    item: Item,
    userGold: Int,
    userGems: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val canAfford = userGold >= item.priceGold && userGems >= item.priceGems

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(C.Card)
                .border(2.dp, item.rarity.color, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(item.icon, fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(item.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(item.rarity.label, color = item.rarity.color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(item.description, color = C.Muted, fontSize = 13.sp)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.bonusStr > 0) StatBadge("+${item.bonusStr} STR", C.Orange)
                if (item.bonusEnd > 0) StatBadge("+${item.bonusEnd} END", C.Green)
                if (item.bonusPre > 0) StatBadge("+${item.bonusPre} PRE", C.Blue)
                if (item.bonusLuck > 0) StatBadge("+${item.bonusLuck} LUCK", C.Yellow)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.priceGold > 0) Chip("Giá: 🪙 ${item.priceGold}", C.Yellow)
                if (item.priceGems > 0) Chip("Giá: ◆ ${item.priceGems}", C.Blue)
            }

            Spacer(Modifier.height(18.dp))
            if (!canAfford) {
                Text("⚠️ Bạn không đủ Vàng hoặc Kim Cương!", color = C.Pink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    BigButton("Hủy", C.Border, C.Text, onDismiss)
                }
                if (canAfford) {
                    Box(Modifier.weight(1f)) {
                        BigButton("Xác nhận", C.Orange, Color.White, onConfirm)
                    }
                }
            }
        }
    }
}
