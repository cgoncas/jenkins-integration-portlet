/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ci.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.liferay.ci.jenkins.vo.JenkinsJob;
import com.liferay.ci.jenkins.vo.JenkinsUnstableJob;
import com.liferay.ci.portlet.JenkinsIntegrationConstants;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;

/**
 * 
 * @author Manuel de la Peña
 */
public class JenkinsConnectUtil {

	public static JSONArray getBuilds(
			AuthConnectionParams connectionParams, String jobName,
			int maxNumber)
		throws IOException, JSONException {

		JSONObject json = getJob(connectionParams, jobName);

		JSONArray builds = (JSONArray)json.get("builds");

		JSONArray result = new JSONArray();

		int end = builds.length();

		if ((maxNumber > 0) && (maxNumber < end)) {
			end = maxNumber;
		}

		for (int i = 0; i < end; i++) {
			JSONObject build = (JSONObject)builds.get(i);

			try {
				JSONObject testReport = getBuildTestReport(
					connectionParams, build);

				testReport.append("buildNumber", build.getInt("number"));

				result.put(testReport);
			}
			catch(FileNotFoundException fnfe) {
				_log.warn(
					"The build " + build.getInt("number") + " is not present",
					fnfe);
			}
		}

		return result;
	}

	public static String getLastBuildStatus(
			AuthConnectionParams connectionParams, String jobName)
		throws IOException, JSONException {

		JSONObject json = getJob(connectionParams, jobName);

		JSONObject build = (JSONObject)json.get("lastBuild");

		String result = StringPool.BLANK;

		try {
			result = getService(connectionParams).getLastBuildStatus(build);
		}
		catch(FileNotFoundException fnfe) {
			_log.warn(
				"The build " + build.getInt("number") + " is not present",
				fnfe);
		}

		return result;
	}

	public static JenkinsJob[] getLastBuildStatuses(
			AuthConnectionParams connectionParams, String... jobNames)
		throws IOException, JSONException {

		JenkinsJob[] result = new JenkinsJob[jobNames.length];

		for (int i = 0; i < jobNames.length; i++) {
			String jobName = jobNames[i];

			String lastBuildStatus = getLastBuildStatus(
				connectionParams, jobName);

			if (lastBuildStatus.equals(
				JenkinsIntegrationConstants.JENKINS_BUILD_STATUS_UNSTABLE)) {

				// retrieve number of broken tests for last build

				JSONArray testResults = getBuilds(connectionParams, jobName, 1);

				JSONObject testResult = (JSONObject)testResults.get(0);

				result[i] = new JenkinsUnstableJob(
					jobName, lastBuildStatus, testResult.getInt("failCount"));
			}
			else {
				result[i] = new JenkinsJob(jobName, lastBuildStatus);
			}
		}

		// sort jobs by status

		Arrays.sort(result);

		return result;
	}

	private JenkinsConnectUtil() {
	}

	private static JSONObject getBuildTestReport(
			AuthConnectionParams connectionParams, JSONObject build)
		throws IOException, JSONException {

		return getService(connectionParams).getBuildTestReport(build);
	}

	private static JSONObject getJob(
			AuthConnectionParams connectionParams, String jobName)
		throws IOException, JSONException {

		return getService(connectionParams).getJob(jobName);
	}

	private static JenkinsConnectImpl getService(
			AuthConnectionParams connectionParams)
		throws IOException {

		if (_service == null) {
			_service = new JenkinsConnectImpl();
		}

		_service.setAuthConnectionParams(connectionParams);

		return _service;
	}

	private static JenkinsConnectImpl _service;
	private static Log _log = LogFactoryUtil.getLog(JenkinsConnectUtil.class);

}