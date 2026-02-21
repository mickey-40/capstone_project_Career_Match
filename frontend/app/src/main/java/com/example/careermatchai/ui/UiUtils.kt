package com.example.careermatchai.ui

/** Convert model score to 0–100 if backend returns 0–1, clamp otherwise. */
fun normalizePct(raw: Double): Double =
    when {
        raw.isNaN() -> 0.0
        raw <= 1.0  -> raw * 100.0
        raw > 100.0 -> 100.0
        else        -> raw
    }

/** Format a score as a percentage string with one decimal place. */
fun fmtPct(raw: Double): String = "${"%.1f".format(normalizePct(raw))}%"

/** For Material progress indicators (needs 0f..1f). */
fun pctProgress(raw: Double): Float = (normalizePct(raw) / 100.0).toFloat()
