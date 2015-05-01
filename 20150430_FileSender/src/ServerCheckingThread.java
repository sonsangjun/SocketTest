import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ServerCheckingThread extends Thread{
	final String flag = "OK";
	String Checking;
	Socket socket;
	SharedData shared;
	BufferedInputStream socketInput = null;
	BufferedOutputStream socketOutput = null;
	
	public ServerCheckingThread(Socket socket,SharedData shared) {
		this.socket = socket;	
		this.shared = shared;
	}
	
	public void run()
	{
		byte[] fileSizeArray;
		SignalData signal = new SignalData();
		//이 스레드는 공유 부분을 통해 파일 사이즈를 받아
		//클라이언트와 연락이 제대로 되는지를 판단 한다.
		try {
			socketInput = new BufferedInputStream(socket.getInputStream());
			socketOutput = new BufferedOutputStream(socket.getOutputStream());
			
			synchronized (shared) {
				shared.wait();
				fileSizeArray = new byte[shared.fileSizeArray.length];
				fileSizeArray = shared.fileSizeArray;
			}
			System.out.println("파일 사이즈를 전송합니다.");
			socketOutput.write(fileSizeArray);
			socketOutput.flush();
			
			while(true)
			{
				byte[] OK = new byte[signal.signalSize];
				socketInput.read(OK);
				Thread.sleep(1000);
				if(signal.OKChecking(OK))
				{
					synchronized (shared) {
						shared.notify();
					}
					System.out.println("클라이언트 측에서 파일사이즈를 알았습니다.");
					break;
				}
				/*
				else
				{
					//System.out.println("파일 사이즈를 재전송 합니다.");
					socketOutput.write(fileSizeArray);
					Thread.sleep(10);
				}
				*/
					
			}
			System.out.println(this.getName()+"는 클라이언트와 연결을 끊습니다.");
			socketInput.close();
			socket.close();
			
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
