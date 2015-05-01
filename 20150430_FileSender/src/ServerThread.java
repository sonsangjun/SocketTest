import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;


public class ServerThread extends Thread{ 
	String fileName;
	Socket socket;
	SharedData shared;
	FileSizeChecking SizeChecking;
	
	FileInputStream fileInput;
	BufferedOutputStream fileOutput;
	
	int fileSizeIndex;
	
	public ServerThread(Socket socket,SharedData shared,String fileName,int fileSizeIndex)
	{
		this.socket = socket;		
		this.shared = shared;
		this.fileName = fileName;
		this.fileSizeIndex = fileSizeIndex;
	}
	
	public void run()
	{
		byte[] fileSize = new byte[fileSizeIndex];
		byte[] fileStream;
		
		try {
			fileInput = new FileInputStream(fileName);
			fileOutput = new BufferedOutputStream(socket.getOutputStream());
			SizeChecking = new FileSizeChecking();
			SizeChecking.getBytes(fileInput.available(), fileSize);
			
			System.out.println("파일 사이즈를 구합니다.");
			SizeChecking.Test(fileInput.available(), fileSize);
			synchronized (shared) {	
				shared.fileSizeArray = fileSize;
				shared.notify();
			}
			System.out.println("파일 사이즈를 SCT에 넘겼습니다.");
			
			synchronized (shared) {
				shared.wait();
			}
			
			System.out.println("클라이언트에게 파일을 보냅니다.");
			fileStream = new byte[fileInput.available()];
			fileInput.read(fileStream);
			fileOutput.write(fileStream);
			
			System.out.println("클라이언트에게 파일을 보냈습니다.");
			System.out.println(this.getName()+"를 종료합니다.");
			
			fileInput.close();
			fileOutput.close();
			socket.close();
			
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
		} catch (InterruptedException e) {
			System.out.println(this.getName()+"스레드쪽 인터럽트 예외");
			e.printStackTrace();
			
		}	
	}
}
