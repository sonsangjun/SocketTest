import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ClientCheckingThread extends Thread {
	final String flag = "OK";
	int fileSize;
	int fileSizeIndex;
	 
	FileSizeChecking sizeChecking ;
	BufferedInputStream packet;	
	BufferedOutputStream OK;
	SharedData shared;
	Socket socket;
	
	public ClientCheckingThread(Socket socket, SharedData shared, int fileSizeIndex) {
		this.socket = socket;
		this.shared = shared;
		this.fileSizeIndex = fileSizeIndex;
	}
	
	public void run()
	{
		sizeChecking = new FileSizeChecking();
		SignalData signal = new SignalData();
		
		byte[] fileSizeArray = new byte[fileSizeIndex];
		
		
		System.out.println("CCT스레드가 시작되었습니다.");
		//서버로부터 파일사이즈 받음
		try {
			packet = new BufferedInputStream(socket.getInputStream());
			OK = new BufferedOutputStream(socket.getOutputStream());
			
			packet.read(fileSizeArray);
			fileSize = sizeChecking.getInt(fileSizeArray);
			System.out.println("받을 파일 사이즈 값은 "+fileSize);
			synchronized (shared) {
				shared.fileSize = fileSize;
				shared.notify();
			}
			System.out.println("서버로부터 파일 사이즈를 받았습니다.");	//문자열로 신호를 보내지 말고 바이트 배열로 보내보자.
			OK.write(signal.OK);	//역시 내 예상이 맞았다. 괜히 문자열로 보내서리 지연시간에 제대로 작동도 안되 이런...
			OK.flush();
			
			socket.close();
			System.out.println(this.getName()+"종료합니다.");
			
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
			e.printStackTrace();
		}
		
		
	}
}
