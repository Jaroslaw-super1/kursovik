# Агрегатор данных из REST API

Консольное Java-приложение, которое опрашивает несколько открытых REST API,
приводит ответы к единой модели и сохраняет их в файл (JSON или CSV).
Умеет читать сохранённые файлы, фильтровать данные и выводить их в удобном формате.

## Содержание

1. [Источники данных](#источники-данных)
2. [Требования](#требования)
3. [Сборка](#сборка)
4. [Команды запуска](#команды-запуска)
5. [Правила написания команд](#правила-написания-команд)
6. [Примеры](#примеры)
7. [Структура проекта](#структура-проекта)
8. [Форматы данных](#форматы-данных)
9. [Логирование](#логирование)

---

## Источники данных

| Имя | API | Что получает |
|---|---|---|
| `moex` | MOEX ISS | Текущие торги по USD/RUB, CNY/RUB, золоту: цена последней сделки (`LAST`), открытие (`OPEN`), bid/offer, объёмы торгов (`VOLTODAY`, `VALTODAY`), количество сделок (`NUMTRADES`), изменение за день (`change`) |
| `weather` | 7Timer! | Погода в Санкт-Петербурге: температура, скорость и направление ветра, облачность, тип осадков, время прогноза |
| `exchangerate` | exchangerate.fun | Курсы к рублю: доллар (`usdRub`), юань (`cnyRub`), золото за грамм (`xauRubPerGram`) |

---

## Требования

- **JDK 17** или новее
- **Gradle** (в проекте есть wrapper — `./gradlew`)

Проверить версию Java:

```bash
java -version
```

---

## Сборка

Полная сборка с тестами:

```bash
./gradlew clean build
```

Только компиляция (быстрее):

```bash
./gradlew clean compileJava
```

Запускаемый скрипт без Gradle (опционально):

```bash
./gradlew installDist
# после этого можно запускать так:
./build/install/aggregator/bin/aggregator --sources=moex --output=data.json --format=json --mode=create
```

---

## Команды запуска

Приложение работает в двух режимах:

### Режим опроса (`--sources` + `--output` + `--format` + `--mode`)

Опросить источники и сохранить в файл.

```bash
./gradlew run --args="--sources=<источники> --output=<файл> --format=<формат> --mode=<режим>"
```

### Режим чтения (`--read`)

Прочитать сохранённый файл и вывести на экран.

```bash
./gradlew run --args="--read=<файл> [--source=<источник>] [--print=<формат_вывода>]"
```

---

## Правила написания команд

### Общие правила

1. **Все аргументы передаются через `--args="..."`** — это особенность Gradle-задачи `run`.
2. **Каждый аргумент начинается с `--`** и имеет вид `--имя=значение`.
3. **Порядок аргументов не важен** — парсер разбирает их в любом порядке.
4. **Значения без пробелов** — если в значении нужен пробел, экранируйте его или используйте `%20`.
5. **Списки источников — через запятую без пробелов**: `--sources=moex,weather`.

### Параметры режима опроса

| Параметр | Обязателен | Допустимые значения | По умолчанию |
|---|---|---|---|
| `--sources` | ✅ да | `moex`, `weather`, `exchangerate` (через запятую) | — |
| `--output` | ✅ да | путь к файлу, например `data.json` | — |
| `--format` | ✅ да | `json` или `csv` | — |
| `--mode` | ✅ да | `create` (перезаписать) или `append` (дописать) | — |

### Параметры режима чтения

| Параметр | Обязателен | Допустимые значения | По умолчанию |
|---|---|---|---|
| `--read` | ✅ да | путь к файлу (`.json` или `.csv`) | — |
| `--source` | ❌ нет | `moex`, `weather`, `exchangerate` | показать все |
| `--print` | ❌ нет | `text`, `json`, `csv`, `raw` | `text` (красивый вывод) |

### Особенности

- **`--read` определяет формат файла по расширению** — отдельный `--format` при чтении не нужен.
- **`--source` при чтении** фильтрует записи по источнику.
- **`--print=text`** (по умолчанию) — красивый человекочитаемый отчёт.
- **`--print=json`** — вывод данных в виде сырого JSON (можно прочитать CSV и вывести как JSON).
- **`--print=csv`** — вывод данных в формате CSV (можно прочитать JSON и вывести как CSV).
- **`--print=raw`** — просто выводит содержимое файла «как есть» на экран, без парсинга и форматирования.
- **`--mode=append`** дописывает записи в существующий файл (файл должен быть создан тем же форматом).

---

## Примеры

### JSON

**Опрос и сохранение:**

```bash
./gradlew run --args="--sources=moex,weather,exchangerate --output=data.json --format=json --mode=create"
```

**Чтение всего файла (красивый текст по умолчанию):**

```bash
./gradlew run --args="--read=data.json"
```

**Чтение только MOEX:**

```bash
./gradlew run --args="--read=data.json --source=moex"
```

**Сырой JSON (если нужно посмотреть структуру файла):**

```bash
./gradlew run --args="--read=data.json --print=json"
```

**Сырой текст (как есть):**

```bash
./gradlew run --args="--read=data.json --print=raw"
```

### CSV

**Опрос и сохранение:**

```bash
./gradlew run --args="--sources=moex,weather,exchangerate --output=data.csv --format=csv --mode=create"
```

**Чтение всего файла:**

```bash
./gradlew run --args="--read=data.csv"
```

**Чтение только MOEX:**

```bash
./gradlew run --args="--read=data.csv --source=moex"
```

**Дозапись в существующий файл:**

```bash
./gradlew run --args="--sources=exchangerate --output=data.csv --format=csv --mode=append"
```

**Смена формата на лету (читаем CSV, выводим JSON):**

```bash
./gradlew run --args="--read=data.csv --print=json"
```

---

## Структура проекта

Проект следует принципам SOLID и разделению ответственности (SoC):

```text
src/main/java/org/example/
├── cli/                    ← консольный интерфейс
│   ├── Main.java               — точка входа (сборка зависимостей в Map)
│   └── CommandParser.java      — разбор аргументов командной строки
├── commands/               ← сценарии выполнения
│   ├── Command.java            — общий интерфейс команды
│   ├── AggregateCommand.java   — сценарий опроса и сохранения
│   └── ReadCommand.java        — сценарий чтения и вывода
├── core/                   ← ядро (не зависит от консоли)
│   ├── Aggregator.java         — опрос источников
│   └── AggregatedRecord.java  — единая модель (record)
├── printers/               ← вывод на экран
│   ├── RecordPrinter.java      — интерфейс вывода
│   ├── TextRecordPrinter.java  — красивый текстовый вывод (вся логика форматирования)
│   ├── JsonRecordPrinter.java  — вывод в JSON
│   └── CsvRecordPrinter.java   — вывод в CSV
├── sources/                ← реализации источников
│   ├── DataSource.java         — интерфейс источника
│   ├── MoexCurrencySource.java
│   ├── SevenTimerSource.java
│   └── ExchangeRateFunSource.java
├── storage/                ← хранилище
│   ├── Storage.java            — интерфейс (save, load, readRaw)
│   ├── JsonStorage.java        — JSON
│   ├── CsvStorage.java         — CSV (long-format)
│   └── WriteMode.java          — CREATE / APPEND
└── http/
    └── HttpClientWrapper.java  — единственное место с HTTP-вызовами
```

### Ключевые архитектурные решения

- **Composition Root (`Main`)** — единственное место, где создаются объекты. Все зависимости собираются в три `HashMap`: `sources`, `storages`, `printers`.
- **Паттерн «Стратегия»** — выбор способа вывода (текст, JSON, CSV, сырой файл) осуществляется через хэш-таблицу принтеров. Никаких `if/else` и тернарных операторов для проверки форматов.
- **Расширяемость** — чтобы добавить новый источник, формат файла или способ вывода, достаточно создать один новый класс и добавить одну строку `put(...)` в `Main`.
- **Ядро не зависит от консоли** — в пакетах `core`, `sources`, `storage`, `http`, `commands`, `printers` нет `System.out` или `Scanner`.

---

## Форматы данных

### JSON

Массив объектов `AggregatedRecord`:

```json
[
  {
    "source": "moex",
    "fetchedAt": "2026-11-08T10:00:00Z",
    "fields": {
      "ticker": "USD000UTSTOM",
      "LAST": 82.5575,
      "OPEN": 81.9600,
      "change": 0.5975
    }
  }
]
```

### CSV (long-format)

Каждая строка — одно поле одной записи. Колонки: `source, fetchedAt, field, value`.

```csv
source,fetchedAt,field,value
moex,2026-11-08T10:00:00Z,ticker,USD000UTSTOM
moex,2026-11-08T10:00:00Z,LAST,82.5575
moex,2026-11-08T10:00:00Z,OPEN,81.96
moex,2026-11-08T10:00:00Z,change,0.5975
```

**Почему long-format?** У разных источников разные поля (у MOEX — `LAST/OPEN`, у погоды — `temperature/windSpeed`, у exchangerate — `usdRub/cnyRub`). Long-format позволяет хранить записи с произвольным набором полей без жёсткой схемы.

---

## Логирование

Пишется в консоль и в файл `app.log` (в рабочей директории). Формат:

```text
2026-11-08 10:00:00.123 INFO  org.example.core.Aggregator - Fetching source: moex
2026-11-08 10:00:01.456 INFO  org.example.core.Aggregator - Source moex returned 3 records
```

Уровни:
- `INFO` — старт, опрос источников, сохранение, чтение;
- `WARN` — неизвестный источник, отсутствующий файл;
- `ERROR` — ошибка HTTP или парсинга (приложение продолжает работу).

---