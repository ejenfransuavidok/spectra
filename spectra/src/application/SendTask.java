package application;

import java.util.concurrent.Callable;

public class SendTask implements Callable<byte []>  {

	private MainWindow mainWIndow;
	
	public SendTask(MainWindow mainWindow) {
		this.mainWIndow = mainWindow;
	}
	
	@Override
	public byte [] call() throws Exception {
		try {
			return this.mainWIndow.sendOne(this.mainWIndow.getSending());
		} catch (Exception e) {
			return null;
		}
	}

}
