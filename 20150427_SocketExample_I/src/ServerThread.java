import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerThread extends Thread{
	final String filename = "Test.jpeg";
	int portNum;
	ServerSocket serversocket = null;
	Socket socket = null;
	
	FileInputStream fileInput;
	BufferedOutputStream fileSender;
	
	public ServerThread(ServerSocket serversocket,int portNum) {
		this.serversocket = serversocket;	
		this.portNum = portNum;
	}
	public void run()
	{
				
		//파일을 엽니다.
		try {
			fileInput = new FileInputStream(filename);
		} catch (FileNotFoundException e1) {
			System.out.println("파일열기 예외발생 \n서버스레드 종료합니다.\n" + e1.getMessage());
			return ;
		}
		
		//서버가 클라이언트와 연결을 시도합니다.
		try {
			System.out.println("서버 시작합니다.");
			socket = serversocket.accept();
			System.out.println("서버 클라이언트 연결되었습니다.");
			fileSender = new BufferedOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("클라이언트와 연결중 예외발생"+ e.getMessage());
		}			
		//서버가 파일전송 준비를 합니다.
		try {
			System.out.println("파일전송이 시작됩니다.");
			byte[] fileStream = new byte[fileInput.available()];
			fileInput.read(fileStream);
			fileSender.write(fileStream);
			fileSender.close();
			System.out.println("파일을 클라이언트에게 전송하였습니다.");
			
			System.out.println("FileInputStream을 닫습니다.");
			fileInput.close();
			
			System.out.println("소켓을 닫습니다.");
			socket.close();
			
		} catch (IOException e) {
			System.out.println("파일 IO 예외발생 "+e.getMessage());
		}
		//파일 전송을 완료하였습니다.
		System.out.println("파일 전송을 끝냈습니다. 서버 종료합니다.");		
	}
}
