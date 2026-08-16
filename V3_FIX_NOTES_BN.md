# Floating Tasbih V3 — Crash Fix

মূল crash fix:
- `android.permission.VIBRATE` যোগ করা হয়েছে। V2-তে vibration চালু থাকা অবস্থায় counter tap করলে permission না থাকায় app crash করতে পারত।
- vibration call-এ defensive try/catch যোগ করা হয়েছে, যাতে device-specific vibration error হলেও app বন্ধ না হয়।
- নতুন Islamic green/gold Tasbih app icon যোগ করা হয়েছে।
- Version: 3.0 (versionCode 3)

GitHub-এ push করার পর Actions থেকে নতুন `FloatingTasbih-APK` artifact download করুন।
