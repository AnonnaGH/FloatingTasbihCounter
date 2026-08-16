# Floating Tasbih Counter V5 Beta

## আজকের correction ও feature
- Floating bubble screen boundary-এর বাইরে যাবে না; status bar বা screen edge-এ কেটে যাবে না।
- Bubble drag করলে Messenger-style নিচে Remove target দেখা যাবে; সেখানে ছেড়ে দিলে floating counter বন্ধ হবে।
- Bubble tap ও main app counter real-time synchronized। Main screen-এ আলাদা touch/refresh লাগবে না।
- Selected Zikr text light UI-তে পরিষ্কার dark text; dropdown-ও readable।
- নতুন cream/ivory + warm gold Islamic-inspired light UI।
- Target: 33 / 100 / 500 / 1000 / Custom।
- Target complete vibration option বাদ। Target complete sound রাখা হয়েছে।
- Vibration, count sound, target sound, bubble opacity control আছে।
- Durood Alert (On Unlock) যোগ করা হয়েছে।
- Durood selection শুধুমাত্র:
  1. صلى الله عليه وسلم
  2. اللهم صل وسلم على نبينا محمد
- Durood alert volume slider আছে।
- Durood audio device-এর Arabic Text-to-Speech engine ব্যবহার করে। Arabic TTS voice/device support না থাকলে unlock audio নাও বাজতে পারে।

## Android note
Durood unlock reminder নির্ভরযোগ্য রাখতে foreground service চালু থাকে এবং notification দেখা যেতে পারে। কিছু Samsung/Xiaomi/অন্যান্য ফোনে Battery optimization থেকে app-কে Unrestricted করা লাগতে পারে।
