package lt.asinica.lm.objects;

public class TorrentComment {
	private String name;
	private String text;
	private String date;
	private String photoUrl; // Dont think that will be necessary
	private String karma;
	private String commentId;
	
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
}
