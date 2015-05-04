package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends Thread {
	int portNum;
	int waitTime;
	
	String ServerIP;
	Socket socket;
	SignalData signal;
	
	BufferedInputStream packetInput;
	BufferedOutputStream packetOutput;
	
	//정적변수로 유틸리티 패키지에 ArrayList 선언하고, 타입은 사용자가 선언 한 클래스 . 클래스 안에는 스트림 입출력및 참여한 방번호를 넣어면 된다.
	//그러면 파일 송수신, 위치 정보 송수신등을 쉽게 관리할 수 있다.
	//그외 정적변수로 방 목록을 담는 ArrayList 필요할듯 방을 만들거나 참여할때 참고해야하므로...
	
	public Client(String ServerIP, int portNum,int waitTime)
	{
		this.ServerIP = ServerIP;		
		this.portNum = portNum;
		this.waitTime = waitTime;
	}
	
	public void run()
	{
		signal = new SignalData(waitTime);
		byte[] signalByte = new byte[signal.signalSize];
		
		try {
			System.out.println("서버 연결중");
			socket = new Socket(ServerIP, portNum);
		} catch (IOException e) {
			System.out.println("서버로 연결중 예외");
			e.printStackTrace();
			return ;
		}
		try {
			packetInput = new BufferedInputStream(socket.getInputStream());
			packetOutput = new BufferedOutputStream(socket.getOutputStream());
		} catch (IOException e1) {
			System.out.println(this.getName()+"스트림 예외");
			e1.printStackTrace();
			return;
		}
	}
	
	public void Test()
	{
		while(true)
		{
			//원래대로면 방 만들던가 참여하던가 선택해야지( 테스트 이므로 제낀다. )
			System.out.println("서버와 연결되었습니다.");
			System.out.println("신호 테스트");
			signal.toRequest(packetInput, packetOutput);
		}	
	}
}
