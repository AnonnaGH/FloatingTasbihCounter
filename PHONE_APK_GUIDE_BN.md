# ফোন দিয়েই APK বানানোর নিয়ম

এই project-এ GitHub Actions আগে থেকেই যোগ করা আছে। Android Studio লাগবে না।

## ১) GitHub-এ নতুন Repository বানান
ফোনের Chrome থেকে github.com খুলুন।
Sign in করুন।
"+" > "New repository" চাপুন।
Repository name দিন: `FloatingTasbihCounter`
Visibility: Private বা Public যেকোনোটি।
Create repository চাপুন।

## ২) Project upload করুন
এই ZIP file ফোনে Extract করুন।

GitHub repository-তে:
"Add file" > "Upload files" ব্যবহার করে project-এর সব file/folder upload করুন।

খেয়াল রাখবেন `.github` folder-টিও upload হতে হবে।
যদি mobile browser-এ folder upload সমস্যা হয়, Chrome menu থেকে "Desktop site" চালু করুন।

Upload শেষে "Commit changes" চাপুন।

## ৩) APK Build করুন
Repository-এর উপরে "Actions" tab খুলুন।
"Build Android APK" workflow select করুন।
"Run workflow" > আবার "Run workflow" চাপুন।

Push করার পর workflow নিজে থেকেও চালু হতে পারে।

## ৪) APK Download করুন
Build সফল হলে একই workflow run খুলুন।
নিচে "Artifacts" section পাবেন।
`FloatingTasbih-APK` চাপুন।

একটি ZIP download হবে।
ZIP Extract করলে পাবেন:
`app-debug.apk`

## ৫) ফোনে Install করুন
`app-debug.apk` চাপুন।
Android যদি permission চায়:
Settings > Allow from this source চালু করুন।
তারপর Install করুন।

App খুলে:
"ভাসমান কাউন্টার চালু করুন" চাপুন।
"Display over other apps / Appear on top" permission Allow করুন।

তারপর floating tasbih অন্য app-এর উপর দেখা যাবে।
