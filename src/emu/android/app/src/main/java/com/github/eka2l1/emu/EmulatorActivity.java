/*
 * Copyright (c) 2020 EKA2L1 Team
 *
 * This file is part of EKA2L1 project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.eka2l1.emu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Process;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.github.eka2l1.R;
import com.github.eka2l1.emu.overlay.VirtualKeyboard;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class EmulatorActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    public static final String APP_UID_KEY = "appUid";
    public static final String APP_NAME_KEY = "appName";

    private Toolbar toolbar;
    private OverlayView overlayView;
    private long uid;
    private boolean launched;
    private VirtualKeyboard keyboard;
    private float displayWidth;
    private float displayHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emulator);
        FrameLayout layout = findViewById(R.id.emulator_container);
        overlayView = new OverlayView(this);
        layout.addView(overlayView);
        SurfaceView surfaceView = findViewById(R.id.surface_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        surfaceView.setWillNotDraw(true);
        surfaceView.getHolder().addCallback(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean keyboardEnabled = sharedPreferences.getBoolean("pref_enable_virtual_keyboard", true);

        if (keyboardEnabled) {
            keyboard = new VirtualKeyboard(this);
            setVirtualKeyboard();
        }

        Intent intent = getIntent();
        uid = intent.getLongExtra(APP_UID_KEY, -1);
        String name = intent.getStringExtra(APP_NAME_KEY);
        setActionBar(name);
        hideSystemUI();

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        displayWidth = display.getWidth();
        displayHeight = display.getHeight();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Ignore it
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        displayWidth = width;
        displayHeight = height;
        updateScreenSize();
        Emulator.surfaceChanged(holder.getSurface(), width, height);
        if (!launched) {
            Emulator.launchApp((int) uid);
            launched = true;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Emulator.surfaceDestroyed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showExitConfirmation();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showExitConfirmation() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(R.string.confirmation_required)
                .setMessage(R.string.force_close_confirmation)
                .setPositiveButton(android.R.string.ok, (d, w) -> Process.killProcess(Process.myPid()))
                .setNegativeButton(android.R.string.cancel, null);
        alertBuilder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (keyboard != null) {
            inflater.inflate(R.menu.emulator, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (keyboard != null) {
            handleVkOptions(id);
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleVkOptions(int id) {
        switch (id) {
            case R.id.action_layout_edit_mode:
                keyboard.setLayoutEditMode(VirtualKeyboard.LAYOUT_KEYS);
                Toast.makeText(this, R.string.layout_edit_mode,
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_layout_scale_mode:
                keyboard.setLayoutEditMode(VirtualKeyboard.LAYOUT_SCALES);
                Toast.makeText(this, R.string.layout_scale_mode,
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_layout_edit_finish:
                keyboard.setLayoutEditMode(VirtualKeyboard.LAYOUT_EOF);
                Toast.makeText(this, R.string.layout_edit_finished,
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_layout_switch:
                showSetLayoutDialog();
                break;
            case R.id.action_hide_buttons:
                showHideButtonDialog();
                break;
        }
    }

    private void showSetLayoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.layout_switch)
                .setSingleChoiceItems(keyboard.getLayoutNames(), -1,
                        (dialogInterface, i) -> keyboard.setLayout(i))
                .setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    private void showHideButtonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.hide_buttons)
                .setMultiChoiceItems(keyboard.getKeyNames(), keyboard.getKeyVisibility(),
                        (dialogInterface, i, b) -> keyboard.setKeyVisibility(i, b))
                .setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    private void setActionBar(String title) {
        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
        actionBar.setTitle(title);
        layoutParams.height = (int) (getToolBarHeight() / 1.5);
    }

    private int getToolBarHeight() {
        int[] attrs = new int[]{R.attr.actionBarSize};
        TypedArray ta = obtainStyledAttributes(attrs);
        int toolBarHeight = ta.getDimensionPixelSize(0, -1);
        ta.recycle();
        return toolBarHeight;
    }

    private void setVirtualKeyboard() {
        int vkAlpha = 0x40000000;
        keyboard.setColor(VirtualKeyboard.BACKGROUND, vkAlpha | 0xD0D0D0);
        keyboard.setColor(VirtualKeyboard.FOREGROUND, vkAlpha | 0x000080);
        keyboard.setColor(VirtualKeyboard.BACKGROUND_SELECTED, vkAlpha | 0x000080);
        keyboard.setColor(VirtualKeyboard.FOREGROUND_SELECTED, vkAlpha | 0xFFFFFF);
        keyboard.setColor(VirtualKeyboard.OUTLINE, vkAlpha | 0xFFFFFF);
        keyboard.setView(overlayView);

        File keylayoutFile = new File(getFilesDir(), "keylayout");
        if (keylayoutFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(keylayoutFile);
                DataInputStream dis = new DataInputStream(fis);
                keyboard.readLayout(dis);
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        keyboard.setLayoutListener(vk -> {
            try {
                FileOutputStream fos = new FileOutputStream(keylayoutFile);
                DataOutputStream dos = new DataOutputStream(fos);
                keyboard.writeLayout(dos);
                fos.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
    }

    private void hideSystemUI() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void updateScreenSize() {
        RectF screen = new RectF(0, 0, displayWidth, displayHeight);
        if (keyboard != null) {
            keyboard.resize(screen, screen);
        }
    }

    private class OverlayView extends View {

        public OverlayView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (keyboard != null) {
                keyboard.paint(canvas);
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            updateScreenSize();
        }

        @Override
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (keyboard != null) {
                        keyboard.show();
                    }
                case MotionEvent.ACTION_POINTER_DOWN:
                    int index = event.getActionIndex();
                    int id = event.getPointerId(index);
                    if ((keyboard == null || !keyboard.pointerPressed(id, event.getX(index), event.getY(index)))
                            && id == 0) {
                        Emulator.touchScreen((int) event.getX(), (int) event.getY(), 0);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    int pointerCount = event.getPointerCount();
                    int historySize = event.getHistorySize();
                    for (int h = 0; h < historySize; h++) {
                        for (int p = 0; p < pointerCount; p++) {
                            id = event.getPointerId(p);
                            if ((keyboard == null || !keyboard.pointerDragged(id, event.getHistoricalX(p, h), event.getHistoricalY(p, h)))
                                    && id == 0) {
                                Emulator.touchScreen((int) event.getHistoricalX(p, h),
                                        (int) event.getHistoricalY(p, h), 1);
                            }
                        }
                    }
                    for (int p = 0; p < pointerCount; p++) {
                        id = event.getPointerId(p);
                        if ((keyboard == null || !keyboard.pointerDragged(id, event.getX(p), event.getY(p)))
                                && id == 0) {
                            Emulator.touchScreen((int) event.getX(p), (int) event.getX(p), 1);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (keyboard != null) {
                        keyboard.hide();
                    }
                case MotionEvent.ACTION_POINTER_UP:
                    index = event.getActionIndex();
                    id = event.getPointerId(index);
                    if ((keyboard == null || !keyboard.pointerReleased(id, event.getX(index), event.getY(index)))
                            && id == 0) {
                        Emulator.touchScreen((int) event.getX(), (int) event.getY(), 2);
                    }
                    break;
                default:
                    return false;
            }
            return true;
        }
    }
}