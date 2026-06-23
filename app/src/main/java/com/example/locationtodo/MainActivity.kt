package com.example.locationtodo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var store: HabitStore
    private lateinit var scheduler: DailyReminderScheduler
    private lateinit var sounds: HabitSoundPlayer
    private lateinit var habitsContainer: LinearLayout
    private lateinit var habitInput: EditText
    private lateinit var hourInput: EditText
    private lateinit var minuteInput: EditText
    private lateinit var soundToggle: CheckBox

    private val roundedTypeface: Typeface = Typeface.create("sans-serif-rounded", Typeface.NORMAL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = HabitStore(this)
        scheduler = DailyReminderScheduler(this)
        sounds = HabitSoundPlayer(this)

        window.statusBarColor = CREAM
        window.navigationBarColor = CREAM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setContentView(buildContentView())
        renderHabits()
        scheduler.schedule(store.notificationHour(), store.notificationMinute())
    }

    private fun buildContentView(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(28))
            setBackgroundColor(CREAM)
        }

        root.addView(headerCard())
        root.addView(addHabitCard())
        root.addView(notificationCard())

        habitsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(habitsContainer)

        root.addView(
            softButton("すべて削除", LEMON, INK) {
                store.clear()
                renderHabits()
                toast("習慣をすべて削除しました")
            }.apply {
                setMargins(top = dp(8))
            }
        )

        return ScrollView(this).apply {
            setBackgroundColor(CREAM)
            addView(root)
        }
    }

    private fun headerCard(): View {
        val card = cardContainer(CORAL).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        textColumn.addView(
            text("習慣トラッカー", 28f, Color.WHITE, Typeface.BOLD).apply {
                includeFontPadding = false
            }
        )
        textColumn.addView(
            text("今日の小さな積み重ねを、気持ちよく残しましょう。", 14f, Color.WHITE, Typeface.NORMAL)
                .apply {
                    alpha = 0.92f
                    setPadding(0, dp(8), dp(10), 0)
                }
        )
        card.addView(textColumn)
        card.addView(HeaderArtView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96))
        })
        return card
    }

    private fun addHabitCard(): View {
        val card = cardContainer(Color.WHITE).apply {
            orientation = LinearLayout.VERTICAL
        }
        card.addView(text("今日の習慣", 21f, INK, Typeface.BOLD))
        habitInput = input("例: 朝に水を飲む")
        card.addView(habitInput)
        card.addView(
            softButton("習慣を追加", MINT, INK) {
                addHabit()
            }.apply {
                setMargins(top = dp(12))
            }
        )
        return card
    }

    private fun notificationCard(): View {
        val card = cardContainer(Color.WHITE).apply {
            orientation = LinearLayout.VERTICAL
        }
        card.addView(text("毎日の通知", 21f, INK, Typeface.BOLD))
        card.addView(
            text("通知する時刻", 14f, SUBTLE, Typeface.NORMAL).apply {
                setPadding(0, dp(10), 0, 0)
            }
        )

        val timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        hourInput = input("20").apply {
            setText(store.notificationHour().toString().padStart(2, '0'))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
        }
        minuteInput = input("00").apply {
            setText(store.notificationMinute().toString().padStart(2, '0'))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
        }
        timeRow.addView(hourInput)
        timeRow.addView(text("時", 16f, INK, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        timeRow.addView(minuteInput)
        timeRow.addView(text("分", 16f, INK, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.WRAP_CONTENT)
        })
        card.addView(timeRow)

        soundToggle = CheckBox(this).apply {
            text = "効果音"
            textSize = 15f
            typeface = roundedTypeface
            setTextColor(INK)
            isChecked = store.soundEnabled()
            setPadding(0, dp(10), 0, 0)
            setOnCheckedChangeListener { _, checked ->
                store.setSoundEnabled(checked)
            }
        }
        card.addView(soundToggle)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        buttonRow.addView(
            softButton("通知を許可", SKY, INK) {
                requestNotificationPermission()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).withEndMargin(dp(8))
            }
        )
        buttonRow.addView(
            softButton("時刻を保存", LEMON, INK) {
                saveNotificationTime()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).withStartMargin(dp(8))
            }
        )
        card.addView(buttonRow)
        return card
    }

    private fun addHabit() {
        val name = habitInput.text.toString().trim()
        if (name.isBlank()) {
            toast("習慣名を入力してください")
            return
        }

        store.add(name)
        habitInput.text.clear()
        sounds.playAdd(store.soundEnabled())
        renderHabits()
        toast("習慣を追加しました")
    }

    private fun saveNotificationTime() {
        val hour = hourInput.text.toString().trim().toIntOrNull()
        val minute = minuteInput.text.toString().trim().toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            toast("通知時刻を正しく入力してください")
            return
        }

        store.setNotificationTime(hour, minute)
        scheduler.schedule(hour, minute)
        toast("毎日の通知を設定しました")
    }

    private fun renderHabits() {
        habitsContainer.removeAllViews()
        val habits = store.all()
        habitsContainer.addView(sectionTitle("習慣の一覧"))

        if (habits.isEmpty()) {
            habitsContainer.addView(emptyCard())
            return
        }

        habits.forEach { habit ->
            habitsContainer.addView(habitCard(habit))
        }
    }

    private fun habitCard(habit: Habit): View {
        val today = LocalDate.now().toString()
        val completedToday = today in habit.completedDates

        val card = cardContainer(Color.WHITE).apply {
            orientation = LinearLayout.VERTICAL
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topRow.addView(
            StatusBadgeView(this, completedToday).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).withEndMargin(dp(12))
            }
        )
        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleColumn.addView(text(habit.name, 20f, INK, Typeface.BOLD))
        titleColumn.addView(
            text(
                if (completedToday) "今日もできました" else "今日はまだです",
                14f,
                if (completedToday) GREEN_TEXT else SUBTLE,
                Typeface.NORMAL
            ).apply {
                setPadding(0, dp(3), 0, 0)
            }
        )
        topRow.addView(titleColumn)
        card.addView(topRow)

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, dp(8))
        }
        statsRow.addView(statPill("連続${streakDays(habit)}日", MINT))
        statsRow.addView(statPill("今週${weekCompletedCount(habit)}/7", LEMON).apply {
            setMargins(left = dp(8))
        })
        card.addView(statsRow)

        card.addView(text("直近7日間", 14f, SUBTLE, Typeface.BOLD))
        card.addView(
            WeekDotsView(this, habit.completedDates).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(66)
                )
                setPadding(0, dp(6), 0, 0)
            }
        )

        card.addView(
            softButton(
                if (completedToday) "取り消す" else "今日できた",
                if (completedToday) SOFT_GRAY else CORAL,
                if (completedToday) INK else Color.WHITE
            ) {
                val completedNow = store.toggleToday(habit.id)
                if (completedNow) {
                    sounds.playComplete(store.soundEnabled())
                } else {
                    sounds.playUndo(store.soundEnabled())
                }
                renderHabits()
            }.apply {
                setMargins(top = dp(12))
            }
        )

        return card
    }

    private fun emptyCard(): View =
        cardContainer(Color.WHITE).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("まだ習慣がありません", 20f, INK, Typeface.BOLD))
            addView(
                text("まずはひとつ、続けたいことを書いてみましょう。", 14f, SUBTLE, Typeface.NORMAL)
                    .apply {
                        setPadding(0, dp(8), 0, 0)
                    }
            )
        }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            toast("通知は利用できます")
            return
        }

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            toast("通知は許可されています")
        } else {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                saveNotificationTime()
            } else {
                toast("通知を使うには許可が必要です")
            }
        }
    }

    private fun streakDays(habit: Habit): Int {
        var date = LocalDate.now()
        var streak = 0
        while (date.toString() in habit.completedDates) {
            streak += 1
            date = date.minusDays(1)
        }
        return streak
    }

    private fun weekCompletedCount(habit: Habit): Int =
        (0L..6L).count { daysAgo ->
            LocalDate.now().minusDays(daysAgo).toString() in habit.completedDates
        }

    private fun cardContainer(color: Int): LinearLayout =
        LinearLayout(this).apply {
            background = rounded(color, dp(26).toFloat())
            elevation = dp(4).toFloat()
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).withBottomMargin(dp(16))
        }

    private fun sectionTitle(title: String): TextView =
        text(title, 22f, INK, Typeface.BOLD).apply {
            setPadding(dp(2), dp(6), 0, dp(10))
        }

    private fun statPill(label: String, color: Int): TextView =
        text(label, 14f, INK, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            background = rounded(color, dp(18).toFloat())
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }

    private fun softButton(label: String, color: Int, textColor: Int, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 15f
            typeface = roundedTypeface
            setTextColor(textColor)
            background = rounded(color, dp(18).toFloat())
            stateListAnimator = null
            setPadding(dp(12), 0, dp(12), 0)
            minHeight = dp(48)
            setOnClickListener { onClick() }
        }

    private fun input(hintText: String): EditText =
        EditText(this).apply {
            hint = hintText
            textSize = 16f
            typeface = roundedTypeface
            setTextColor(INK)
            setHintTextColor(SUBTLE)
            setSingleLine(true)
            background = rounded(INPUT_BG, dp(18).toFloat(), STROKE, dp(1))
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).withTopMargin(dp(12))
        }

    private fun text(value: String, size: Float, color: Int, style: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = Typeface.create("sans-serif-rounded", style)
            letterSpacing = 0f
        }

    private fun rounded(
        color: Int,
        radius: Float,
        strokeColor: Int? = null,
        strokeWidth: Int = 0
    ) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeColor != null) {
            setStroke(strokeWidth, strokeColor)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        sounds.release()
        super.onDestroy()
    }

    private fun LinearLayout.LayoutParams.withTopMargin(value: Int): LinearLayout.LayoutParams =
        apply { topMargin = value }

    private fun LinearLayout.LayoutParams.withBottomMargin(value: Int): LinearLayout.LayoutParams =
        apply { bottomMargin = value }

    private fun LinearLayout.LayoutParams.withStartMargin(value: Int): LinearLayout.LayoutParams =
        apply { marginStart = value }

    private fun LinearLayout.LayoutParams.withEndMargin(value: Int): LinearLayout.LayoutParams =
        apply { marginEnd = value }

    private fun View.setMargins(
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0
    ) {
        val params = layoutParams as? LinearLayout.LayoutParams ?: return
        params.setMargins(left, top, right, bottom)
        layoutParams = params
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 5001
        const val CREAM = 0xFFFFF8EF.toInt()
        const val CORAL = 0xFFFF7A8A.toInt()
        const val MINT = 0xFFB9F3D4.toInt()
        const val LEMON = 0xFFFFE989.toInt()
        const val SKY = 0xFFAEDBFF.toInt()
        const val INK = 0xFF263238.toInt()
        const val SUBTLE = 0xFF7C858A.toInt()
        const val GREEN_TEXT = 0xFF24795A.toInt()
        const val INPUT_BG = 0xFFFFFCF7.toInt()
        const val STROKE = 0xFFFFD6BD.toInt()
        const val SOFT_GRAY = 0xFFECEFF1.toInt()
    }
}

