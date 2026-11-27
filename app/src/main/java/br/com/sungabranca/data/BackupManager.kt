package br.com.sungabranca.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Callable

class BackupException(message: String) : Exception(message)

object BackupManager {
    private const val APP_TAG = "SungaBranca"
    private const val FORMAT = 1

    // Tabelas do app, com as "pais" antes das "filhas" (respeita as chaves estrangeiras).
    private fun orderedTables(db: AppDatabase): List<String> {
        val names = mutableListOf<String>()
        db.query(
            SimpleSQLiteQuery(
                "SELECT name FROM sqlite_master WHERE type='table' " +
                        "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' " +
                        "AND name <> 'room_master_table'"
            )
        ).use { c -> while (c.moveToNext()) names += c.getString(0) }

        val deps = names.associateWith { t ->
            val set = mutableSetOf<String>()
            db.query(SimpleSQLiteQuery("PRAGMA foreign_key_list(`$t`)")).use { c ->
                val idx = c.getColumnIndexOrThrow("table")
                while (c.moveToNext()) set += c.getString(idx)
            }
            set - t
        }
        val ordered = mutableListOf<String>()
        val pending = names.toMutableList()
        while (pending.isNotEmpty()) {
            val next = pending.firstOrNull { t -> deps.getValue(t).all { it in ordered } }
                ?: pending.first()
            ordered += next
            pending -= next
        }
        return ordered
    }

    private fun columnsOf(db: AppDatabase, table: String): Set<String> {
        val cols = mutableSetOf<String>()
        db.query(SimpleSQLiteQuery("PRAGMA table_info(`$table`)")).use { c ->
            val idx = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) cols += c.getString(idx)
        }
        return cols
    }

    /** Gera o texto JSON do backup. Chamar fora da thread principal. */
    fun export(db: AppDatabase): String {
        return db.runInTransaction(Callable {
            val tables = JSONObject()
            for (t in orderedTables(db)) {
                val rows = JSONArray()
                db.query(SimpleSQLiteQuery("SELECT * FROM `$t`")).use { c ->
                    while (c.moveToNext()) {
                        val o = JSONObject()
                        for (i in 0 until c.columnCount) {
                            val col = c.getColumnName(i)
                            when (c.getType(i)) {
                                Cursor.FIELD_TYPE_NULL -> o.put(col, JSONObject.NULL)
                                Cursor.FIELD_TYPE_INTEGER -> o.put(col, c.getLong(i))
                                Cursor.FIELD_TYPE_FLOAT -> o.put(col, c.getDouble(i))
                                Cursor.FIELD_TYPE_STRING -> o.put(col, c.getString(i))
                                else -> Unit
                            }
                        }
                        rows.put(o)
                    }
                }
                tables.put(t, rows)
            }
            JSONObject()
                .put("app", APP_TAG)
                .put("format", FORMAT)
                .put("schemaVersion", db.openHelper.readableDatabase.version)
                .put("exportedAt", System.currentTimeMillis())
                .put("tables", tables)
                .toString(2)
        })
    }

    private fun toValues(obj: JSONObject, allowed: Set<String>, table: String): ContentValues {
        val cv = ContentValues()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key !in allowed) throw BackupException("Coluna desconhecida em $table: $key")
            when (val v = obj.get(key)) {
                JSONObject.NULL -> cv.putNull(key)
                is Int -> cv.put(key, v.toLong())
                is Long -> cv.put(key, v)
                is Double -> cv.put(key, v)
                is String -> cv.put(key, v)
                else -> throw BackupException("Valor inesperado em $table.$key")
            }
        }
        return cv
    }

    /** Substitui TODOS os dados atuais pelos do backup. Chamar fora da thread principal. */
    fun restore(db: AppDatabase, json: String): Int {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw BackupException("O arquivo não é um JSON válido.")
        }
        if (root.optString("app") != APP_TAG || root.optInt("format", -1) != FORMAT) {
            throw BackupException("Este arquivo não é um backup do Sunga Branca.")
        }
        val version = db.openHelper.readableDatabase.version
        if (root.optInt("schemaVersion", -1) != version) {
            throw BackupException("Backup de outra versão do banco. Atualize o app e tente de novo.")
        }
        val data = root.optJSONObject("tables")
            ?: throw BackupException("O backup não tem dados.")

        val tables = orderedTables(db)
        val parsed = LinkedHashMap<String, List<ContentValues>>()
        for (t in tables) {
            val arr = data.optJSONArray(t)
                ?: throw BackupException("Faltam dados da tabela $t no backup.")
            val cols = columnsOf(db, t)
            parsed[t] = List(arr.length()) { i -> toValues(arr.getJSONObject(i), cols, t) }
        }

        var total = 0
        db.runInTransaction(Runnable {
            val sdb = db.openHelper.writableDatabase
            tables.asReversed().forEach { sdb.delete(it, null, null) }
            for (t in tables) {
                for (cv in parsed.getValue(t)) {
                    if (sdb.insert(t, SQLiteDatabase.CONFLICT_ABORT, cv) == -1L) {
                        throw BackupException("Falha ao inserir dados em $t.")
                    }
                    total++
                }
            }
        })
        return total
    }
}