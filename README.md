# BetterRecipeMemory — Paper 1.21.8

把原本的「datapack + 配套外掛」合併成**單一 Paper 插件 JAR**。安裝後不需要再把 zip
放進世界的 `datapacks/`。

*crafted by 廢土貓大 LogoCat · 廢土 · mcfallout.net*

## 功能

- 在 Paper 啟動早期註冊 JAR 內嵌的資料包過濾器，移除
  `minecraft:recipes/*` 原版配方成就。
- 玩家首次使用本插件進服時，以 Paper API 一次解鎖所有已註冊配方，保留配方書的
  「點配方一鍵帶料」體驗。
- 使用玩家 PDC 記錄是否已完成配方書初始化，之後登入不會重複掃描或解鎖。
- 精準過濾舊玩家進度檔造成的
  `Ignored advancement ... doesn't exist anymore` log 洪水。
- 提供 `/brm about`、`/brm version`、`/brm help` 與管理用
  `/brm refill [player]`。

## 相容性

| 項目 | 版本 |
|---|---|
| Minecraft | **1.21.8** |
| Paper API | **1.21.8-R0.1-SNAPSHOT** |
| Java | **21** |
| 內嵌 datapack format | **81** |

這是 Paper plugin（使用 `paper-plugin.yml` 與 Datapack Discovery Lifecycle API），不支援
Spigot、Bukkit 或其他未實作對應 Paper API 的核心。

## 安裝

1. 刪除舊版 `BetterRecipeMemory.zip`，避免同一個 filter 被重複載入。
2. 刪除舊版 `BetterRecipeMemoryPlugin-26.2-1.jar`。
3. 把 `dist/BetterRecipeMemory-1.21.8-1.0.0.jar` 放進伺服器 `plugins/`。
4. **完整重啟** Paper；不要使用 `/reload`。

啟動成功時應看到：

```text
Found new data pack BetterRecipeMemory/core, loading it automatically
[BetterRecipeMemory] Orphan-advancement log filter installed.
[BetterRecipeMemory] The recipe-advancement filter is bundled in this jar; no separate datapack is needed.
```

可在後台執行以下指令確認：

```text
datapack list enabled
brm version
```

`datapack list enabled` 應包含 `BetterRecipeMemory/core (plugin)`。

## 指令與權限

| 指令 | 說明 | 權限 |
|---|---|---|
| `/brm about` | 顯示專案資訊 | 無 |
| `/brm version` | 顯示版本 | 無 |
| `/brm help` | 顯示說明 | 無 |
| `/brm refill [player]` | 立即重新解鎖全部配方 | `betterrecipememory.admin`（OP 預設擁有） |

## 實作方式

Paper 的一般 Bukkit API 無法在世界載入後安全移除原版 advancement registry 項目，因此本版
採用 Paper 1.21.8 官方提供的 Datapack Discovery Lifecycle API：

1. `BetterRecipeMemoryBootstrap` 在 bootstrap 階段註冊 JAR 內的
   `better_recipe_memory_pack/`。
2. 內嵌 pack 只負責在 registry 載入前過濾 `minecraft:recipes/*`；它不是需要管理員另外
   安裝的外部 datapack。
3. `BetterRecipeMemoryPlugin` 在玩家首次登入時讀取所有 `Keyed` recipe，呼叫
   `Player#discoverRecipes`，並寫入玩家 PDC 標記。
4. `OrphanAdvancementLogFilter` 只拒絕已被移除配方成就的孤兒警告，其餘 log 保持不變。

## 建置

```bash
cd paper-plugin
mvn clean package
```

輸出：

```text
paper-plugin/target/BetterRecipeMemory-1.21.8-1.0.0.jar
```

## 驗證結果

已使用 Paper `1.21.8-60`、Java 21 做乾淨伺服器啟動測試：

- 插件被辨識為 Paper plugin 並正常啟用。
- `BetterRecipeMemory/core (plugin)` 自動啟用，不需要世界 datapack zip。
- 載入 `1407 recipes`，配方 registry 未被刪除。
- 過濾後載入 `124 advancements`。
- `/brm version`、`/brm about` 可正常執行。
- log filter 自測誘餌成功被吞掉，沒有誤傷其他啟動 log。

## 舊實驗資料

`experiments/` 保留原專案在 Paper 26.2 測得的 heap histogram 與重現步驟，作為機制與歷史
效益證據；那些絕對數值不是本次 Paper 1.21.8 啟動測試的數字。

## 回滾

刪除插件 JAR 後完整重啟。原版 recipe advancements 會恢復載入；玩家已解鎖的 recipe book
資料不會因此被清除。
