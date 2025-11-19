package com.example.focusease

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClockActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnAddAlarm: ImageView
    private lateinit var alarmsContainer: LinearLayout

    private val alarmsList = mutableListOf<AlarmData>()
    private var alarmIdCounter = 0

    private val colors = listOf(
        "#9B8AB8" to "#D4C5E3",
        "#8B7BA8" to "#C8B8DC",
        "#7A6A98" to "#B4A5C7",
        "#6A5A88" to "#A495B7"
    )

    // ✅ RECEIVER UNTUK MATIKAN SWITCH
    private val turnOffAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val alarmId = intent.getIntExtra("ALARM_ID", -1)
            if (alarmId != -1) {
                turnOffAlarmSwitch(alarmId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clock)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        createNotificationChannel()

        initViews()
        setupListeners()
        setupBottomNavigation()

        // ✅ Load alarms dari storage
        loadAlarmsFromStorage()

        // Kalau belum ada alarm, buat default
        if (alarmsList.isEmpty()) {
            addDefaultAlarms()
            saveAlarmsToStorage()
        }

        // ✅ REGISTER RECEIVER
        val filter = IntentFilter("com.example.focusease.TURN_OFF_ALARM")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(turnOffAlarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(turnOffAlarmReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(turnOffAlarmReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ✅ FUNGSI MATIKAN SWITCH ALARM
    private fun turnOffAlarmSwitch(alarmId: Int) {
        val alarm = alarmsList.find { it.id == alarmId }
        if (alarm != null) {
            alarm.isActive = false
            refreshAlarmsList()
            saveAlarmsToStorage()
            Toast.makeText(this, "${alarm.alarmName} completed", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ SAVE & LOAD FUNCTIONS
    private fun saveAlarmsToStorage() {
        AlarmStorageManager.saveAlarms(this, alarmsList)
        AlarmStorageManager.saveAlarmCounter(this, alarmIdCounter)
    }

    private fun loadAlarmsFromStorage() {
        alarmsList.clear()
        alarmsList.addAll(AlarmStorageManager.loadAlarms(this))
        alarmIdCounter = AlarmStorageManager.loadAlarmCounter(this)

        alarmsContainer.removeAllViews()
        alarmsList.forEach { alarm ->
            createAlarmView(alarm)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied. Alarms won't work properly.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnAddAlarm = findViewById(R.id.btnAddAlarm)
        alarmsContainer = findViewById(R.id.alarmsContainer)
    }

    private fun addDefaultAlarms() {
        addAlarmToList(9, 30, false)
        addAlarmToList(13, 30, false)
        addAlarmToList(20, 45, false)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnAddAlarm.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showTimePickerDialog(existingAlarm: AlarmData? = null) {
        val calendar = Calendar.getInstance()
        val hour = existingAlarm?.hour ?: calendar.get(Calendar.HOUR_OF_DAY)
        val minute = existingAlarm?.minute ?: calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                if (existingAlarm != null) {
                    existingAlarm.hour = selectedHour
                    existingAlarm.minute = selectedMinute
                    showRepeatDaysDialog(existingAlarm)
                } else {
                    val alarmId = alarmIdCounter++
                    val colorPair = colors[alarmId % colors.size]
                    val newAlarm = AlarmData(
                        alarmId, selectedHour, selectedMinute, false,
                        colorPair.first, colorPair.second
                    )
                    alarmsList.add(newAlarm)
                    showRepeatDaysDialog(newAlarm)
                }
            },
            hour,
            minute,
            false
        )

        timePickerDialog.show()
    }

    private fun showRepeatDaysDialog(alarm: AlarmData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_repeat_days, null)
        val dialog = AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.white)

        val editAlarmName = dialogView.findViewById<android.widget.EditText>(R.id.editAlarmName)
        editAlarmName.setText(alarm.alarmName)

        val radioGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.radioGroupRepeatMode)
        val radioOnce = dialogView.findViewById<android.widget.RadioButton>(R.id.radioOnce)
        val radioSpecificDate = dialogView.findViewById<android.widget.RadioButton>(R.id.radioSpecificDate)
        val radioDaily = dialogView.findViewById<android.widget.RadioButton>(R.id.radioDaily)
        val radioWeekdays = dialogView.findViewById<android.widget.RadioButton>(R.id.radioWeekdays)
        val radioWeekends = dialogView.findViewById<android.widget.RadioButton>(R.id.radioWeekends)
        val radioCustom = dialogView.findViewById<android.widget.RadioButton>(R.id.radioCustom)

        val specificDateContainer = dialogView.findViewById<LinearLayout>(R.id.specificDateContainer)
        val customDaysContainer = dialogView.findViewById<LinearLayout>(R.id.customDaysContainer)
        val btnSelectDate = dialogView.findViewById<android.widget.Button>(R.id.btnSelectDate)
        val txtSelectedDate = dialogView.findViewById<TextView>(R.id.txtSelectedDate)

        val checkBoxes = listOf(
            dialogView.findViewById<android.widget.CheckBox>(R.id.checkSun) to Calendar.SUNDAY,
            dialogView.findViewById<android.widget.CheckBox>(R.id.checkMon) to Calendar.MONDAY,
            dialogView.findViewById<android.widget.CheckBox>(R.id.checkTue) to Calendar.TUESDAY,
            dialogView.findViewById<android.widget.CheckBox>(R.id.checkWed) to Calendar.WEDNESDAY,
            dialogView.findViewById<android.widget.CheckBox>(R.id.checkThu) to Calendar.THURSDAY,
            dialogView.findViewById<android.widget.CheckBox>(R.id.checkFri) to Calendar.FRIDAY,
            dialogView.findViewById<android.widget.CheckBox>(R.id.checkSat) to Calendar.SATURDAY
        )

        when (alarm.repeatMode) {
            RepeatMode.ONCE -> radioOnce.isChecked = true
            RepeatMode.SPECIFIC_DATE -> {
                radioSpecificDate.isChecked = true
                specificDateContainer.visibility = View.VISIBLE
                alarm.specificDate?.let {
                    val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
                    txtSelectedDate.text = dateFormat.format(it)
                }
            }
            RepeatMode.DAILY -> radioDaily.isChecked = true
            RepeatMode.WEEKDAYS -> radioWeekdays.isChecked = true
            RepeatMode.WEEKENDS -> radioWeekends.isChecked = true
            RepeatMode.CUSTOM -> {
                radioCustom.isChecked = true
                customDaysContainer.visibility = View.VISIBLE
                checkBoxes.forEach { (checkBox, day) ->
                    checkBox.isChecked = alarm.repeatDays.contains(day)
                }
            }
        }

        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            alarm.specificDate?.let { calendar.timeInMillis = it }

            val datePicker = android.app.DatePickerDialog(
                this,
                androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog,
                { _, year, month, day ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, day, alarm.hour, alarm.minute)
                    alarm.specificDate = selectedCalendar.timeInMillis

                    val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
                    txtSelectedDate.text = dateFormat.format(selectedCalendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            specificDateContainer.visibility = if (checkedId == R.id.radioSpecificDate) {
                View.VISIBLE
            } else {
                View.GONE
            }

            customDaysContainer.visibility = if (checkedId == R.id.radioCustom) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        dialogView.findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btnOk).setOnClickListener {
            val newName = editAlarmName.text.toString().trim()
            alarm.alarmName = if (newName.isEmpty()) "Alarm" else newName

            alarm.repeatMode = when (radioGroup.checkedRadioButtonId) {
                R.id.radioOnce -> RepeatMode.ONCE
                R.id.radioSpecificDate -> RepeatMode.SPECIFIC_DATE
                R.id.radioDaily -> RepeatMode.DAILY
                R.id.radioWeekdays -> RepeatMode.WEEKDAYS
                R.id.radioWeekends -> RepeatMode.WEEKENDS
                R.id.radioCustom -> RepeatMode.CUSTOM
                else -> RepeatMode.ONCE
            }

            if (alarm.repeatMode == RepeatMode.CUSTOM) {
                alarm.repeatDays.clear()
                checkBoxes.forEach { (checkBox, day) ->
                    if (checkBox.isChecked) {
                        alarm.repeatDays.add(day)
                    }
                }
            } else if (alarm.repeatMode != RepeatMode.SPECIFIC_DATE) {
                alarm.specificDate = null
                alarm.repeatDays.clear()
                when (alarm.repeatMode) {
                    RepeatMode.DAILY -> {
                        alarm.repeatDays.addAll(listOf(
                            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
                        ))
                    }
                    RepeatMode.WEEKDAYS -> {
                        alarm.repeatDays.addAll(listOf(
                            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                            Calendar.THURSDAY, Calendar.FRIDAY
                        ))
                    }
                    RepeatMode.WEEKENDS -> {
                        alarm.repeatDays.addAll(listOf(Calendar.SATURDAY, Calendar.SUNDAY))
                    }
                    else -> {}
                }
            }

            refreshAlarmsList()

            if (alarm.isActive) {
                setAlarm(alarm)
            }

            saveAlarmsToStorage() // ✅ SAVE

            Toast.makeText(this, "Alarm saved!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun getRepeatDaysText(alarm: AlarmData): String {
        return when (alarm.repeatMode) {
            RepeatMode.ONCE -> "Once"
            RepeatMode.SPECIFIC_DATE -> {
                alarm.specificDate?.let {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    dateFormat.format(it)
                } ?: "Once"
            }
            RepeatMode.DAILY -> "Every day"
            RepeatMode.WEEKDAYS -> "Mon - Fri"
            RepeatMode.WEEKENDS -> "Sat - Sun"
            RepeatMode.CUSTOM -> {
                if (alarm.repeatDays.isEmpty()) {
                    "Once"
                } else {
                    val dayNames = mapOf(
                        Calendar.SUNDAY to "Sun",
                        Calendar.MONDAY to "Mon",
                        Calendar.TUESDAY to "Tue",
                        Calendar.WEDNESDAY to "Wed",
                        Calendar.THURSDAY to "Thu",
                        Calendar.FRIDAY to "Fri",
                        Calendar.SATURDAY to "Sat"
                    )
                    alarm.repeatDays.sorted().joinToString(", ") { dayNames[it] ?: "" }
                }
            }
        }
    }

    private fun addAlarmToList(hour: Int, minute: Int, isActive: Boolean) {
        val alarmId = alarmIdCounter++
        val colorPair = colors[alarmId % colors.size]
        val alarm = AlarmData(alarmId, hour, minute, isActive, colorPair.first, colorPair.second)
        alarmsList.add(alarm)
        createAlarmView(alarm)
        saveAlarmsToStorage() // ✅ SAVE
    }

    private fun createAlarmView(alarm: AlarmData) {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 40
            }
            radius = 50f
            cardElevation = 0f
        }

        val currentColor = if (alarm.isActive) alarm.colorOn else alarm.colorOff
        cardView.setCardBackgroundColor(Color.parseColor(currentColor))

        val mainLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
        }

        val topLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val timeLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = LinearLayout.VERTICAL
        }

        val nameTextView = TextView(this).apply {
            text = alarm.alarmName
            textSize = 16f
            setTextColor(Color.parseColor("#E8E8E8"))
            setPadding(0, 0, 0, 8)
        }

        val timeTextView = TextView(this).apply {
            text = formatTime(alarm.hour, alarm.minute)
            textSize = 32f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        val repeatTextView = TextView(this).apply {
            text = getRepeatDaysText(alarm)
            textSize = 14f
            setTextColor(Color.parseColor("#E8E8E8"))
            setPadding(0, 4, 0, 0)
        }

        val dateTextView = TextView(this).apply {
            text = getNextAlarmDate(alarm)
            textSize = 12f
            setTextColor(Color.parseColor("#D0D0D0"))
            setPadding(0, 4, 0, 0)
        }

        timeLayout.addView(nameTextView)
        timeLayout.addView(timeTextView)
        timeLayout.addView(repeatTextView)
        timeLayout.addView(dateTextView)

        val switch = SwitchCompat(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isChecked = alarm.isActive

            thumbDrawable?.setTint(Color.WHITE)

            trackDrawable?.setTintList(android.content.res.ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                ),
                intArrayOf(
                    Color.parseColor("#4DFFFFFF"),
                    Color.parseColor("#33FFFFFF")
                )
            ))
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            alarm.isActive = isChecked

            val newColor = if (isChecked) alarm.colorOn else alarm.colorOff
            cardView.setCardBackgroundColor(Color.parseColor(newColor))

            if (isChecked) {
                setAlarm(alarm)
                Toast.makeText(this, "Alarm ${formatTime(alarm.hour, alarm.minute)} ON", Toast.LENGTH_SHORT).show()
            } else {
                cancelAlarm(alarm)
                Toast.makeText(this, "Alarm ${formatTime(alarm.hour, alarm.minute)} OFF", Toast.LENGTH_SHORT).show()
            }

            saveAlarmsToStorage() // ✅ SAVE
        }

        topLayout.addView(timeLayout)
        topLayout.addView(switch)

        val optionsButton = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
                gravity = Gravity.CENTER
            }
            setImageResource(R.drawable.ic_more)
            setPadding(16, 16, 16, 16)
            setColorFilter(Color.WHITE)
        }

        optionsButton.setOnClickListener {
            showOptionsMenu(it, alarm)
        }

        mainLayout.addView(topLayout)
        mainLayout.addView(optionsButton)

        cardView.addView(mainLayout)
        alarmsContainer.addView(cardView)
    }

    private fun showOptionsMenu(view: View, alarm: AlarmData) {
        val wrapper = android.view.ContextThemeWrapper(
            this,
            androidx.appcompat.R.style.Theme_AppCompat_Light
        )

        val popupMenu = PopupMenu(
            wrapper,
            view,
            Gravity.NO_GRAVITY,
            0,
            androidx.appcompat.R.style.Widget_AppCompat_Light_PopupMenu
        )

        popupMenu.menuInflater.inflate(R.menu.alarm_options_menu, popupMenu.menu)

        try {
            val popup = PopupMenu::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true
            val menu = popup.get(popupMenu)
            menu.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(menu, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        for (i in 0 until popupMenu.menu.size()) {
            val item = popupMenu.menu.getItem(i)
            item.icon?.setTint(Color.BLACK)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    showTimePickerDialog(alarm)
                    true
                }
                R.id.menu_delete -> {
                    showDeleteConfirmation(alarm)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showDeleteConfirmation(alarm: AlarmData) {
        val dialog = AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog)
            .setTitle("Delete Alarm")
            .setMessage("Are you sure you want to delete this alarm?")
            .setPositiveButton("Delete") { _, _ ->
                deleteAlarm(alarm)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.white)

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(Color.parseColor("#FF0000"))
            isAllCaps = false
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(Color.parseColor("#666666"))
            isAllCaps = false
        }
    }

    private fun deleteAlarm(alarm: AlarmData) {
        cancelAlarm(alarm)
        alarmsList.remove(alarm)
        refreshAlarmsList()
        saveAlarmsToStorage() // ✅ SAVE
        Toast.makeText(this, "Alarm deleted", Toast.LENGTH_SHORT).show()
    }

    private fun refreshAlarmsList() {
        alarmsContainer.removeAllViews()
        alarmsList.forEach { alarm ->
            createAlarmView(alarm)
        }
    }

    private fun getNextAlarmDate(alarm: AlarmData): String {
        val calendar = Calendar.getInstance()

        if (alarm.repeatMode == RepeatMode.SPECIFIC_DATE && alarm.specificDate != null) {
            calendar.timeInMillis = alarm.specificDate!!
            calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
            calendar.set(Calendar.MINUTE, alarm.minute)
            calendar.set(Calendar.SECOND, 0)
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
            calendar.set(Calendar.MINUTE, alarm.minute)
            calendar.set(Calendar.SECOND, 0)

            if (alarm.repeatMode == RepeatMode.ONCE || alarm.repeatDays.isEmpty()) {
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                val currentTime = System.currentTimeMillis()

                if (calendar.timeInMillis <= currentTime) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                var daysToAdd = 0
                while (daysToAdd < 7) {
                    val checkDay = calendar.get(Calendar.DAY_OF_WEEK)
                    if (alarm.repeatDays.contains(checkDay)) {
                        break
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    daysToAdd++
                }
            }
        }

        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val day = dayFormat.format(calendar.time)
        val date = dateFormat.format(calendar.time)

        return "$day, $date"
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour > 12 -> hour - 12
            hour == 0 -> 12
            else -> hour
        }
        return String.format("%02d : %02d %s", hour12, minute, amPm)
    }

    private fun setAlarm(alarm: AlarmData) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_TIME", formatTime(alarm.hour, alarm.minute))
            putExtra("ALARM_NAME", alarm.alarmName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelAlarm(alarm: AlarmData) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ALARM_CHANNEL",
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for alarm notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<View>(R.id.navHome)
        val navMusic = findViewById<View>(R.id.navMusic)
        val navClock = findViewById<View>(R.id.navClock)
        val navProfile = findViewById<View>(R.id.navProfile)

        navHome?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            Log.d("NAV", "Home clicked")
        }

        navMusic?.setOnClickListener {
            val intent = Intent(this, MusicActivity::class.java)
            startActivity(intent)
            Log.d("NAV", "Music clicked")
        }

        navClock?.setOnClickListener {
            Log.d("NAV", "Clock clicked")
        }

        navProfile?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            Log.d("NAV", "Profile clicked")
        }
    }
}