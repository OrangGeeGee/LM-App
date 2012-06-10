package lt.asinica.lm.objects;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import lt.asinica.lm.Const;
import lt.asinica.lm.R;
import lt.asinica.lm.activity.TorrentDescriptionTabs;
import lt.asinica.lm.exceptions.ExternalStorageNotAvaliableException;
import lt.asinica.lm.exceptions.InvalidTokenException;
import lt.asinica.lm.exceptions.NotLoggedInException;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;


public class Torrent {
	
	private Bundle mInfo = new Bundle(15);
	// class vars
	/*private String id;
	private String fileName;
	private String title;
	private boolean freeLeech = false;
	private boolean newTorrent = false;
	private String descriptionUrl;
	private String categoryUrl;
	private String category;
	private String downloadUrl;
	private String dateAdded;
	private String size;
	private String seeders;
	private String leechers;
	private String fullDescription;*/
	private static String baseUrl = "http://www.linkomanija.net/";
	// getters
	public String getId() { return mInfo.getString("id"); }
	public String getFileName() { return mInfo.getString("fileName"); }
	public String getTitle() { return mInfo.getString("title"); }
	public boolean isFreeLeech() { return mInfo.getBoolean("freeLeech"); }
	public boolean isNew() { return mInfo.getBoolean("new"); }
	public String getDescriptionUrl() { return baseUrl+mInfo.getString("descriptionUrl"); }
	public String getCategory() { return mInfo.getString("category"); }
	public String getCategoryName() { return LM.getInstance().getCategories().get(Integer.parseInt( mInfo.getString("category") )).getName(); }
	public String getCategoryUrl() { return baseUrl+mInfo.getString("categoryUrl"); }
	public String getDownloadUrl() { return baseUrl+mInfo.getString("downloadUrl"); }
	public String getDateAdded() { return mInfo.getString("dateAdded"); }
	public String getSize() { return mInfo.getString("size"); }
	public String getSeeders() { return mInfo.getString("seeders"); }
	public String getLeechers() { return mInfo.getString("leechers"); }
	public String getFullDescription() { return mInfo.getString("fullDescription"); }
	public String getComments() {return mInfo.getString(Const.COMMENT_STRING); }
	public String getUploadedBy() { return mInfo.getString("uploadedBy"); }
	public String getFileCount() { return mInfo.getString("fileCount"); }
	public String getThankYous() { return mInfo.getString("thankYous"); }
	public boolean getThanked() { return mInfo.getBoolean("thanked"); }
	
	public Torrent() { }
	public Torrent(Bundle informationBundle) {
		mInfo = informationBundle;
	}
	public Torrent(Element row) {
		parseTableRow(row);
	}
	
