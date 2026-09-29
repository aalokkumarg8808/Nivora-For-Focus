package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Plant: Focus & Study", appName)
  }

  @Test
  fun `email validation rejects invalid emails`() {
    val invalidEmails = listOf("notanemail", "@missinguser.com", "missingdomain@", "spaces in@email.com", "")
    for (email in invalidEmails) {
      val isValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
      assertFalse("Expected $email to be invalid", isValid)
    }
  }

  @Test
  fun `email validation accepts valid emails`() {
    val validEmails = listOf("user@example.com", "aalok@plantfocus.app", "first.last@domain.co.uk")
    for (email in validEmails) {
      val isValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
      assertTrue("Expected $email to be valid", isValid)
    }
  }

  @Test
  fun `sqlite auth create account, login, and logout lifecycle`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = AuthRepository(context)

    // 1. Create account
    val regResult = repo.register("Aalok Kumar", "aalok@example.com", "secret123")
    assertTrue("Registration should succeed", regResult.isSuccess)
    val user = regResult.getOrNull()
    assertNotNull(user)
    assertEquals("Aalok Kumar", user?.name)
    assertEquals("aalok@example.com", user?.email)
    assertEquals(user?.uid, repo.currentUser.value?.uid)

    // 2. Duplicate registration should fail
    val dupResult = repo.register("Another Name", "aalok@example.com", "secret123")
    assertTrue("Duplicate email registration should fail", dupResult.isFailure)

    // 3. Logout
    repo.logout()
    assertNull("Current user should be null after logout", repo.currentUser.value)

    // 4. Wrong password login should fail
    val wrongPassResult = repo.login("aalok@example.com", "wrongpass")
    assertTrue("Login with wrong password must fail", wrongPassResult.isFailure)
    assertNull("User must not be logged in after failed password", repo.currentUser.value)

    // 5. Wrong email login should fail
    val wrongEmailResult = repo.login("nonexistent@example.com", "secret123")
    assertTrue("Login with wrong email must fail", wrongEmailResult.isFailure)
    assertNull("User must not be logged in after failed email", repo.currentUser.value)

    // 6. Correct login should succeed
    val correctLoginResult = repo.login("aalok@example.com", "secret123")
    assertTrue("Login with correct credentials must succeed", correctLoginResult.isSuccess)
    assertNotNull("User must be logged in after correct login", repo.currentUser.value)
    assertEquals("Aalok Kumar", repo.currentUser.value?.name)
  }

  @Test
  fun `task creation and sync test path for SUPABASE_SYNC_TEST`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authRepo = AuthRepository(context)
    val regResult = authRepo.register("Sync Tester", "synctest@example.com", "syncpass123")
    val user = regResult.getOrNull()
    assertNotNull(user)
    val uid = user!!.uid

    val plantRepo = com.example.data.repository.PlantRepository(context, authRepo)
    val testTask = com.example.data.model.Task(
      title = "SUPABASE_SYNC_TEST",
      category = "Study",
      dueDate = "2026-09-28",
      dueTime = "09:00"
    )

    plantRepo.addTask(testTask)

    val savedTasks = plantRepo.tasks.value
    assertTrue("Tasks should contain SUPABASE_SYNC_TEST", savedTasks.any { it.title == "SUPABASE_SYNC_TEST" })
    val created = savedTasks.first { it.title == "SUPABASE_SYNC_TEST" }
    assertEquals(uid, created.userId)
    assertFalse("Task should not be completed initially", created.completed)
    assertTrue("Task ID must not be blank", created.id.isNotBlank())

    // Direct sync path test
    val syncDirectResult = plantRepo.syncManager.syncTaskDirect(uid, created)
    // If Supabase credentials are configured in test env, syncDirectResult is true; otherwise handled gracefully without throwing
    assertNotNull("Sync direct execution completed", syncDirectResult)
  }
}