class HeaderArtView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        paint.color = 0x33FFFFFF
        canvas.drawCircle(w * 0.72f, h * 0.26f, w * 0.18f, paint)
        canvas.drawCircle(w * 0.22f, h * 0.76f, w * 0.13f, paint)

        paint.color = 0xFFFFE989.toInt()
        canvas.drawRoundRect(RectF(w * 0.16f, h * 0.18f, w * 0.84f, h * 0.82f), 22f, 22f, paint)
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(w * 0.22f, h * 0.30f, w * 0.78f, h * 0.72f), 16f, 16f, paint)
        paint.color = 0xFFFF7A8A.toInt()
        canvas.drawRoundRect(RectF(w * 0.22f, h * 0.30f, w * 0.78f, h * 0.44f), 16f, 16f, paint)

        paint.color = 0xFFB9F3D4.toInt()
        canvas.drawCircle(w * 0.38f, h * 0.58f, 7f, paint)
        canvas.drawCircle(w * 0.50f, h * 0.58f, 7f, paint)
        canvas.drawCircle(w * 0.62f, h * 0.58f, 7f, paint)
        canvas.drawText("✓", w * 0.50f, h * 0.67f, textPaint)
    }
}

class StatusBadgeView(context: Context, private val completed: Boolean) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        paint.style = Paint.Style.FILL
        paint.color = if (completed) 0xFFB9F3D4.toInt() else 0xFFFFE989.toInt()
        canvas.drawCircle(width / 2f, height / 2f, size * 0.42f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = if (completed) 0xFF24795A.toInt() else 0xFFFF7A8A.toInt()
        if (completed) {
            canvas.drawLine(size * 0.30f, size * 0.52f, size * 0.45f, size * 0.66f, paint)
            canvas.drawLine(size * 0.45f, size * 0.66f, size * 0.72f, size * 0.36f, paint)
        } else {
            canvas.drawLine(size * 0.50f, size * 0.28f, size * 0.50f, size * 0.56f, paint)
            canvas.drawPoint(size * 0.50f, size * 0.70f, paint)
        }
    }
}

class WeekDotsView(
    context: Context,
    private val completedDates: Set<String>
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C858A.toInt()
        textSize = 24f
        typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gap = width / 7f
        val today = LocalDate.now()
        for (index in 0..6) {
            val date = today.minusDays((6 - index).toLong())
            val centerX = gap * index + gap / 2f
            val completed = date.toString() in completedDates
            paint.color = if (completed) 0xFFFF7A8A.toInt() else 0xFFECEFF1.toInt()
            canvas.drawCircle(centerX, height * 0.36f, 15f, paint)
            if (completed) {
                paint.color = Color.WHITE
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(centerX - 7f, height * 0.36f, centerX - 1f, height * 0.44f, paint)
                canvas.drawLine(centerX - 1f, height * 0.44f, centerX + 9f, height * 0.28f, paint)
                paint.style = Paint.Style.FILL
            }
            val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPAN).take(1)
            canvas.drawText(label, centerX, height * 0.82f, textPaint)
        }
    }
}
