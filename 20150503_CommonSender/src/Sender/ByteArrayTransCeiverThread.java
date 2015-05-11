package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
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
 * 	public void transCeiveFailed()			
 * 	└ 송수신 실패를 클라이언트 매니지먼트에 기록한다. (실패하면 false기록)
 * 	public void transCeiveSuccess()			
 * 	└ 송수신 성공을 클라이언트 매니지먼트에 기록한다. (성공하면 true 기록)
 * 
 *	public void toClientUsedChecking(ClientManagement clientManagement, boolean used)
 * 	└ 내용은 usedChecking와 같다. 이건 static ClientManagementList에 있는걸 건들기 위해 선언했다.
 *	public void toClientTransCeiveFailed(ClientManagement clientManagement)			
 * 	└ TransCeiveFailed과 같다. 상동
 *	public void toClientTransCeiveSuccess(ClientManagement clientManagement)
 *	└ TransCeiveSuccess와 같다. 상동			
 */
public class ByteArrayTransCeiverThread {
	boolean transCeive;					//Trans(송신)(업로드)는 true, Ceive(수신)(다운로드)는 false
	boolean CameraVoice;				//Camera 는 true,		Voice는 false;
	byte[] fileByteArray;
	int clientID;						
	
	SignalData signal;
	ArrayList<ClientManagement> clientManagementList;
	ByteArrayTransCevierRule byteArrayTransCeiverRule; 
	ClientManagement clientManagement;
	
	BufferedInputStream input;
	BufferedOutputStream output;
	IntegerToByteArray integerToByteArray;
	
	//생성자가 크고 아름답다.
	/* 바이트 배열 전송포트중
	 * 9001은 카메라 프리뷰 포트
	 * 9002는 음성 포트이다. 참고할것
	 */
	
	//데이트 스트림 전송 스레드는 9000번 포트로부터 신호를 받아서 생성되는 스레드이다. 이놈 자체가 데이터 스트림 요청입력을 대기하지 않는다.
	//서버에서 ClientManageList를 일일이 뒤지려면 오래걸리므로 이 스레드를 돌리기전에 뿌려야하는 명단을 서버에게 추려서 주도록하자.(ServerThread에도 남김)
	//클라이언트는 ClientManagementList가 필요없다. 즉, 생성자 호출후 ClientManagementList 가 null이면 클라, 아니면 서버로 간주.
	public ByteArrayTransCeiverThread(int clientID, boolean transCeive, boolean CameraVoice, byte[] fileByteArray, ArrayList<ClientManagement> clientManagementList, ClientManagement clientManagement) {
		
		this.clientID = clientID;
		this.transCeive = transCeive;						//서버일경우 이 값은 무의미하다.
		this.CameraVoice = CameraVoice;						//프리뷰 이미지 전송이면 true 아니면 false
		this.fileByteArray = fileByteArray;					//전송할 데이터스트림
		this.clientManagementList = clientManagementList;	//클라이언트면 null을 매개변수가 받는다.
		this.clientManagement = clientManagement;			//서버와 연결된 클라이언트
	}
	
	public void run()
	{
		//전송전에 스트림 초기화
		if(CameraVoice)
		{
			signal = new SignalData(clientManagement.cameraSocket);
			try {
				input = new BufferedInputStream(clientManagement.cameraSocket.getInputStream());
				output = new BufferedOutputStream(clientManagement.cameraSocket.getOutputStream());
			} catch (IOException e) {
				System.out.println("바이트배열 전송전 초기화 실패");
				e.printStackTrace();
				return ;
			}			
		}
		else
		{
			signal = new SignalData(clientManagement.voiceSocket);
			try {
				input = new BufferedInputStream(clientManagement.voiceSocket.getInputStream());
				output = new BufferedOutputStream(clientManagement.voiceSocket.getOutputStream());
			} catch (IOException e) {
				System.out.println("바이트배열 전송전 초기화 실패");
				e.printStackTrace();
				return ;
			}					
		}		
		integerToByteArray = new IntegerToByteArray();
				
		
		//송수신이 클라이언트인가 서버인가 판단
		if(clientManagementList == null)
		{
			if(transCeive)
				clientTrans();
			else
				clientReceive();
		}
		else 
		{
			serverTransCeive();		
			return ;
		}			
	}
	
