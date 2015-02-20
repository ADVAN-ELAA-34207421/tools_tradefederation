/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.tradefed.targetprep;

import com.android.ddmlib.FileListingService;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.build.IDeviceBuildInfo;
import com.android.tradefed.config.Option;
import com.android.tradefed.config.Option.Importance;
import com.android.tradefed.config.OptionClass;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.util.ArrayUtil;
import com.android.tradefed.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A {@link ITargetPreparer} that pushes one or more files/dirs from a
 * {@link IDeviceBuildInfo#getTestsDir()} folder onto device.
 *
 */
@OptionClass(alias = "tests-zip-file")
public class TestFilePushSetup implements ITargetPreparer {

    @Option(name = "test-file-name", description =
            "the relative path of a test zip file/directory to install on device. Can be repeated.",
            importance = Importance.IF_UNSET)
    private Collection<String> mTestPaths = new ArrayList<String>();

    @Option(name = "throw-if-not-found", description =
            "Throw exception if the specified file is not found.")
    private boolean mThrowIfNoFile = true;

    /**
     * Adds a file to the list of items to push
     *
     * Used for unit testing
     *
     * @param fileName
     */
    void addTestFileName(String fileName) {
        mTestPaths.add(fileName);
    }

    /**
     * Resolve the host side path based on testing artifact information inside build info.
     *
     * @param buildInfo build artifact information
     * @param fileName filename of artifacts to push
     * @return a {@link File} representing the physical file/path on host
     */
    protected File getLocalPathForFilename(IBuildInfo buildInfo, String fileName)
            throws TargetSetupError {
        File testsDir = ((IDeviceBuildInfo)buildInfo).getTestsDir();
        if (testsDir == null || !testsDir.exists()) {
            throw new TargetSetupError(
                    "Provided buildInfo does not contain a valid tests directory");
        }
        return FileUtil.getFileForPath(testsDir, "DATA", fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp(ITestDevice device, IBuildInfo buildInfo) throws TargetSetupError,
            BuildError, DeviceNotAvailableException {
        if (!(buildInfo instanceof IDeviceBuildInfo)) {
            throw new IllegalArgumentException(String.format("Provided buildInfo is not a %s",
                    IDeviceBuildInfo.class.getCanonicalName()));
        }
        if (mTestPaths.size() == 0) {
            CLog.d("No test files to push, skipping");
            return;
        }
        int filePushed = 0;
        for (String fileName : mTestPaths) {
            File localFile = getLocalPathForFilename(buildInfo, fileName);
            if (!localFile.exists()) {
                if (mThrowIfNoFile) {
                    throw new TargetSetupError(String.format(
                            "Could not find test file %s directory in extracted tests.zip",
                            localFile));
                } else {
                    continue;
                }
            }
            fileName = getDevicePathFromUserData(fileName);
            CLog.d("Pushing file: %s -> %s", localFile.getAbsoluteFile(), fileName);
            if (localFile.isDirectory()) {
                device.pushDir(localFile, fileName);
            } else if (localFile.isFile()) {
                device.pushFile(localFile, fileName);
            }
            // there's no recursive option for 'chown', best we can do here
            device.executeShellCommand(String.format("chown system.system %s", fileName));
            filePushed++;
        }
        if (filePushed == 0) {
            throw new TargetSetupError("No file is pushed from tests.zip");
        }
    }

    static String getDevicePathFromUserData(String path) {
        return ArrayUtil.join(FileListingService.FILE_SEPARATOR,
                "", FileListingService.DIRECTORY_DATA, path);
    }
}
