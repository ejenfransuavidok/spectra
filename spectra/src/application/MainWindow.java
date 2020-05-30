package application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.jtransforms.fft.DoubleFFT_1D;

import exceptions.InvalidData;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainWindow {
	
	private static final byte MODBUS_WRITE_FUNCTION = 16;
	private static final short MODBUS_READ_ADC_VALUES = 1277;
	private static final int ADC_RATE = 11000;
	
	private Alert alertError = new Alert(AlertType.ERROR);
	private Alert goodMessage = new Alert(AlertType.INFORMATION);
	static int tmp = 0;
	
	@FXML
	private Button btnStartSpectra;
	private Stage serialSettingsStage;
	private SerialController serialController;
	private Stage primaryStage;
	private Scene primatyScene;
	@FXML
	private LineChart<String, Double> mainChart;
	@FXML
	private TextField modbusAddress;
	
	public SerialController getSerialController () {
		return this.serialController;
	}
	
	public void init(Stage primaryStage, Scene primatyScene) throws IOException {
		this.primaryStage = primaryStage;
		this.primatyScene = primatyScene;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Serial.fxml"));
		loader.load();
		AnchorPane root = (AnchorPane)loader.getRoot();
		Scene scene = new Scene(root, 350, 300);
		serialSettingsStage = new Stage();
		serialSettingsStage.setTitle("Serial settings");
		serialSettingsStage.setScene(scene);
		serialSettingsStage.initModality(Modality.APPLICATION_MODAL);
		serialController = loader.getController();
		serialController.init(this);
	}
	
	@FXML
	protected void handleOpenSerialSettings(Event event) {
		serialSettingsStage.showAndWait();
	}
	
	@FXML
	protected void handleStartSpectra(Event event) throws IOException {
		byte [] data = getDataFromController();
		if (data != null) {
			double [] ddata = new double [512];
			for (int i=0; i<512; i++) {
				ddata [i] = 0;
			}
			int i=8;
			int j=0;
			while ((i+1) < data.length) {
				ddata [j++] = bytes2Short(data[i], data[i+1]);
				i+=2;
			}
			
			//for(int k=0; k<512; k++) {
			//	ddata [k] = Math.sin(2 * Math.PI * 4000 * k/ADC_RATE);
			//}
			
			this.mainChart.getData().removeAll(this.mainChart.getData());
			this.primaryStage.setTitle("TM+(Spectra)");
	        this.mainChart.setTitle("Spectr");
	        XYChart.Series<String, Double> series = fftTest(ddata, ADC_RATE);
	        series.setName("Spectr");
	        this.mainChart.getData().add(series);
	        this.primatyScene.getStylesheets().add(getClass().getResource("chart.css").toExternalForm());
	        this.primaryStage.setScene(this.primatyScene);
	        this.primaryStage.show();
		}
    }
	
	private byte [] getDataFromController() {
		try {
			if (serialController.getSerialPort() == null) {
				throw new Exception("Порт не установлен!!!");
			}
			if (!serialController.getSerialPort().isOpen()) {
				serialController.getSerialPort().setBaudRate(serialController.getBaudRate());
				if(!serialController.getSerialPort().openPort()) {
					throw new Exception();
				}
			}
			try {
				SendTask task = new SendTask(this);
				ExecutorService executorService = Executors.newSingleThreadExecutor();
				Future<byte []> future = executorService.submit(task);
			    byte [] result = future.get();
			    executorService.shutdown();
			    if (result != null) {
				    return result;
				}
				throw new Exception("Ошибка при считывании данных из контроллера");
			} catch (Exception e) {
				Platform.runLater(() -> {
					e.printStackTrace();
					alertError.setTitle("Ошибка при подготовке данных для записи в контроллер");
					alertError.setContentText(e.getMessage());
					alertError.show();
				});
			}
			finally {
				if (serialController.getSerialPort().isOpen()) {
					serialController.getSerialPort().closePort();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			alertError.setTitle("Ошибка при подготовке данных для записи в контроллер");
			alertError.setContentText(e.getMessage());
			alertError.show();
		}
		return null;
	}
	
	public byte [] sendOne(List<Byte> sending) 
			throws InterruptedException, InvalidData {
		int counter = 0;
		for (;;) {
			try {
				System.out.println(sending.size());
				sending.forEach(t->System.out.print(String.valueOf(String.format("%02x", (int) t)) + " "));
				System.out.println("--------------------------------------------");
				System.out.println();
				serialController.write(sending);
				int attemptions = 10;
				byte [] ret = serialController.read(attemptions, 100);
				if (ret != null) {
					return ret;
				}
				else {
					throw new InvalidData("Ошибка при чтении данных из контроллера");
				}
			}
			catch (InvalidData e) {
				if (counter < 10) {
					counter++;
					System.out.println("Попытка " + (counter + 1));
				} else {
					throw e;
				}
			}
		}
	}
	
	public List<Byte> getSending() {
		List<Byte> sending = new ArrayList<>();
		String sModbusAddress = modbusAddress.getText();
		byte modbusAddress = Byte.valueOf(sModbusAddress);
		sending.add(modbusAddress);
		sending.add(MODBUS_WRITE_FUNCTION);
		byte [] address = short2Bytes(MODBUS_READ_ADC_VALUES);
		// First register address – high byte
		byte addrHi = address [1];
		// First register address – low byte
		byte addrLo = address [0];
		sending.add(addrHi);
		sending.add(addrLo);
		// количество регистров в посылке
		sending.add((byte) 0);
		sending.add((byte) 1);
		// количество байт в посылке
		sending.add((byte) 2);
		// значение записываемого регистра
		sending.add((byte) 0);
		sending.add((byte) 1);
		int [] crc = CRC16.getCrc(sending);
		sending.add((byte) crc [0]);
		sending.add((byte) crc [1]);
		return sending;
	}
	
	private int bytes2Short(byte hi, byte lo) {
		return ((hi << 8) & 0x0000ff00) | (lo & 0x000000ff);
	}
	
	private byte [] short2Bytes(short x) {
		byte [] ret = new byte [2];
		ret[0] = (byte)(x & 0xff);
		ret[1] = (byte)((x >> 8) & 0xff);
		return ret;
	}
	
	private XYChart.Series<String, Double> fftTest(double [] inBuffer, int samples) {
		XYChart.Series<String, Double> series = new XYChart.Series<>();
		int n = 512;
		DoubleFFT_1D fft = new DoubleFFT_1D(n);
		fft.realForward(inBuffer);
		//Plan plan = new Plan(n, Plan.REAL_TO_COMPLEX, Plan.ESTIMATE);
		//double[] outBuffer = plan.transform(inBuffer);
		
		for (int i=0; i<10; i++) {
			inBuffer [i] = 0;
		}
		for (int i=0; i<inBuffer.length; i++) {
			double x = (double) (samples / 2) * ((double) i / (double) inBuffer.length);
			double y = inBuffer [i];
			series.getData().add(new XYChart.Data<>(String.valueOf(Math.round(x)) + " Гц", y));
		}
		return series;
	}
	
}
