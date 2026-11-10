package org.example.storage;

// Режим записи файла: CREATE — перезаписать файл новым массивом,
// APPEND — прочитать существующий массив, добавить новые записи и перезаписать.
// Используется: JsonStorage.save, CommandLineApp (парсит из --mode).
public enum WriteMode {
    CREATE, APPEND
}
