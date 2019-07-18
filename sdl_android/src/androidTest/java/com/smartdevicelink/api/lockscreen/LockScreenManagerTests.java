package com.smartdevicelink.api.lockscreen;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;

import com.smartdevicelink.proxy.interfaces.ISdl;
import com.smartdevicelink.proxy.rpc.enums.LockScreenStatus;
import com.smartdevicelink.test.Test;

import static org.mockito.Mockito.mock;

/**
 * This is a unit test class for the SmartDeviceLink library manager class :
 * {@link com.smartdevicelink.api.lockscreen.LockScreenManager}
 */
public class LockScreenManagerTests extends AndroidTestCase {

	private LockScreenManager lockScreenManager;

	@Override
	public void setUp() throws Exception{
		super.setUp();

		ISdl internalInterface = mock(ISdl.class);

		Context context = new MockContext();
		// create config
		LockScreenConfig lockScreenConfig = new LockScreenConfig();
		lockScreenConfig.setCustomView(Test.GENERAL_INT);
		lockScreenConfig.setAppIcon(Test.GENERAL_INT);
		lockScreenConfig.setBackgroundColor(Test.GENERAL_INT);
		lockScreenConfig.showDeviceLogo(true);
		lockScreenConfig.setEnabled(true);

		lockScreenManager = new LockScreenManager(lockScreenConfig, context, internalInterface);
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testVariables() {
		assertEquals(Test.GENERAL_INT, lockScreenManager.customView);
		assertEquals(Test.GENERAL_INT, lockScreenManager.lockScreenIcon);
		assertEquals(Test.GENERAL_INT, lockScreenManager.lockScreenColor);
		assertEquals(true, lockScreenManager.deviceLogoEnabled);
		assertEquals(true, lockScreenManager.lockScreenEnabled);
		assertNull(lockScreenManager.deviceLogo);
	}

	public void testGetLockScreenStatus(){
		assertEquals(LockScreenStatus.OFF, lockScreenManager.getLockScreenStatus());
	}

}