	//클라이언트가 서버에게 전송
	public void clientTrans()
	{		
		transCeiveSuccess();
		usedChecking(true);
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		integerToByteArray.initialByteArray(fileSize);
		if(signal.toRequest())
		{
			if(signal.toDoRequest(signal.byteReceive))
			{
				integerToByteArray.getBytes(fileByteArray.length,fileSize);		
				try {
					output.write(fileSize);
					output.flush();
					if(signal.toResponse(signal.byteSize))
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
									transCeiveSuccess();
									usedChecking(false);
									return ;									
								}
								if(signal.toDoRequest(signal.byteSend))
								{
									output.write(fileByteArray, counter*unitSize, byteArrayTransCeiverRule.extra);
									output.flush();
									transCeiveSuccess();
									usedChecking(false);
									return ;
								}								
							}
							
							else if(signal.toDoRequest(signal.byteSend))
							{
								output.write(fileByteArray, counter*unitSize, unitSize);
								output.flush();
								counter++;
							}
						}						
					}
					
				} catch (IOException e) {
					System.out.println("clientTrans() output.write(fileSize) 예외");
					e.printStackTrace();
					usedChecking(false);
					transCeiveFailed();
					return ;
				}
				
			}
		}
		usedChecking(false);
		transCeiveFailed();
		return ;		
	}
	
	public void clientReceive()
	{
		transCeiveSuccess();
		usedChecking(true);
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		byte[] fileByteArray = null;
		int intFileSize;
		integerToByteArray.initialByteArray(fileByteArray);
		
		if(signal.toResponse(signal.request))
		{
			if(signal.toResponse(signal.byteReceive))
			{	
				try {
					input.read(fileSize);
					intFileSize = integerToByteArray.getInt(fileSize);
					fileByteArray = new byte[intFileSize];		
					if(!signal.toConfirm(signal.byteSize))
					{
						System.out.println("byte 받았다는 확인을 보내는 중 서버와 연결 예외 발생");
						transCeiveFailed();
						usedChecking(false);
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
								transCeiveSuccess();
								usedChecking(false);
								System.out.println("서버로 전송을 완료했습니다.");
								break;
							}
							
							else if(signal.toResponse(signal.byteSend))
							{
								input.read(fileByteArray, unitSize, byteArrayTransCeiverRule.extra);
								transCeiveSuccess();
								usedChecking(false);
								System.out.println("서버로 전송을 완료했습니다.");
								break;								
							}							
						}
						else if(signal.toResponse(signal.byteSend))
						{
							input.read(fileByteArray, counter*unitSize, unitSize);
							counter++;
						}						
					}
				} catch (IOException e) {
					System.out.println("서버가 요청을 하지 않습니다.");
					e.printStackTrace();
					transCeiveFailed();
					usedChecking(false);
					return ;
				}							
			}
		}
	}
	
	//서버가 클라이언트에게 받아 데이터 스트림을 전송한 클라이언트를 제외하고 방여 참여중인 다른 클라이언트에게 데이터 스트림 전송.
	public void serverTransCeive()
	{
		transCeiveSuccess();
		usedChecking(true);
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		byte[] fileByteArray = null;
		int intFileSize;
		integerToByteArray.initialByteArray(fileSize);
		
		//자기 자신은 필요없다.
		clientManagementList.remove(clientManagement);
		
		if(clientManagementList.size() <= 0)
			return ;
		
		
		if(signal.toResponse(signal.request))
		{
			if(signal.toResponse(signal.byteReceive))
			{	
				try {
					input.read(fileSize);
					intFileSize = integerToByteArray.getInt(fileSize);
					fileByteArray = new byte[intFileSize];		
					if(!signal.toConfirm(signal.byteSize))
					{
						System.out.println("클라이언트에게 byte사이즈 받았다는 응답을 보내지 못했습니다.");
						transCeiveFailed();
						usedChecking(false);
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
								transCeiveSuccess();
								usedChecking(false);
								System.out.println(clientManagement.joinRoom+"에 참가한 "+clientManagement.clientID+"의 데이터 스트림을 받았습니다.");
								System.out.println("받은 스트림의 크기는 "+intFileSize+"Byte 입니다.");
								break;
							}
							
							if(signal.toResponse(signal.byteSend))
							{
								input.read(fileByteArray, unitSize, byteArrayTransCeiverRule.extra);
								transCeiveSuccess();
								usedChecking(false);
								System.out.println(clientManagement.joinRoom+"에 참가한 "+clientManagement.clientID+"의 데이터 스트림을 받았습니다.");
								System.out.println("받은 스트림의 크기는 "+intFileSize+"Byte 입니다.");
								break;								
							}							
						}
						else if(signal.toResponse(signal.byteSend))
						{
							input.read(fileByteArray, counter*unitSize, unitSize);
							counter++;
						}						
					}
					
				} catch (IOException e) {
					System.out.println("클라이언트에게서 정상적으로 데이터를 못받았습니다.");
					e.printStackTrace();
					transCeiveFailed();
					usedChecking(false);
					return ;
				}							
			}
		}
		//여기까지는 클라에게 서버가 파일 받는거고(정확히는 데이터스트림)
		
		//아래부터는 서버가 클라이언트에게 뿌린다.		
		
		//clientManagementList는 서버스레드의 정적배열리스트가 아니라 따로 추려서 만든 리스트.
		while(true)
		{
			//호스트 클라이언트(프리뷰 전송을 요청한 클라이언트) 역시 사용중이므로 사용중 표시
			transCeiveSuccess();
			usedChecking(true);
			
			//다른클라이언트에게 전송을 위해 반복문 선언
			for(ClientManagement C:clientManagementList)
			{
				//다른 클라이언트와 송수신을 위해 버퍼스트림 선언
				BufferedOutputStream outputOtherClient;
				SignalData signalOtherClient = null;
				
				//서버와 연결된 클라이언트의 클래스 값 수정( 각 포트로의 중복 전송 요청을 사전에 막기위해 )
				toClientTransCeiveSuccess(C);
				toClientUsedChecking(C, true);
				
				//카메라인지 소리인지 구분해 전송 소켓 열기(초기화)
				if(this.CameraVoice)
				{
					try {
						outputOtherClient = new BufferedOutputStream(C.cameraSocket.getOutputStream());
						signalOtherClient = new SignalData(C.cameraSocket);
						
					} catch (IOException e) {
						System.out.println("["+C.clientID+"]["+C.clientIP+" 에게 프리뷰 전송실패");
						toClientTransCeiveFailed(C);
						toClientUsedChecking(C, false);
						transCeiveFailed();
						usedChecking(true);
						e.printStackTrace();
						continue;
					}					
				}
				else
				{
					try {
						outputOtherClient = new BufferedOutputStream(C.voiceSocket.getOutputStream());
						signalOtherClient = new SignalData(C.voiceSocket);
						
					} catch (IOException e) {
						System.out.println("["+C.clientID+"]["+C.clientIP+" 에게 보이스 전송실패");
						e.printStackTrace();
						toClientTransCeiveFailed(C);
						toClientUsedChecking(C, false);
						transCeiveFailed();
						usedChecking(true);
						continue;
					}					
				}
						
				//클라이언트에게 준비하라고 요청 및 데이터 스트림 전송
				if(signalOtherClient.toRequest())
				{
					if(signalOtherClient.toDoRequest(signal.byteReceive))
					{
						try {
							outputOtherClient.write(fileSize);
							outputOtherClient.flush();
							
							if(signalOtherClient.toResponse(signal.byteSize))
							{
								int counter = 0;
								int unitSize = byteArrayTransCeiverRule.fileUnitSize;
								int maxCounter = byteArrayTransCeiverRule.counter;
								while(true)
								{
									if(counter >= maxCounter)
									{
										if(counter*unitSize == fileByteArray.length)
										{
											transCeiveSuccess();
											usedChecking(false);
											toClientTransCeiveSuccess(C);
											toClientUsedChecking(C, true);
											System.out.println(C.joinRoom+"에 참가한"+C.clientID+"에게 데이터 스트림을 보냈습니다.");
											System.out.println("보낸 스트림의 크기는 "+byteArrayTransCeiverRule.fileSize+"Byte 입니다.");
											continue;
										}
										else if(signalOtherClient.toDoRequest(signal.byteSend))
										{
											outputOtherClient.write(fileByteArray, counter*unitSize, byteArrayTransCeiverRule.extra);
											outputOtherClient.flush();
											transCeiveSuccess();
											usedChecking(false);
											toClientTransCeiveSuccess(C);
											toClientUsedChecking(C, true);
											System.out.println(C.joinRoom+"에 참가한"+C.clientID+"에게 데이터 스트림을 보냈습니다.");
											System.out.println("보낸 스트림의 크기는 "+byteArrayTransCeiverRule.fileSize+"Byte 입니다.");											
											continue;
										}
									}
									else if(signalOtherClient.toDoRequest(signal.byteSend))
									{
										outputOtherClient.write(fileByteArray, counter*unitSize, unitSize);
										output.flush();
										counter++;										
									}
								}
							}
						} catch (IOException e) {
							System.out.println("["+C.clientID+"]["+C.clientIP+" 에게 전송실패");
							e.printStackTrace();
							toClientTransCeiveFailed(C);
							toClientUsedChecking(C, false);
							transCeiveFailed();
							usedChecking(true);
							continue;
						}						
					}
				}
				toClientTransCeiveFailed(C);		//실패할경우 if문을 빠져나온다.
				toClientUsedChecking(C, false);
				transCeiveFailed();
				usedChecking(true);
				continue;							//실패하면 다음 클라이언트에게 전송한다.
			}
		}
	}
	
	//간단한 체크 함수
	
	public void usedChecking(boolean used)	//사용하면 true,	안사용하면 false
	{
		if(this.CameraVoice)
			clientManagement.cameraUsed = used;
		else
			clientManagement.voiceUsed = used;
	}
	
	public void transCeiveFailed()			//송수신 실패
	{
		if(this.CameraVoice)
			clientManagement.cameraTransCeive = false;
		else
			clientManagement.voiceTransCeive = false;
	}
	
	public void transCeiveSuccess()			//송수신 성공
	{
		if(this.CameraVoice)
			clientManagement.cameraTransCeive = true;
		else
			clientManagement.voiceTransCeive = true;
	}
	
	// 이부분은 서버가 클라이언트에게 뿌릴때 사용하는 메소드
	public void toClientUsedChecking(ClientManagement clientManagement, boolean used)	//사용하면 true,	안사용하면 false
	{
		if(this.CameraVoice)
			clientManagement.cameraUsed = used;
		else
			clientManagement.voiceUsed = used;
	}
	
	public void toClientTransCeiveFailed(ClientManagement clientManagement)			//송수신 실패
	{
		if(this.CameraVoice)
			clientManagement.cameraTransCeive = false;
		else
			clientManagement.voiceTransCeive = false;
	}
	
	public void toClientTransCeiveSuccess(ClientManagement clientManagement)			//송수신 성공
	{
		if(this.CameraVoice)
			clientManagement.cameraTransCeive = true;
		else
			clientManagement.voiceTransCeive = true;
	}
}