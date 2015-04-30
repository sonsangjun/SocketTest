import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;


public class ServerThread extends Thread{
	Integer filesize = 0;
	String fileName;
	Socket socket;
	SharedData shared;
	FileInputStream fileInput;
	BufferedOutputStream fileOutput;
	
	
	public ServerThread(Socket socket,SharedData shared,String fileName)
	{
		this.socket = socket;		
		this.shared = shared;
		this.fileName = fileName;
	}
	
	public void run()
	{
		
		try {
			fileInput = new FileInputStream(fileName);
			fileOutput = new BufferedOutputStream(socket.getOutputStream());
			filesize = fileInput.available();	//파일사이즈를 바이트 배열에 넣어야하나
			
			
			
			
			
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
		}
		
		
		
		
		
	}
}
