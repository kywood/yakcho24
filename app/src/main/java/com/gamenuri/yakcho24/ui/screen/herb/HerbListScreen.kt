package com.gamenuri.yakcho24.ui.screen.herb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gamenuri.yakcho24.ui.navigation.herbDetailRoute

data class CocoClass(val id: Int, val nameEn: String, val nameKo: String)

val cocoClasses = listOf(
    CocoClass(0,  "person",         "사람"),
    CocoClass(1,  "bicycle",        "자전거"),
    CocoClass(2,  "car",            "자동차"),
    CocoClass(3,  "motorcycle",     "오토바이"),
    CocoClass(4,  "airplane",       "비행기"),
    CocoClass(5,  "bus",            "버스"),
    CocoClass(6,  "train",          "기차"),
    CocoClass(7,  "truck",          "트럭"),
    CocoClass(8,  "boat",           "보트"),
    CocoClass(9,  "traffic light",  "신호등"),
    CocoClass(10, "fire hydrant",   "소화전"),
    CocoClass(11, "stop sign",      "정지 표지판"),
    CocoClass(12, "parking meter",  "주차 미터기"),
    CocoClass(13, "bench",          "벤치"),
    CocoClass(14, "bird",           "새"),
    CocoClass(15, "cat",            "고양이"),
    CocoClass(16, "dog",            "개"),
    CocoClass(17, "horse",          "말"),
    CocoClass(18, "sheep",          "양"),
    CocoClass(19, "cow",            "소"),
    CocoClass(20, "elephant",       "코끼리"),
    CocoClass(21, "bear",           "곰"),
    CocoClass(22, "zebra",          "얼룩말"),
    CocoClass(23, "giraffe",        "기린"),
    CocoClass(24, "backpack",       "배낭"),
    CocoClass(25, "umbrella",       "우산"),
    CocoClass(26, "handbag",        "핸드백"),
    CocoClass(27, "tie",            "넥타이"),
    CocoClass(28, "suitcase",       "여행 가방"),
    CocoClass(29, "frisbee",        "프리스비"),
    CocoClass(30, "skis",           "스키"),
    CocoClass(31, "snowboard",      "스노보드"),
    CocoClass(32, "sports ball",    "스포츠 공"),
    CocoClass(33, "kite",           "연"),
    CocoClass(34, "baseball bat",   "야구 배트"),
    CocoClass(35, "baseball glove", "야구 글러브"),
    CocoClass(36, "skateboard",     "스케이트보드"),
    CocoClass(37, "surfboard",      "서핑보드"),
    CocoClass(38, "tennis racket",  "테니스 라켓"),
    CocoClass(39, "bottle",         "병"),
    CocoClass(40, "wine glass",     "와인 잔"),
    CocoClass(41, "cup",            "컵"),
    CocoClass(42, "fork",           "포크"),
    CocoClass(43, "knife",          "나이프"),
    CocoClass(44, "spoon",          "숟가락"),
    CocoClass(45, "bowl",           "그릇"),
    CocoClass(46, "banana",         "바나나"),
    CocoClass(47, "apple",          "사과"),
    CocoClass(48, "sandwich",       "샌드위치"),
    CocoClass(49, "orange",         "오렌지"),
    CocoClass(50, "broccoli",       "브로콜리"),
    CocoClass(51, "carrot",         "당근"),
    CocoClass(52, "hot dog",        "핫도그"),
    CocoClass(53, "pizza",          "피자"),
    CocoClass(54, "donut",          "도넛"),
    CocoClass(55, "cake",           "케이크"),
    CocoClass(56, "chair",          "의자"),
    CocoClass(57, "couch",          "소파"),
    CocoClass(58, "potted plant",   "화분"),
    CocoClass(59, "bed",            "침대"),
    CocoClass(60, "dining table",   "식탁"),
    CocoClass(61, "toilet",         "변기"),
    CocoClass(62, "tv",             "TV"),
    CocoClass(63, "laptop",         "노트북"),
    CocoClass(64, "mouse",          "마우스"),
    CocoClass(65, "remote",         "리모컨"),
    CocoClass(66, "keyboard",       "키보드"),
    CocoClass(67, "cell phone",     "휴대폰"),
    CocoClass(68, "microwave",      "전자레인지"),
    CocoClass(69, "oven",           "오븐"),
    CocoClass(70, "toaster",        "토스터"),
    CocoClass(71, "sink",           "싱크대"),
    CocoClass(72, "refrigerator",   "냉장고"),
    CocoClass(73, "book",           "책"),
    CocoClass(74, "clock",          "시계"),
    CocoClass(75, "vase",           "꽃병"),
    CocoClass(76, "scissors",       "가위"),
    CocoClass(77, "teddy bear",     "테디베어"),
    CocoClass(78, "hair drier",     "헤어드라이어"),
    CocoClass(79, "toothbrush",     "칫솔"),
)

val placeholderColors = listOf(
    Color(0xFF76C442), Color(0xFF4CAF50), Color(0xFF388E3C),
    Color(0xFF2E7D32), Color(0xFF558B2F), Color(0xFF689F38),
)

@Composable
fun HerbListScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "도감",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(cocoClasses, key = { it.id }) { item ->
                CocoGridItem(
                    item = item,
                    onClick = { navController.navigate(herbDetailRoute(item.id)) },
                )
            }
        }
    }
}

@Composable
private fun CocoGridItem(item: CocoClass, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(placeholderColors[item.id % placeholderColors.size]),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "#${item.id}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
        }
        Text(
            text = item.nameKo,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
        )
    }
}
