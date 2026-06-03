package com.speaksnap.english.ui.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speaksnap.english.data.local.entity.LearningPlan
import com.speaksnap.english.data.repository.ScanRepository
import com.speaksnap.english.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlanViewModel(private val scanRepo: ScanRepository) : ViewModel() {
    val plans = MutableStateFlow<List<LearningPlan>>(emptyList())
    val selectedDay = MutableStateFlow(1)

    fun loadPlans(scanId: Long) {
        viewModelScope.launch {
            scanRepo.getPlansByScanId(scanId).collect { plans.value = it }
        }
    }

    fun toggleDay(day: Int) { selectedDay.value = day }

    fun markCompleted(plan: LearningPlan) {
        viewModelScope.launch { scanRepo.markPlanCompleted(plan.id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    scanRepo: ScanRepository,
    viewModel: PlanViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(scanRepo) as T
    })
) {
    val plans by viewModel.plans.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val filteredPlans = plans.filter { it.dayNumber == selectedDay }
    val dayTitles = listOf("周一" to "Day 1", "周二" to "Day 2", "周三" to "Day 3", "周四" to "Day 4", "周五" to "Day 5")

    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryIndigo, PrimaryViolet))).padding(horizontal = 20.dp, vertical = 50.dp)) {
                Column {
                    Text("📅 一周学习计划", color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("5天攻克课程内容", color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            // Day tabs
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayTitles.forEachIndexed { i, (dayName, dayLabel) ->
                        val day = i + 1
                        val isActive = day == selectedDay
                        Surface(
                            modifier = Modifier.clickable { viewModel.toggleDay(day) },
                            color = if (isActive) PrimaryIndigo else CardBackground,
                            shape = RoundedCornerShape(30.dp),
                            border = if (!isActive) BorderStroke(1.dp, BorderDark) else null
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dayName, color = if (isActive) TextWhite else TextMuted, fontSize = 11.sp)
                                Text(dayLabel, color = if (isActive) TextWhite else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Day plan cards
            if (filteredPlans.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📅", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("还没有生成学习计划", color = TextMuted, fontSize = 14.sp)
                            Text("拍照上传课程内容后自动生成", color = TextDim, fontSize = 12.sp)
                        }
                    }
                }
            }

            items(filteredPlans) { plan ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(plan.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(plan.description, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                Text("⏱️ 约 ${plan.estimatedMinutes} 分钟", color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Box(Modifier.padding(start = 12.dp).size(24.dp).clip(CircleShape).background(if (plan.isCompleted) SuccessGreen else BorderDark).clickable { viewModel.markCompleted(plan) }, contentAlignment = Alignment.Center) {
                                if (plan.isCompleted) Text("✓", color = TextWhite, fontSize = 14.sp)
                            }
                        }
                        // Progress bar
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { if (plan.isCompleted) 1f else 0f },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
                            color = if (plan.isCompleted) SuccessGreen else PrimaryIndigo,
                            trackColor = SurfaceDark
                        )
                        if (plan.isCompleted) Text("✅ 已完成", color = SuccessGreen, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
