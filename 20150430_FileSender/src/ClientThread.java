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
	
	int unitSize;
	int counter;	//총 분할 횟수
	int receive;	//받은 분할 파일횟수
	int extra;		//마지막 분할 잔여공간
	
	public ClientThread(Socket socket, SharedData shared,String fileName,int unitSize) {
		this.socket = socket;
		this.shared = shared;
		this.fileName = fileName;
		this.unitSize = unitSize;
		
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
				counter = shared.counter;
				extra = shared.extra;
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
			while(true)
			{
				try {
					if(counter <0)
					{
						System.out.println("파일을 성공적으로 받았습니다.");
						fileOutput.write(fileStream);
						break;
					}
					
					packetInput.read(fileStream, receive*unitSize, unitSize);
					receive++;
					System.out.println("파일 "+receive*unitSize+"Byte 받음");
				} catch (IOException e) {
					System.out.println("IO예외 "+e.getMessage());
					e.printStackTrace();
				}
				synchronized (shared) {
					shared.notify();
				}
				if(receive > counter)
				{
					try {
						packetInput.read(fileStream, receive*unitSize, extra);
						fileOutput.write(fileStream);
						System.out.println("파일 "+(receive*unitSize+extra)+"Byte 받음");
						System.out.println("파일을 성공적으로 받았습니다.");
						synchronized (shared) {
							shared.DownComplete = true;
							shared.notify();
						}
					} catch (IOException e) {
						System.out.println("파일에 쓰는 중 예외");
						e.printStackTrace();
					}
					break;					
				}
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
