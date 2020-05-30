package application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

import exceptions.InvalidData;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

public class SerialController {
	
	private MainWindow parentController;
	private SerialPort comPort;
	private int baudRate = 19200;
	
	@FXML
	ChoiceBox selectPort;
	
	@FXML
	ChoiceBox selectSpeed;
	
	@FXML
	Label statusLabel;
	
	public void init(MainWindow controller) {
		parentController = controller;
		int index = 0;
		for (SerialPort port : SerialPort.getCommPorts()) {
			selectPort.getItems().add(index++, port.getSystemPortName());
		}
		selectSpeed.getItems().add(0, String.valueOf(baudRate));
	}
	
	@FXML
	protected void handleConnect(Event event) {
		if (comPort != null) {
			return;
		}
		statusLabel.setText("...");
		if (selectSpeed.getValue() == null) {
			statusLabel.setText("Установите скорость!!!");
			return;
		}
		if (selectPort.getValue() == null) {
			statusLabel.setText("Установите порт!!!");
			return;
		}
		statusLabel.setText("Соединяюсь...");
		int comIndex = selectPort.getItems().indexOf(selectPort.getValue());
		comPort = SerialPort.getCommPorts()[comIndex];
		if (comPort.openPort()) {
			comPort.setBaudRate(baudRate);
			statusLabel.setText("Соединение установлено");
		}
		else {
			statusLabel.setText("Соединение не может быть установлено!");
		}
	}
	
	public SerialPort getSerialPort() {
		return comPort;
	}
	
	public void write(List<Byte> data) throws InvalidData {
		try {
			comPort.writeBytes(list2array(data), data.size());
		} catch (Exception e) {
			if (comPort == null || (comPort != null && !comPort.isOpen())) {
				// если порт существует, но закрыт
				comPort = null;
				statusLabel.setText("Установите соединение!");
			}
			e.printStackTrace();
			throw new InvalidData("Ошибка при записи в устройство!");
		}
	}
	
	public byte [] read(int attemptions, int wait) throws InvalidData {
		List<List<byte []>> responses = new ArrayList<>(); 
		try {
		   while (attemptions > 0) {
			   byte [] readBuffer = new byte[comPort.bytesAvailable()];
			   int numRead = comPort.readBytes(readBuffer, readBuffer.length);
			   responses.add(Arrays.asList(readBuffer));
			   Thread.sleep(wait);
			   attemptions--;
		   }
		   int summ = 0;
		   for (List<byte []> item : responses) {
			   System.out.println("SIZE: " + item.get(0).length);
			   summ += item.get(0).length;
		   }
		   System.out.println("TOTAL: " + summ);
		   if (summ > 1000) {
			   int i = 0;
			   byte [] result = new byte [summ];
			   for (List<byte []> item : responses) {
				   if (item.get(0).length != 0) {
					   for (int j=0; j<item.get(0).length; j++) {
						   result [i++] = item.get(0)[j];
						   System.out.print(String.valueOf(String.format("%02x", (int) item.get(0)[j])) + " ");
					   }
				   }
			   }
			   return result;
		   }
		   return null;
		} catch (Exception e) {
			e.printStackTrace();
			if (comPort == null || (comPort != null && !comPort.isOpen())) {
				// если порт существует, но закрыт
				comPort = null;
				statusLabel.setText("Установите соединение!");
			}
			throw new InvalidData("Ошибка при чтении из устройства!"); 
		}
	}
	
	public int getBaudRate() {
		return baudRate;
	}
	
	private byte [] list2array(List<Byte> data) {
		byte [] d = new byte [data.size()];
		int i=0;
		for (Byte b : data) {
			d [i++] = b;
		}
		return d;
	}
	
}
