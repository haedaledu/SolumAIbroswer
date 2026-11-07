# Solum 브라우저

Android 7.0 이상 휴대폰에서 사용하는 AI 웹 브라우저입니다. Material 3 Expressive 스타일의 파란색 화면과 물결 모양 로딩바를 사용합니다.

## APK 다운로드와 설치

이 저장소의 [app-debug.apk](app-debug.apk)를 열어 **Download raw file**로 다운로드한 뒤 Android 휴대폰에 복사하여 설치합니다. 테스트용으로 서명된 디버그 APK이며 Google Play 배포용이 아닙니다. iPhone과 Windows에서는 APK를 직접 실행할 수 없습니다.

Windows PC에서는 Android Studio의 **Tools > Device Manager**에서 가상 휴대폰을 실행하고 APK를 에뮬레이터 화면에 끌어다 놓습니다.

## 기본 사용법

1. 위쪽 주소창에 검색어나 웹 주소를 입력하고 키보드의 **이동**을 누릅니다.
2. 주소창 아래에서 **뒤로 / 앞으로 / 새로고침 / AI 서치 / 설정**을 사용할 수 있습니다.
3. 웹페이지가 열리는 동안 위쪽에 물결 모양 로딩바가 표시됩니다. 페이지가 열리거나 오류가 발생하면 사라집니다.
4. **설정(톱니바퀴)**에서 검색엔진을 **Google, Bing, Naver** 중 선택하고 **저장**합니다. 이후 검색어 검색에 적용되며 직접 입력한 웹 주소는 그대로 열립니다.
5. 설정에 **Google Gemini API 키**를 입력하고 저장합니다. 저장한 키는 기기의 Android Keystore를 이용해 암호화하며, APK에는 개인 API 키가 포함되지 않습니다. 키를 비우고 저장하면 삭제됩니다.
6. 웹페이지를 연 뒤 새로고침 옆의 **AI 서치**를 누르고 질문합니다. 답변 생성 중에는 물결 로딩바가 표시되고, 답변이나 오류가 나오면 사라집니다. **닫기**를 누르면 요청을 취소합니다.

아래쪽의 큰 AI 버튼을 제거하고 WebView의 좌우 여백도 없애 웹페이지 표시 공간을 넓혔습니다.

## AI와 개인정보

AI 기능은 Google Gemini의 `gemini-3.5-flash-lite` 모델을 사용합니다. 질문과 현재 페이지의 제목·주소·본문 일부가 Google Gemini로 전송됩니다. 민감한 내용이 포함된 페이지에서는 사용하지 마세요. AI 답변은 틀릴 수 있으므로 중요한 내용은 원본에서 확인하세요.

API 사용에는 요금이 발생할 수 있습니다. 답변 길이는 최대 1,024 토큰이며 응답 대기 시간은 최대 60초입니다. 취소해도 서버에 이미 전달된 요청은 사용량에 반영될 수 있습니다. 429 오류는 사용량 한도, 503 오류는 서버 혼잡을 뜻합니다.

**API 키를 소스 코드, README 또는 APK에 넣어 공유하지 마세요. 이전에 대화나 APK로 공개한 키는 폐기하고 새 키를 발급하세요.** 저장된 키는 앱 데이터 삭제 또는 앱 삭제 시 사라집니다.

## Android Studio에서 실행 / APK 만들기

Android Studio에서 이 폴더를 열고 Gradle 동기화를 마칩니다. JDK 17 또는 21, Android SDK Platform 35가 필요합니다. 연결한 휴대폰 또는 에뮬레이터를 선택하고 **Run**을 누릅니다.

Windows의 프로젝트 폴더에서 APK를 만들려면:

```powershell
.\gradlew.bat :app:assembleDebug
```

결과 파일은 `app\build\outputs\apk\debug\app-debug.apk`입니다. AI 키는 빌드할 때 넣지 않고 설치 후 앱의 설정에서 입력합니다. 개발자는 `local.properties`의 `AI_MODEL` 속성으로 사용 가능한 Gemini 모델을 지정할 수 있습니다.

자세한 안내는 [사용법.md](사용법.md)를 참고하세요.
