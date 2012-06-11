package lt.asinica.lm.objects;

import java.util.ArrayList;
import java.util.StringTokenizer;

import lt.asinica.lm.Const;

import org.jsoup.select.Elements;

import android.util.Log;

import com.google.gson.Gson;

public class TorrentCommentUtils {
	private static boolean parseOutReplied(String lf) {
		if (lf.contains("expand")) {
			return true;
		}
		return false;
	}

	private static String parseOutId(String lf) {
		String parsedPrefix = takeToken(2, "(", lf);
		String parsedSuffix = takeToken(0, ")", parsedPrefix);
		return parsedSuffix;
	}

	/**
	 * Tokenize string and take token number i
	 * 
	 * @param i
	 *            - number of token to take
	 * @param delim
	 *            - token delimiter
	 * @param lf
	 *            - string to look into
	 * @return - Delimited string
	 */
	private static String takeToken(int i, String delim, String lf) {
		StringTokenizer tokens = new StringTokenizer(lf, delim);
		int c = 0;
		String needed;
		while (tokens.hasMoreTokens()) {
			needed = tokens.nextToken();
			if (c == i) {
				return needed;
			}
			c++;
		}
		return null;
	}

	public static String parseComments(Elements cRows) {
		// Parsing elements and putting them to json format to read
		// Šioje vietoje galima buvo apseiti ir be papildomo lib'o, kas
		// sumažintų vietą pačio apso,
		// tačiau niekad nedariau tokiu būdu, tad pačiam buvo įdomu ar suveiks.

		ArrayList<TorrentComment> tComments = new ArrayList<TorrentComment>();
		try {

			Log.e(Const.LOG, "We have " + cRows.size() + " elements");
			for (int i = 0; i < cRows.size(); i++) {
				if (cRows.get(i) != null) {
					String name = cRows.get(i)
							.getElementsByClass("comment-user").text();
					String text = cRows.get(i)
							.getElementsByClass("comment-text").html();
					String karma = cRows.get(i)
							.getElementsByClass("comment-balance").text();
					String avatar = cRows.get(i)
							.getElementsByClass("comment-avatar").html();
					Elements commentId = cRows.get(i).getElementsByClass(
							"comment-actions");

					if (!name.equals("")) {
						TorrentComment tCom = new TorrentComment();
						String[] tmp = name.split(",");
						tCom.setName(tmp[0]);
						tCom.setDate(tmp[1]);
						tCom.setText(text);
						tCom.setKarma(karma);
						tCom.setPhotoUrl(avatar);
						tCom.setCommentId(parseOutId(commentId.html()));
						tCom.setMoreComments(parseOutReplied(commentId.html()));
						tComments.add(tCom);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		Gson gson = new Gson();
		String jsoned = gson.toJson(tComments);
		return jsoned;
		// Log.e(Const.LOG, "Parsed elements: "+jsoned);
	}
}
