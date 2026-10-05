package dev.siam.quietremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Independent, offline WRC03 remote. No network or advertising SDKs. */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = 0xff080a09;
    private static final int PANEL = 0xff191e1b;
    private static final int KEY_SURFACE = 0xff2b3f33;
    private static final int KEY_BORDER = 0xff718e7d;
    private static final int GREEN = 0xff83e5b0;
    private static final int TEXT = 0xfff1f5f2;
    private static final int MUTED = 0xffa0aaa4;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService transmitter = Executors.newSingleThreadExecutor();
    private final AtomicBoolean transmitting = new AtomicBoolean();
    private ConsumerIrManager ir;
    private SharedPreferences preferences;
    private boolean ready;
    private volatile boolean foreground;
    private volatile int pressSequence;
    private View heldView;
    private String heldKey;
    private float startX, startY;

    private final Runnable repeat = new Runnable() {
        @Override public void run() {
            if (heldKey == null || !foreground) return;
            send(heldKey, true, pressSequence);
            // Every full recorded pattern lasts at most 221 ms. The busy gate
            // prevents overlap, and no work queues up while a button is held.
            handler.postDelayed(this, 240);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("preferences", MODE_PRIVATE);
        ir = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        try {
            ready = ir != null && ir.hasIrEmitter();
            if (ready) {
                ConsumerIrManager.CarrierFrequencyRange[] ranges = ir.getCarrierFrequencies();
                if (ranges != null && ranges.length > 0) {
                    ready = false;
                    for (ConsumerIrManager.CarrierFrequencyRange range : ranges) {
                        if (range.getMinFrequency() <= Wrc03.FREQUENCY
                                && range.getMaxFrequency() >= Wrc03.FREQUENCY) ready = true;
                    }
                }
            }
        } catch (RuntimeException error) { ready = false; }
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = column();
        root.setBackgroundColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        } else root.setFitsSystemWindows(true);
        setContentView(root);

        LinearLayout content = column();
        content.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(content, new LinearLayout.LayoutParams(-1, -1));
        boolean compact = getResources().getConfiguration().screenHeightDp < 580;

        LinearLayout header = row();
        LinearLayout title = column();
        TextView name = text("Quiet Remote", 24, TEXT, true);
        name.setSingleLine(true);
        name.setAutoSizeTextTypeUniformWithConfiguration(14, 24, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        title.addView(name, new LinearLayout.LayoutParams(-1, dp(30)));
        TextView subtitle = text(ready ? "WALTON / WRC03 · IR ready" : "Compatible IR blaster not found", 12,
                ready ? GREEN : 0xffffa29b, false);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.addView(subtitle);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button options = localButton("Options", MUTED);
        options.setContentDescription("Remote preferences and help");
        options.setOnClickListener(v -> showOptions());
        header.addView(options, new LinearLayout.LayoutParams(dp(76), dp(48)));
        content.addView(header, new LinearLayout.LayoutParams(-1, dp(compact ? 48 : 60)));

        // Both pages occupy the same fixed space. Opening numbers replaces
        // navigation; it never adds height or puts controls below the screen.
        ViewFlipper pages = new ViewFlipper(this);
        pages.setBackground(shape(PANEL, 28));
        pages.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout navigation = column();
        addDirectionRow(navigation, new String[]{null, "up", null}, new String[]{"", "▲", ""});
        addDirectionRow(navigation, new String[]{"left", "ok", "right"}, new String[]{"◀", "OK", "▶"});
        addDirectionRow(navigation, new String[]{null, "down", null}, new String[]{"", "▼", ""});
        // Keep the five controls close together at thumb height. Clamp to the
        // actual page bounds as well, so shorter windows still fit without scrolling.
        FrameLayout navigationPage = new FrameLayout(this) {
            @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
                super.onSizeChanged(width, height, oldWidth, oldHeight);
                int side = Math.min(dp(252), Math.min(width, height));
                navigation.setLayoutParams(new FrameLayout.LayoutParams(side, side,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
            }
        };
        navigationPage.addView(navigation, new FrameLayout.LayoutParams(-1, -1));
        pages.addView(navigationPage, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout numbers = column();
        for (int i = 1; i <= 7; i += 3) {
            String[] keys = {String.valueOf(i), String.valueOf(i + 1), String.valueOf(i + 2)};
            addDirectionRow(numbers, keys, keys);
        }
        addDirectionRow(numbers, new String[]{null, "0", null}, new String[]{"", "0", ""});
        pages.addView(numbers, new android.widget.FrameLayout.LayoutParams(-1, -1));
        boolean showNumbers = preferences.getBoolean("numbers", false);
        pages.setDisplayedChild(showNumbers ? 1 : 0);
        Button numbersToggle = localButton(showNumbers ? "Navigation" : "Numbers", GREEN);
        numbersToggle.setContentDescription(showNumbers ? "Show navigation controls" : "Show number keypad");
        numbersToggle.setOnClickListener(v -> {
            cancelHold();
            boolean show = pages.getDisplayedChild() == 0;
            pages.setDisplayedChild(show ? 1 : 0);
            numbersToggle.setText(show ? "Navigation" : "Numbers");
            numbersToggle.setContentDescription(show ? "Show navigation controls" : "Show number keypad");
            preferences.edit().putBoolean("numbers", show).apply();
        });

        LinearLayout actions = compact ? column() : content;
        if (compact) {
            LinearLayout body = row();
            content.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
            body.addView(actions, new LinearLayout.LayoutParams(0, -1, 1));
            LinearLayout.LayoutParams pageParams = new LinearLayout.LayoutParams(0, -1, 1);
            pageParams.setMargins(dp(8), dp(4), 0, dp(4));
            body.addView(pages, pageParams);
        }
        addButtons(actions, new String[]{"power", "input", "menu"},
                new String[]{"Power", "Input", "Menu"}, compact ? 48 : 56);
        addButtons(actions, new String[]{"mute", "back", "home"},
                new String[]{"Mute", "Back", "Home"}, compact ? 48 : 56);
        if (!compact) {
            LinearLayout.LayoutParams pageParams = new LinearLayout.LayoutParams(-1, 0, 1);
            pageParams.setMargins(0, dp(6), 0, dp(6));
            content.addView(pages, pageParams);
        }
        LinearLayout rockers = row();
        addRocker(rockers, "VOLUME", "volume_up", "volume_down");
        addRocker(rockers, "CHANNEL", "channel_up", "channel_down");
        actions.addView(rockers, new LinearLayout.LayoutParams(-1, compact ? 0 : dp(88), compact ? 1 : 0));
        actions.addView(numbersToggle, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private void showOptions() {
        cancelHold();
        new AlertDialog.Builder(this)
                .setTitle("Quiet Remote")
                .setMultiChoiceItems(new String[]{"Button vibration"},
                        new boolean[]{preferences.getBoolean("haptics", true)},
                        (dialog, which, checked) -> preferences.edit().putBoolean("haptics", checked).apply())
                .setNeutralButton("Help", (dialog, which) -> new AlertDialog.Builder(this)
                        .setTitle("Using your remote")
                        .setMessage("Point the top of your Redmi Note 10 Pro toward the TV. "
                                + "Hold volume, channel or arrow buttons to repeat.\n\n"
                                + "WRC03 profile for Walton WD1-JX32-SY200. This app runs entirely offline. "
                                + "IR is one-way: it cannot read the TV's power or volume state.\n\n"
                                + "Settings, Guide, Info and dash had no usable WRC03 signals in the reference app. "
                                + "Use Menu to access your TV settings.\n\nVersion 1.0.3 · Independent app")
                        .setPositiveButton("Done", null).show())
                .setPositiveButton("Done", null).show();
    }

    private void send(String key, boolean isRepeat, int sequence) {
        if (!ready || !foreground || !transmitting.compareAndSet(false, true)) return;
        int[] pattern = Wrc03.pattern(key);
        transmitter.execute(() -> {
            try {
                if (!foreground || (isRepeat && sequence != pressSequence)) return;
                ir.transmit(Wrc03.FREQUENCY, pattern);
            } catch (RuntimeException error) {
                handler.post(() -> {
                    cancelHold();
                    if (foreground) Toast.makeText(this,
                            "IR transmission failed. Please try again.", Toast.LENGTH_SHORT).show();
                });
            } finally { transmitting.set(false); }
        });
    }

    // All buttons retain OnClickListener for keyboard and TalkBack activation;
    // touch repeats deliberately send on DOWN instead of performing a second click.
    @SuppressLint("ClickableViewAccessibility")
    private Button remoteButton(String key, String label) {
        Button button = localButton(label, key.equals("power") ? 0xffffa29b : TEXT);
        button.setContentDescription(description(key));
        button.setEnabled(ready);
        button.setAlpha(ready ? 1f : 0.4f);
        boolean repeats = key.endsWith("_up") || key.endsWith("_down")
                || key.equals("up") || key.equals("down") || key.equals("left") || key.equals("right");
        button.setOnClickListener(v -> {
            haptic(v);
            send(key, false, pressSequence);
        });
        if (repeats) button.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    cancelHold();
                    heldView = v;
                    heldKey = key;
                    startX = event.getX();
                    startY = event.getY();
                    v.setPressed(true);
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    haptic(v);
                    send(key, false, pressSequence);
                    handler.postDelayed(repeat, 400);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int slop = ViewConfiguration.get(this).getScaledTouchSlop();
                    if (Math.abs(event.getX() - startX) > slop || Math.abs(event.getY() - startY) > slop) {
                        cancelHold();
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelHold();
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    // Touch already transmitted on DOWN. performClick is reserved
                    // for keyboard and accessibility activation to avoid double sends.
                    return true;
                default: return true;
            }
        });
        return button;
    }

    private void cancelHold() {
        handler.removeCallbacks(repeat);
        pressSequence++;
        if (heldView != null) heldView.setPressed(false);
        heldView = null;
        heldKey = null;
    }

    private void haptic(View view) {
        if (preferences.getBoolean("haptics", true))
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void addRocker(LinearLayout parent, String label, String up, String down) {
        LinearLayout rocker = column();
        TextView caption = text(label, 11, MUTED, false);
        caption.setGravity(Gravity.CENTER);
        caption.setSingleLine(true);
        caption.setAutoSizeTextTypeUniformWithConfiguration(9, 11, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        rocker.addView(caption, new LinearLayout.LayoutParams(-1, dp(20)));
        LinearLayout line = row();
        Button minus = remoteButton(down, "−");
        Button plus = remoteButton(up, "+");
        minus.setAutoSizeTextTypeUniformWithConfiguration(14, 24, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        plus.setAutoSizeTextTypeUniformWithConfiguration(14, 24, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        for (Button button : new Button[]{minus, plus}) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
            params.setMargins(dp(3), dp(3), dp(3), dp(3));
            line.addView(button, params);
        }
        rocker.addView(line, new LinearLayout.LayoutParams(-1, 0, 1));
        parent.addView(rocker, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void addButtons(LinearLayout parent, String[] keys, String[] labels, int height) {
        LinearLayout line = row();
        for (int i = 0; i < keys.length; i++) {
            Button button = remoteButton(keys[i], labels[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
            params.setMargins(dp(3), dp(4), dp(3), dp(4));
            line.addView(button, params);
        }
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(height)));
    }

    private void addDirectionRow(LinearLayout parent, String[] keys, String[] labels) {
        LinearLayout line = row();
        for (int i = 0; i < keys.length; i++) {
            View view = keys[i] == null ? new View(this) : remoteButton(keys[i], labels[i]);
            if (view instanceof Button) {
                ((Button) view).setAutoSizeTextTypeUniformWithConfiguration(12, 24, 1,
                        android.util.TypedValue.COMPLEX_UNIT_SP);
                boolean selected = "ok".equals(keys[i]);
                GradientDrawable surface = shape(selected ? 0xff315d44 : KEY_SURFACE, 22);
                surface.setStroke(dp(2), selected ? GREEN : KEY_BORDER);
                view.setBackgroundTintList(null);
                view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x4483e5b0), surface, null));
                view.setElevation(0);
                ((Button) view).setTextColor(selected ? GREEN : TEXT);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
            params.setMargins(dp(5), dp(5), dp(5), dp(5));
            line.addView(view, params);
        }
        parent.addView(line, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private Button localButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAutoSizeTextTypeUniformWithConfiguration(10, 16, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        button.setTextColor(color);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(6), 0, dp(6), 0);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3383e5b0), shape(PANEL, 20), null));
        return button;
    }

    private String description(String key) {
        if (key.equals("up") || key.equals("down") || key.equals("left") || key.equals("right"))
            return "Navigate " + key;
        if (key.matches("[0-9]")) return "Number " + key;
        if (key.equals("ok")) return "OK, select";
        if (key.equals("input")) return "Select input source";
        if (key.equals("power")) return "Toggle TV power";
        return key.replace('_', ' ');
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return view;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }
    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }
    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onResume() { super.onResume(); foreground = true; }
    @Override protected void onPause() { foreground = false; cancelHold(); super.onPause(); }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (!focus) cancelHold();
    }
    @Override protected void onDestroy() {
        foreground = false;
        cancelHold();
        handler.removeCallbacksAndMessages(null);
        transmitter.shutdownNow();
        super.onDestroy();
    }
}
