package com.example.simpletaskmanagement.presentation.main

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpletaskmanagement.domain.model.Task
import com.example.simpletaskmanagement.presentation.main.components.AdBannerView
import com.example.simpletaskmanagement.presentation.main.components.AddCategoryDialog
import com.example.simpletaskmanagement.presentation.main.components.SearchBarComponent
import com.example.simpletaskmanagement.presentation.main.components.TaskItem
import com.example.simpletaskmanagement.ui.theme.CompletedGreen
import com.example.simpletaskmanagement.ui.theme.OverdueRed
import com.example.simpletaskmanagement.ui.theme.SplendBlue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    uiState: MainUiState,
    onMenuClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSyncClicked: () -> Unit,
    onToggleTaskCompletion: (String) -> Unit,
    onTaskClicked: (String) -> Unit,
    onCreateTaskClicked: () -> Unit,
    onAddCategoryConfirm: (String) -> Unit,
    onAddCategoryDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.selectedCategory == null) "Tareas / Todas" else "Tareas / ${uiState.selectedCategory.name}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClicked) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menú",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClicked) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onSyncClicked) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sincronizar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SplendBlue
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTaskClicked,
                containerColor = SplendBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nueva tarea"
                )
            }
        },
        bottomBar = {
            AdBannerView()
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar when active
            if (uiState.isSearchActive) {
                SearchBarComponent(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChanged,
                    onClose = onSearchClicked
                )
            }

            // Task content
            if (uiState.groupedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No se encontraron tareas" else "¡No hay tareas pendientes!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toca el botón + para añadir una tarea",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    uiState.groupedTasks.forEach { (sectionTitle, tasks) ->
                        stickyHeader(key = sectionTitle) {
                            SectionHeader(sectionTitle = sectionTitle, count = tasks.size)
                        }

                        items(
                            items = tasks,
                            key = { it.id }
                        ) { task ->
                            TaskItem(
                                task = task,
                                onToggleCompletion = { onToggleTaskCompletion(task.id) },
                                onClick = { onTaskClicked(task.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (uiState.showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = onAddCategoryDismiss,
            onConfirm = onAddCategoryConfirm
        )
    }
}

@Composable
private fun SectionHeader(
    sectionTitle: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    val headerColor = when (sectionTitle) {
        "Atrasado" -> OverdueRed
        "Hoy" -> SplendBlue
        "Completadas" -> CompletedGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val headerIcon = when (sectionTitle) {
        "Atrasado" -> Icons.Default.Warning
        "Completadas" -> Icons.Default.CheckCircleOutline
        else -> Icons.Default.CalendarToday
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = headerIcon,
                contentDescription = null,
                tint = headerColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$sectionTitle ($count)",
                color = headerColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
