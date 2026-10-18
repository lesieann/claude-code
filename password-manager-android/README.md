# 密碼保險箱（PassVault）— Android

一個離線、本機加密的 Android 密碼管理 App，使用 Kotlin + Jetpack Compose（Material 3）開發。

## 功能

- **主密碼加密**：主密碼經 PBKDF2-HMAC-SHA256（210,000 次迭代、隨機 salt）衍生出 256-bit 金鑰，整個保險箱以 AES-256-GCM 加密存在 App 私有目錄。
- **生物辨識解鎖**（選用）：保險箱金鑰由 Android Keystore 中「每次使用都需強生物辨識」的金鑰包裝；新增指紋/臉部資料後會自動失效。
- **項目管理**：名稱、帳號、密碼、網址、分類、備註；搜尋、新增、編輯、刪除。
- **分類**：可從預設分類（社群、金融、工作、購物、娛樂、遊戲）選擇或自訂；列表上方用分類標籤篩選，顯示全部時依分類分組；設定裡可以改名、合併或刪除分類（刪除分類不會刪除項目，只會移到「未分類」）。
- **密碼產生器**：可調長度（8–64）、字元類別、排除易混淆字元，並顯示強度。
- **安全剪貼簿**：複製的密碼會標記為敏感（Android 13+ 不顯示預覽），30 秒後自動清除。
- **自動上鎖**：App 進入背景超過 30 秒即上鎖；也可手動上鎖。
- **防窺**：`FLAG_SECURE` 禁止截圖/錄影，最近使用的 App 畫面也不會顯示內容。
- **從 Google 匯入**：讀取 Google 密碼管理工具／Chrome 匯出的 CSV 檔（`name,url,username,password,note`），依標題列對應欄位；已存在的密碼自動略過，可指定匯入到哪個分類。
- **加密備份**：可匯出/還原以主密碼加密的備份檔（透過系統檔案選擇器）。
- **無網路權限**，且停用系統雲端備份與裝置轉移，資料不會離開裝置（除非你自己匯出）。

> ⚠️ 主密碼無法找回。忘記主密碼就無法解開保險箱，請定期匯出備份並牢記主密碼。

## 建置

需求：JDK 17+、Android SDK（compileSdk 35）。

```bash
cd password-manager-android
./gradlew assembleDebug          # 產生 app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 單元測試（加密、密碼產生器、資料格式、分類、CSV 匯入）
```

也可以直接用 Android Studio 開啟 `password-manager-android/` 資料夾。

每次推送到這個資料夾，GitHub Actions（`.github/workflows/password-manager-android.yml`）都會建置 debug APK，並上傳成 `passvault-debug-apk` artifact，可以下載後直接安裝到手機。

最低支援 Android 8.0（API 26）。

## 專案結構

```
app/src/main/java/com/lesieann/passvault/
├── MainActivity.kt          # FLAG_SECURE、背景自動上鎖
├── VaultViewModel.kt        # 狀態、解鎖/上鎖、CRUD、備份
├── crypto/
│   ├── VaultCrypto.kt       # PBKDF2 + AES-GCM
│   └── BiometricKeyStore.kt # Keystore 生物辨識包裝金鑰
├── data/                    # 資料模型、檔案格式、原子寫入
├── ui/                      # Compose 畫面
└── util/                    # 密碼產生器、安全剪貼簿
```

## 保險箱檔案格式

`files/vault.json`：

```json
{
  "version": 1,
  "kdf": "PBKDF2WithHmacSHA256",
  "iterations": 210000,
  "salt": "<base64>",
  "iv": "<base64>",
  "data": "<base64：AES-GCM 加密的項目 JSON>",
  "biometric": { "iv": "<base64>", "key": "<base64：被 Keystore 包裝的金鑰>" }
}
```

除了 KDF 參數外沒有任何明文；`biometric` 只在本機有效，匯出備份時會被移除。
