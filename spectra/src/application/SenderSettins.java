package application;

import java.util.List;
import java.util.concurrent.Callable;

import exceptions.InvalidData;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

public class SenderSettins implements Runnable {

	private List<List<Byte>> sendings;
	private ProgressBar progress;
	private SerialController serialController;
	
	public SenderSettins(List<List<Byte>> sendings, ProgressBar progress, SerialController serialController) {
		this.sendings = sendings;
		this.progress = progress;
		this.serialController = serialController;
	}
	
	@Override
	public void run() {
		int i = 1;
		int j = 0;
		for (List<Byte> sending : sendings) {
			//synchronized(this) {
				try {
					i = sendOne(sendings, sending, i);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidData e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//}
			j++;
			final double step = j;
			Platform.runLater(() -> progress.setProgress(step / sendings.size()));
		}
		//return 0;
	}
	
	private int sendOne(List<List<Byte>> sendings, List<Byte> sending, int number) 
			throws InterruptedException, InvalidData {
		int counter = 0;
		for (;;) {
			try {
				System.out.println(sending.size());
				sending.forEach(t->System.out.print(String.valueOf(String.format("%02x", (int) t)) + " "));
				System.out.println("--------------------------------------------");
				System.out.println();
				serialController.write(sending);
				Thread.sleep(100);
				int attemptions = 3;
				byte [] ret = serialController.read(attemptions, number == sendings.size() ? 1000 : 100);
				System.out.println("*****************************");
				for (byte b : ret) {
					System.out.print(((int) b) + " ");
				}
				number++;
				System.out.println("*************" + (number-1) + " из " + sendings.size() + "****************");
				break;
			}
			catch (InvalidData e) {
				if (counter < 1) {
					counter++;
					System.out.println("Попытка 2");
				} else {
					throw e;
				}
			}
		}
		return number;
	}

}
