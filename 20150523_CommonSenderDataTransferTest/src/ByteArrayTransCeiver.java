


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

//서버,클라이언트 스레드와 독립적으로 돌아감 .
//크기가 큰 카메라프리뷰나 음성 송수신에 사용.

/* 20150506 메소드 목록
 * public ByteArrayTransCeiverThread(int clientID, boolean transCeive, boolean CameraVoice, byte[] fileByteArray, ArrayList<ClientManagement> clientManagementList, ClientManagement clientManagement)
 * 	└ 생성자
 * public void run()
 * 	└ 코어
 * 	public void clientTrans()
 * 	└ 클라이언트가 서버에게 데이터스트림 보냄
 * 	public void clientReceive()
 * 	└ 클라이언트가 서버에게 데이터스트림 받음.
 * 	public void serverTransCeive()
 * 	└ 서버가 클라이언트에게 받은 데이터스트림을 그외 다른 클라이언트에게 뿌림.
 * 
 * 	public void usedChecking(boolean used)
 * 	└ 카메라,보이스 전송 소켓을 사용하는지 여부 체크 (사용하면 true)
 * 	└ 사용됨을 표시해야 클라이언트간 송수신 경쟁이 일어나지 않는다. (혼돈...) 		
 * 
 * 
 * 20150523 데이터 송수신 부분만 떼와서 테스트 중.
 * 1. 소켓포트가 같아서 생기는 것아님
 * 2. 데이터 전송 코드가 틀린듯. 소켓 바꾸고 합치고 이것저것 했는데 그림이 자꾸 깨짐.
 */
public class ByteArrayTransCeiver {
	final int portNum = 9000;
	final String ServerIP = "221.156.9.145";
	final String fileName = "test.jpeg";
	
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
					byteArrayTransCeiverRule.Calc(fileByteArray.length);
					int counter = 0;
					int unitSize = byteArrayTransCeiverRule.fileUnitSize;
					int maxCounter = byteArrayTransCeiverRule.counter;
						
					while(true)
					{
						if(counter >= maxCounter)
						{
							if(counter*unitSize == fileByteArray.length)
							{
								
								return returnValue;									
							}
							else if(signal.toDoRequest(signal.byteSend))
							{
								outputStream.write(fileByteArray, counter*unitSize, byteArrayTransCeiverRule.extra);
								//outputStream.flush();								
								
								return returnValue;
							}
							else
							{
								System.out.println("ClientTrans 데이터 전송중 예외");
								break;
							}
						}						
						else if(signal.toDoRequest(signal.byteSend))
						{
							System.out.println("보낸 파일 크기는 : "+counter*(unitSize));
							outputStream.write(fileByteArray, counter*unitSize, unitSize);
							//outputStream.flush();
							counter++;
						}						
					}
					outputStream.flush();
				}				
			} catch (IOException e) {
				System.out.println("clientTrans() output.write(fileSize) 예외");
				e.printStackTrace();
				
				return null;
			}	
				
		//파일 보내기 끝		
		serverSocket.close();
		
		return null;		
	}
	
	//20150522
	//clientReceiver 와 Server의 송신 부분의 signal을 camera나 voice로 바꾸자.
	public void clientReceive() throws IOException
	{				
		
		//초기에 카메라인지 음성인지 판단
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
			
			byteArrayTransCeiverRule.Calc(intFileSize);
			int counter=0;
			int unitSize = byteArrayTransCeiverRule.fileUnitSize;
			int maxCounter = byteArrayTransCeiverRule.counter;
			
			while(true)
			{
				if(counter >= maxCounter)
				{
					if(counter*unitSize == fileByteArray.length)
					{								
						
						System.out.println(fileByteArray.length+"Byte를 서버에서 받았습니다.");						
						break;
					}					
					else if(signal.toAccept(signal.byteSend))
					{
						inputStream.read(fileByteArray, unitSize, byteArrayTransCeiverRule.extra);
						
						System.out.println(fileByteArray.length+"Byte를 서버에서 받았습니다.");
						break;								
					}
					else
					{
						System.out.println("ClientReceiver 데이터 전송중 예외");
						break;
					}
				}
				else if(signal.toAccept(signal.byteSend))
				{
					inputStream.read(fileByteArray, counter*unitSize, unitSize);
					counter++;
				}						
			}
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