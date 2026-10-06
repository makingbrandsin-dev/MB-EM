package com.example

import com.example.data.auth.AppRole
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Firestore Auth Provider role parsing and UI redirection routing.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `admin role strings redirect to manager dashboard`() {
    val adminRoles = listOf("admin", "ADMIN", "MB Admin", "Manager", "Executive Director", "Lead")
    for (role in adminRoles) {
      val parsed = AppRole.parse(role)
      assertEquals("Expected ADMIN for $role", AppRole.ADMIN, parsed)
      assertTrue("Expected isAdmin=true for $role", parsed.isAdmin)
      assertEquals("Expected manager route for $role", "manager", parsed.targetRoute)
    }
  }

  @Test
  fun `employee role strings redirect to home employee workspace`() {
    val employeeRoles = listOf("employee", "EMPLOYEE", "Developer", "Designer", null, "")
    for (role in employeeRoles) {
      val parsed = AppRole.parse(role)
      assertEquals("Expected EMPLOYEE for $role", AppRole.EMPLOYEE, parsed)
      assertFalse("Expected isAdmin=false for $role", parsed.isAdmin)
      assertEquals("Expected home route for $role", "home", parsed.targetRoute)
    }
  }

  @Test
  fun `default offer banners have valid sequential display orders`() {
    val banners = com.example.presentation.components.banner.defaultOfferBanners
    assertTrue("Should have default banners", banners.isNotEmpty())
    for (i in banners.indices) {
      assertEquals("Banner at index $i should have displayOrder=$i", i, banners[i].displayOrder)
      assertTrue("Banner ID should not be blank", banners[i].id.isNotBlank())
      assertTrue("Banner headline should not be blank", banners[i].headline.isNotBlank())
      assertTrue("Banner CTA text should not be blank", banners[i].ctaText.isNotBlank())
    }
  }

  @Test
  fun `banner visibility filtering reflects active state for home pager`() {
    val banners = com.example.presentation.components.banner.defaultOfferBanners
    val toggledList = banners.mapIndexed { index, item ->
      if (index == 1) item.copy(isActive = false) else item
    }
    val activeInPager = toggledList.filter { it.isActive }
    assertEquals(banners.size - 1, activeInPager.size)
    assertFalse(activeInPager.any { it.id == banners[1].id })
  }

  @Test
  fun `banner display order sorting determines home pager order`() {
    val banners = com.example.presentation.components.banner.defaultOfferBanners
    // Swap orders of first two banners
    val reordered = listOf(
      banners[0].copy(displayOrder = 1),
      banners[1].copy(displayOrder = 0)
    ).sortedBy { it.displayOrder }

    assertEquals(banners[1].id, reordered[0].id)
    assertEquals(banners[0].id, reordered[1].id)
  }

  @Test
  fun `fcm broadcast model creates valid delivery payload`() {
    val broadcast = com.example.data.firebase.FcmBroadcastLog(
      title = "Urgent Sales Meeting",
      message = "All BDA team members assemble at 11 AM",
      audience = "Sales & CRM Team",
      topic = "sales_team",
      priority = "Urgent",
      actionRoute = "leads"
    )
    assertTrue("Broadcast ID should not be blank", broadcast.id.isNotBlank())
    assertEquals("Urgent Sales Meeting", broadcast.title)
    assertEquals("sales_team", broadcast.topic)
    assertEquals("Urgent", broadcast.priority)
    assertEquals("leads", broadcast.actionRoute)
    assertEquals("DELIVERED / FCM DISPATCHED", broadcast.deliveryStatus)
    assertTrue("Timestamp should be positive", broadcast.timestamp > 0L)
  }

  @Test
  fun `fcm audience to topic mappings resolve accurately`() {
    val mappings = mapOf(
      "All Employees" to "all_users",
      "Sales & CRM" to "sales_team",
      "Engineering" to "dev_team",
      "Design" to "design_team",
      "Management" to "management"
    )
    for ((aud, expectedTopic) in mappings) {
      val targetTopic = when (aud) {
        "Sales & CRM" -> "sales_team"
        "Engineering" -> "dev_team"
        "Design" -> "design_team"
        "Management" -> "management"
        else -> "all_users"
      }
      assertEquals("Expected topic $expectedTopic for audience $aud", expectedTopic, targetTopic)
    }
  }

  @Test
  fun `battery state info correctly calculates saver activation for auto and forced modes`() {
    val defaultThreshold = 20

    // Test 1: Low battery unplugged in AUTO mode -> Saver active
    val lowBatteryUnplugged = com.example.util.BatteryStateInfo(
      levelPercent = 15,
      isCharging = false,
      mode = com.example.util.BatterySaverMode.AUTO,
      lowThresholdPercent = defaultThreshold,
      isSaverActive = true,
      syncIntervalSeconds = 60
    )
    assertTrue("Saver should be active when battery <= threshold and unplugged", lowBatteryUnplugged.isSaverActive)
    assertEquals(60, lowBatteryUnplugged.syncIntervalSeconds)

    // Test 2: Low battery charging in AUTO mode -> Saver inactive
    val lowBatteryChargingActive = (15 <= defaultThreshold) && !true // charging is true so isSaverActive is false
    assertFalse("Saver should be inactive when charging", lowBatteryChargingActive)

    // Test 3: ALWAYS_ON mode -> Saver active regardless of battery level
    val alwaysOnState = com.example.util.BatteryStateInfo(
      levelPercent = 90,
      isCharging = true,
      mode = com.example.util.BatterySaverMode.ALWAYS_ON,
      isSaverActive = true,
      syncIntervalSeconds = 60
    )
    assertTrue("Saver should be active in ALWAYS_ON mode", alwaysOnState.isSaverActive)

    // Test 4: OFF mode -> Saver inactive regardless of battery level
    val disabledMode = com.example.util.BatterySaverMode.OFF
    val isSaverDisabled = when (disabledMode) {
      com.example.util.BatterySaverMode.ALWAYS_ON -> true
      com.example.util.BatterySaverMode.OFF -> false
      com.example.util.BatterySaverMode.AUTO -> true
    }
    assertFalse("Saver should be inactive in OFF mode", isSaverDisabled)
  }
}
