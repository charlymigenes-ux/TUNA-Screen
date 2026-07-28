package com.nopal.tunascreen.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nopal.tunascreen.feature.discovery.DiscoveryScreen
import com.nopal.tunascreen.feature.machine.AdvancedMachineScreen
import com.nopal.tunascreen.feature.machine.MachineScreen
import com.nopal.tunascreen.feature.pairing.PairingScreen
import com.nopal.tunascreen.feature.workshop.WorkshopScreen
import com.nopal.tunascreen.feature.workshop.CamerasScreen
import com.nopal.tunascreen.feature.workshop.MachinesScreen
import com.nopal.tunascreen.feature.workshop.SettingsScreen

object TunaDestinations {
    const val DISCOVERY = "discovery"
    const val PAIRING = "pairing/{host}/{port}"
    const val WORKSHOP = "workshop"
    const val MACHINES = "machines"
    const val CAMERAS = "cameras"
    const val SETTINGS = "settings"
    const val MACHINE = "machine/{machineId}"
    const val MACHINE_ADVANCED = "machine/{machineId}/advanced"

    fun pairing(host: String, port: Int) = "pairing/$host/$port"
    fun machine(machineId: String) = "machine/${Uri.encode(machineId)}"
    fun machineAdvanced(machineId: String) = "machine/${Uri.encode(machineId)}/advanced"
}

@Composable
fun TunaNavGraph(navController: NavHostController, startDestination: String) {
    val navigateDashboard: (String) -> Unit = { destination ->
        navController.navigate(destination) {
            popUpTo(TunaDestinations.WORKSHOP) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(TunaDestinations.DISCOVERY) {
            DiscoveryScreen(
                onServerChosen = { host, port ->
                    navController.navigate(TunaDestinations.pairing(host, port))
                },
            )
        }
        composable(
            TunaDestinations.PAIRING,
            arguments = listOf(
                navArgument("host") { type = NavType.StringType },
                navArgument("port") { type = NavType.IntType },
            ),
        ) { backStackEntry ->
            val host = backStackEntry.arguments?.getString("host").orEmpty()
            val port = backStackEntry.arguments?.getInt("port") ?: 8420
            PairingScreen(
                host = host,
                port = port,
                onPaired = {
                    navController.navigate(TunaDestinations.WORKSHOP) {
                        popUpTo(TunaDestinations.DISCOVERY) { inclusive = true }
                    }
                },
            )
        }
        composable(TunaDestinations.WORKSHOP) {
            WorkshopScreen(
                onMachineSelected = { machineId ->
                    navController.navigate(TunaDestinations.machine(machineId))
                },
                onNavigate = navigateDashboard,
            )
        }
        composable(TunaDestinations.MACHINES) {
            MachinesScreen(
                onMachineSelected = { machineId ->
                    navController.navigate(TunaDestinations.machine(machineId))
                },
                onNavigate = navigateDashboard,
            )
        }
        composable(TunaDestinations.CAMERAS) {
            CamerasScreen(onNavigate = navigateDashboard)
        }
        composable(TunaDestinations.SETTINGS) {
            SettingsScreen(
                onNavigate = navigateDashboard,
                onPairingRemoved = {
                    navController.navigate(TunaDestinations.DISCOVERY) {
                        popUpTo(TunaDestinations.WORKSHOP) { inclusive = true }
                    }
                },
            )
        }
        composable(
            TunaDestinations.MACHINE,
            arguments = listOf(navArgument("machineId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val machineId = backStackEntry.arguments?.getString("machineId").orEmpty()
            MachineScreen(
                machineId = machineId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            TunaDestinations.MACHINE_ADVANCED,
            arguments = listOf(navArgument("machineId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val machineId = backStackEntry.arguments?.getString("machineId").orEmpty()
            AdvancedMachineScreen(machineId = machineId, onBack = { navController.popBackStack() })
        }
    }
}
