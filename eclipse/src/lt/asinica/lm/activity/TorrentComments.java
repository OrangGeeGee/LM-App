package lt.asinica.lm.activity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import lt.asinica.lm.R;
import lt.asinica.lm.exceptions.NotLoggedInException;
import lt.asinica.lm.objects.LM;
import lt.asinica.lm.objects.Torrent;
import lt.asinica.lm.objects.TorrentComment;
import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class TorrentComments extends Activity {
	// public void onCreate(Bundle savedInstanceState) {
	// super.onCreate(savedInstanceState);
	// setContentView(R.layout.torrentcomments);
	// }

	private Torrent mTorrent;
	private Gson gson;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.torrentcomments);

		// get info from intent
		mTorrent = new Torrent(getIntent().getExtras().getBundle("torrent"));
		gson = new Gson();

		Type collectionType = new TypeToken<ArrayList<TorrentComment>>() {
		}.getType();
		ArrayList<TorrentComment> tComment = gson.fromJson(
				mTorrent.getComments(), collectionType);

		TextView commentSize = (TextView) findViewById(R.id.comment_size);
		if (tComment != null) {
			commentSize.setText(getResources().getString(
					R.string.comment_header)
					+ tComment.size());
			// for(int i = 0; i < tComment.size(); i++){
			// Log.e(Const.LOG, ""+tComment.get(i).toString());
			// }
			LinearLayout ll = (LinearLayout) findViewById(R.id.comment_container);
			putCommentsToLL(tComment, ll);
		} else {
			commentSize.setText(getResources().getString(R.string.comment_no_comment));
		}
	}

	private void putCommentsToLL(ArrayList<TorrentComment> comments,
			LinearLayout container) {
		try {
			LinearLayout ll = container;
			ll.removeAllViews();
			for (int i = 0; i < comments.size(); i++) {
				View child = getLayoutInflater().inflate(R.layout.comment_item,
						null);

				TextView name = (TextView) child
						.findViewById(R.id.comment_name);
				name.setText(comments.get(i).getName()
						+ comments.get(i).getCommentId());
				TextView text = (TextView) child
						.findViewById(R.id.comment_text);
				text.setText((Html.fromHtml(comments.get(i).getText())));
				TextView date = (TextView) child
						.findViewById(R.id.comment_date);
				date.setText(comments.get(i).getDate());
				TextView karma = (TextView) child
						.findViewById(R.id.comment_karma);
				karma.setText(comments.get(i).getKarma());
				Button moreButton = (Button) child
						.findViewById(R.id.more_button);
				if (comments.get(i).isMoreComments()) {
					final String commentId = comments.get(i).getCommentId();
					final LinearLayout moreLL = (LinearLayout) child
							.findViewById(R.id.more_comments);
					final int ii = i;
					moreButton.setVisibility(View.VISIBLE);
					moreButton.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View arg0) {
							try {
								String commentJson = LM.getInstance()
										.getMoreComments(mTorrent, commentId);
								Type collectionType = new TypeToken<ArrayList<TorrentComment>>() {
								}.getType();
								ArrayList<TorrentComment> newComments = gson
										.fromJson(commentJson, collectionType);
								putCommentsToLL(newComments, moreLL);

							} catch (NotLoggedInException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				} else {
					moreButton.setVisibility(View.GONE);
				}

				ll.addView(child);
			}
			ll.invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}