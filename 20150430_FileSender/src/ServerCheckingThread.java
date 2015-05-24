import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ServerCheckingThread extends Thread{
	final String flag = "OK";
	boolean exceptionFlag = false;
	byte[] fileSizeArray;
	int fileSize;
	int unitSize;
	int counter;		//총 분할횟수
	int extra;			//마지막 분할 잔여공간
	
	String Checking;
	Socket socket;
	SharedData shared;
	BufferedInputStream socketInput = null;
	BufferedOutputStream socketOutput = null;
	SignalData signal;
	
	public ServerCheckingThread(Socket socket,SharedData shared,int unitSize) {
		this.socket = socket;	
		this.shared = shared;
		this.unitSize = unitSize;
		signal = new SignalData();
	}
	
	public void run()
	{

		//이 스레드는 공유 부분을 통해 파일 사이즈를 받아
		//클라이언트와 연락이 제대로 되는지를 판단 한다.
		try {
			socketInput = new BufferedInputStream(socket.getInputStream());
			socketOutput = new BufferedOutputStream(socket.getOutputStream());
			
			synchronized (shared) {
				shared.wait();
				fileSize = shared.fileSize;
				fileSizeArray = new byte[shared.fileSizeArray.length];
				fileSizeArray = shared.fileSizeArray;
				counter = (fileSize/unitSize)-1;				
				extra = fileSize%unitSize;
				
				shared.extra = extra;
				shared.counter = counter;
			}
			System.out.println("파일 사이즈를 전송합니다.");
			socketOutput.write(fileSizeArray);
			socketOutput.flush();
			
			while(true)
			{
				byte[] OK = new byte[signal.signalSize];
				socketInput.read(OK);
				if(signal.OKChecking(OK))
				{
					synchronized (shared) {
						shared.notify();
					}
					System.out.println("클라이언트 측에서 파일사이즈를 알았습니다.");
					break;
				}					
			}			
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
			exceptionFlag = true;
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("인터럽트 예외"+e.getMessage());
			exceptionFlag = true;
			e.printStackTrace();
		}
		
		if(!exceptionFlag)
		{
			System.out.println("서버와 클라이언트간 파일전송을 중계합니다.");			
			while(true)
			{
				byte[] OK = new byte[signal.signalSize];
				try {
					socketInput.read(OK);
					//for(byte i:OK)
					//	System.out.println("OK싸인 배열 :"+i);
					
				} catch (IOException e) {
					System.out.println("파일 전송 중계중 예외"+e.getMessage());
					e.printStackTrace();
				}
				
				if(signal.OKChecking(OK))
				{
					synchronized (shared) {
						shared.notify();
					}
				}
				else
				{
					synchronized (shared) {
						shared.DownComplete=true;
						shared.notify();
						break;
					}
				}
			}			
		}	
		
		System.out.println("SSC를 닫습니다.");
		try {
			socketInput.close();
			socketOutput.close();
			socket.close();
		} catch (IOException e) {
			System.out.println("마무리 중 예외 "+e.getMessage());
			e.printStackTrace();
		}
		
	}
}
