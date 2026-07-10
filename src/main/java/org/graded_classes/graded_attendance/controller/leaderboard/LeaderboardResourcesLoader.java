

package org.graded_classes.graded_attendance.controller.leaderboard;

import java.io.InputStream;
import java.net.URL;

public class LeaderboardResourcesLoader {

	private LeaderboardResourcesLoader() {
	}

	public static URL loadURL(String path) {
		return LeaderboardResourcesLoader.class.getResource(path);
	}

	public static String load(String path) {
		return loadURL(path).toString();
	}

	public static InputStream loadStream(String name) {
		return LeaderboardResourcesLoader.class.getResourceAsStream(name);
	}

}
