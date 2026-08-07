package com.linkbridge.watch

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.*

// 100 beautiful backgrounds for watch home screen
data class WatchBackground(
    val id: Int,
    val name: String,
    val category: String,
    val brush: Brush,
    val accent: Color
)

object WatchBackgroundRepository {
    // Helper to create vibrant gradients
    private fun linear(c1: Color, c2: Color) = Brush.linearGradient(listOf(c1, c2))
    private fun radial(c1: Color, c2: Color, c3: Color? = null) =
        if (c3 != null) Brush.radialGradient(listOf(c1, c2, c3))
        else Brush.radialGradient(listOf(c1, c2))
    private fun sweep(c1: Color, c2: Color, c3: Color) = Brush.sweepGradient(listOf(c1, c2, c3))
    private fun vertical(c1: Color, c2: Color, c3: Color? = null) =
        if (c3 != null) Brush.verticalGradient(listOf(c1, c2, c3))
        else Brush.verticalGradient(listOf(c1, c2))

    val all: List<WatchBackground> = listOf(
        // --- Galaxy & Space (1-15) ---
        WatchBackground(1, "کهکشان بنفش", "Galaxy", radial(Color(0xFF2E1052), Color(0xFF05010A), Color(0xFF9D4EDD)), Color(0xFF9D4EDD)),
        WatchBackground(2, "سحابی آبی", "Galaxy", radial(Color(0xFF0B1E42), Color(0xFF020611), Color(0xFF00D4FF)), Color(0xFF00D4FF)),
        WatchBackground(3, "شفق قطبی", "Galaxy", vertical(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2BC0E4)), Color(0xFF2BC0E4)),
        WatchBackground(4, "سیاه‌چاله", "Galaxy", radial(Color(0xFF1A1A2E), Color(0xFF0F0F1A), Color(0xFFE94560)), Color(0xFFE94560)),
        WatchBackground(5, "ستاره دنباله‌دار", "Galaxy", linear(Color(0xFF141E30), Color(0xFF243B55)), Color(0xFF4A90E2)),
        WatchBackground(6, "مریخ", "Galaxy", radial(Color(0xFF4A0F0F), Color(0xFF1A0505), Color(0xFFFF6B35)), Color(0xFFFF6B35)),
        WatchBackground(7, "نپتون", "Galaxy", radial(Color(0xFF0A1931), Color(0xFF000000), Color(0xFF00A8CC)), Color(0xFF00A8CC)),
        WatchBackground(8, "کهکشان صورتی", "Galaxy", sweep(Color(0xFF20002C), Color(0xFFCBB4D4), Color(0xFF20002C)), Color(0xFFCBB4D4)),
        WatchBackground(9, "فضای عمیق", "Galaxy", vertical(Color(0xFF000000), Color(0xFF0F0C29), Color(0xFF302B63)), Color(0xFF7B68EE)),
        WatchBackground(10, "ابر کیهانی", "Galaxy", radial(Color(0xFF3A1C71), Color(0xFF11052C), Color(0xFFD76D77)), Color(0xFFD76D77)),
        WatchBackground(11, "کوانتوم", "Galaxy", linear(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)), Color(0xFF8E2DE2)),
        WatchBackground(12, "متریکس فضایی", "Galaxy", vertical(Color(0xFF000000), Color(0xFF001100), Color(0xFF00FF41)), Color(0xFF00FF41)),
        WatchBackground(13, "سفر زمان", "Galaxy", sweep(Color(0xFF141E30), Color(0xFF243B55), Color(0xFF00F260)), Color(0xFF00F260)),
        WatchBackground(14, "اگزوپلنت", "Galaxy", radial(Color(0xFF232526), Color(0xFF000000), Color(0xFF414345)), Color(0xFF8A8A8A)),
        WatchBackground(15, "ابرنواختر", "Galaxy", radial(Color(0xFFFF416C), Color(0xFF1A0005), Color(0xFFFF4B2B)), Color(0xFFFF416C)),

        // --- Sunset & Sunrise (16-30) ---
        WatchBackground(16, "غروب ساحل", "Sunset", vertical(Color(0xFFFF5E62), Color(0xFFFF9966), Color(0xFFFFD194)), Color(0xFFFF5E62)),
        WatchBackground(17, "طلوع صحرا", "Sunset", vertical(Color(0xFFFF512F), Color(0xFFDD2476), Color(0xFFFF9966)), Color(0xFFFF512F)),
        WatchBackground(18, "عصر طلایی", "Sunset", linear(Color(0xFFFF9A3B), Color(0xFFFFD700)), Color(0xFFFFD700)),
        WatchBackground(19, "غروب توکیو", "Sunset", vertical(Color(0xFF2C3E50), Color(0xFFFD746C), Color(0xFFFF9068)), Color(0xFFFD746C)),
        WatchBackground(20, "آتش ساحلی", "Sunset", radial(Color(0xFF8E0E00), Color(0xFF1F1C18), Color(0xFFFF6A00)), Color(0xFFFF6A00)),
        WatchBackground(21, "نارنجی نئون", "Sunset", linear(Color(0xFFFF9966), Color(0xFFFF5E62)), Color(0xFFFF5E62)),
        WatchBackground(22, "صورتی پاستلی", "Sunset", vertical(Color(0xFFFF9A9E), Color(0xFFFECFEF), Color(0xFFFECFEF)), Color(0xFFFF9A9E)),
        WatchBackground(23, "مانگای غروب", "Sunset", vertical(Color(0xFFFF6B6B), Color(0xFFFFE66D), Color(0xFF4ECDC4)), Color(0xFFFF6B6B)),
        WatchBackground(24, "غروب بنفش", "Sunset", linear(Color(0xFF6A3093), Color(0xFFA044FF)), Color(0xFFA044FF)),
        WatchBackground(25, "آسمان آتشین", "Sunset", radial(Color(0xFFFE8C00), Color(0xFF1A0A00), Color(0xFFF83600)), Color(0xFFFE8C00)),
        WatchBackground(26, "غروب اقیانوس", "Sunset", vertical(Color(0xFF0B486B), Color(0xFFF56217), Color(0xFFFCB045)), Color(0xFFF56217)),
        WatchBackground(27, "سرخ مخملی", "Sunset", vertical(Color(0xFF6D0B0B), Color(0xFF000000), Color(0xFFFF0000)), Color(0xFFFF0000)),
        WatchBackground(28, "غروب درخشان", "Sunset", sweep(Color(0xFFFF5E62), Color(0xFFFF9966), Color(0xFFFFD194)), Color(0xFFFF9966)),
        WatchBackground(29, "شعله", "Sunset", radial(Color(0xFFF12711), Color(0xFF1A0500), Color(0xFFF5AF19)), Color(0xFFF12711)),
        WatchBackground(30, "غروب استوایی", "Sunset", linear(Color(0xFFFC4A1A), Color(0xFFF7B733)), Color(0xFFFC4A1A)),

        // --- Ocean & Water (31-45) ---
        WatchBackground(31, "اقیانوس عمیق", "Ocean", vertical(Color(0xFF0A3D62), Color(0xFF079992), Color(0xFF38ADA9)), Color(0xFF38ADA9)),
        WatchBackground(32, "مرجان", "Ocean", radial(Color(0xFF00B4DB), Color(0xFF003B4F), Color(0xFF0083B0)), Color(0xFF00B4DB)),
        WatchBackground(33, "فیروزه", "Ocean", linear(Color(0xFF2193B0), Color(0xFF6DD5ED)), Color(0xFF6DD5ED)),
        WatchBackground(34, "آتلانتیس", "Ocean", vertical(Color(0xFF1A2980), Color(0xFF26D0CE), Color(0xFF1A2980)), Color(0xFF26D0CE)),
        WatchBackground(35, "موج آبی", "Ocean", sweep(Color(0xFF00C9FF), Color(0xFF09203F), Color(0xFF00C9FF)), Color(0xFF00C9FF)),
        WatchBackground(36, "آب یخی", "Ocean", vertical(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E)), Color(0xFF00C9FF)),
        WatchBackground(37, "دریای شب", "Ocean", radial(Color(0xFF141E30), Color(0xFF000000), Color(0xFF243B55)), Color(0xFF243B55)),
        WatchBackground(38, "لاجورد", "Ocean", linear(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2BC0E4)), Color(0xFF2BC0E4)),
        WatchBackground(39, "اقیانوس آرام", "Ocean", vertical(Color(0xFF2E3192), Color(0xFF1BFFFF), Color(0xFF2E3192)), Color(0xFF1BFFFF)),
        WatchBackground(40, "بلو دیپ", "Ocean", radial(Color(0xFF000428), Color(0xFF000000), Color(0xFF004E92)), Color(0xFF004E92)),
        WatchBackground(41, "مرجان آبی", "Ocean", linear(Color(0xFF4CA1AF), Color(0xFFC4E0E5)), Color(0xFF4CA1AF)),
        WatchBackground(42, "آبشار", "Ocean", vertical(Color(0xFF00B4DB), Color(0xFF0083B0), Color(0xFF00B4DB)), Color(0xFF0083B0)),
        WatchBackground(43, "نهنگ", "Ocean", radial(Color(0xFF2C5364), Color(0xFF0F2027), Color(0xFF203A43)), Color(0xFF203A43)),
        WatchBackground(44, "یخ دریا", "Ocean", linear(Color(0xFFA1C4FD), Color(0xFFC2E9FB)), Color(0xFFA1C4FD)),
        WatchBackground(45, "سونامی", "Ocean", sweep(Color(0xFF2193B0), Color(0xFF6DD5ED), Color(0xFF2193B0)), Color(0xFF6DD5ED)),

        // --- Forest & Nature (46-60) ---
        WatchBackground(46, "جنگل بارانی", "Nature", vertical(Color(0xFF134E5E), Color(0xFF71B280), Color(0xFF134E5E)), Color(0xFF71B280)),
        WatchBackground(47, "زمرد", "Nature", radial(Color(0xFF348F50), Color(0xFF0A1F0A), Color(0xFF56B4D3)), Color(0xFF56B4D3)),
        WatchBackground(48, "شمال", "Nature", linear(Color(0xFF0B486B), Color(0xFFF56217)), Color(0xFF0B486B)),
        WatchBackground(49, "جنگل سحرآمیز", "Nature", vertical(Color(0xFF000000), Color(0xFF0F3D0F), Color(0xFF00FF00)), Color(0xFF00FF00)),
        WatchBackground(50, "برگ پاییز", "Nature", vertical(Color(0xFFD1913C), Color(0xFF503500), Color(0xFFFFD700)), Color(0xFFD1913C)),
        WatchBackground(51, "کوه مه", "Nature", linear(Color(0xFF757F9A), Color(0xFFD7DDE8)), Color(0xFF757F9A)),
        WatchBackground(52, "جنگل شب", "Nature", radial(Color(0xFF0F2027), Color(0xFF000000), Color(0xFF2BC0E4)), Color(0xFF2BC0E4)),
        WatchBackground(53, "بامبو", "Nature", vertical(Color(0xFF2E7D32), Color(0xFFAED581), Color(0xFF1B5E20)), Color(0xFF1B5E20)),
        WatchBackground(54, "صحرای سبز", "Nature", linear(Color(0xFFAAFFA9), Color(0xFF11FFBD)), Color(0xFF11FFBD)),
        WatchBackground(55, "جنگل جادویی", "Nature", sweep(Color(0xFF134E5E), Color(0xFF71B280), Color(0xFF134E5E)), Color(0xFF71B280)),
        WatchBackground(56, "مه صبحگاهی", "Nature", vertical(Color(0xFF3E5151), Color(0xFFDECBA4), Color(0xFF3E5151)), Color(0xFFDECBA4)),
        WatchBackground(57, "سبز نئون", "Nature", radial(Color(0xFF00F260), Color(0xFF001A0A), Color(0xFF0575E6)), Color(0xFF00F260)),
        WatchBackground(58, "درخت زندگی", "Nature", vertical(Color(0xFF0D2818), Color(0xFF1B5E20), Color(0xFF4CAF50)), Color(0xFF4CAF50)),
        WatchBackground(59, "جنگل آبی", "Nature", linear(Color(0xFF103D4D), Color(0xFF174D25)), Color(0xFF106D30)),
        WatchBackground(60, "چمن", "Nature", vertical(Color(0xFF134E5E), Color(0xFF71B280), Color(0xFFA8E063)), Color(0xFFA8E063)),

        // --- Neon & Cyberpunk (61-75) ---
        WatchBackground(61, "سایبرپانک", "Neon", vertical(Color(0xFFFF00CC), Color(0xFF333399), Color(0xFF00FFFF)), Color(0xFFFF00CC)),
        WatchBackground(62, "نئون بنفش", "Neon", radial(Color(0xFF8E2DE2), Color(0xFF000000), Color(0xFF4A00E0)), Color(0xFF8E2DE2)),
        WatchBackground(63, "لیزر", "Neon", sweep(Color(0xFFFF0080), Color(0xFF00FFFF), Color(0xFFFF0080)), Color(0xFFFF0080)),
        WatchBackground(64, "سینث‌ویو", "Neon", vertical(Color(0xFF2A0A4A), Color(0xFFFF00CC), Color(0xFF00FFFF)), Color(0xFFFF00CC)),
        WatchBackground(65, "ماتریکس", "Neon", vertical(Color(0xFF000000), Color(0xFF001A00), Color(0xFF00FF00)), Color(0xFF00FF00)),
        WatchBackground(66, "نئون آبی", "Neon", radial(Color(0xFF00F260), Color(0xFF000000), Color(0xFF0575E6)), Color(0xFF0575E6)),
        WatchBackground(67, "رترو", "Neon", linear(Color(0xFFFC00FF), Color(0xFF00DBDE)), Color(0xFFFC00FF)),
        WatchBackground(68, "نئون صورتی", "Neon", radial(Color(0xFFF857A6), Color(0xFF1A0010), Color(0xFFFF5858)), Color(0xFFFF5858)),
        WatchBackground(69, "سایبر سیتی", "Neon", vertical(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFFFF00CC)), Color(0xFFFF00CC)),
        WatchBackground(70, "لیزر صورتی", "Neon", sweep(Color(0xFF8E2DE2), Color(0xFF4A00E0), Color(0xFF8E2DE2)), Color(0xFF4A00E0)),
        WatchBackground(71, "نئون سبز", "Neon", vertical(Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFF11998E)), Color(0xFF38EF7D)),
        WatchBackground(72, "ویپورویو", "Neon", linear(Color(0xFF64C8EF), Color(0xFFEE4CA6)), Color(0xFFEE4CA6)),
        WatchBackground(73, "نئون نارنجی", "Neon", radial(Color(0xFFFF9966), Color(0xFF1A0A00), Color(0xFFFF5E62)), Color(0xFFFF5E62)),
        WatchBackground(74, "سایبر زرد", "Neon", vertical(Color(0xFFF7971E), Color(0xFFFFD200), Color(0xFFF7971E)), Color(0xFFFFD200)),
        WatchBackground(75, "نئون قرمز", "Neon", radial(Color(0xFFED213A), Color(0xFF000000), Color(0xFF93291E)), Color(0xFFED213A)),

        // --- Minimal & Dark Pro (76-100) ---
        WatchBackground(76, "مشکی مات", "Minimal", vertical(Color(0xFF0F0F0F), Color(0xFF1A1A1A), Color(0xFF000000)), Color(0xFF555555)),
        WatchBackground(77, "ذغال", "Minimal", radial(Color(0xFF232526), Color(0xFF000000), Color(0xFF414345)), Color(0xFF414345)),
        WatchBackground(78, "گرافیت", "Minimal", linear(Color(0xFF2C3E50), Color(0xFF4CA1AF)), Color(0xFF4CA1AF)),
        WatchBackground(79, "مینیمال آبی", "Minimal", vertical(Color(0xFF101A2C), Color(0xFF080A0F), Color(0xFF101A2C)), Color(0xFF7CB7FF)),
        WatchBackground(80, "شب", "Minimal", radial(Color(0xFF0B0B0B), Color(0xFF000000), Color(0xFF1A1A1A)), Color(0xFF7CB7FF)),
        WatchBackground(81, "دودی", "Minimal", vertical(Color(0xFF4B4B4B), Color(0xFF000000), Color(0xFF4B4B4B)), Color(0xFF888888)),
        WatchBackground(82, "کلاسیک", "Minimal", linear(Color(0xFF000000), Color(0xFF434343)), Color(0xFF434343)),
        WatchBackground(83, "کاربن", "Minimal", radial(Color(0xFF0F0F0F), Color(0xFF000000), Color(0xFF2C2C2C)), Color(0xFF2C2C2C)),
        WatchBackground(84, "مینیمال بنفش", "Minimal", vertical(Color(0xFF1A0B2E), Color(0xFF000000), Color(0xFF1A0B2E)), Color(0xFF9D4EDD)),
        WatchBackground(85, "مینیمال سبز", "Minimal", vertical(Color(0xFF0A1F0A), Color(0xFF000000), Color(0xFF00FF41)), Color(0xFF00FF41)),
        WatchBackground(86, "مینیمال قرمز", "Minimal", vertical(Color(0xFF1F0A0A), Color(0xFF000000), Color(0xFFFF0000)), Color(0xFFFF0000)),
        WatchBackground(87, "تیتانیوم", "Minimal", linear(Color(0xFF283C86), Color(0xFF45A247)), Color(0xFF45A247)),
        WatchBackground(88, "ماتریکس دارک", "Minimal", radial(Color(0xFF000000), Color(0xFF001100), Color(0xFF003300)), Color(0xFF00FF41)),
        WatchBackground(89, "مینیمال صورتی", "Minimal", vertical(Color(0xFF1A0A1A), Color(0xFF000000), Color(0xFFFF00CC)), Color(0xFFFF00CC)),
        WatchBackground(90, "شیشه مات", "Minimal", linear(Color(0xFF141E30), Color(0xFF243B55)), Color(0xFF243B55)),
        WatchBackground(91, "مینیمال طلایی", "Minimal", radial(Color(0xFF1A1600), Color(0xFF000000), Color(0xFFFFD700)), Color(0xFFFFD700)),
        WatchBackground(92, "نفت", "Minimal", vertical(Color(0xFF000000), Color(0xFF0F1A1A), Color(0xFF1F3A3A)), Color(0xFF00FFFF)),
        WatchBackground(93, "مینیمال نارنجی", "Minimal", vertical(Color(0xFF1F1100), Color(0xFF000000), Color(0xFFFF6A00)), Color(0xFFFF6A00)),
        WatchBackground(94, "دارک بلو", "Minimal", vertical(Color(0xFF0A1628), Color(0xFF000000), Color(0xFF0A1628)), Color(0xFF7CB7FF)),
        WatchBackground(95, "مینیمال فیروزه", "Minimal", linear(Color(0xFF004D4D), Color(0xFF000000)), Color(0xFF00FFFF)),
        WatchBackground(96, "اونیکس", "Minimal", radial(Color(0xFF0F0F0F), Color(0xFF000000), Color(0xFF1C1C1C)), Color(0xFF1C1C1C)),
        WatchBackground(97, "دارک نئون", "Minimal", sweep(Color(0xFF000000), Color(0xFF1A1A2E), Color(0xFF16213E)), Color(0xFFFF00CC)),
        WatchBackground(98, "مینیمال سفید", "Minimal", vertical(Color(0xFF1A1A1A), Color(0xFF000000), Color(0xFF2A2A2A)), Color(0xFFFFFFFF)),
        WatchBackground(99, "لینک‌بریج", "Brand", radial(Color(0xFF101A2C), Color(0xFF080A0F), Color(0xFF7CB7FF)), Color(0xFF7CB7FF)),
        WatchBackground(100, "اولترا دارک", "Minimal", vertical(Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFF000000)), Color(0xFF555555)),
    )
}
