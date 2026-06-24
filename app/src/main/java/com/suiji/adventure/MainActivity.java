package com.suiji.adventure;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "sui_ji_adventure";
    private static final String KEY_DATE = "today_date";
    private static final String KEY_TASKS = "today_tasks";

    private static final String IMPORTANCE_MAIN = "关键主线";
    private static final String IMPORTANCE_SIDE = "重要支线";
    private static final String IMPORTANCE_LIGHT = "轻量探索";
    private static final String[] IMPORTANCE_OPTIONS = {
            IMPORTANCE_MAIN,
            IMPORTANCE_SIDE,
            IMPORTANCE_LIGHT
    };

    private static final int COLOR_BG = Color.rgb(250, 250, 248);
    private static final int COLOR_SURFACE = Color.WHITE;
    private static final int COLOR_INK = Color.rgb(31, 41, 51);
    private static final int COLOR_MUTED = Color.rgb(87, 97, 108);
    private static final int COLOR_PRIMARY = Color.rgb(47, 107, 79);
    private static final int COLOR_PRIMARY_DARK = Color.rgb(30, 78, 57);
    private static final int COLOR_AMBER = Color.rgb(168, 77, 61);
    private static final int COLOR_BLUE = Color.rgb(53, 92, 125);
    private static final int COLOR_BORDER = Color.rgb(224, 226, 224);

    private final Random random = new Random();
    private final List<TaskItem> tasks = new ArrayList<>();

    private CountDownTimer countDownTimer;
    private TaskItem activeTask;
    private long totalMillis;
    private long remainingMillis;
    private boolean timerRunning;
    private TextView timerText;
    private TextView timerHintText;
    private ProgressBar timerProgress;
    private Button pauseButton;
    private String currentScreen = "setup";

    private final String[] promptPool = {
            "请开始今天的冒险。你准备用什么挑战自己？",
            "你今天想尝试什么新东西？",
            "一起继续坚持原来伟大的冒险。",
            "今天只要向前一点点，也算赢。",
            "给自己安排一个能完成的小任务，然后出发。"
    };

    private final String[] challengePool = {
            "做 20 分钟家务，让房间更好住",
            "散步 15 分钟，不带耳机观察路边",
            "发呆 5 分钟，只看窗外或墙面",
            "联系一个很久没有联系的朋友，发一句近况问候",
            "整理桌面 10 分钟，只留下现在需要的东西",
            "读 8 页纸质书，读完写一句感想",
            "写下今天最想感谢的一件小事",
            "做 15 分钟拉伸或徒手运动",
            "把手机放远，认真喝一杯水",
            "学习一个一直想了解的小知识 20 分钟",
            "拍一张今天让你停下来看的照片",
            "归位 5 件不需要摆在外面的东西",
            "给未来的自己写 100 字",
            "认真洗一个杯子或清理一个角落",
            "出门晒 10 分钟太阳",
            "听一首完整的歌，中间不切走",
            "做一件可以在 2 分钟内帮到别人的小事",
            "复盘今天最想逃避的一件事，写下下一步",
            "尝试 10 分钟静坐呼吸",
            "为明天准备一个小物件"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(COLOR_BG);
        getWindow().setNavigationBarColor(COLOR_BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        loadTodayTasks();
        showSetupScreen();
    }

    private void showSetupScreen() {
        currentScreen = "setup";
        activeTask = null;
        cancelTimer();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG);

        LinearLayout page = vertical();
        page.setPadding(dp(20), dp(24), dp(20), dp(28));
        scrollView.addView(page, matchWrap());

        TextView title = text("今日冒险板", 29, COLOR_INK, Typeface.BOLD);
        page.addView(title, matchWrap());

        TextView prompt = text(randomPrompt(), 16, COLOR_MUTED, Typeface.NORMAL);
        prompt.setLineSpacing(dp(3), 1.0f);
        page.addView(prompt, margins(matchWrap(), 0, dp(8), 0, dp(18)));

        page.addView(challengePanel(), margins(matchWrap(), 0, 0, 0, dp(16)));
        page.addView(inputPanel(), margins(matchWrap(), 0, 0, 0, dp(18)));

        TextView listTitle = text("今天的任务", 20, COLOR_INK, Typeface.BOLD);
        page.addView(listTitle, margins(matchWrap(), 0, 0, 0, dp(10)));

        if (tasks.isEmpty()) {
            TextView empty = text("先写下一个任务，或者换一个随机挑战。", 15, COLOR_MUTED, Typeface.NORMAL);
            page.addView(empty, margins(matchWrap(), 0, dp(6), 0, dp(12)));
        } else {
            for (int i = 0; i < tasks.size(); i++) {
                TaskItem task = tasks.get(i);
                page.addView(taskCard(task), margins(matchWrap(), 0, 0, 0, dp(10)));
            }
        }

        setContentView(scrollView);
    }

    private View challengePanel() {
        LinearLayout panel = panel();
        TextView label = text("随机生活挑战", 14, COLOR_PRIMARY, Typeface.BOLD);
        panel.addView(label, matchWrap());

        TaskItem challenge = getRandomChallengeTask();
        TextView challengeText = text(challenge.title, 19, COLOR_INK, Typeface.BOLD);
        challengeText.setLineSpacing(dp(3), 1.0f);
        panel.addView(challengeText, margins(matchWrap(), 0, dp(8), 0, dp(12)));

        LinearLayout row = horizontal();
        Button reroll = secondaryButton("换一个");
        Button start = primaryButton("开始这个");
        row.addView(reroll, weightWrap(1));
        row.addView(start, margins(weightWrap(1), dp(10), 0, 0, 0));
        panel.addView(row, matchWrap());

        reroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replaceRandomChallenge();
                saveTodayTasks();
                showSetupScreen();
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrepareScreen(getRandomChallengeTask());
            }
        });

        return panel;
    }

    private View inputPanel() {
        LinearLayout panel = panel();
        TextView title = text("布置自己的挑战", 18, COLOR_INK, Typeface.BOLD);
        panel.addView(title, matchWrap());

        final EditText input = new EditText(this);
        input.setHint("例如：复习高数第 3 章 / 写完周报初稿");
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setMaxLines(4);
        input.setTextColor(COLOR_INK);
        input.setHintTextColor(Color.rgb(142, 150, 158));
        input.setTextSize(16);
        input.setBackground(rounded(Color.rgb(247, 248, 246), 8, COLOR_BORDER));
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        panel.addView(input, margins(matchWrap(), 0, dp(12), 0, dp(12)));

        LinearLayout row = horizontal();
        final Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, IMPORTANCE_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(1);
        row.addView(spinner, weightWrap(1));

        Button add = primaryButton("加入");
        row.addView(add, margins(wrapWrap(), dp(10), 0, 0, 0));
        panel.addView(row, matchWrap());

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = input.getText().toString().trim();
                if (title.length() == 0) {
                    Toast.makeText(MainActivity.this, "先写下一个具体任务。", Toast.LENGTH_SHORT).show();
                    return;
                }
                tasks.add(new TaskItem(newId(), title, spinner.getSelectedItem().toString(), false, false));
                saveTodayTasks();
                showSetupScreen();
            }
        });

        return panel;
    }

    private View taskCard(final TaskItem task) {
        LinearLayout card = vertical();
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.setBackground(rounded(COLOR_SURFACE, 8, task.done ? Color.rgb(202, 213, 206) : COLOR_BORDER));

        LinearLayout topRow = horizontal();
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView chip = text(task.importance, 13, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), dp(4), dp(8), dp(4));
        chip.setBackground(rounded(importanceColor(task.importance), 8, importanceColor(task.importance)));
        topRow.addView(chip, wrapWrap());

        if (task.randomChallenge) {
            TextView randomLabel = text("随机", 13, COLOR_MUTED, Typeface.BOLD);
            randomLabel.setGravity(Gravity.CENTER);
            randomLabel.setPadding(dp(8), dp(4), dp(8), dp(4));
            randomLabel.setBackground(rounded(Color.rgb(244, 246, 242), 8, COLOR_BORDER));
            topRow.addView(randomLabel, margins(wrapWrap(), dp(8), 0, 0, 0));
        }

        if (task.done) {
            TextView done = text("已完成", 13, COLOR_PRIMARY, Typeface.BOLD);
            topRow.addView(done, margins(wrapWrap(), dp(8), 0, 0, 0));
        }
        card.addView(topRow, matchWrap());

        TextView taskTitle = text(task.title, 18, COLOR_INK, Typeface.BOLD);
        taskTitle.setLineSpacing(dp(3), 1.0f);
        card.addView(taskTitle, margins(matchWrap(), 0, dp(10), 0, dp(12)));

        LinearLayout actions = horizontal();
        Button start = primaryButton(task.done ? "再来一次" : "开始计时");
        actions.addView(start, weightWrap(1));

        if (!task.randomChallenge) {
            Button remove = secondaryButton("移除");
            actions.addView(remove, margins(wrapWrap(), dp(10), 0, 0, 0));
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tasks.remove(task);
                    saveTodayTasks();
                    showSetupScreen();
                }
            });
        }
        card.addView(actions, matchWrap());

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrepareScreen(task);
            }
        });

        return card;
    }

    private void showPrepareScreen(final TaskItem task) {
        currentScreen = "prepare";
        cancelTimer();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG);

        LinearLayout page = vertical();
        page.setPadding(dp(20), dp(24), dp(20), dp(28));
        scrollView.addView(page, matchWrap());

        TextView title = text("准备出发", 29, COLOR_INK, Typeface.BOLD);
        page.addView(title, matchWrap());

        TextView subtitle = text("选好这次挑战的时长。短一点也可以，关键是现在开始。", 16, COLOR_MUTED, Typeface.NORMAL);
        subtitle.setLineSpacing(dp(3), 1.0f);
        page.addView(subtitle, margins(matchWrap(), 0, dp(8), 0, dp(18)));

        LinearLayout taskPanel = panel();
        TextView label = text(task.importance, 14, importanceColor(task.importance), Typeface.BOLD);
        taskPanel.addView(label, matchWrap());
        TextView taskTitle = text(task.title, 21, COLOR_INK, Typeface.BOLD);
        taskTitle.setLineSpacing(dp(3), 1.0f);
        taskPanel.addView(taskTitle, margins(matchWrap(), 0, dp(8), 0, 0));
        page.addView(taskPanel, margins(matchWrap(), 0, 0, 0, dp(18)));

        LinearLayout durationPanel = panel();
        final TextView durationText = text("25 分钟", 34, COLOR_PRIMARY_DARK, Typeface.BOLD);
        durationText.setGravity(Gravity.CENTER);
        durationPanel.addView(durationText, matchWrap());

        final SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(23);
        seekBar.setProgress(4);
        durationPanel.addView(seekBar, margins(matchWrap(), 0, dp(10), 0, dp(8)));

        LinearLayout quickRow = horizontal();
        final int[] minutes = {25};
        addQuickDurationButton(quickRow, seekBar, minutes, durationText, 10);
        addQuickDurationButton(quickRow, seekBar, minutes, durationText, 20);
        addQuickDurationButton(quickRow, seekBar, minutes, durationText, 45);
        addQuickDurationButton(quickRow, seekBar, minutes, durationText, 60);
        durationPanel.addView(quickRow, matchWrap());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minutes[0] = (progress + 1) * 5;
                durationText.setText(minutes[0] + " 分钟");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        page.addView(durationPanel, margins(matchWrap(), 0, 0, 0, dp(18)));

        Button start = primaryButton("开始倒计时");
        page.addView(start, matchWrap());

        Button back = plainButton("回到任务板");
        page.addView(back, margins(matchWrap(), 0, dp(10), 0, 0));

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTaskTimer(task, minutes[0]);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetupScreen();
            }
        });

        setContentView(scrollView);
    }

    private void addQuickDurationButton(LinearLayout row, final SeekBar seekBar, final int[] minutes,
                                        final TextView durationText, final int value) {
        Button button = secondaryButton(value + "");
        row.addView(button, margins(weightWrap(1), dp(4), 0, dp(4), 0));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minutes[0] = value;
                durationText.setText(value + " 分钟");
                seekBar.setProgress(value / 5 - 1);
            }
        });
    }

    private void startTaskTimer(TaskItem task, int minutes) {
        currentScreen = "timer";
        activeTask = task;
        totalMillis = minutes * 60L * 1000L;
        remainingMillis = totalMillis;

        LinearLayout page = vertical();
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(20), dp(28), dp(20), dp(28));
        page.setBackgroundColor(COLOR_BG);

        TextView label = text("正在挑战", 16, importanceColor(task.importance), Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        page.addView(label, matchWrap());

        TextView taskTitle = text(task.title, 24, COLOR_INK, Typeface.BOLD);
        taskTitle.setGravity(Gravity.CENTER);
        taskTitle.setLineSpacing(dp(4), 1.0f);
        page.addView(taskTitle, margins(matchWrap(), 0, dp(12), 0, dp(24)));

        timerText = text(formatMillis(remainingMillis), 58, COLOR_PRIMARY_DARK, Typeface.BOLD);
        timerText.setGravity(Gravity.CENTER);
        page.addView(timerText, matchWrap());

        timerHintText = text("把注意力放回这件事。", 16, COLOR_MUTED, Typeface.NORMAL);
        timerHintText.setGravity(Gravity.CENTER);
        page.addView(timerHintText, margins(matchWrap(), 0, dp(8), 0, dp(18)));

        timerProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        timerProgress.setMax(1000);
        page.addView(timerProgress, margins(matchWrap(), 0, 0, 0, dp(24)));

        pauseButton = primaryButton("暂停");
        page.addView(pauseButton, matchWrap());

        Button complete = secondaryButton("我已完成");
        page.addView(complete, margins(matchWrap(), 0, dp(10), 0, 0));

        Button quit = plainButton("先回任务板");
        page.addView(quit, margins(matchWrap(), 0, dp(10), 0, 0));

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerRunning) {
                    pauseTimer();
                } else {
                    resumeTimer();
                }
            }
        });

        complete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishTask(activeTask);
            }
        });

        quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelTimer();
                showSetupScreen();
            }
        });

        setContentView(page);
        updateTimerViews();
        resumeTimer();
    }

    private void resumeTimer() {
        cancelTimer();
        timerRunning = true;
        if (pauseButton != null) {
            pauseButton.setText("暂停");
        }
        countDownTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                updateTimerViews();
            }

            @Override
            public void onFinish() {
                remainingMillis = 0;
                updateTimerViews();
                finishTask(activeTask);
            }
        };
        countDownTimer.start();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        if (pauseButton != null) {
            pauseButton.setText("继续");
        }
        if (timerHintText != null) {
            timerHintText.setText("暂停中。准备好了再继续。");
        }
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        timerRunning = false;
    }

    private void updateTimerViews() {
        if (timerText != null) {
            timerText.setText(formatMillis(remainingMillis));
        }
        if (timerProgress != null && totalMillis > 0) {
            long elapsed = Math.max(0L, totalMillis - remainingMillis);
            timerProgress.setProgress((int) (elapsed * 1000L / totalMillis));
        }
        if (timerHintText != null && timerRunning) {
            timerHintText.setText("把注意力放回这件事。");
        }
    }

    private void finishTask(TaskItem task) {
        cancelTimer();
        if (task != null) {
            task.done = true;
            saveTodayTasks();
        }
        showFinishScreen(task);
    }

    private void showFinishScreen(final TaskItem task) {
        currentScreen = "finish";
        LinearLayout page = vertical();
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(24), dp(36), dp(24), dp(28));
        page.setBackgroundColor(COLOR_BG);

        TextView title = text("完成了一段冒险", 29, COLOR_INK, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        page.addView(title, matchWrap());

        String taskTitle = task == null ? "刚才的任务" : task.title;
        TextView subtitle = text("你刚刚把时间交还给了自己：\n" + taskTitle, 17, COLOR_MUTED, Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(dp(5), 1.0f);
        page.addView(subtitle, margins(matchWrap(), 0, dp(16), 0, dp(28)));

        Button board = primaryButton("回到今日冒险板");
        page.addView(board, matchWrap());

        if (task != null) {
            Button again = secondaryButton("继续这个任务");
            page.addView(again, margins(matchWrap(), 0, dp(10), 0, 0));
            again.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPrepareScreen(task);
                }
            });
        }

        board.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSetupScreen();
            }
        });

        setContentView(page);
    }

    private void loadTodayTasks() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String today = todayString();
        String storedDate = prefs.getString(KEY_DATE, "");
        tasks.clear();

        if (today.equals(storedDate)) {
            String raw = prefs.getString(KEY_TASKS, "[]");
            try {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    tasks.add(new TaskItem(
                            object.optString("id", newId()),
                            object.optString("title", ""),
                            object.optString("importance", IMPORTANCE_SIDE),
                            object.optBoolean("randomChallenge", false),
                            object.optBoolean("done", false)
                    ));
                }
            } catch (JSONException ignored) {
                tasks.clear();
            }
        }

        if (tasks.isEmpty()) {
            tasks.add(new TaskItem(newId(), randomChallenge(), IMPORTANCE_LIGHT, true, false));
        } else if (getRandomChallengeTaskOrNull() == null) {
            tasks.add(0, new TaskItem(newId(), randomChallenge(), IMPORTANCE_LIGHT, true, false));
        }
        saveTodayTasks();
    }

    private void saveTodayTasks() {
        JSONArray array = new JSONArray();
        for (TaskItem task : tasks) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", task.id);
                object.put("title", task.title);
                object.put("importance", task.importance);
                object.put("randomChallenge", task.randomChallenge);
                object.put("done", task.done);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_DATE, todayString())
                .putString(KEY_TASKS, array.toString())
                .apply();
    }

    private TaskItem getRandomChallengeTask() {
        TaskItem task = getRandomChallengeTaskOrNull();
        if (task == null) {
            task = new TaskItem(newId(), randomChallenge(), IMPORTANCE_LIGHT, true, false);
            tasks.add(0, task);
            saveTodayTasks();
        }
        return task;
    }

    private TaskItem getRandomChallengeTaskOrNull() {
        for (TaskItem task : tasks) {
            if (task.randomChallenge) {
                return task;
            }
        }
        return null;
    }

    private void replaceRandomChallenge() {
        TaskItem task = getRandomChallengeTask();
        String next = randomChallenge();
        int guard = 0;
        while (next.equals(task.title) && guard < 8) {
            next = randomChallenge();
            guard++;
        }
        task.title = next;
        task.done = false;
    }

    private String randomPrompt() {
        return promptPool[random.nextInt(promptPool.length)];
    }

    private String randomChallenge() {
        return challengePool[random.nextInt(challengePool.length)];
    }

    private String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }

    private String newId() {
        return System.currentTimeMillis() + "-" + Math.abs(random.nextInt());
    }

    private String formatMillis(long millis) {
        long safe = Math.max(0L, millis);
        long totalSeconds = (safe + 999L) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format(Locale.CHINA, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.CHINA, "%02d:%02d", minutes, seconds);
    }

    private int importanceColor(String importance) {
        if (IMPORTANCE_MAIN.equals(importance)) {
            return COLOR_AMBER;
        }
        if (IMPORTANCE_LIGHT.equals(importance)) {
            return COLOR_BLUE;
        }
        return COLOR_PRIMARY;
    }

    private LinearLayout panel() {
        LinearLayout panel = vertical();
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackground(rounded(COLOR_SURFACE, 8, COLOR_BORDER));
        return panel;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private TextView text(String value, float sizeSp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private Button primaryButton(String label) {
        Button button = baseButton(label);
        button.setTextColor(Color.WHITE);
        button.setBackground(rounded(COLOR_PRIMARY, 8, COLOR_PRIMARY));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = baseButton(label);
        button.setTextColor(COLOR_PRIMARY_DARK);
        button.setBackground(rounded(Color.rgb(242, 247, 242), 8, Color.rgb(194, 214, 200)));
        return button;
    }

    private Button plainButton(String label) {
        Button button = baseButton(label);
        button.setTextColor(COLOR_MUTED);
        button.setBackground(rounded(COLOR_BG, 8, COLOR_BG));
        return button;
    }

    private Button baseButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setMinHeight(dp(46));
        button.setPadding(dp(12), 0, dp(12), 0);
        return button;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams weightWrap(float weight) {
        return new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
        );
    }

    private LinearLayout.LayoutParams margins(LinearLayout.LayoutParams params, int left, int top, int right, int bottom) {
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if ("timer".equals(currentScreen)) {
            cancelTimer();
            showSetupScreen();
        } else if ("prepare".equals(currentScreen) || "finish".equals(currentScreen)) {
            showSetupScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        cancelTimer();
        super.onDestroy();
    }

    private static class TaskItem {
        String id;
        String title;
        String importance;
        boolean randomChallenge;
        boolean done;

        TaskItem(String id, String title, String importance, boolean randomChallenge, boolean done) {
            this.id = id;
            this.title = title;
            this.importance = importance;
            this.randomChallenge = randomChallenge;
            this.done = done;
        }
    }
}
