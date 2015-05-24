package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
 */
public class ByteArrayTransCeiver {
	ValueCollections value = new ValueCollections();
	byte[] returnValue = {1};
	ByteArrayTransCeiverRule byteArrayTransCeiverRule; 
	
	IntegerToByteArray integerToByteArray = new IntegerToByteArray();
		
	/* 바이트 배열 전송포트중
	 * 9001은 카메라 프리뷰 포트
	 * 9002는 음성 포트이다. 참고할것
	 */
	
	/*	동작방식
	 * 	Server	<─────────────────────────>	client
	 * toAccept()						todoRequest
	 * 
	 * 각자 데이터 성격에 맞는 스트림을 연다.(camera, voice)socket
	 * 
	 * client가 서버에게 데이터 사이즈를 보낸다. 
	 * Server는 데이터 사이즈를 받았다는 OK사인을 보낸다. (toDoResponse)
	 * client는 toCatchResponse로 서버로부터의 OK사인을 알아챈다.
	 * 
	 * 데이터 전송을 시작한다.
	 * 다 보내면 서로 알아서 전송을 끝냈다.
	 * 
	 * 		┌Server는 데이터를 보내준 클라이언트 외 다른 클라이언트에게 데이터 전송을 시작한다.
	 * 		│client는 데이터 전송을 받기위해 스레드를 따로 돌리고 있고, 그 스레드에서 instream을 통해 fileSize받기를 대기하고 있다.(read(byte[])
	 * 		│client는 Server로부터 파일 사이즈를 받으면 TodoResponse(signal.byteSize)로 크기값을 받았다는 OK사인을 보낸다.
	 * 	  반복│Server는 toCatchResponse를 통해 OK사인을 받고, 데이터 전송을 시작한다.
	 * 		│이후는 Server <-> Client 파일 받던 방식과 같다.
	 * 		└모든 클라이언트가 받을때까지 반복한다.
	 * 
	 * 다 받으면 해당 명령을 끝낸다.
	 */
	
	//데이트 스트림 전송 스레드는 9000번 포트로부터 신호를 받아서 생성되는 스레드이다. 이놈 자체가 데이터 스트림 요청입력을 대기하지 않는다.
	//다시말하지만, roomData == null은 클라이언트이다.
	public ByteArrayTransCeiver(ByteArrayTransCeiverRule byteArrayTransCeiverRule)
	{
		this.byteArrayTransCeiverRule = byteArrayTransCeiverRule;
	}
		
	
	//이걸 실행시키면 된다.
	public byte[] TransCeiver()
	{				
		//송수신이 클라이언트인가 서버인가 판단
		if(byteArrayTransCeiverRule.roomData == null)
		{
			if(byteArrayTransCeiverRule.transCeive)
				return clientTrans();
			else
				return clientReceive();		
		}
		else 
		{
			return serverTransCeive();
		}			
	}
	
	
	//현재 아래 짠 코드는 카메라 프리뷰 전송때 오버헤드가 크다... 이점을 어떻게 해야할지...
	//클라이언트가 서버에게 전송
	public byte[] clientTrans()
	{			
		//cameraSocket or voiceSocket을 잠근다. 내가 쓸꺼니까

		if(byteArrayTransCeiverRule.cameraVoice)
		{
			if(byteArrayTransCeiverRule.socketCameraUsed.socketCameraUsed)
				return null;
			else
				;
		}
		else
			if(byteArrayTransCeiverRule.socketVoiceUsed.socketVoiceUsed)
				return null;
			else
				;			

		
		usedChecking(true);
		
		//clientTransferSignal을 signal대신 쓴다.
		//이 시그널은 camera,voice포트를 대신 사용한다.
		SignalData clientTransferSignal;
		
		//outputStream을 데이터 전송에 써야한다.
		BufferedOutputStream outputStream;
		//초기에 카메라인지 음성인지 판단
		byte[] fileByteArray;
		
		if(byteArrayTransCeiverRule.cameraVoice)
		{			
			try {
				outputStream = new BufferedOutputStream(byteArrayTransCeiverRule.cameraSocket.getOutputStream());
				clientTransferSignal = new SignalData(byteArrayTransCeiverRule.cameraSocket);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("camera스트림 선언 예외");
				return null;
			}				
			fileByteArray = byteArrayTransCeiverRule.socketCameraUsed.message;
			
		}			
		else
		{		
			try {				
				outputStream = new BufferedOutputStream(byteArrayTransCeiverRule.voiceSocket.getOutputStream());
				clientTransferSignal = new SignalData(byteArrayTransCeiverRule.voiceSocket);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("voice스트림 선언 예외");
				return null;
			}			
			fileByteArray = byteArrayTransCeiverRule.socketCameraUsed.message;
		}		
		//초기에 카메라인지 음성인지 판단 끝		
		
		
		//clientTransferSignal 초기화 (ClientTrans() )
		clientTransferSignal.initial();
		
		//파일 보내기
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		integerToByteArray.initialByteArray(fileSize);
		if(clientTransferSignal.toDoRequest(clientTransferSignal.byteReceive))
		{			
			integerToByteArray.getBytes(fileByteArray.length,fileSize);		
			try {
				outputStream.write(fileSize);
				outputStream.flush();
				if(clientTransferSignal.toCatchResponse(clientTransferSignal.byteSize))
				{
					int counter = 0;	
					int packetSize = value.packetSize;
					while(true)
					{
						if(counter >= fileByteArray.length)
							break;
						outputStream.write(fileByteArray, counter, packetSize);
						counter++;
					}	
					outputStream.flush();
				}				
			} catch (IOException e) {
				System.out.println("clientTrans() output.write(fileSize) 예외");
				e.printStackTrace();
				usedChecking(false);
				return null;
			}	
		}		
		//파일 보내기 끝		
	
		usedChecking(false);
		return returnValue;		
	}
	
