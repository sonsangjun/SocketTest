package Sender;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*서버는 클라이언트에게 파일스트림을 받고,
 *받은 파일을 클라이언트가 참여한 방 사람들에게 날려야한다.
 *프리뷰 이미지나, 음성 메시지를 보내는 것 둘다 해당한다. 
 *위치 정보역시 해당된다. 으... 복잡하다. 
 *클라이언트간 연결은 유지하면서 파일 전송을 위한 소켓을 따로 열어야 하지...않을까?
 *이건 그 생각을 바탕으로 설계한다.
 *
 * 파일 송수신은 단일 스레드간에만 이루어진다.. (서버 1개 <-> 클라 1개)
 * 이유는 스레드를 각각 두개씩 두어 파일 체킹과 보내는걸 분리했는데, 둘 사이에 통신이 잘 안되서 락걸리더라... Busy loop 하면 되겠지만,
 * 이미 짠 소스를 수정하기는 귀찮(사실 어디부터 손대야할지 모름)아서 새로 짠다.
 */
public class Server {
	int waitTime = 500;
	int portNum;
	String fileName = null;		//서버 테스트 용으로 만듬
	
	ServerSocket serverSocket;
	Socket socket;
	Socket fileReceiver;
	SharedData shared;
	byte[] fileStream;
	
	Server(int waitTime, int PortNum)
	{
		this.waitTime = waitTime;
		this.portNum = PortNum;
	}
	
	Server(String fileName,int PortNum)
	{
		this.fileName = fileName;
		this.portNum = PortNum;
	}
	
	public void mainServer()
	{
		//일단 테스트 용이므로 테스트서버 메소드를 호출한다.
		testServer(fileName);		
	}
	
	//정식 서버
	public void standardServer()
	{
		while(true)
		{
			try {
				
				serverSocket = new ServerSocket(portNum);
				socket = serverSocket.accept();
				Thread serverThread = new ServerThread(socket,waitTime);
				
				serverThread.start();				
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}	
		
	}
	
	//테스트 서버
	public void testServer(String fileName)
	{		
		while(true)
		{
			System.out.println("서버 연결 대기중");
			try{
				serverSocket = new ServerSocket(portNum);
				socket = serverSocket.accept();
				Thread serverThread = new ServerThread(socket,fileName,waitTime);
				
				serverThread.start();
			}catch(IOException e) {
				System.out.println("테스트 서버 IO예외 발생 "+e.getMessage());
				e.printStackTrace();
			}
		}
		
	}
}