package com.retailshelflabel.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Convenience wrapper around [SharedPreferences] for app settings.
 *
 * All keys are defined as constants here — both [SettingsFragment] and the
 * rest of the app read from this single source of truth.
 */
object PreferencesHelper {

    // ── Label / printer keys (must match preference XML keys) ─────────────
    const val KEY_STORE_NAME        = "pref_store_name"
    const val KEY_CURRENCY_SYMBOL   = "pref_currency_symbol"
    const val KEY_LABEL_WIDTH_MM    = "pref_label_width_mm"
    const val KEY_LABEL_HEIGHT_MM   = "pref_label_height_mm"
    const val KEY_BARCODE_TYPE      = "pref_barcode_type"
    const val KEY_PRINTER_MODE      = "pref_printer_mode"
    const val KEY_DEFAULT_COPIES    = "pref_default_copies"

    // ── Bulk history retention (must match preference XML keys) ──────────
    const val KEY_BULK_HISTORY_RETENTION_DAYS = "pref_bulk_history_retention_days"
    const val DEFAULT_BULK_HISTORY_RETENTION_DAYS = "90"

    // ── Modisoft sync keys (must match preference XML keys) ───────────────
    const val KEY_MODISOFT_SYNC_MODE      = "pref_modisoft_sync_mode"
    const val KEY_MODISOFT_API_BASE_URL   = "pref_modisoft_api_base_url"
    const val KEY_MODISOFT_STORE_ID       = "pref_modisoft_store_id"
    const val KEY_MODISOFT_API_KEY        = "pref_modisoft_api_key"
    const val KEY_MODISOFT_AUTO_SYNC      = "pref_modisoft_auto_sync"
    const val KEY_MODISOFT_SYNC_INTERVAL  = "pref_modisoft_sync_interval_hours"
    /** Not a preference UI key — written programmatically after each sync */
    private const val KEY_LAST_SYNC_TS    = "last_modisoft_sync_ts"

    // ── Defaults ──────────────────────────────────────────────────────────
    const val DEFAULT_STORE_NAME    = "MY STORE"
    const val DEFAULT_CURRENCY      = "$"
    const val DEFAULT_LABEL_WIDTH   = "80"
    const val DEFAULT_LABEL_HEIGHT  = "40"
    const val DEFAULT_BARCODE_TYPE  = "CODE128"
    const val DEFAULT_PRINTER_MODE  = "receipt"
    const val DEFAULT_COPIES        = "1"
    const val DEFAULT_SYNC_MODE     = "api"
    const val DEFAULT_SYNC_INTERVAL = "24"   // hours

    private fun prefs(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    // ── Label / printer getters ───────────────────────────────────────────

    fun getStoreName(context: Context): String =
        prefs(context).getString(KEY_STORE_NAME, DEFAULT_STORE_NAME) ?: DEFAULT_STORE_NAME

    fun getCurrencySymbol(context: Context): String =
        prefs(context).getString(KEY_CURRENCY_SYMBOL, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY

    fun getLabelWidthMm(context: Context): Int =
        prefs(context).getString(KEY_LABEL_WIDTH_MM, DEFAULT_LABEL_WIDTH)?.toIntOrNull() ?: 80

    fun getLabelHeightMm(context: Context): Int =
        prefs(context).getString(KEY_LABEL_HEIGHT_MM, DEFAULT_LABEL_HEIGHT)?.toIntOrNull() ?: 40

    fun getBarcodeType(context: Context): String =
        prefs(context).getString(KEY_BARCODE_TYPE, DEFAULT_BARCODE_TYPE) ?: DEFAULT_BARCODE_TYPE

    fun getPrinterMode(context: Context): String =
        prefs(context).getString(KEY_PRINTER_MODE, DEFAULT_PRINTER_MODE) ?: DEFAULT_PRINTER_MODE

    fun getDefaultCopies(context: Context): Int =
        prefs(context).getString(KEY_DEFAULT_COPIES, DEFAULT_COPIES)?.toIntOrNull() ?: 1

    // ── Bulk history retention getter ─────────────────────────────────────

    fun getBulkHistoryRetentionDays(context: Context): Int =
        prefs(context).getString(
            KEY_BULK_HISTORY_RETENTION_DAYS, DEFAULT_BULK_HISTORY_RETENTION_DAYS
        )?.toIntOrNull() ?: 90

    // ── Modisoft sync getters/setters ─────────────────────────────────────

    fun getModisoftSyncMode(context: Context): String =
        prefs(context).getString(KEY_MODISOFT_SYNC_MODE, DEFAULT_SYNC_MODE) ?: DEFAULT_SYNC_MODE

    fun setModisoftSyncMode(context: Context, mode: String) =
        prefs(context).edit().putString(KEY_MODISOFT_SYNC_MODE, mode).apply()

    fun getModisoftApiBaseUrl(context: Context): String =
        prefs(context).getString(KEY_MODISOFT_API_BASE_URL, "") ?: ""

    fun getModisoftStoreId(context: Context): String =
        prefs(context).getString(KEY_MODISOFT_STORE_ID, "") ?: ""

    fun getModisoftApiKey(context: Context): String =
        prefs(context).getString(KEY_MODISOFT_API_KEY, "") ?: ""

    fun isModisoftAutoSyncEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MODISOFT_AUTO_SYNC, false)

    fun getModisoftSyncIntervalHours(context: Context): Int =
        prefs(context).getString(KEY_MODISOFT_SYNC_INTERVAL, DEFAULT_SYNC_INTERVAL)
            ?.toIntOrNull() ?: 24

    fun getLastSyncTimestamp(context: Context): Long =
        prefs(context).getLong(KEY_LAST_SYNC_TS, 0L)

    fun setLastSyncTimestamp(context: Context, ts: Long) =
        prefs(context).edit().putLong(KEY_LAST_SYNC_TS, ts).apply()
}
