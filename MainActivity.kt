package com.example.coursework

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.coursework.ui.FridgePage
import com.example.coursework.ui.ShoppingPage
import com.example.coursework.ui.theme.CourseworkTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val fridgeViewModel = FridgeViewModel()
                //create a NavController
                val navController: NavHostController = rememberNavController()
                //add a NavHost
                NavHost(
                    navController = navController,
                    startDestination = AppScreens.Fridge.name
                ) {
                    //call the composable() function once for each of the routes
                    composable(route = AppScreens.Fridge.name) {
                        FridgePage(navController)
                    }
                    composable(route = AppScreens.Shopping.name) {
                        ShoppingPage(navController)
                    }
                }

                val postNotificationPermission = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
                val expiryNotificationService = ExpiryNotificationService(this, fridgeViewModel)

                LaunchedEffect(key1=true) {
                    if (!postNotificationPermission.status.isGranted){
                        postNotificationPermission.launchPermissionRequest()
                    } else {
                        expiryNotificationService.showNotification()
                    }
                }
            }
        }
    }
}

enum class AppScreens{
    Fridge,
    Shopping
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CourseworkTheme {
        Greeting("Android")
    }
}