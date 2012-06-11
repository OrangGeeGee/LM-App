package lt.asinica.lm.objects;

import java.util.ArrayList;

public class TorrentComment {
	private String name;
	private String text;
	private String date;
	private String photoUrl; // Dont think that will be necessary
	private String karma;
	private String commentId;
	private boolean moreComments;
	private ArrayList<TorrentComment> moreTorrents;
	private boolean expanded = false;
	
	public TorrentComment() {
		super();
	}
	public TorrentComment(String name, String text, String date) {
		super();
		this.name = name;
		this.text = text;
		this.date = date;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getPhotoUrl() {
		return photoUrl;
	}
	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}
	
	public String getKarma() {
		return karma;
	}
	public void setKarma(String karma) {
		this.karma = karma;
	}
	public String toString(){
		return name+"/"+date+"/"+text+"/"+karma+"/"+photoUrl;
	}
	public String getCommentId() {
		if(commentId != null){
			return commentId;
		} else {
			return "";
		}
	}
	public void setCommentId(String commentId) {
		this.commentId = commentId;
	}
	public boolean isMoreComments() {
		return moreComments;
	}
	public void setMoreComments(boolean moreComments) {
		this.moreComments = moreComments;
	}
	public ArrayList<TorrentComment> getMoreTorrents() {
		return moreTorrents;
	}
	public void setMoreTorrents(ArrayList<TorrentComment> moreTorrents) {
		this.moreTorrents = moreTorrents;
	}
	public boolean isExpanded() {
		return expanded;
	}
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}
	
}
