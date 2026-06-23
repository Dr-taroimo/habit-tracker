# 習慣トラッカー

Android 向け Kotlin の習慣トラッカーアプリです。自然な日本語UI、丸みのあるカード、明るい配色、直近7日間の達成表示、かわいい効果音、毎日の通知を入れています。

Web公開用の静的版も `docs/` に入っています。GitHub Pages では `main` ブランチの `/docs` を公開対象にしてください。

## できること

- 習慣を追加する
- 今日できた習慣をチェックする
- 連続日数と直近7日間の達成状況を見る
- 効果音のオン/オフを切り替える
- 毎日の通知時刻を設定する

## 動かし方

### Android版

1. Android Studio でこのフォルダを開く
2. Gradle Sync を実行する
3. 実機またはエミュレーターで起動する
4. Android 13 以降では `通知を許可` を押して通知権限を許可する
5. 習慣名を入力して `習慣を追加` を押す
6. 今日できたら `今日できた` を押す

### Web版

1. `docs/index.html` をブラウザで開く
2. 習慣名を入力して `習慣を追加` を押す
3. 今日できたら `今日できた` を押す

GitHub Pagesで公開する場合:

1. GitHub の `Settings` を開く
2. `Pages` を開く
3. `Deploy from a branch` を選ぶ
4. Branch を `main`、folder を `/docs` にする
5. 公開URL `https://dr-taroimo.github.io/habit-tracker/` を開く

## 構成

- `MainActivity.kt`: 1画面UI、習慣追加、達成切り替え、通知時刻、効果音設定
- `HabitStore.kt`: SharedPreferences への習慣・設定保存
- `DailyReminderScheduler.kt`: 毎日の通知予約
- `DailyReminderReceiver.kt`: 通知表示
- `HabitSoundPlayer.kt`: 効果音再生
- `docs/`: GitHub Pages公開用のWeb版

## 注意

通知は Android の省電力設定やエミュレーターの状態により、実行タイミングが多少前後することがあります。習慣チェック、保存、効果音はエミュレーター上でもすぐ確認できます。
