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
    }
}
