import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;


public class ClientCheckingThread extends Thread {
	boolean exceptionFlag = false;
	final String flag = "OK";
	int fileSize;
	int fileSizeIndex;
	int unitSize;
	int counter;	//총 분할 횟수
	int extra;		//마지막 분할 잔여공간
	 
	FileSizeChecking sizeChecking ;
	BufferedInputStream packet;	
	BufferedOutputStream OK;
	SharedData shared;
	Socket socket;
	
	public ClientCheckingThread(Socket socket, SharedData shared, int fileSizeIndex, int unitSize) {
		this.socket = socket;
		this.shared = shared;
		this.fileSizeIndex = fileSizeIndex;
		this.unitSize = unitSize;
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
				counter = (fileSize/unitSize)-1;
				extra = fileSize%unitSize;

				shared.extra = extra;
				shared.counter = counter;
				shared.notify();
			}
			System.out.println("서버로부터 파일 사이즈를 받았습니다.");	//문자열로 신호를 보내지 말고 바이트 배열로 보내보자.
			OK.write(signal.OK);	//역시 내 예상이 맞았다. 괜히 문자열로 보내서리 지연시간에 제대로 작동도 안되 이런...
			OK.flush();
			
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
			exceptionFlag = true;
			e.printStackTrace();
		}
		
		//여기부턴 클라이언트 파일 다운로드 중계
		if(!exceptionFlag)
		{
			while(true)
			{
				synchronized (shared) {
					try {
						shared.wait();
						if(!shared.DownComplete)
						{
							OK.write(signal.OK);
							OK.flush();
						}
						else
						{
							OK.write(signal.NO);
							OK.flush();
							System.out.println("파일 중계를 마칩니다.");
							break;
						}						
					} catch (InterruptedException e) {
						System.out.println("스레드가 못기다림");
						e.printStackTrace();
					} catch (IOException e) {
						System.out.println("파일수신체킹 예외");
						e.printStackTrace();
					}
				}
			}
		}
		
		try {
			System.out.println(this.getName()+"종료합니다.");
			socket.close();
			packet.close();
			OK.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
