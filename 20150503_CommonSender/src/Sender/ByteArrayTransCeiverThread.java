package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;

//서버,클라이언트 스레드와 독립적으로 돌아감 .
//크기가 큰 카메라프리뷰나 음성 송수신에 사용.


/* 20150506 메소드 목록
 * 
 * 
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
	
	//서버에서 ClientManageList를 일일이 뒤지려면 오래걸리므로 이 스레드를 돌리기전에 뿌려야하는 명단을 서버에게 추려서 주도록하자.(ServerThread에도 남김)
	//클라이언트는 ClientManagementList가 필요없다. 즉, 생성자 호출후 ClientManagementList 가 null이면 클라, 아니면 서버로 간주.
	public ByteArrayTransCeiverThread(int clientID, boolean transCeive, boolean CameraVoice, byte[] fileByteArray, ArrayList<ClientManagement> clientManagementList, ClientManagement clientManagement) {
		
		this.clientID = clientID;
		this.transCeive = transCeive;						//서버일경우 이 값은 무의미하다.
		this.CameraVoice = CameraVoice;
		this.fileByteArray = fileByteArray;
		this.clientManagementList = clientManagementList;	//클라이언트면 null을 매개변수가 받는다.
		this.clientManagement = clientManagement;			
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
									output.write(fileByteArray);
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
		
	}
	
	public void serverTransCeive()
	{
		transCeiveSuccess();
		usedChecking(true);
		byte[] fileSize = new byte[integerToByteArray.fileSizeIndex];
		int intFileSize;
		integerToByteArray.initialByteArray(fileSize);
		
		//자기 자신은 필요없다.
		clientManagementList.remove(clientManagement);
		
		if(signal.toResponse(signal.request))
		{
			if(signal.toResponse(signal.byteReceive))
			{	
				try {
					input.read(fileSize);
					intFileSize = integerToByteArray.getInt(fileSize);
					signal.toConfirm(signal.byteSize);
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					transCeiveFailed();
					usedChecking(false);
					return ;
				}							
			}
		}
		
		
		//clientManagementList는 서버스레드의 정적배열리스트가 아니라 따로 추려서 만든 리스티이므로 안에 내용물을 수정해도 무방하다.
		while(true)
		{
			for(ClientManagement C:clientManagementList)
			{
				
			}
		}
		
	}
	
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
}

