# LinkBridge — Phone + Android Watch

پروژه چندماژوله Kotlin برای گوشی Android 10+ و ساعت Android (از جمله Telzeal TC4G در صورت پشتیبانی BLE/GATT استاندارد).

## ساخت
1. پروژه را با Android Studio Ladybug یا جدیدتر باز کنید.
2. JDK 17 و Android SDK 35 را نصب کنید.
3. Sync Project with Gradle Files و سپس `phoneApp` و `watchApp` را جداگانه اجرا کنید.
4. مجوزهای Bluetooth/Notification را تأیید و در MIUI/HyperOS، Autostart و No restrictions را دستی فعال کنید.

## امنیت و معماری
- AES-256-GCM با کلید سخت‌افزاری Android Keystore، nonce تصادفی و AAD شناسه انتقال.
- BLE برای discovery/control و RFCOMM Classic برای فایل حجیم.
- backoff نمایی، اسکن پنجره‌ای و RSSI دوره‌ای برای مصرف پایین.
- Room برای تاریخچه، Hilt، Flow و Foreground Service.

## محدودیت‌های واقعی Android بدون Root/Device Owner
بستن برنامه‌های دیگر، reboot/shutdown، نصب بی‌تعامل APK، تغییر بعضی تنظیمات امن، روشن‌کردن اجباری صفحه در نسخه‌های جدید و نادیده‌گرفتن DND توسط اپ عادی تضمین‌شده نیست. کنترل‌های حساس فقط با Intent/صفحه تأیید سیستم یا نقش Device Owner ممکن‌اند. داده‌های دما/CPU/Firmware نیز باید توسط Agent ساعت در پروتکل telemetry ارائه شوند. هدف مصرف روزانه ۲–۳٪ قابل تضمین عمومی نیست و به firmware، RSSI و محدودیت‌های OEM وابسته است.

## قبل از انتشار
UUIDها و نام دستگاه را برای firmware واقعی TC4G نهایی کنید، pairing certificate/device allow-list را provision کنید، کلید امضای release بسازید، تست روی سخت‌افزار واقعی و تست قطع/وصل ۲۴ ساعته انجام دهید.
