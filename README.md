# Solum 브라우저

Android 휴대폰용 Google 검색 브라우저입니다. 검색어는 Google 검색으로 열고, 입력한 웹 주소는 앱 안의 WebView에서 보여 줍니다. 설치 및 기본 사용 방법은 아래 안내를 참고하세요.

## 휴대폰에서 사용하기

Android 7.0 이상 휴대폰에서 사용할 수 있습니다. iPhone에서는 APK를 실행할 수 없습니다.

1. `app\build\outputs\apk\debug\app-debug.apk`를 휴대폰에 복사합니다.
2. 휴대폰의 파일 앱에서 APK를 열고 설치합니다.
3. **Solum 브라우저**를 열고 위쪽 입력창에 검색어나 웹 주소를 입력합니다.
4. 웹페이지를 연 뒤 아래의 **AI 서치** 버튼으로 페이지 내용에 대해 질문합니다. AI 기능에는 유효한 Gemini API 키가 필요합니다.

현재 APK는 테스트용 디버그 APK이며 Google Play 배포용이 아닙니다.

## PC에서 사용하기

Windows에서 APK를 직접 실행할 수는 없습니다. Android Studio의 **Tools > Device Manager**에서 Android 7.0 이상 가상 기기를 실행한 뒤 APK를 가상 기기 화면으로 끌어다 놓아 설치합니다. 또는 아래 안내대로 프로젝트를 열고 **Run** 버튼으로 실행합니다.

## Android Studio에서 실행

1. Android Studio에서 이 폴더를 열고 Gradle 동기화가 끝날 때까지 기다립니다.
2. Android SDK 35와 JDK 17 또는 21을 설치합니다.
3. AI 질문을 사용하려면 프로젝트 루트의 `local.properties`에 다음 값을 추가합니다. API 키 입력 화면은 앱에 포함되어 있지 않습니다.

   ```properties
   AI_API_KEY=발급받은_API_키
   AI_MODEL=gemini-3.5-flash-lite
   ```

   `AI_MODEL`은 사용 가능한 Google Gemini 모델로 설정합니다. 같은 이름의 Gradle 프로젝트 속성 또는 환경 변수도 사용할 수 있습니다. 키를 설정하지 않아도 Google 검색과 웹 탐색은 이용할 수 있습니다.
4. Android 휴대폰 또는 에뮬레이터를 선택하고 `app` 구성을 실행합니다.

Windows에서 설치용 디버그 APK를 만들려면 Android SDK가 설정된 상태로 프로젝트 폴더에서 `.\gradlew.bat :app:assembleDebug`를 실행합니다. 결과는 `app\build\outputs\apk\debug\app-debug.apk`에 생성됩니다.

> 이 앱은 학습용 프로토타입입니다. API 키를 앱에 포함하면 APK에서 추출할 수 있으므로 실제 배포에서는 키를 앱에 넣지 말고 인증을 처리하는 서버를 통해 AI API를 호출해야 합니다.

## 포함된 기능

- Google 검색어 검색 및 웹 주소 직접 열기
- 앱 안에서 페이지를 표시하는 WebView
- 뒤로 가기, 앞으로 가기, 새로고침
- 현재 페이지 내용을 참고하는 AI 질문과 답변
- 페이지 텍스트를 외부 AI 서비스로 보내기 전 안내 표시
