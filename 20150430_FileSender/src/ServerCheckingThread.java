import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;


public class ServerCheckingThread extends Thread{
	final String flag = "OK";
	String Checking;
	Socket socket;
	SharedData shared;
	BufferedInputStream socketInput = null;
	
	public ServerCheckingThread(Socket socket,SharedData shared) {
		this.socket = socket;	
		this.shared = shared;
	}
	
	public void run()
	{
		//이 스레드는 클라이언트와 연락이 제대로 되는지를 판단만 한다.
		try {
			socketInput = new BufferedInputStream(socket.getInputStream());
			while(true)
			{
				byte[] OK = new byte[2];
				socketInput.read(OK);
				if(flag.equals(OK.toString()))
				{
					synchronized (shared) {
						shared.checking = true;
						shared.notify();
					}
					System.out.println("클라이언트 측에서 파일사이즈를 알았습니다.");
					break;
				}				
			}
			System.out.println("파일사이즈는 클라이언트와 연결을 끊습니다.");
			socketInput.close();
			socket.close();
			
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
		}		
	}
}
