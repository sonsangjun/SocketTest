package Sender;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;


/////////////////////////////////////////////////////////////////////
//79, 111 Line에 테스트 메소드 있습니다. 안드로이드 개발할때 지우던가 주석처리 해주세요/
/////////////////////////////////////////////////////////////////////
public class SocketPushThread extends Thread{
	ValueCollections value = new ValueCollections();
	boolean _server = false;	//서버 여부
	byte[] cameraByteArray = null;
	byte[] voiceByteArray = null;
	
	SignalData pushSignal;
	
	Socket pushSocket;
	SocketPushUsed socketPushUsed;
	
	ByteArrayTransCeiverRule cameraTransCeiver;
	ByteArrayTransCeiverRule voiceTransCeiver;
	
	ByteArrayTransCeiverRule byteArrayTransCeiverRule;
	ByteArrayTransCeiver byteArrayTransCeiver;
	
	//클라이언트 측 생성자
	public SocketPushThread(Socket pushSocket,SocketPushUsed socketPushUsed, ByteArrayTransCeiverRule cameraTransCeiver, ByteArrayTransCeiverRule voiceTransCeiver) {
		this.pushSocket = pushSocket;
		this.socketPushUsed = socketPushUsed;
		this.cameraTransCeiver = cameraTransCeiver;
		this.voiceTransCeiver = voiceTransCeiver;
	}
	
	//서버측 생성자
	public SocketPushThread(boolean _server, ByteArrayTransCeiverRule byteArrayTransCeiverRule) {
		this._server = _server;
		this.byteArrayTransCeiverRule = byteArrayTransCeiverRule;
	}
	
	public void run()
	{
		//클라이언트(받는 부분)	푸쉬로 뭘 받을지 알려준다. (read(byte[]) 그리고, transCeive값을 주어 if문 분기한다.
		if(!_server)	
		{			
			//push시작
			while(true)
			{				
				//push 스레드가 두개가 생성되기 때문에 락걸어준다. 
				//(camera,voice 다 쓰면 서버측에서도 푸쉬를 못날리니 걱정 안해도 된다.)
				if(socketPushUsed.socketPushUsed)
					continue;
				
				pushSignal = new SignalData(pushSocket);
				pushSignal.initial();
				byte[] tempSignal = new byte[pushSignal.signalSize];
				
				tempSignal = pushSignal.receiveSignalToByteArray();
				synchronized (socketPushUsed) {
					socketPushUsed.socketPushUsed = true;
				}
				
				if(pushSignal.signalChecking(tempSignal, pushSignal.camera))
				{
					if(pushSignal.toDoResponse(pushSignal.response))	//서버측에서 push.todorequest요청(429line.ByteArray~Ceiver.java)
					{
						byteArrayTransCeiver = new ByteArrayTransCeiver(cameraTransCeiver);	
						cameraByteArray = byteArrayTransCeiver.TransCeiver(); 	//TransCeiver()가 서버에서 받은 카메라 데이터 스트림을 반환한다.(byte[])
						if(cameraByteArray == null)								//안드로이드에서 이 데이터를 살릴 방법을 찾아야 한다.
						{
							synchronized (socketPushUsed) {
								socketPushUsed.socketPushUsed = false;
							}
							System.out.println("Camera이미지 받기 실패");
							continue;
						}
						synchronized (socketPushUsed) {
							socketPushUsed.socketPushUsed = false;
						}
						/////////////////////////////////////////////////////////////////////////////
						//테스트 메소드 호출. 안드로이드에서 작업할시 이 데이터 스트림을 이용해 화면에 출력되도록 코드를 짜주세요.
						if(testMethod(value.imageFileName,cameraByteArray))
							System.out.println("데이터스트림을 파일로 출력하였습니다.");	
						/////////////////////////////////////////////////////////////////////////////
					}
					else
					{
						System.out.println("push스레드 응답실패");
						synchronized (socketPushUsed) {
							socketPushUsed.socketPushUsed = false;
						}
						continue;
					}
				}
				else if(pushSignal.signalChecking(tempSignal, pushSignal.voice))
				{
					if(pushSignal.toDoResponse(pushSignal.response))	//서버측에서 push.todorequest요청(429line.ByteArray~Ceiver.java)
					{
						byteArrayTransCeiver = new ByteArrayTransCeiver(voiceTransCeiver);	//왜 TestMethod에서 호출하면 null예외가 발생하나?
						voiceByteArray = byteArrayTransCeiver.TransCeiver();	//TransCeiver()가 서버에서 받은 음성 데이터 스트림을 반환한다.(byte[])
						if(voiceByteArray == null)								//안드로이드에서 이 데이터를 살릴 방법을 찾아야 한다.
						{
							synchronized (socketPushUsed) {
								socketPushUsed.socketPushUsed = false;
							}
							System.out.println("Voice 받기 실패");
							continue;
						}
						synchronized (socketPushUsed) {
							socketPushUsed.socketPushUsed = false;
						}
						/////////////////////////////////////////////////////////////////////////////
						//테스트 메소드 호출. 안드로이드에서 작업할시 이 데이터 스트림을 이용해 화면에 출력되도록 코드를 짜주세요.
						if(testMethod(value.voiceFileName,voiceByteArray))
							System.out.println("데이터스트림을 파일로 출력하였습니다.");	
						/////////////////////////////////////////////////////////////////////////////
					}
					else
					{
						System.out.println("push스레드 응답실패");
						synchronized (socketPushUsed) {
							socketPushUsed.socketPushUsed = false;
						}
						continue;						
					}
				}
				else		//푸쉬는 위의 두 신호가 아닌 다른게 날라오면 죽는다.
					break;
				
				synchronized (socketPushUsed) {
					socketPushUsed.socketPushUsed = false;
				}			
			}
		}		
		
		
		//서버(보내고 받는 부분)
		else			
		{
			byteArrayTransCeiver = new ByteArrayTransCeiver(byteArrayTransCeiverRule);
			if(byteArrayTransCeiver.TransCeiver() != null);
		}
		//서버 끝
		
	}

	//테스트 메소드(결과물을 정상적으로 받았는지 파일로 출력해 확인)
	public boolean testMethod(String fileName, byte[] byteArray)
	{
		FileOutputStream outputfile;
		try {
			outputfile = new FileOutputStream(fileName);			
			outputfile.write(byteArray);			
			outputfile.flush();
			outputfile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}		
}