	//20150522
	//clientReceiver 와 Server의 송신 부분의 signal을 camera나 voice로 바꾸자.
	public byte[] clientReceive()
	{		
		//cameraSocket or voiceSocket을 잠근다. 내가 쓸꺼니까
		if(byteArrayTransCeiverRule.cameraVoice)
		{
			if(byteArrayTransCeiverRule.socketCameraUsed.socketCameraUsed)
				return null;
			else
				;
		}
		else
			if(byteArrayTransCeiverRule.socketVoiceUsed.socketVoiceUsed)
				return null;
			else
				;			
				
		usedChecking(true);
		BufferedInputStream inputStream;
		SignalData clientReceiveSignal;		//데이터 전송중에도 event시그널을 써야하는 독립성을 보장함.
		
		
		//초기에 카메라인지 음성인지 판단
		byte[] fileByteArray;
		if(byteArrayTransCeiverRule.cameraVoice)
		{
			try {
				inputStream = new BufferedInputStream(byteArrayTransCeiverRule.cameraSocket.getInputStream());
				clientReceiveSignal = new SignalData(byteArrayTransCeiverRule.cameraSocket);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("camera스트림 선언 예외");
				usedChecking(false);
				return null;
			}					
		}			
		else
		{
			try {
				inputStream = new BufferedInputStream(byteArrayTransCeiverRule.voiceSocket.getInputStream());
				clientReceiveSignal = new SignalData(byteArrayTransCeiverRule.voiceSocket);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("voice스트림 선언 예외");
				usedChecking(false);
				return null;
			}			
		}			
		//signal은 초기화 시켜야 하므로
		clientReceiveSignal.initial();
		
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		int intFileSize;
		integerToByteArray.initialByteArray(fileSize);

		try {
			inputStream.read(fileSize);
			intFileSize = integerToByteArray.getInt(fileSize);
			fileByteArray = new byte[intFileSize];		
			if(!clientReceiveSignal.toDoResponse(clientReceiveSignal.byteSize))
			{
				System.out.println("byte 받았다는 확인을 보내는 중 서버와 연결 예외 발생");					
				usedChecking(false);
				return null;						
			}
			
			int counter=0;
			int packetSize = value.packetSize;			
			while(true)
			{
				if(counter >= fileByteArray.length)
					break;
				inputStream.read(fileByteArray, counter, packetSize);
				counter++;
			}
			System.out.println("받은 데이터 크기 "+counter+"Byte");
			
		} catch (IOException e) {
			System.out.println("서버가 요청을 하지 않습니다.");
			e.printStackTrace();
			usedChecking(false);
			return null;
		}
		
		usedChecking(false);		
		//데이터를 받았다.
		//받은 데이터 스트림을 반환한다.
		if(byteArrayTransCeiverRule.cameraVoice)
			return fileByteArray;
		else
			return fileByteArray;
	}

	
	//서버 데이터 전송
	//서버가 클라이언트에게 받아 데이터 스트림을 전송한 클라이언트를 제외하고 방여 참여중인 다른 클라이언트에게 데이터 스트림 전송.
	public byte[] serverTransCeive()
	{

		if(byteArrayTransCeiverRule.cameraVoice)
		{
			if(byteArrayTransCeiverRule.socketCameraUsed.socketCameraUsed)
			{
				return null;
			}
			else
				;
		}
		else
			if(byteArrayTransCeiverRule.socketVoiceUsed.socketVoiceUsed)
				return null;
			else
				;		
	
		//cameraSocket or voiceSocket을 잠근다. 내가 쓸꺼니까		
		usedChecking(true);
		BufferedInputStream inputStream;
		BufferedOutputStream outputStream;
		SignalData serverTransferSignal; 		//해당 시그널은 각 클라이언트에게 전송할때 쓰인다.
		SignalData pushSignal;					//대상 클라이언트에게 push하기 위해 만들었다.
		
												//독립성을 보장하기 위해서 시그널을 따로 선언한다.
		
		//초기에 카메라인지 음성인지 판단
		byte[] fileByteArray = null;
		if(byteArrayTransCeiverRule.cameraVoice)
		{
			try {
				inputStream = new BufferedInputStream(byteArrayTransCeiverRule.cameraSocket.getInputStream());
				serverTransferSignal = new SignalData(byteArrayTransCeiverRule.cameraSocket);				
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("camera스트림 선언 예외");
				usedChecking(false);
				return null;
			}					
		}			
		else
		{
			try {
				inputStream = new BufferedInputStream(byteArrayTransCeiverRule.voiceSocket.getInputStream());	
				serverTransferSignal = new SignalData(byteArrayTransCeiverRule.voiceSocket);				
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("voice스트림 선언 예외");
				usedChecking(false);
				return null;
			}			
		}
		
		//signal은 초기화를 시켜야하므로.
		serverTransferSignal.initial();
		

		//여기부터 serverTransferSignal을 쓴다.
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		int intFileSize;
		integerToByteArray.initialByteArray(fileSize);
		
		//clientTrans toDoRequest(clientTransFerSignal.byteReceive) 대응 ( 약 153Line )
		if(serverTransferSignal.toAccept(serverTransferSignal.byteReceive))
		{	
			try {
				inputStream.read(fileSize);
				intFileSize = integerToByteArray.getInt(fileSize);
				fileByteArray = new byte[intFileSize];		
				if(!serverTransferSignal.toDoResponse(serverTransferSignal.byteSize))
				{
					System.out.println("클라이언트에게 byte사이즈 받았다는 응답을 보내지 못했습니다.");
					usedChecking(false);
					return null;						
				}
				System.out.println("데이터 사이즈를 받았습니다."+intFileSize+"Byte");
				
				int counter=0;
				int packetSize = value.packetSize;
				while(true)
				{
					if(counter >= fileByteArray.length)
						break;
					inputStream.read(fileByteArray, counter, packetSize);
					counter++;
				}				
				System.out.println("받은 데이터 "+counter+"Byte");
				
			} catch (IOException e) {
				System.out.println("클라이언트에게서 정상적으로 데이터를 못받았습니다.");
				e.printStackTrace();
				usedChecking(false);
				return null;
			}							
		}
		else
		{
			System.out.println("Client ID : "+byteArrayTransCeiverRule.clientID+" 로 부터 데이터 스트림을 못 받았습니다.");
			return null;
		}
		
		
		//테스트 서버가 확실히 그림을 받았는지 확인해야 겠음
		try {
			FileOutputStream testOutput = new FileOutputStream("test.jpg");
			testOutput.write(fileByteArray);
			testOutput.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//여기까지는 클라에게 서버가 파일 받는거고(정확히는 데이터스트림)
		//아래부터는 서버가 클라이언트에게 뿌린다.		
		while(true)
		{			
			//다른클라이언트에게 전송을 위해 반복문 선언
			for(int i=0; i<byteArrayTransCeiverRule.roomData.clientManage.clientID.size(); i++)
			{				
				//자기 자신이면 통과
				if(byteArrayTransCeiverRule.roomData.clientManage.clientID.get(i).equals(this.byteArrayTransCeiverRule.clientID))
					continue;				
					
				
				//카메라인지 소리인지 구분해 전송 소켓 열기(초기화)
				if(this.byteArrayTransCeiverRule.cameraVoice)
				{
					try {
						outputStream = new BufferedOutputStream(byteArrayTransCeiverRule.roomData.clientManage.cameraSocket.get(i).getOutputStream());
						pushSignal = new SignalData(byteArrayTransCeiverRule.roomData.clientManage.pushSocket.get(i));
						serverTransferSignal = new SignalData(byteArrayTransCeiverRule.roomData.clientManage.cameraSocket.get(i));
						
						pushSignal.initial();				//각 클라이언트 pushSocket 초기화
						serverTransferSignal.initial();
						
					} catch (IOException e) {
						System.out.println("["+byteArrayTransCeiverRule.roomData.clientManage.clientID.get(i)+"] 에게 프리뷰 전송실패");
						e.printStackTrace();
						continue;	//전송실패하면 다음 클라이언트에게 전송
					}
					if(!pushSignal.toDoRequest(pushSignal.camera))
					{
						System.out.println("Client ID : "+this.byteArrayTransCeiverRule.clientID+" camera이미지 전송중 연결끊김");
						continue;
					}
				}
				else
				{
					try {
						outputStream = new BufferedOutputStream(byteArrayTransCeiverRule.roomData.clientManage.voiceSocket.get(i).getOutputStream());
						pushSignal = new SignalData(byteArrayTransCeiverRule.roomData.clientManage.pushSocket.get(i));
						serverTransferSignal = new SignalData(byteArrayTransCeiverRule.roomData.clientManage.voiceSocket.get(i));
						
						pushSignal.initial();				//각 클라이언트 pushSocket 초기화
						serverTransferSignal.initial();
					} catch (IOException e) {
						System.out.println("["+byteArrayTransCeiverRule.roomData.clientManage.clientID.get(i)+"] 에게 보이스 전송실패");
						e.printStackTrace();
						continue;
					}
					if(!pushSignal.toDoRequest(pushSignal.voice))
					{
						System.out.println("Client ID : "+this.byteArrayTransCeiverRule.clientID+" voice 전송중 연결끊김");
						continue;
					}
				}				
				
				//클라이언트에게 준비하라고 요청 및 데이터 스트림 전송
				try {
					outputStream.write(fileSize);
					outputStream.flush();
					
					if(serverTransferSignal.toCatchResponse(serverTransferSignal.byteSize))
					{
						int counter = 0;
						int packetSize = value.packetSize;
						while(true)
						{
							if(counter >= fileByteArray.length)
								break;
							outputStream.write(fileByteArray, counter, packetSize);
							counter++;
						}	
						outputStream.flush();
						System.out.println("["+byteArrayTransCeiverRule.roomData.roomName+"]방에 참여한 Client ID : "+byteArrayTransCeiverRule.roomData.clientManage.clientID.get(i)+" 에게"+counter+"Byte 데이터 스트림 전송");
					}
				} catch (IOException e) {
					System.out.println("["+byteArrayTransCeiverRule.roomData.roomName+"]방에 참여한 Client ID : "+byteArrayTransCeiverRule.roomData.clientManage.clientID.get(i)+" 에게 전송실패");
					e.printStackTrace();						
					continue;
				}						
															//실패할경우 if문을 빠져나온다.
			}//실패하면 다음 클라이언트에게 전송한다.			
			
			//for문이 끝나면 리턴한다.
			System.out.println("["+byteArrayTransCeiverRule.roomData.roomName+"] 에 참가한 클라이언트에게 데이터를 전송했습니다.");
			usedChecking(false);
			return returnValue;
		}
	}
	//서버 데이터 전송 끝
	
	
	//간단한 체크 함수
	public void usedChecking(boolean used)	//사용하면 true,	안사용하면 false
	{		
		if(this.byteArrayTransCeiverRule.cameraVoice)
		{
			synchronized (byteArrayTransCeiverRule.socketCameraUsed) {
				byteArrayTransCeiverRule.socketCameraUsed.socketCameraUsed = used;
			}			
		}					
		else
		{
			synchronized (byteArrayTransCeiverRule.socketVoiceUsed) {
				byteArrayTransCeiverRule.socketVoiceUsed.socketVoiceUsed = used;
			}			
		}
	}
}