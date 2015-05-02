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
	int unitSize;
	int counter;	//총 분할횟수
	int sender;		//파일을 몇 번 분할해 보냈는지 카운트
	int extra;		//마지막 분할 횟수 잔여크기
	
	public ServerThread(Socket socket,SharedData shared,String fileName,int fileSizeIndex,int unitSize)
	{
		this.socket = socket;		
		this.shared = shared;
		this.fileName = fileName;
		this.fileSizeIndex = fileSizeIndex;
		this.unitSize = unitSize;
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
				shared.fileSize = fileInput.available();
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
			
			synchronized (shared) {
				counter = shared.counter;
				extra = shared.extra;
				shared.wait();	//SCT 스레드가 인풋 스트림 받기전에 신호가 오는 경우가 발생하는거 같아 일단 적어놓
			}
			sender = 0;
			
			while(true)
			{
				if(counter < 0)
				{
					System.out.println("클라이언트에게 파일 전송 완료했습니다.");
					fileOutput.write(fileStream);
					break;
				}
				fileOutput.write(fileStream, sender*unitSize, unitSize);
				fileOutput.flush();
				sender++;
				System.out.println("파일 "+sender*unitSize+"Byte 보냄");
				synchronized (shared) {
					shared.wait();
				}
				if(sender > counter)
				{
					fileOutput.write(fileStream, sender*unitSize, extra );
					System.out.println("파일 "+(sender*unitSize+extra)+"Byte 보냄");
					System.out.println("클라이언트에게 파일 전송을 완료했습니다.");
					break;
				}			
					
			}	
			
			System.out.println(this.getName()+"를 종료합니다.");
			
			fileInput.close();
			fileOutput.close();
			socket.close();
			
		} catch (IOException e) {
			System.out.println("IO예외 "+e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println(this.getName()+"스레드쪽 인터럽트 예외");
			e.printStackTrace();
			
		}	
	}
}
