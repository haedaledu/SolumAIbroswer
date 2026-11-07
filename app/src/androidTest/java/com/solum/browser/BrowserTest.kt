package com.solum.browser

import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import android.webkit.WebView
import android.widget.TextView
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class BrowserTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun keyIsEncryptedAndSettingsPersist() {
        val settings = BrowserSettings(context)
        val oldKey = settings.apiKey()
        val oldEngine = settings.searchEngine
        try {
            for (engine in SearchEngine.entries) {
                settings.save("test-key-not-a-real-credential", engine)
                val reopened = BrowserSettings(context)
                assertEquals(engine, reopened.searchEngine)
                assertEquals("test-key-not-a-real-credential", reopened.apiKey())
                val stored = context.getSharedPreferences("browser_settings", 0).getString("api_key", "")!!
                assertFalse(stored.contains("test-key-not-a-real-credential"))
            }
            settings.save("", SearchEngine.GOOGLE)
            assertEquals("", BrowserSettings(context).apiKey())
        } finally {
            settings.save(oldKey, oldEngine)
        }
    }

    @Test
    fun settingsScreenSavesSelectedEngine() {
        val settings = BrowserSettings(context)
        val oldKey = settings.apiKey()
        val oldEngine = settings.searchEngine
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                clickDescription("설정")
                clickText("Bing")
                clickText("저장")
                assertEquals(SearchEngine.BING, settings.searchEngine)
                onMain { assertTrue(views().any { it is EditText && it.hint == "Bing 검색 또는 주소 입력" }) }
                clickDescription("설정")
                clickText("Naver")
                clickText("저장")
                assertEquals(SearchEngine.NAVER, settings.searchEngine)
                onMain { assertTrue(views().any { it is EditText && it.hint == "Naver 검색 또는 주소 입력" }) }
            }
        } finally {
            settings.save(oldKey, oldEngine)
        }
    }

    @Test
    fun webLoadingAndAiLoadingAreVisibleAndCancellable() {
        val settings = BrowserSettings(context)
        val oldKey = settings.apiKey()
        val oldEngine = settings.searchEngine
        val server = ServerSocket(0)
        val executor = Executors.newSingleThreadExecutor()
        val response = executor.submit {
            server.accept().use { socket ->
                socket.soTimeout = 5000
                val reader = socket.getInputStream().bufferedReader()
                while (!reader.readLine().isNullOrEmpty()) { }
                Thread.sleep(1500)
                val body = "<html><body>Solum test page</body></html>".toByteArray()
                socket.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    write(body)
                    flush()
                }
            }
        }
        try {
            settings.save("test-key-not-a-real-credential", SearchEngine.GOOGLE)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val open = MainActivity::class.java.getDeclaredMethod("openInput", String::class.java)
                    open.isAccessible = true
                    open.invoke(activity, "http://127.0.0.1:${server.localPort}/")
                    assertEquals(View.VISIBLE, field<View>(activity, "progressBar").visibility)
                }
                response.get(15, TimeUnit.SECONDS)
                var loaded = false
                for (attempt in 0..100) {
                    scenario.onActivity { activity ->
                        loaded = field<View>(activity, "progressBar").visibility == View.GONE
                        val web = field<WebView>(activity, "webView")
                        assertTrue(web.width >= activity.resources.displayMetrics.widthPixels * 0.95)
                    }
                    if (loaded) break
                    Thread.sleep(100)
                }
                assertTrue("Web loading indicator must stop", loaded)
                clickText("AI 서치")
                onMain {
                    (views().first { it is EditText && it.hint == "예: 이 페이지의 핵심 내용을 알려줘" } as EditText)
                        .setText("Summarize this page")
                    views().first { it is TextView && it.text == "AI에게 질문" }.performClick()
                    val indicator = views().first { it.contentDescription == "AI 답변 생성 중" }
                    assertEquals(View.VISIBLE, indicator.visibility)
                }
                clickText("닫기")
                onMain { assertTrue(views().any { it is TextView && it.text == "AI 서치" }) }
            }
        } finally {
            server.close()
            executor.shutdownNow()
            settings.save(oldKey, oldEngine)
        }
    }

    private inline fun <reified T> field(activity: MainActivity, name: String): T =
        MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.get(activity) as T

    private fun descendants(view: View): List<View> =
        listOf(view) + if (view is ViewGroup) {
            (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
        } else emptyList()

    private fun views(): List<View> =
        WindowInspector.getGlobalWindowViews().flatMap { descendants(it) }

    private fun onMain(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun clickText(text: String) =
        onMain { views().first { it is TextView && it.text == text && it.isShown }.performClick() }

    private fun clickDescription(description: String) =
        onMain { views().first { it.contentDescription == description && it.isShown }.performClick() }
}
