import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ClientThread extends Thread {
	String fileName;
	SharedData shared;
	
	BufferedInputStream packetInput;
	FileOutputStream fileOutput;
	Socket socket;
	
	public ClientThread(Socket socket, SharedData shared,String fileName) {
		this.socket = socket;
		this.shared = shared;
		this.fileName = fileName;
		
	}
	
	public void run()
	{
		byte[] fileStream;
		int fileSize=0;
		System.out.println("클라이언트에서 파일 받기를 준비합니다.");
		try {
			fileOutput = new FileOutputStream(fileName);
			packetInput = new BufferedInputStream(socket.getInputStream());			
		} catch (FileNotFoundException e) {
			System.out.println("파일 아웃풋인데 이런 예외가 뜨네");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
			e.printStackTrace();
		}
		
		synchronized (shared) {
			try {
				shared.wait();
				fileSize = shared.fileSize;
				System.out.println("파일 사이즈를 알아냈습니다.");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("파일 사이즈 받기를 실패했습니다.");
				e.printStackTrace();
			}			
		}
		if(fileSize != 0)
		{
			System.out.println("클라이언트에서 파일 받기를 시작합니다.");
			fileStream = new byte[fileSize];
			try {
				packetInput.read(fileStream);
				fileOutput.write(fileStream);
				System.out.println("파일을 성공적으로 받았습니다.");
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		
		
		try {
			System.out.println(this.getName()+"을 종료합니다.");
			packetInput.close();
			fileOutput.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}	
}
