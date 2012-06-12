package lt.asinica.lm.activity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import lt.asinica.lm.R;
import lt.asinica.lm.exceptions.NotLoggedInException;
import lt.asinica.lm.helpers.URLImageParser;
import lt.asinica.lm.objects.LM;
import lt.asinica.lm.objects.Torrent;
import lt.asinica.lm.objects.TorrentComment;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
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
			putCommentsToLL(tComment, null);
		} else {
			commentSize.setText(getResources().getString(R.string.comment_no_comment));
		}
	}

	private void putCommentsToLL(ArrayList<TorrentComment> comments,
			String tag) {
		try {
			LinearLayout ll = (LinearLayout) findViewById(R.id.comment_container);
			for (int i = 0; i < comments.size(); i++) {
				View child = getLayoutInflater().inflate(R.layout.comment_item,
						null);
				LinearLayout divider = (LinearLayout) child.findViewById(R.id.divider);
				TextView name = (TextView) child
						.findViewById(R.id.comment_name);
				name.setText(comments.get(i).getName());
				TextView text = (TextView) child
						.findViewById(R.id.comment_text);
				URLImageParser p = new URLImageParser(text, this);
				Spanned htmlSpan = Html.fromHtml(comments.get(i).getText(), p, null);
				text.setText(htmlSpan);
				TextView date = (TextView) child
						.findViewById(R.id.comment_date);
				date.setText(comments.get(i).getDate());
				TextView karma = (TextView) child
						.findViewById(R.id.comment_karma);
				karma.setText(comments.get(i).getKarma());
				final Button moreButton = (Button) child
						.findViewById(R.id.more_button);
				if (comments.get(i).isMoreComments()) {
					child.setTag(comments.get(i).getCommentId());
					
					final String commentId = comments.get(i).getCommentId();
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
								putCommentsToLL(newComments, commentId);
								moreButton.setEnabled(false);

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

				if (tag == null) {
					divider.setVisibility(View.GONE);
					ll.addView(child);
				} else {
					for(int k = 0; k < ll.getChildCount(); k++){
						if (ll.getChildAt(k).getTag() != null) {
							if (ll.getChildAt(k).getTag().equals(tag)) {
								int count = ((LinearLayout)ll.getChildAt(k).findViewById(R.id.divider)).getChildCount();
								Log.e("LM", "have "+count+" pipes");
								fillDividers(divider, count);
								divider.setVisibility(View.VISIBLE);
								ll.addView(child, k + 1);
								break;
							}
						}
					}
				}
			}
			ll.invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void fillDividers(LinearLayout ll, int k){
		int i = 0;
		do{
			View pipe = new View(this);
			pipe.setLayoutParams(new LayoutParams(5, LayoutParams.FILL_PARENT));
			pipe.setBackgroundColor(Color.TRANSPARENT);
			ll.addView(pipe);
			i++;
		}while(i <= k);
		Log.e("LM", i+" pipes added");
	}
}