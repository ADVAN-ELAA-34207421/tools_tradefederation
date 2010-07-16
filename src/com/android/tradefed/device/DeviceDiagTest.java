/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tradefed.device;

import com.android.ddmlib.Log;
import com.android.tradefed.util.RunUtil;

import java.util.Collection;

import junit.framework.TestCase;

/**
 * A test that diagnoses the devices available to run tests.
 * <p/>
 * Intended to be used when setting up a machine to run TradeFederation, to verify that all
 * device's are operating correctly.
 */
public class DeviceDiagTest extends TestCase {

    private static final String LOG_TAG = "DeviceDiagTest";

    /**
     * Queries the {@link DeviceManager} to verify all visible devices are available for testing.
     */
    public void testAllDevicesAvailable() {
        Collection<String> unavailDevices = DeviceManager.getInstance().getUnavailableDevices();
        for (int i=0; i < 5 && unavailDevices.size() > 0; i++) {
            Log.i(LOG_TAG, "Unavailable devices detected, sleeping and polling");
            RunUtil.getInstance().sleep(1*1000);
            unavailDevices = DeviceManager.getInstance().getUnavailableDevices();
        }
        for (String device : unavailDevices) {
            System.out.println(String.format(
                    "Device %s is not available for testing. Check that package manager is up " +
                    "and sdcard is mounted.", device));
            // TODO: add more specific hints
        }
        assertEquals("Not all devices are available", 0, unavailDevices.size());
    }
}