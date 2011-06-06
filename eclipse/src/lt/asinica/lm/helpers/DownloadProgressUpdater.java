package lt.asinica.lm.helpers;

public abstract class DownloadProgressUpdater implements Runnable {
	protected int sizeTotal = 0;
	protected int sizeDownloaded = 0;
	public void updateProgress(int total, int downloaded) {
		sizeTotal = total;
		sizeDownloaded = downloaded;
		run();
	}
}
