package com.solum.browser

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.Future

class MainActivity : AppCompatActivity() {
    private val aiExecutor = Executors.newSingleThreadExecutor()
    private var aiRequest: Future<*>? = null
    @Volatile private var aiConnection: HttpURLConnection? = null
    private lateinit var addressField: EditText
    private lateinit var webView: WebView
    private lateinit var homeView: View
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var homeSearchButton: MaterialButton
    private lateinit var browserSettings: BrowserSettings
    private lateinit var backButton: TextView
    private lateinit var forwardButton: TextView
    private var isShowingPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        browserSettings = BrowserSettings(this)
        window.statusBarColor = getColor(R.color.surface)
        window.navigationBarColor = getColor(R.color.surface)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.surface))
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(4))
        }

        backButton = toolbarButton("‹").apply {
            contentDescription = "뒤로 가기"
            isEnabled = false
            setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        }
        forwardButton = toolbarButton("›").apply {
            contentDescription = "앞으로 가기"
            isEnabled = false
            setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        }
        addressField = EditText(this).apply {
            setSingleLine(true)
            textSize = 16f
            hint = "${browserSettings.searchEngine.label} 검색 또는 주소 입력"
            setTextColor(getColor(R.color.on_surface))
            setHintTextColor(getColor(R.color.on_surface_variant))
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(dp(18), 0, dp(12), 0)
            background = roundedBackground(getColor(R.color.surface_container), dp(28))
            setOnEditorActionListener { _, actionId, event ->
                val enterPressed = event?.keyCode == KeyEvent.KEYCODE_ENTER
                if (actionId == EditorInfo.IME_ACTION_GO || enterPressed) {
                    openInput(addressField.text.toString())
                    true
                } else {
                    false
                }
            }
        }
        val navigationRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), dp(4))
        }
        navigationRow.addView(backButton, LinearLayout.LayoutParams(dp(48), dp(48)))
        navigationRow.addView(forwardButton, LinearLayout.LayoutParams(dp(48), dp(48)))
        addressRow.addView(addressField, LinearLayout.LayoutParams(0, dp(56), 1f))
        root.addView(addressRow)
        navigationRow.addView(toolbarButton("↻").apply {
            contentDescription = "새로고침"
            setOnClickListener {
                if (isShowingPage) webView.reload() else addressField.requestFocus()
            }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        navigationRow.addView(expressiveButton().apply {
            text = "AI 서치"
            textSize = 16f
            minWidth = 0
            minimumWidth = 0
            setOnClickListener { showAiSearch() }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        navigationRow.addView(toolbarButton("⚙").apply {
            contentDescription = "설정"
            setOnClickListener { showSettings() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(navigationRow)

        val content = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
        }
        progressBar = expressiveProgress("웹페이지 로딩 중")
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    isShowingPage = true
                    homeView.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                    if (!addressField.hasFocus()) addressField.setText(url.orEmpty())
                    updateNavigationButtons()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isShowingPage = true
                    progressBar.visibility = View.GONE
                    if (!addressField.hasFocus()) addressField.setText(url.orEmpty())
                    updateNavigationButtons()
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) progressBar.visibility = View.GONE
                }

                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    if (request?.isForMainFrame == true) progressBar.visibility = View.GONE
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress >= 100) progressBar.visibility = View.GONE
                }
            }
        }
        content.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        homeView = createHomeView()
        content.addView(homeView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        content.addView(progressBar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(16),
            Gravity.TOP
        ))
        root.addView(content, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        setContentView(root)
    }

    private fun createHomeView(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = roundedBackground(getColor(R.color.primary_container), dp(32))
        }
        container.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = null
        }, LinearLayout.LayoutParams(dp(88), dp(88)).apply { bottomMargin = dp(20) })
        container.addView(TextView(this).apply {
            text = "Solum"
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(getColor(R.color.on_primary_container))
            gravity = Gravity.CENTER
        })
        container.addView(TextView(this).apply {
            text = "검색하고, 둘러보고,\nAI에게 물어보세요."
            textSize = 20f
            setLineSpacing(dp(4).toFloat(), 1f)
            setTextColor(getColor(R.color.on_primary_container))
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(24))
        })
        homeSearchButton = expressiveButton().apply {
            text = "${browserSettings.searchEngine.label}에서 검색"
            setOnClickListener {
                if (addressField.text.isBlank()) {
                    addressField.requestFocus()
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .showSoftInput(addressField, InputMethodManager.SHOW_IMPLICIT)
                } else {
                    openInput(addressField.text.toString())
                }
            }
        }
        container.addView(homeSearchButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
        return ScrollView(this).apply {
            isFillViewport = true
            addView(container)
        }
    }

    private fun showAiSearch() {
        if (!isShowingPage) {
            MaterialAlertDialogBuilder(this)
                .setMessage("먼저 웹페이지를 연 뒤 AI 서치를 이용해 주세요.")
                .setPositiveButton("확인", null)
                .show()
            return
        }
        val apiKey = try {
            browserSettings.apiKey()
        } catch (error: Exception) {
            MaterialAlertDialogBuilder(this)
                .setMessage(error.message ?: "API 키를 읽지 못했습니다. 설정에서 다시 저장해 주세요.")
                .setPositiveButton("설정") { _, _ -> showSettings() }
                .setNegativeButton("닫기", null)
                .show()
            return
        }
        if (apiKey.isBlank()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("AI 연결 설정 필요")
                .setMessage("설정에서 Google Gemini API 키를 입력해 주세요.")
                .setPositiveButton("설정") { _, _ -> showSettings() }
                .setNegativeButton("닫기", null)
                .show()
            return
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(8))
        }
        panel.addView(TextView(this).apply {
            text = "현재 페이지에 대해 질문하세요"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(getColor(R.color.on_surface))
        })
        panel.addView(TextView(this).apply {
            text = "페이지의 글과 주소가 Google Gemini에 전송됩니다."
            textSize = 13f
            setTextColor(getColor(R.color.on_surface_variant))
            setPadding(0, dp(6), 0, dp(12))
        })
        val questionField = EditText(this).apply {
            hint = "예: 이 페이지의 핵심 내용을 알려줘"
            textSize = 16f
            minLines = 2
            maxLines = 4
            gravity = Gravity.TOP
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setTextColor(getColor(R.color.on_surface))
            setHintTextColor(getColor(R.color.on_surface_variant))
            background = roundedBackground(getColor(R.color.surface_container), dp(20))
        }
        panel.addView(questionField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(94)
        ))
        val aiProgress = expressiveProgress("AI 답변 생성 중")
        panel.addView(aiProgress, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(24)
        ).apply { topMargin = dp(12) })
        val resultScroll = ScrollView(this).apply {
            visibility = View.GONE
            setPadding(0, dp(14), 0, dp(4))
        }
        val resultText = TextView(this).apply {
            textSize = 16f
            setLineSpacing(dp(4).toFloat(), 1f)
            setTextColor(getColor(R.color.on_surface))
            setTextIsSelectable(true)
        }
        resultScroll.addView(resultText)
        panel.addView(resultScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(180)
        ))
        val askButton = expressiveButton().apply {
            text = "AI에게 질문"
        }
        panel.addView(askButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56)
        ).apply { topMargin = dp(12) })
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(ScrollView(this).apply { addView(panel) })
            .setNegativeButton("닫기", null)
            .create()
        dialog.setOnDismissListener {
            aiProgress.visibility = View.GONE
            cancelAiRequest()
        }
        askButton.setOnClickListener {
            val question = questionField.text.toString().trim()
            if (question.isEmpty()) {
                questionField.error = "질문을 입력해 주세요."
                return@setOnClickListener
            }
            askButton.isEnabled = false
            askButton.text = "답변 만드는 중..."
            resultText.text = ""
            aiProgress.visibility = View.VISIBLE
            resultScroll.visibility = View.VISIBLE
            webView.evaluateJavascript(
                "(function(){return JSON.stringify({title:document.title,url:location.href,text:(document.body&&document.body.innerText||'').slice(0,12000)});})()"
            ) { rawPage ->
                if (!dialog.isShowing || isDestroyed) return@evaluateJavascript
                val page = try {
                    val jsonString = org.json.JSONTokener(rawPage.orEmpty()).nextValue() as? String
                        ?: throw IllegalStateException("페이지 내용을 읽지 못했습니다.")
                    JSONObject(jsonString)
                } catch (error: Exception) {
                    aiProgress.visibility = View.GONE
                    askButton.isEnabled = true
                    askButton.text = "다시 시도"
                    resultText.text = "페이지 내용을 읽지 못했습니다: ${error.message ?: "페이지를 다시 열어 주세요."}"
                    return@evaluateJavascript
                }
                requestAiAnswer(
                    apiKey = apiKey,
                    question = question,
                    pageTitle = page.optString("title"),
                    pageUrl = page.optString("url"),
                    pageText = page.optString("text"),
                    onResult = { answer ->
                        if (!dialog.isShowing) return@requestAiAnswer
                        aiProgress.visibility = View.GONE
                        askButton.isEnabled = true
                        askButton.text = "다시 질문"
                        resultText.text = answer
                    },
                    onError = { message ->
                        if (!dialog.isShowing) return@requestAiAnswer
                        aiProgress.visibility = View.GONE
                        askButton.isEnabled = true
                        askButton.text = "다시 시도"
                        resultText.text = message
                    }
                )
            }
        }
        dialog.show()
    }

    private fun requestAiAnswer(
        apiKey: String,
        question: String,
        pageTitle: String,
        pageUrl: String,
        pageText: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        aiRequest = aiExecutor.submit {
            var connection: HttpURLConnection? = null
            try {
                require(BuildConfig.AI_MODEL.matches(Regex("[A-Za-z0-9._-]+"))) {
                    "AI 모델 이름을 확인해 주세요."
                }
                val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.AI_MODEL}:generateContent")
                connection = endpoint.openConnection() as HttpURLConnection
                aiConnection = connection
                connection.requestMethod = "POST"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000
                connection.doOutput = true
                connection.setRequestProperty("x-goog-api-key", apiKey)
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

                val userPrompt = """
                    사용자의 질문: $question

                    현재 페이지 제목: $pageTitle
                    현재 페이지 주소: $pageUrl
                    현재 페이지 내용:
                    $pageText
                """.trimIndent()
                val body = JSONObject()
                    .put("generationConfig", JSONObject().put("maxOutputTokens", 1024))
                    .put("systemInstruction", JSONObject()
                        .put("parts", JSONArray().put(JSONObject()
                            .put("text", "현재 페이지 내용에 근거해 한국어로 간결하고 정확하게 답하세요. 페이지에 없는 내용은 추측하지 마세요."))))
                    .put("contents", JSONArray()
                        .put(JSONObject()
                            .put("role", "user")
                            .put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))))
                connection.outputStream.use {
                    it.write(body.toString().toByteArray(StandardCharsets.UTF_8))
                }
                val status = connection.responseCode
                if (status !in 200..299) {
                    val reason = when (status) {
                        400, 401, 403 -> "API 키와 모델 사용 권한을 확인해 주세요."
                        404 -> "이 계정에서 사용할 수 없는 모델입니다. AI_MODEL 설정을 확인해 주세요."
                        429 -> "사용량 한도를 초과했습니다. 잠시 후 다시 시도하거나 API 사용량을 확인해 주세요."
                        500, 502, 503, 504 -> "Gemini 서버가 혼잡하거나 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해 주세요."
                        else -> "API 설정과 서비스 상태를 확인해 주세요."
                    }
                    throw IllegalStateException("Gemini 서비스 응답 오류 ($status). $reason")
                }
                val responseBody = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val response = JSONObject(responseBody)
                val candidate = response.optJSONArray("candidates")?.optJSONObject(0)
                    ?: throw IllegalStateException("Gemini가 답변을 반환하지 않았습니다. 질문이나 페이지 내용을 확인해 주세요.")
                val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
                val answer = buildString {
                    if (parts != null) {
                        for (index in 0 until parts.length()) {
                            val part = parts.getJSONObject(index)
                            if (!part.optBoolean("thought")) append(part.optString("text"))
                        }
                    }
                }.trim()
                if (answer.isBlank()) {
                    throw IllegalStateException("Gemini가 텍스트 답변을 반환하지 않았습니다. 질문을 짧게 바꿔 다시 시도해 주세요.")
                }
                if (!Thread.currentThread().isInterrupted) {
                    runOnUiThread { if (!isDestroyed) onResult(answer) }
                }
            } catch (_: SocketTimeoutException) {
                if (!Thread.currentThread().isInterrupted) {
                    runOnUiThread {
                        if (!isDestroyed) onError("응답 대기 시간이 초과되었습니다. 인터넷 연결을 확인하고 잠시 후 다시 시도해 주세요.")
                    }
                }
            } catch (error: Exception) {
                val message = "AI 답변을 가져오지 못했습니다: ${error.message ?: "연결을 확인해 주세요."}"
                if (!Thread.currentThread().isInterrupted) {
                    runOnUiThread { if (!isDestroyed) onError(message) }
                }
            } finally {
                connection?.disconnect()
                aiConnection = null
            }
        }
    }

    private fun cancelAiRequest() {
        aiRequest?.cancel(true)
        aiConnection?.disconnect()
    }

    private fun openInput(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty()) return
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(addressField.windowToken, 0)
        addressField.clearFocus()
        val target = when {
            input.startsWith("https://", ignoreCase = true) ||
                input.startsWith("http://", ignoreCase = true) -> input
            input.contains(" ") || !input.contains(".") ->
                browserSettings.searchEngine.searchUrl + URLEncoder.encode(input, "UTF-8")
            else -> "https://$input"
        }
        progressBar.visibility = View.VISIBLE
        webView.loadUrl(target)
    }

    private fun showSettings() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), dp(8))
        }
        panel.addView(TextView(this).apply {
            text = "검색엔진"
            textSize = 18f
            setTextColor(getColor(R.color.on_surface))
        })
        val engines = RadioGroup(this)
        SearchEngine.entries.forEach { engine ->
            engines.addView(RadioButton(this).apply {
                id = View.generateViewId()
                tag = engine
                text = engine.label
                textSize = 16f
                isChecked = browserSettings.searchEngine == engine
            })
        }
        panel.addView(engines)
        val keyField = EditText(this).apply {
            hint = "Gemini API 키 (비우고 저장하면 삭제)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            textSize = 14f
            ViewCompat.setImportantForAutofill(this, View.IMPORTANT_FOR_AUTOFILL_NO)
        }
        try {
            keyField.setText(browserSettings.apiKey())
        } catch (error: Exception) {
            keyField.error = error.message ?: "저장된 키를 읽지 못했습니다. 다시 입력해 주세요."
        }
        panel.addView(keyField)
        panel.addView(TextView(this).apply {
            text = "키는 이 기기에 암호화해 저장됩니다. 질문할 때 페이지 내용과 함께 Google Gemini에 전송됩니다."
            textSize = 13f
            setTextColor(getColor(R.color.on_surface_variant))
            setPadding(0, dp(8), 0, dp(8))
        })
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("설정")
            .setView(ScrollView(this).apply { addView(panel) })
            .setPositiveButton("저장", null)
            .setNegativeButton("취소", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val selected = engines.findViewById<RadioButton>(engines.checkedRadioButtonId).tag as SearchEngine
                val key = keyField.text.toString().trim()
                if (key.any { it.isWhitespace() || it.code < 32 }) {
                    keyField.error = "API 키에 공백이나 줄바꿈이 포함되어 있습니다."
                    return@setOnClickListener
                }
                try {
                    browserSettings.save(key, selected)
                    addressField.hint = "${selected.label} 검색 또는 주소 입력"
                    homeSearchButton.text = "${selected.label}에서 검색"
                    dialog.dismiss()
                } catch (error: Exception) {
                    keyField.error = error.message ?: "설정을 저장하지 못했습니다."
                }
            }
        }
        dialog.show()
    }

    private fun expressiveProgress(description: String): LinearProgressIndicator =
        LinearProgressIndicator(this).apply {
            isIndeterminate = true
            trackThickness = dp(4)
            trackCornerRadius = dp(2)
            setIndicatorColor(getColor(R.color.primary))
            trackColor = getColor(R.color.primary_container)
            waveAmplitude = dp(3)
            setWavelength(dp(28))
            waveSpeed = dp(36)
            visibility = View.GONE
            contentDescription = description
        }

    private fun updateNavigationButtons() {
        backButton.isEnabled = webView.canGoBack()
        forwardButton.isEnabled = webView.canGoForward()
        backButton.alpha = if (backButton.isEnabled) 1f else 0.4f
        forwardButton.alpha = if (forwardButton.isEnabled) 1f else 0.4f
    }

    private fun toolbarButton(label: String): TextView =
        MaterialButton(this).apply {
            text = label
            textSize = 24f
            insetTop = 0
            insetBottom = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(0, 0, 0, 0)
            cornerRadius = dp(24)
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.surface_container))
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.on_surface))
        }

    private fun expressiveButton(): MaterialButton =
        MaterialButton(this).apply {
            textSize = 18f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            cornerRadius = dp(32)
            insetTop = 0
            insetBottom = 0
            backgroundTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(getColor(R.color.primary), getColor(R.color.surface_container))
            )
            setTextColor(ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(Color.WHITE, getColor(R.color.on_surface_variant))
            ))
        }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        cancelAiRequest()
        aiExecutor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private val BLUE = Color.rgb(23, 105, 224)
        private val NAVY = Color.rgb(22, 38, 63)
    }
}
