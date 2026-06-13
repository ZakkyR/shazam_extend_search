# Shazam検索ショートカット

ShazamのAndroid通知を受け取り、認識した曲をGoogle・YouTube・Wikipediaで即座に検索できるAndroidアプリです。

## 機能

- **通知ショートカット** — Shazamが曲を認識すると、検索ボタン付きの通知を自動表示
- **共有レシーバー** — ShazamアプリやShazam履歴の共有ボタンから直接検索
- **自動消去** — 通知を指定秒数後に自動消去（デフォルト30秒、設定変更可）
- **スマートウォッチ非転送** — Wear OSへの通知転送をブロック
- **デバッグ通知** — 通知内容のダンプ表示（設定でON/OFF可能）

## 動作環境

- Android 8.0（API 26）以上
- Shazam（`com.shazam.android` / `com.shazam.encore.android`）がインストールされていること

## 必要な権限

| 権限 | 用途 |
|---|---|
| 通知へのアクセス | Shazamの通知を読み取る（手動で許可が必要） |
| 通知の投稿（Android 13+） | 検索ショートカット通知を表示する |

## セットアップ

1. アプリをインストール
2. アプリを起動 → 「通知アクセスを許可する」をタップ
3. システム設定で **Shazam検索ショートカット** を ON にする

### バックグラウンド動作について（Xiaomi / MIUI等）

省電力機能が強いデバイスでは追加の設定が必要な場合があります。

- 設定 → アプリ → **Shazam検索ショートカット** → 自動起動 → ON
- 設定 → アプリ → **Shazam検索ショートカット** → バッテリーセーバー → 制限なし

## ビルド方法

Android Studio（Meerkat以降推奨）でプロジェクトを開き、そのままビルドできます。

```bash
./gradlew assembleDebug
```

## 設定項目

| 項目 | 内容 |
|---|---|
| 自動消去時間（秒） | 0〜120秒、0で消去なし |
| Google検索ボタン | 表示/非表示 |
| YouTubeボタン | 表示/非表示 |
| Wikipediaボタン | 表示/非表示 |
| 通知の詳細設定 | Android通知チャンネル設定へのリンク |
| デバッグ通知を表示 | 通知の内容をそのまま通知バーに表示（開発用） |

## アーキテクチャ

```
ShazamListenerService       Shazamの通知を受信・フィルタ
NotificationHelper          通知チャンネル管理・通知発行・URL生成
MainActivity                権限チェック・デバッグログ表示
SettingsActivity/Fragment   設定画面
ShareReceiverActivity       共有インテント受信・検索ダイアログ
App                         アプリケーションクラス（チャンネル初期化）
```

## 検索URL形式

| 検索先 | URL |
|---|---|
| Google | `https://www.google.com/search?q={クエリ}` |
| YouTube | `https://www.youtube.com/results?search_query={クエリ}` |
| Wikipedia | `https://ja.wikipedia.org/wiki/Special:Search?search={クエリ}` |

## 注意事項

- Shazamの通知フォーマットは公式に公開されていないため、アプリのアップデートにより動作しなくなる可能性があります
- 設定の「デバッグ通知」をONにすると通知の内容を確認できます
