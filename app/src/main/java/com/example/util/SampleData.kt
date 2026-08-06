package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

object SampleData {

    const val SAMPLE_RESUME = """
# ALEX MORGAN
**Senior Software Engineer & Product Specialist**
*Email:* alex.morgan@example.com | *Phone:* +1 (555) 019-2834 | *Location:* San Francisco, CA

---

### PROFESSIONAL SUMMARY
Innovative Software Engineer with 7+ years of experience building high-performance mobile and cloud architectures. Proven track record in leading cross-functional engineering teams, optimizing application runtime efficiency, and delivering customer-centric digital products.

---

### CORE COMPETENCIES
- **Languages:** Kotlin, Java, Python, TypeScript, SQL
- **Frameworks:** Jetpack Compose, Android SDK, Room DB, Coroutines, Ktor
- **Architecture:** Clean Architecture, MVVM, Microservices, CI/CD Pipelines
- **Tools & Utilities:** Git, Gradle, Firebase, Docker, Figma, PDF Generation

---

### WORK EXPERIENCE

#### Lead Mobile Engineer | TechSphere Solutions
*Jan 2023 – Present | San Francisco, CA*
- Architectural redesign of enterprise Android app resulting in 42% faster render times.
- Led a team of 6 engineers to launch real-time document sync and offline PDF generation module.
- Integrated automated screenshot and UI regression test suite with Roborazzi and JUnit.

#### Senior Android Developer | Apex Systems
*Mar 2020 – Dec 2022 | Austin, TX*
- Developed custom Jetpack Compose UI component library used across 4 flagship products.
- Reduced cold app startup time by 310ms using Baseline Profiles and runtime optimizations.
- Co-authored high-throughput PDF export engine processing over 50,000 documents daily.

---

### EDUCATION & CERTIFICATIONS
- **B.S. in Computer Science** — University of Texas at Austin (2016 – 2020)
- **Google Certified Professional Android Developer** (2021)
"""

    const val SAMPLE_INVOICE = """
# INVOICE #INV-2026-084
**Issued By:** Nova Digital Services LLC
**Date:** August 5, 2026
**Due Date:** August 19, 2026

**Bill To:**
Acme Global Technologies Inc.
100 Innovation Way, Suite 400
New York, NY 10001

---

### DESCRIPTION & SERVICES
1. **Custom Mobile App UI/UX Design System**
   - Clean Material 3 design tokens, color palette, custom icons
   - Qty: 1 | Rate: $3,200.00 | Total: $3,200.00

2. **Android Document & PDF Conversion Engine**
   - High-resolution A4 page layout engine, page reordering, text editor
   - Qty: 1 | Rate: $4,500.00 | Total: $4,500.00

3. **Database & Room Local Persistence Integration**
   - Offline-first cache, schema migration strategy, reactive state flows
   - Qty: 1 | Rate: $1,800.00 | Total: $1,800.00

---

### SUMMARY
- **Subtotal:** $9,500.00
- **Tax (8%):** $760.00
- **Total Amount Due:** $10,260.00

**Payment Terms:** Net 14 Days. Please remit payment via Direct Bank Transfer or Credit.
*Thank you for your business!*
"""

    const val SAMPLE_MEETING_NOTES = """
# TEAM PROJECT SYNC NOTES
**Project:** PDF Converter & Page Organizer
**Date:** August 5, 2026
**Attendees:** Product Lead, UI Specialist, Core Engineer

---

### KEY DECISIONS
1. **Drag-and-Drop Page Reordering:** Users can long-press and drag thumbnails to instantly rearrange PDF pages before export.
2. **Text Formatting Toolbar:** Include live font size adjustment, font family toggles (Sans, Serif, Mono), margins, and custom watermarks.
3. **High Quality Export:** Guarantee crisp 300 DPI page rendering and vector text output.

### ACTION ITEMS
- [x] Configure Room DB for history persistence
- [x] Build drag-and-drop page reordering canvas
- [x] Implement FileProvider for sharing exported files
- [ ] Add batch conversion support for photo galleries

### UPCOMING MILESTONES
- Release v1.0 Production APK
- Benchmark rendering performance on large 100+ page PDFs
"""

    fun createSampleImageFile(context: Context, name: String, text: String): File {
        val file = File(context.cacheDir, "$name.png")
        if (file.exists()) return file

        val bitmap = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#1E293B"))

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
            strokeWidth = 3f
        }

        val cardPaint = Paint().apply {
            color = Color.parseColor("#334155")
        }

        canvas.drawRoundRect(50f, 50f, 750f, 950f, 24f, 24f, cardPaint)
        canvas.drawText(text, 100f, 200f, paint)

        // Draw sample document lines
        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 6f
        for (y in 300..800 step 60) {
            canvas.drawLine(100f, y.toFloat(), 700f, y.toFloat(), paint)
        }

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bitmap.recycle()
        return file
    }
}
