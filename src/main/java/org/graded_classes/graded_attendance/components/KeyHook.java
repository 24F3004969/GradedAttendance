package org.graded_classes.graded_attendance.components;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.*;

public class KeyHook {

    private static HHOOK hhk;
    private static LowLevelKeyboardProc keyboardHook;
    private static User32 lib;

    private static Thread hookThread;
    private static int hookThreadId;

    public static void blockWindowsKey() {
        if (!isWindows()) return;

        // Prevent multiple threads
        if (hookThread != null && hookThread.isAlive()) {
            return;
        }

        hookThread = new Thread(() -> {
            lib = User32.INSTANCE;

            // Store thread ID
            hookThreadId = Kernel32.INSTANCE.GetCurrentThreadId();

            HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);

            keyboardHook = (nCode, wParam, info) -> {
                if (nCode >= 0) {
                    switch (info.vkCode) {
                        case 0x1B: // ESC
                        case 0x5B: // Left Windows key
                        case 0x5C: // Right Windows key
                            return new LRESULT(1); // block key
                        default:
                            // do nothing
                    }
                }

                return lib.CallNextHookEx(
                        hhk,
                        nCode,
                        wParam,
                        new LPARAM(Pointer.nativeValue(info.getPointer()))
                );
            };

            // WH_KEYBOARD_LL = 13
            hhk = lib.SetWindowsHookEx(13, keyboardHook, hMod, 0);

            MSG msg = new MSG();
            int result;

            // Message loop
            while ((result = lib.GetMessage(msg, null, 0, 0)) != 0) {
                if (result == -1) {
                    break;
                } else {
                    lib.TranslateMessage(msg);
                    lib.DispatchMessage(msg);
                }
            }

            // Cleanup
            if (hhk != null) {
                lib.UnhookWindowsHookEx(hhk);
                hhk = null;
            }
        });

        hookThread.setDaemon(true); // optional but recommended
        hookThread.start();
    }

    public static void unblockWindowsKey() {
        if (!isWindows() || lib == null) return;

        // Remove hook
        if (hhk != null) {
            lib.UnhookWindowsHookEx(hhk);
            hhk = null;
        }

        // Stop message loop using WM_QUIT
        if (hookThreadId != 0) {
            User32.INSTANCE.PostThreadMessage(
                    hookThreadId,
                    WinUser.WM_QUIT,
                    null,
                    null
            );
            hookThreadId = 0;
        }

        hookThread = null;
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
}