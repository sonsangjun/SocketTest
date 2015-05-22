package Sender;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

//20150521 카메라 프리뷰 스레드
public class SocketCameraThread extends Thread{
	ValueCollections value = new ValueCollections();
	boolean _server = false;	//서버 여부
	
	Socket eventSocket;
	Socket cameraSocket;
	SocketCameraUsed socketCameraUsed;
	SocketEventUsed socketEventUsed;
	ByteArrayTransCeiverRule byteArrayTransCeiverRule = new ByteArrayTransCeiverRule();
	ByteArrayTransCeiver byteArrayTransCeiver;
	
	public SocketCameraThread(Socket eventSocket, SocketEventUsed socketEventUsed, Socket cameraSocket, SocketCameraUsed socketCameraUsed) {
		this.eventSocket = eventSocket;
		this.cameraSocket = cameraSocket;
		this.socketCameraUsed = socketCameraUsed;
	}
	public SocketCameraThread(boolean _server, ByteArrayTransCeiverRule byteArrayTransCeiverRule) {
		this._server = _server;
		this.byteArrayTransCeiverRule = byteArrayTransCeiverRule;
	}
	
	public void rnu()
	{
		if(!_server)	//클라이언트(받는 부분)
		{
			byteArrayTransCeiverRule.socketEventUsed = this.socketEventUsed;
			byteArrayTransCeiverRule.socketCameraUsed = this.socketCameraUsed;	//여기로 데이터가 들어온다. (message에 데이터 있음)
			byteArrayTransCeiverRule.cameraSocket = this.cameraSocket;
			byteArrayTransCeiverRule.transCeive = false;	//해당 스레드는 무조건 받는다.
			byteArrayTransCeiverRule.CameraVoice = true;	//카메라 프리뷰를 받는다.
			
			byteArrayTransCeiver = new ByteArrayTransCeiver(byteArrayTransCeiverRule);
			
			while(true)
			{
				if(!this.socketCameraUsed.socketCameraThreadKill)
					byteArrayTransCeiver.clientReceive();			//데이터 스트림을 받는 메소드 입니다.
				else
					break;			
				
				//여기부터는 테스트 메소드 호출. 안드로이드에서 작업할시 이 데이터 스트림을 이용해 화면에 출력되도록 코드를 짜주세요.
				if(testMethod())
					System.out.println("데이터스트림을 파일로 출력하였습니다.");
			}	
			
		}
		else			//서버(보내고 받는 부분)
		{
			byteArrayTransCeiver = new ByteArrayTransCeiver(byteArrayTransCeiverRule);
			byteArrayTransCeiver.serverTransCeive();
		}			
	}
	
	
	
	//테스트 메소드(결과물을 정상적으로 받았는지 파일로 출력해 확인)
	public boolean testMethod()
	{
		FileOutputStream outputfile;
		try {
			outputfile = new FileOutputStream(value.fileName);
			outputfile.write(this.socketCameraUsed.message);
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
