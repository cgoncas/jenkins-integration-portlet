package com.liferay.ci.http;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public class JenkinsConnectUtil {

	public static JSONArray getBuilds(String jobName, int maxNumber)
		throws IOException, JSONException {

		JSONObject json = getJob(jobName);

		JSONArray builds = (JSONArray)json.get("builds");

		JSONArray result = new JSONArray();

		int end = builds.length() - 1;

		if ((maxNumber > 0) && (maxNumber < end)) {
			end = maxNumber;
		}

		for (int i = 0; i <= end; i++) {
			JSONObject build = (JSONObject)builds.get(i);

			try {
				JSONObject testReport = getBuildTestReport(build);
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

	public static void setAuthConfiguration(String user, String password)
		throws IOException {

		getService().setAuthConfiguration(user, password);
	}

	public static void setJenkinsBaseApiURL(String baseApiURL)
		throws IOException {

		getService().setBaseAPIURL(baseApiURL);
	}

	private JenkinsConnectUtil() {
	}

	private static JSONObject getBuildTestReport(JSONObject build)
		throws IOException, JSONException {

		return getService().getBuildTestReport(build);
	}

	private static JSONObject getJob(String jobName)
		throws IOException, JSONException {

		return getService().getJob(jobName);
	}

	private static JenkinsConnectImpl getService() throws IOException {
		if (_service == null) {
			_service = new JenkinsConnectImpl();
		}

		return _service;
	}

	private static JenkinsConnectImpl _service;
	private static Log _log = LogFactoryUtil.getLog(JenkinsConnectUtil.class);

}