	public void parseTableRow(Element row) {
		try {			
			Elements cells = row.select("td");
			
			// title and link
			Element titleCell = cells.get(1);
			Element anchor = titleCell.select("a").first();
			mInfo.putString("title", anchor.text());
			mInfo.putString("descriptionUrl", anchor.attributes().get("href"));
			// downlaod url
			String downloadUrl = titleCell.select("a.index").first().attributes().get("href");
			mInfo.putString("downloadUrl", downloadUrl);
			// id and file name
			try {
				URI u = new URI(baseUrl+downloadUrl);
				List<NameValuePair> pieces = URLEncodedUtils.parse(u, "UTF-8");
				NameValuePair pair = pieces.get(0);
				mInfo.putString("id", pair.getValue());
				pair = pieces.get(1);
				mInfo.putString("fileName", pair.getValue());
			} catch (Exception e) {
				Log.e("DEBUG", "Failed to parse download URL into id and filename strings. "+e.getMessage());
				e.printStackTrace();
			}
			// free leech
			mInfo.putBoolean("freeLeech", titleCell.select("a:not(.index) img").size()>0 );
			// new
			mInfo.putBoolean("new", titleCell.select("font").size()>0 );
			//<font color="red"
				
			// category
			String categoryUrl = cells.get(0).select("a").first().attributes().get("href");
			mInfo.putString("categoryUrl", categoryUrl);
			mInfo.putString("category", categoryUrl.substring(categoryUrl.lastIndexOf("=") + 1));
			// Date
			mInfo.putString("dateAdded", cells.get(4).text());
			// Size
			mInfo.putString("size", cells.get(5).text());
			// Seeders
			mInfo.putString("seeders", cells.get(7).text());
			// Leechers
			mInfo.putString("leechers", cells.get(8).text());
		} catch (NullPointerException e) {
			Log.e("DEBUG", "Can't parse torrent data to an object. "+e.getMessage());
			e.printStackTrace();
		}
	}
	public void parseTorrentInfo(Document doc) {
    	try {
    		Elements rows = doc.select("#content table:not(.main) > tr");
	    	// apraymo tr > antras td
	    	int freeLeechOffset = isFreeLeech() ? 1 : 0; 
	    	int nfoOffset = rows.get(3 + freeLeechOffset).select("td").get(0).text().contains("NFO") ? 1 : 0;
	    	
	    	mInfo.putString("fullDescription", rows.get(1 + freeLeechOffset).select("td").get(1).html());
	    	// uploaded by tr right after descrip
	    	Element row = rows.get(3 + freeLeechOffset + nfoOffset);
	    	Elements elems = row.select("td");
	    	Element cell = elems.get(1);
	    	
	    	mInfo.putString("uploadedBy", cell.text());
	    	// file count goes after uploaded by
	    	mInfo.putString("fileCount", rows.get(4 + freeLeechOffset + nfoOffset).select("td").get(1).ownText());
	    	// thank yous in the bottom
	    	mInfo.putString("thankYous", rows.select("#padekos").get(0).text());
	    	mInfo.putBoolean("thanked", rows.select("#padekoti input").size() == 0);
	    	// For testing purposes of parsing comments
	    	Elements commRows = doc.select("#comments");
	    	
	    	// Parsing user comments
	    	mInfo.putString(Const.COMMENT_STRING, parseComments(commRows.get(0).children()));
	    	String lfId = "2261854";
	    	URI url = new URI("http://www.linkomanija.net/ajax/getreplies.php");
	    	Document doc2 = Jsoup.connect(url.toASCIIString())
	    			
	    			.data("id", "2261854")
	    			.data("details", "218126")
	    			.userAgent("Mozilla")
	    			.post(); 
//	    	Document doc2 = Jsoup.connect("http://www.linkomanija.net/ajax/getreplies.php?id="+lfId+"&details="+getId())
//	    			  // and other fields which are being passed in post request.
//	    			  .userAgent("Mozilla")
//	    			  .get(); 
	    	Log.e("TEST", doc2.html());
//	    	Log.e("LM_DEBUG", comm_rows.get(0).html());
	    			//get(6 + freeLeechOffset).select("td").get(1).ownText());
	    	// TODO find uploaded_by, file_count, download_count, thank_yous 
    	} catch (Exception e) { 
    		Log.e("DEBUG", "Can not parse document to torrent info. "+e.getMessage());
    		e.printStackTrace();
    	}		
	}
	private String parseComments(Elements cRows){
		// Parsing elements and putting them to json format to read
		// Šioje vietoje galima buvo apseiti ir be papildomo lib'o, kas sumažintų vietą pačio apso, 
		// tačiau niekad nedariau tokiu būdu, tad pačiam buvo įdomu ar suveiks.
		
		try {
			ArrayList<TorrentComment> tComments = new ArrayList<TorrentComment>();
			
			Log.e(Const.LOG, "We have "+cRows.size()+" elements");
//		Log.e(Const.LOG, "We have "+cRows.get(0).html()+" elements");
			for(int i = 0; i < cRows.size(); i++){
				if(cRows.get(i) != null){
					String name = cRows.get(i).getElementsByClass("comment-user").text();
					String text = cRows.get(i).getElementsByClass("comment-text").html();
					String karma = cRows.get(i).getElementsByClass("comment-balance").text();
					String avatar = cRows.get(i).getElementsByClass("comment-avatar").html();
					if (!name.equals("")) {
						TorrentComment tCom = new TorrentComment();
						String[] tmp = name.split(",");
						tCom.setName(tmp[0]);
						tCom.setDate(tmp[1]);
						tCom.setText(text);
						tCom.setKarma(karma);
						tCom.setPhotoUrl(avatar);
						tComments.add(tCom);
					}
				}
			}
			Gson gson = new Gson();
			String jsoned = gson.toJson(tComments);
			return jsoned;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
//		Log.e(Const.LOG, "Parsed elements: "+jsoned);
	}
	public void view(Activity context) {
		Intent intent = new Intent(context, TorrentDescriptionTabs.class);
		intent.putExtra("torrent", toBundle());
		context.startActivity(intent);
	}
	
	public void open(Activity context) {
		String str = String.format( context.getString(R.string.lm_file_downloading), getFileName() );
		final ProgressDialog progressDialog = ProgressDialog.show(context,    
  	          context.getString(R.string.please_wait), str, true);
		final Activity cntxt = context;
		
	    Runnable openTorrents = new Runnable() {
	        @Override
	        public void run() {
	        	String error = null;
				try {
					final File torrent = LM.getInstance().downloadFile(Torrent.this);
					progressDialog.dismiss();
					
					final String msg = String.format( cntxt.getString(R.string.lm_file_downloaded_prompt), torrent.getAbsolutePath() );
					
					cntxt.runOnUiThread(new Runnable() { public void run() {					
						new AlertDialog.Builder(cntxt)
					        .setTitle(R.string.lm_file_downloaded)
					        .setMessage(msg)
					        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					            public void onClick(DialogInterface dialog, int which) {
									Intent intent = new Intent();
						    		intent.setAction(android.content.Intent.ACTION_VIEW);
						    		intent.setDataAndType(Uri.fromFile(torrent), "application/x-bittorrent");
						    		try {
						    			cntxt.startActivity(intent);
						    		} catch(ActivityNotFoundException e) {
						    			Toast.makeText(cntxt, R.string.no_application_associated, Toast.LENGTH_LONG).show();
						    			Log.e("DEBUG", "Activity associated with this file type not found");
						    		}
					            }
					        })
					        .setNegativeButton(R.string.no, null)
					        .show();
					}});
				} catch (MalformedURLException e) {
					String str = "While opening torrent MalformedURLException. "+e.getMessage();
					Log.e("DEBUG", str);
					error = str;
					e.printStackTrace();
				} catch (ExternalStorageNotAvaliableException e) {
					String str = cntxt.getString(R.string.sd_card_not_avaliable)+" "+e.getMessage();
					Log.e("DEBUG", "External storage not avaliable. "+e.getMessage());
					error = str;
					e.printStackTrace();
				} catch (NotLoggedInException e) {
					error = cntxt.getString(R.string.lm_not_logged_in);
					e.printStackTrace();
				} catch (IOException e) {
					error = cntxt.getString(R.string.lm_no_connectivity)+" "+e.getMessage();
					e.printStackTrace();
				} finally {
					progressDialog.dismiss();
				}
				if(error!=null) {
					Log.e("DEBUG", error);
					final String err = error;
					cntxt.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(cntxt, err, Toast.LENGTH_LONG).show();
						}
					});
				}
	        }
	    };
	    
	    Thread thread =  new Thread(null, openTorrents, "Torrent Opener Thread");
	    thread.start();
	}
	public void sendToUTorrent(Activity context) {
		String str = String.format( context.getString(R.string.ut_send_in_progress), getFileName() );
		final ProgressDialog progressDialog = ProgressDialog.show(context,    
	  	          context.getString(R.string.please_wait), str, true);
		final Activity cntxt = context;
		
	    Runnable sendTorrents = new Runnable() {
	        @Override
	        public void run() {
	        	String msg = null;
				try {
					UTorrent ut = UTorrent.getInstance();
					String cookie = "login="+LM.getInstance().getSecret();
					
					ut.addTorrent(getDownloadUrl(), cookie);
					msg = String.format( cntxt.getString(R.string.ut_send_success), ut.getLabel() );
				} catch (InvalidTokenException e) {
					msg = cntxt.getString(R.string.ut_unexpected_response);
					e.printStackTrace();
				} catch (NotLoggedInException e) {
					msg = cntxt.getString(R.string.lm_not_logged_in);
					e.printStackTrace();
				} catch (Exception e) {
					msg = cntxt.getString(R.string.ut_cant_connect) + " " + e.getMessage();
					e.printStackTrace();
				} finally {
					progressDialog.dismiss();
				}
				if(msg!=null) {
					Log.i("DEBUG", msg);
					final String m = msg;
					cntxt.runOnUiThread(new Runnable() { public void run() {
						Toast.makeText(cntxt, m, Toast.LENGTH_LONG).show();
					}});
				}
	        }
	    };
	    
	    Thread thread =  new Thread(null, sendTorrents, "Torrent Sender Thread");
	    thread.start();		
	}
	public void sendToTransmission(Activity context) {
		String str = String.format( context.getString(R.string.tr_send_in_progress), getFileName() );
		final ProgressDialog progressDialog = ProgressDialog.show(context,    
	  	          context.getString(R.string.please_wait), str, true);
		final Activity cntxt = context;
		
	    Runnable sendTorrents = new Runnable() {
	        @Override
	        public void run() {
	        	String msg = null;
				try {
					Transmission tr = Transmission.getInstance();
					String cookie = "login="+LM.getInstance().getSecret();
					
					tr.addTorrent(getDownloadUrl(), cookie);
					msg = String.format( cntxt.getString(R.string.tr_send_success), tr.getLabel() );
				} catch (InvalidTokenException e) {
					msg = cntxt.getString(R.string.tr_unexpected_response);
					e.printStackTrace();
				} catch (NotLoggedInException e) {
					msg = cntxt.getString(R.string.lm_not_logged_in);
					e.printStackTrace();
				} catch (Exception e) {
					msg = cntxt.getString(R.string.tr_cant_connect) + " " + e.getMessage();
					e.printStackTrace();
				} finally {
					progressDialog.dismiss();
				}
				if(msg!=null) {
					Log.i("DEBUG", msg);
					final String m = msg;
					cntxt.runOnUiThread(new Runnable() { public void run() {
						Toast.makeText(cntxt, m, Toast.LENGTH_LONG).show();
					}});
				}
	        }
	    };
	    
	    Thread thread =  new Thread(null, sendTorrents, "Torrent Sender Thread");
	    thread.start();		
	}
	
	public Bundle toBundle() {
		return mInfo;
	}
}
