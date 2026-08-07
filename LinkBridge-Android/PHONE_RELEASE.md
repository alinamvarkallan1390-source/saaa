# انتشار در GitHub و دریافت APK

1. پوشه پروژه را در یک مخزن GitHub قرار دهید و branch اصلی را `main` بگذارید.
2. با هر Push، تب Actions دو APK دیباگ را در Artifact با نام `LinkBridge-debug-apks` می‌سازد.
3. برای Release امضاشده، یک keystore بسازید و این Secrets را اضافه کنید: `KEYSTORE_BASE64`، `KEYSTORE_PASSWORD`، `KEY_ALIAS` و `KEY_PASSWORD`.
4. سپس tag بسازید: `git tag v1.0.0 && git push origin v1.0.0`. APKهای Release به صفحه Releases متصل می‌شوند.
5. گوشی و ساعت باید با یک certificate pairing واقعی provision شوند. پیش از انتشار عمومی UUID و allow-list دستگاه را نهایی کنید.
