package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.DeviceType
import com.example.data.model.ScannedDevice
import com.example.data.model.SignalTier
import com.example.data.model.TacticalMarker
import com.example.data.repository.WarWalkingRepository
import com.example.navigation.NavigationMode
import com.example.navigation.SignalRoutingEngine
import com.example.scanner.BleScanner
import com.example.scanner.WifiScanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: WarWalkingRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = WarWalkingRepository(db.scannedNetworkDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SignalRadar", appName)
  }

  @Test
  fun `verify scanners start completely empty without any mock data`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val wifiScanner = WifiScanner(context)
    val bleScanner = BleScanner(context)

    assertTrue("Wi-Fi scanner must be empty initially", wifiScanner.wifiDevices.value.isEmpty())
    assertTrue("BLE scanner must be empty initially", bleScanner.bleDevices.value.isEmpty())
  }

  @Test
  fun `test war walking room database auto-save and persistence`() = runBlocking {
    val scannedDevice = ScannedDevice(
      id = "wifi_room_test",
      name = "FibraOptica_5G",
      macAddress = "E4:8D:8C:11:22:33",
      rssi = -55,
      type = DeviceType.WIFI,
      frequency = 5180
    )

    repository.saveOrUpdateDevice(scannedDevice, 40.4168, -3.7038)

    val savedList = repository.allSavedNetworks.first()
    assertEquals(1, savedList.size)
    assertEquals("FibraOptica_5G", savedList[0].ssid)
    assertEquals("E4:8D:8C:11:22:33", savedList[0].bssidMac)
    assertEquals(-55, savedList[0].ultimoRssi)
    assertEquals(40.4168, savedList[0].latitud, 0.0001)
  }

  @Test
  fun `test scanned device model and signal tiers`() {
    val wifiDevice = ScannedDevice(
      id = "wifi_test",
      name = "MiFibra_5G",
      macAddress = "AA:BB:CC:DD:EE:FF",
      rssi = -50,
      type = DeviceType.WIFI,
      frequency = 5180
    )

    assertEquals("MiFibra_5G", wifiDevice.displayName)
    assertEquals(SignalTier.EXCELLENT, wifiDevice.signalTier)
    assertTrue(wifiDevice.estimatedDistanceMeters > 0.0)
    assertTrue(wifiDevice.bandLabel?.contains("5 GHz") == true)
  }

  @Test
  fun `test routing engine calculates route for tactical marker`() {
    val userLat = 40.4168
    val userLon = -3.7038

    val marker = TacticalMarker(
      id = "marker_1",
      name = "Red Histórica Guardada",
      bssidMac = "AA:11:22:33:44:55",
      lat = 40.4172,
      lon = -3.7032,
      rssi = -68,
      type = DeviceType.WIFI,
      isOnline = false,
      distanceMeters = 65.0
    )

    val route = SignalRoutingEngine.calculateRouteForMarker(
      userLat = userLat,
      userLon = userLon,
      marker = marker,
      mode = NavigationMode.STREETS
    )

    assertNotNull(route)
    assertEquals(NavigationMode.STREETS, route.mode)
    assertEquals("Red Histórica Guardada", route.targetName)
    assertTrue(route.waypoints.size >= 2)
  }
}
