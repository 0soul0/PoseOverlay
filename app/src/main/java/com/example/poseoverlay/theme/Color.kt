package com.example.poseoverlay.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
//  Google Photos — Material You palette
//  Source: inspected via Android 14 system theme + Google Photos 6.x
// ═══════════════════════════════════════════════════════════════════════════

// ── Primary — Google Blue ──────────────────────────────────────────────────
val GBlue700    = Color(0xFF0B57D0)   // M3 primary (light mode)
val GBlue500    = Color(0xFF1A73E8)   // classic Google Blue
val GBlue200    = Color(0xFFA8C7FA)   // primary (dark mode)
val GBlueCont   = Color(0xFFD3E3FD)   // primaryContainer (light)
val GBlueContDk = Color(0xFF0842A0)   // primaryContainer (dark)

// ── Google Surface Stack — Light ──────────────────────────────────────────
val GSurface0   = Color(0xFFFFFFFF)   // cards, bottom nav
val GSurface1   = Color(0xFFF8F9FA)   // page background (Google gray-50)
val GSurface2   = Color(0xFFF1F3F4)   // SurfaceVariant / chip bg
val GOutline    = Color(0xFFDADCE0)   // dividers, borders

// ── Google Surface Stack — Dark ───────────────────────────────────────────
val GDarkBg     = Color(0xFF202124)   // Google dark background
val GDarkSurf   = Color(0xFF292A2D)   // cards
val GDarkSurfV  = Color(0xFF3C4043)   // SurfaceVariant, chip bg
val GDarkOutline= Color(0xFF5F6368)   // dividers

// ── Text — Light ──────────────────────────────────────────────────────────
val GText900    = Color(0xFF202124)   // primary text
val GText600    = Color(0xFF5F6368)   // secondary / captions
val GText400    = Color(0xFF9AA0A6)   // disabled / placeholder

// ── Text — Dark ───────────────────────────────────────────────────────────
val GTextDk900  = Color(0xFFE8EAED)
val GTextDk600  = Color(0xFF9AA0A6)

// ── Accent tones (used sparingly — like Google Photos share FAB) ───────────
val GRed        = Color(0xFFEA4335)
val GGreen      = Color(0xFF34A853)
val GYellow     = Color(0xFFFBBC04)