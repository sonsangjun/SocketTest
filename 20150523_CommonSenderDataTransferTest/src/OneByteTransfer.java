import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class OneByteTransfer {
	final int portNum = 9000;
	//final String ServerIP = "221.156.9.145";
	final String ServerIP = "168.131.151.169";
	final String fileName = "test.jpeg";
	final int packetSize = 4;	//4바이트까지는 무난하다. 8바이트부터는 호러사진...
	
	byte[] returnValue = {1};
	byte[] fileByteArray;
	
	SignalData signal;
	Socket dataSocket;
	Socket signalSocket;
	
	IntegerToByteArray integerToByteArray = new IntegerToByteArray();
	ByteArrayTransCeiverRule byteArrayTransCeiverRule = new ByteArrayTransCeiverRule();

	//현재 아래 짠 코드는 카메라 프리뷰 전송때 오버헤드가 크다... 이점을 어떻게 해야할지...
	//클라이언트가 서버에게 전송
	public byte[] clientTrans() throws IOException
	{		
		
		BufferedOutputStream outputStream=null;
		BufferedOutputStream sizeOutputStream = null;
		ServerSocket serverSocket = null;
		ServerSocket signalServerSocket = null;
		byte[] fileSize=null;
		
		try {
			serverSocket = new ServerSocket(portNum);
			signalServerSocket = new ServerSocket(portNum+1);
			
			dataSocket = serverSocket.accept();
			signalSocket = signalServerSocket.accept();
			
			signal = new SignalData(signalSocket);
			signal.initial();
			
			
			FileInputStream fileInput = new FileInputStream(fileName);
			fileSize = new byte[integerToByteArray.fileSizeIndex];
			
			fileByteArray = new byte[fileInput.available()];
			fileInput.read(fileByteArray);	
			
			outputStream = new BufferedOutputStream(dataSocket.getOutputStream());
			sizeOutputStream = new BufferedOutputStream(signalSocket.getOutputStream());
			fileInput.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("서버");
		
		
			integerToByteArray.getBytes(fileByteArray.length,fileSize);		
			try {
				sizeOutputStream.write(fileSize);
				sizeOutputStream.flush();
				if(signal.toCatchResponse(signal.byteSize))
				{
					int count = 0;
					while(true)
					{
						if(count >= fileByteArray.length-packetSize)
						{
							outputStream.write(fileByteArray, count, count-(fileByteArray.length-packetSize)); 
							break;
						}
							
						outputStream.write(fileByteArray, count, packetSize);
						count+=packetSize;
					}
					outputStream.flush();
					System.out.println(count + "Byte 보냈습니다.");
				}				
			} catch (IOException e) {
				System.out.println("clientTrans() output.write(fileSize) 예외");
				e.printStackTrace();
				
				return null;
			}	
				
			
			System.out.println("서버는 대기");
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("서버 끝");
		//파일 보내기 끝		
		serverSocket.close();
		
		return null;		
	}
	
	//20150522
	//clientReceiver 와 Server의 송신 부분의 signal을 camera나 voice로 바꾸자.
	public void clientReceive() throws IOException
	{				
		BufferedInputStream inputStream=null; 
		BufferedInputStream sizeInputStream = null;
		FileOutputStream outputStream = null;
		
		try {
			dataSocket = new Socket(ServerIP,portNum);
			signalSocket = new Socket(ServerIP,portNum+1);
			
			inputStream = new BufferedInputStream(dataSocket.getInputStream());
			sizeInputStream = new BufferedInputStream(signalSocket.getInputStream());
			outputStream = new FileOutputStream(fileName);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		System.out.println("클라");
		
		//signal은 초기화 시켜야 하므로
		signal = new SignalData(signalSocket);
		signal.initial();
		
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		int intFileSize;
		integerToByteArray.initialByteArray(fileSize);

		try {
			sizeInputStream.read(fileSize);
			intFileSize = integerToByteArray.getInt(fileSize);
			fileByteArray = new byte[intFileSize];		
			if(!signal.toDoResponse(signal.byteSize))
			{
				System.out.println("byte 받았다는 확인을 보내는 중 서버와 연결 예외 발생");					
				
				return ;					
			}
			int count = 0;
			while(true)
			{
				if(count >= fileByteArray.length-packetSize)
				{
					inputStream.read(fileByteArray, count, count-(fileByteArray.length-packetSize)); 
					break;
				}
					
				inputStream.read(fileByteArray, count, packetSize);
				count+=packetSize;
			}	
			System.out.println("받음 : "+count+"Byte");
		} catch (IOException e) {
			System.out.println("서버가 요청을 하지 않습니다.");
			e.printStackTrace();
			return ;
		}
		if(fileByteArray != null)
			outputStream.write(fileByteArray);
		outputStream.close();		
	}
}


