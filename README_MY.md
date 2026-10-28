# BeeChat Store Native Android Project

ဒီ ZIP သည် Android Studio ဖြင့် APK အဖြစ် build လုပ်နိုင်သော **source project** ဖြစ်သည်။ ဒီနေရာတွင် compiled APK ဖိုင် မပါဝင်သေးပါ။

## ပါဝင်သော Feature များ
- Native Android UI
- Firebase Firestore `apps` collection မှ App စာရင်းရယူခြင်း
- App name / developer အလိုက် Search
- Android DownloadManager ဖြင့် APK download
- DownloadManager မှ downloaded bytes / total bytes ကို စစ်ပြီး ရာခိုင်နှုန်းပြခြင်း
- Download ပြီးလျှင် Android package installer ကို ဖွင့်ရန် ကြိုးစားခြင်း
- Dark/Light background toggle

## Firebase Data Structure
သင့် Website က `beechatstore` Firebase project ၏ Firestore `apps` collection ကို သုံးနေသောကြောင့် ဒီ app ကလည်း အဲဒီ collection ကိုဖတ်ရန် configure လုပ်ထားသည်။

Document တစ်ခုတွင် အနည်းဆုံး:
```json
{
  "name": "Example App",
  "version": "1.0.0",
  "link": "https://example.com/example.apk",
  "dev": "Developer name",
  "iconUrl": "https://example.com/icon.png",
  "rating": 4.5
}
```

`link` သည် HTTPS ဖြင့် တိုက်ရိုက်ရယူနိုင်သော APK URL ဖြစ်ရပါမည်။ Google Drive preview page သို့မဟုတ် HTML download page မဟုတ်ဘဲ APK ဖိုင်ကို တိုက်ရိုက်ပေးသော URL ဖြစ်သင့်သည်။

## Build လုပ်နည်း
1. ကွန်ပျူတာတွင် Android Studio ကို install လုပ်ပါ။
2. Android Studio မှ `Open` ကိုနှိပ်ပြီး `BeeChatStoreNative` folder ကို ရွေးပါ။
3. Gradle Sync ပြီးသည်အထိ စောင့်ပါ။
4. `Build > Build Bundle(s) / APK(s) > Build APK(s)` ကို ရွေးပါ။
5. Build အောင်မြင်လျှင် `app/build/outputs/apk/debug/app-debug.apk` မှာ ရပါမည်။

## သတိပြုရန်
- ဒီ source project ကို ဒီ environment ထဲမှာ Android SDK မရှိသဖြင့် compile/test မလုပ်နိုင်ခဲ့ပါ။
- Firebase Firestore Security Rules က public read ကို ခွင့်ပြုထားရပါမည်၊ မဟုတ်ပါက Firebase rules / authentication ကို ပြင်ဆင်ရန် လိုပါမည်။
- Android 8.0+ တွင် Settings ထဲက `Allow from this source` ကို user ကိုယ်တိုင် ဖွင့်ပေးရနိုင်ပါသည်။ Install အတည်ပြုချက်ကို Android က ပြသပါမည်။ ဒီ app က silent install မလုပ်ပါ။
- APK တစ်ခုချင်းစီကို download မလုပ်ခင် ယုံကြည်ရသော source မှဖြစ်ကြောင်း စစ်ဆေးပါ။
- Firebase API key များသည် client configuration အတွက်ဖြစ်သော်လည်း Firestore Security Rules ကို သေချာကန့်သတ်ထားရန် အရေးကြီးသည်။
