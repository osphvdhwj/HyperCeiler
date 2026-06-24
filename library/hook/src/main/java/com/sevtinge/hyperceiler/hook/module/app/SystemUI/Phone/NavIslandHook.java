package com.sevtinge.hyperceiler.hook.module.app.SystemUI.Phone;

import android.content.Context;
import android.view.WindowManager;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

@HookBase(targetPackage = "com.android.systemui", minSdk = 34)
public class NavIslandHook extends BaseModule {

    private NavIslandView mNavIslandView;

    @Override
    public void handleLoadPackage() {
        if (!isMoreHyperOSVersion(2.0f)) {
            XposedLogUtils.logI(TAG, "NavIslandHook skipped: HyperOS version < 2.0");
            return;
        }

        if (!mPrefsMap.getBoolean("nav_island_master_enable")) {
            XposedLogUtils.logI(TAG, "NavIslandHook skipped: Master toggle is disabled");
            return;
        }

        XposedLogUtils.logI(TAG, "NavIslandHook: Successfully initialized on HyperOS 2.0+");

        XposedHelpers.findAndHookMethod("com.android.systemui.SystemUIApplication", mLoadPackageParam.classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.thisObject;
                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                
                if (mNavIslandView == null) {
                    mNavIslandView = new NavIslandView(context, mPrefsMap);
                    mNavIslandView.attachToWindow(windowManager);
                    XposedLogUtils.logI(TAG, "NavIslandHook: Pill injected into WindowManager");
                }
            }
        });

        // 1. Notification Interception & Integration
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.statusbar.phone.HeadsUpManagerPhone", 
                mLoadPackageParam.classLoader, 
                "showNotification", 
                "com.android.systemui.statusbar.notification.collection.NotificationEntry", 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (mPrefsMap.getBoolean("nav_island_notification_interception", true)) {
                            Object entry = param.args[0];
                            Object sbn = XposedHelpers.callMethod(entry, "getSbn");
                            android.app.Notification notification = (android.app.Notification) XposedHelpers.callMethod(sbn, "getNotification");
                            CharSequence tickerText = notification.tickerText;
                            
                            if (mNavIslandView != null && tickerText != null) {
                                mNavIslandView.showNotification(tickerText.toString());
                                // Suppress original headsup by preventing the original method from running
                                param.setResult(null);
                                XposedLogUtils.logI(TAG, "NavIslandHook: Intercepted HeadsUp Notification");
                            }
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedLogUtils.logI(TAG, "NavIslandHook: HeadsUpManagerPhone not found or failed to hook. " + t.getMessage());
        }

        // 2. Per-App Profiles (Monitor Foreground App)
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.systemui.shared.system.ActivityManagerWrapper", 
                mLoadPackageParam.classLoader, 
                "registerTaskStackListener", 
                "com.android.systemui.shared.system.TaskStackChangeListener", 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object listener = param.args[0];
                        XposedHelpers.findAndHookMethod(listener.getClass(), "onTaskStackChangedBackground", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                // Since we don't have the exact package name easily here in background,
                                // we can fetch the top activity component name from ActivityManagerWrapper
                                Object amWrapper = XposedHelpers.callStaticMethod(
                                    XposedHelpers.findClass("com.android.systemui.shared.system.ActivityManagerWrapper", mLoadPackageParam.classLoader), 
                                    "getInstance"
                                );
                                Object runningTaskInfo = XposedHelpers.callMethod(amWrapper, "getRunningTask");
                                if (runningTaskInfo != null && mNavIslandView != null) {
                                    android.content.ComponentName topActivity = (android.content.ComponentName) XposedHelpers.getObjectField(runningTaskInfo, "topActivity");
                                    if (topActivity != null) {
                                        String packageName = topActivity.getPackageName();
                                        // Update UI on main thread
                                        mNavIslandView.post(() -> {
                                            mNavIslandView.updatePerAppProfile(packageName);
                                        });
                                    }
                                }
                            }
                        });
                    }
                }
            );
        } catch (Throwable t) {
            XposedLogUtils.logI(TAG, "NavIslandHook: ActivityManagerWrapper not found or failed to hook. " + t.getMessage());
        }
    }
}
