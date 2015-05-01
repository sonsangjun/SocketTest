import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Server {
	final int intervalTime = 5000;
	int portNum;
	int fileSizeIndex;
	String fileName;
	
	ServerSocket fileServerSize = null;
	ServerSocket fileServerSocket = null;
	Socket fileSocket = null;
	Socket fileSizeSocket = null;
	
	ArrayList<Thread> threadList = new ArrayList<Thread>();
	
	
	Server(int portNum,String fileName,int fileSizeIndex)
	{
		this.portNum = portNum;
		this.fileName = fileName;		
		this.fileSizeIndex = fileSizeIndex;
	}
	
	
	public void mainServer()
	{
		try {
			//파일 사이즈 보내느 소켓 portNum
			//파일 보내는 소켓 portNum+1
			System.out.println("서버 초기화 중");
			fileServerSize = new ServerSocket(portNum);
			fileServerSocket = new ServerSocket(portNum+1);
			SharedData shared = new SharedData(0,fileSizeIndex);
			
			System.out.println("파일 사이즈 받는 소켓 연결대기");
			fileSizeSocket = fileServerSize.accept();
			
			System.out.println("파일 받는 소켓 연결대기");
			fileSocket = fileServerSocket.accept();
			
			System.out.println("두 소켓 연걸 모두 성공 \n파일사이즈 "+portNum+"포트 \n파일받는곳 "+(portNum+1)+"포트");
			System.out.println("두 소켓에 대한 스레드 작성");
			
			ServerCheckingThread SCT = new ServerCheckingThread(fileSizeSocket,shared);
			ServerThread ST = new ServerThread(fileSocket,shared,fileName,fileSizeIndex);
			
			threadList.add(SCT);
			threadList.add(ST);
			
			MonitorThread Monitor = new MonitorThread(threadList, intervalTime);
			
			//스레드 시작
			System.out.println("스레드 시작");
			SCT.start();
			ST.start();
			Monitor.start();
			
		} catch (IOException e) {
			System.out.println("서버 포트 여는중 예외 "+e.getMessage());
		}
		
	}

}
