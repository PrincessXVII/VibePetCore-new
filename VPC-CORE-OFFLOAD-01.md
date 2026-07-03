# VPC-CORE-OFFLOAD-01 — Разгрузка ядра VibePetCore

Автор задачи: PrincessXVII  
Репозиторий: https://github.com/VibeCraft-one/VibePetCore-new  
Baseline: plugin `2.6.36`, MC `1.21.11`, pack_format `75`

Отдельная архитектурная задача. Не смешивать с release bug-hunt (`VPC-REL-*`) и stack gate.

## Resource Pack для сервера (готово к подключению)

| Параметр | Значение |
|----------|----------|
| Файл | `dist/VibePetCore-resource-pack.zip` |
| Прямая ссылка | https://github.com/VibeCraft-one/VibePetCore-new/raw/main/dist/VibePetCore-resource-pack.zip |
| SHA1 | `2123c95717a1bab66ed75f1d39e77c1d54374649` |
| Размер | ~31 KiB |
| pack_format | 75 |
| Описание | Миниатюры яиц питомцев (17 типов: egg + empty) |

### config.yml на сервере

```yaml
resource-pack:
  enabled: true
  url: "https://github.com/VibeCraft-one/VibePetCore-new/raw/main/dist/VibePetCore-resource-pack.zip"
  sha1: "2123c95717a1bab66ed75f1d39e77c1d54374649"
  required: false
  prompt: "Для миниатюр питомцев нужен ресурс-пак VibePetCore."
  auto-host:
    enabled: false
```

> **Примечание:** runtime-модуль `setResourcePack` в плагине пока не реализован — URL/SHA1 нужно прописать вручную или включить auto-host после реализации задачи (фаза 2).

---

## Контекст

| Компонент | Размер | Где | Статус |
|-----------|--------|-----|--------|
| JAR плагина | ~18.1 MiB | release | Лимит checkJarSize: 40 MiB |
| SQLite JDBC natives | ~23 MiB (внутри JAR) | fat-jar | Главный груз |
| MySQL + protobuf | ~11 MiB | fat-jar | Вшито всегда |
| Java-код | ~1.4 MiB | fat-jar | OK |
| YAML/локализация | ~0.21 MiB | resources | OK |
| Resource Pack | ~31 KiB | `dist/` | **Вынесен, залит в repo** |
| Кастомные `.ogg` | 0 | — | Звуки = vanilla `Sound.*` |
| Entity-модели | 0 | — | Vanilla entities + scale |

---

## Подзадачи

### 1. Инвентаризация тяжёлых ассетов

- [x] JAR breakdown: sqlite / mysql / protobuf / code / yml
- [x] Inventory `resource-pack/` (86 файлов)
- [x] Подтверждено: png/ogg/zip не в JAR
- [ ] Gap: active-button texture (CMD=0, material=WARPED_BUTTON)
- [ ] 52× MIGRATION-*.txt в resources — вынести из src
- [ ] God-classes: PetEngineManager (1886 LOC), PetGuiService (901 LOC)

### 2. Resource Pack pipeline

- [x] Zip собран: `dist/VibePetCore-resource-pack.zip`
- [x] SHA1 зафиксирован
- [x] Прямая ссылка для сервера
- [ ] Gradle task `:buildResourcePack`
- [ ] ResourcePackModule: auto-host + setResourcePack on join
- [ ] Active-button texture в pack
- [ ] Smoke: pack on / pack off

### 3. Снижение размера и связанности JAR

- [ ] SQLite: target-platform natives или optional
- [ ] MySQL driver: optional module
- [ ] JAR ≤ 12 MiB (interim) → ≤ 8 MiB (target)
- [ ] PetEngineManager decomposition
- [ ] Продолжить GUI *Page extraction

### 4. Совместимость клиента

| Параметр | Значение |
|----------|----------|
| Minecraft | 1.21.11 |
| pack_format | 75 |
| egg CMD | 200101–200117 |
| empty CMD | 200201–200217 |
| active-button CMD | 0 (TODO: 200301+) |

**17 типов:** axolotl, bat, bee, blaze, breeze, cat, fox, frog, ghast, panda, parrot, phantom, rabbit, allay, armadillo, vex, wolf.

**Без pack (`required: false`):** играбельно, vanilla иконки spawn egg.  
**С pack:** миниатюры яиц по custom_model_data.

---

## Stop-rules

- Один PR = одна ось (pack **или** JAR slimming **или** coupling).
- `./gradlew test jar checkJarSize` зелёный.
- Regression-тесты save/rollback не трогать без необходимости.

## Порядок работ

| # | PR | Effort | Impact |
|---|-----|--------|--------|
| 1 | Asset audit + pack в repo | 0.5d | Baseline + deploy |
| 2 | ResourcePack runtime module | 2d | Авто-раздача клиентам |
| 3 | JDBC slimming | 1–2d | −15 MiB JAR |
| 4 | PetEngineManager split | 3–5d | Maintainability |
