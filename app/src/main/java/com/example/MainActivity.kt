package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.BatchProcessingScreen
import com.example.ui.screens.CompressPdfScreen
import com.example.ui.screens.DocumentEditorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PageReorganizerScreen
import com.example.ui.screens.PdfToImagesScreen
import com.example.ui.screens.ViewerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PdfViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val viewModel: PdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PdfAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun PdfAppNavigation(viewModel: PdfViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { navController.navigate("editor") },
                onNavigateToReorganizer = { navController.navigate("reorganizer") },
                onNavigateToCompress = { navController.navigate("compress") },
                onNavigateToPdfToImages = { navController.navigate("pdf_to_images") },
                onNavigateToBatch = { navController.navigate("batch") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToViewer = { path ->
                    val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    navController.navigate("viewer/$encoded")
                }
            )
        }

        composable("batch") {
            BatchProcessingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { path ->
                    val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    navController.navigate("viewer/$encoded")
                }
            )
        }

        composable("pdf_to_images") {
            PdfToImagesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { path ->
                    val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    navController.navigate("viewer/$encoded")
                }
            )
        }

        composable("compress") {
            CompressPdfScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { path ->
                    val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    navController.navigate("viewer/$encoded")
                }
            )
        }

        composable("editor") {
            DocumentEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { path ->
                    val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    navController.navigate("viewer/$encoded")
                }
            )
        }

        composable("reorganizer") {
            PageReorganizerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { path ->
                    val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    navController.navigate("viewer/$encoded")
                }
            )
        }

        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { path ->
                    val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    navController.navigate("viewer/$encoded")
                }
            )
        }

        composable(
            route = "viewer/{encodedPath}",
            arguments = listOf(navArgument("encodedPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("encodedPath") ?: ""
            val decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
            ViewerScreen(
                filePath = decodedPath,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
