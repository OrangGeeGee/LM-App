package lt.asinica.lm.objects;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import lt.asinica.lm.LMApp;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.util.Log;

public class Categories extends HashMap<Integer, Category> {
	public final static String CATEGORIES_FILENAME = "categories";
	
	public Categories() {
		super();
	}
	
	public void save() {
		Context context = LMApp.getDefaultContext();
		try {
			// prepare for output
			FileOutputStream fos = context.openFileOutput(CATEGORIES_FILENAME, Context.MODE_PRIVATE);
			Writer out = new OutputStreamWriter(fos, "UTF8");
			
			Category cat;
			// iterate over all categories
			Set<Integer> keys = keySet();
			Iterator<Integer> iterator = keys.iterator();
			while(iterator.hasNext()) {
				cat = get( iterator.next() );
				out.write( cat.toString()+"\n" );
				
			}
			out.close();
		} catch(IOException e) {
			Log.e("DEBUG", "Couldn't save categories. "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static Categories parse(Document doc) {
		Categories cats = new Categories();
		
		Elements tableCells = doc.select("td.browsecat");
		Iterator<Element> iterator = tableCells.iterator();
		Category cat;
		while(iterator.hasNext()) {
			cat = Category.parse(iterator.next());
			cats.put(cat.getId(), cat);
		}		
		return cats;
	}
	
	public static Categories restore() {
		Categories cats = new Categories();
		
		Context context = LMApp.getDefaultContext();
		File categoriesFile = new File(context.getFilesDir(),CATEGORIES_FILENAME);
		if(categoriesFile.isFile() && categoriesFile.length()>0) {
			try {
				FileInputStream fis = context.openFileInput(CATEGORIES_FILENAME);
				DataInputStream in = new DataInputStream(fis);
				BufferedReader br = new BufferedReader(new InputStreamReader( new DataInputStream(in) ));
				
				String line;
				Category cat;
				while ((line = br.readLine()) != null) {
					if(line.length()>0) {
						cat = Category.parse(line);
						cats.put(cat.getId(), cat);
					}
				}
				
            	in.close();
			} catch(IOException e) {
				Log.e("DEBUG", "Couldn't restore categories from file. "+e.getMessage());
				e.printStackTrace();
			}
		}		
		
		return cats;
	}
	
}
