package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

//신호송수신 자체를 SignalData의 메소드로 작성할 예정이다.
public class SignalData {
	
	final int signalSize 		= 2;				//시그널 길이
	final int signalCount		= 15;				//시그널 갯수
	final int maxReCount 		= 3;				//최대 신호 재시도 횟수
	final byte[] request 		=	{ 0,0 };	//요청
	final byte[] response		=	{ 0,1 };	//응답함
	final byte[] wrong			=	{ 0,2 };	//올바르지 않음
	
	final byte[] location 		= 	{ 1,0 }; 	//위치
	final byte[] camera			=	{ 1,1 }; 	//카메라 프리뷰 이미지
	final byte[] voice			=	{ 1,2 }; 	//목소리
	final byte[] makeRoom		=	{ 1,3 };	//방 만들기
	final byte[] joinRoom		=	{ 1,5 };	//방 참여
	final byte[] exitRoom		=	{ 1,6 };	//방 나가기
	final byte[] roomList		=	{ 1,7 };	//방 목록
	final byte[] talk			=	{ 1,8 };	//채팅
	final byte[] writeYourName	=	{ 1,9 };	//이름 입력

	final byte[] byteSize		=	{ 2,0 };	//바이트배열크기
	final byte[] byteSend		= 	{ 2,1 }; 	//바이트배열보냄
	final byte[] byteReceive	=	{ 2,2 }; 	//바이트배열받음
	
	final byte[] exitServer		=	{ 9,0 };	//서버와 연결종료
	
	
	int reCount = 0;						//신호 재시도 횟수
	
	
	
	Socket socket;
	BufferedInputStream input=null;
	BufferedOutputStream output=null;
	
	
	//20150506
	//BufferedExceptionProcessingThread	=> read()가 오랫동안 블락하는걸 방지
	//문제점은 BEPThread가 본의아니게 신호를 보내는 도중에 닫아버릴수도 있다. 
	//그렇다면 버퍼스트림을 함수 호출때마다 여는건 어떨까 생각하는데, 오버헤드가 크지 않을까?
	//상관없으려나...?
	//BEPT란 공유영역을 만들어서 신호송수신이 완료되면 스레드가 자동으로 죽게끔 만들었다.
	//java서적의 채팅예제 창에서는 버퍼가 read()중에 상대방의 소켓이 닫히면 null을 반환한다. 그러면 굳이 내가 
	//그 부분에 대해 처리할 필요는 없는거네.
	//Closes this input stream and releases any system resources associated with the stream. Once the stream has been closed, further read(), available(), reset(), or skip() invocations will throw an IOException. Closing a previously closed stream has no effect.
	//인풋 스트림을 닫고 스트림과 관련된 자원을 방출한다는건, 소켓이려나... 헐!
	
	
	/*	메소드 목록
	*	public SignalData()													생성자
	*	public boolean initial()											버퍼스트림 초기화
	*	public boolean signalReCount(boolean countFlag)						숫자 카운트 함수, 무의미한 명령어가 3회 이상 들어오면 true 반환하는 기능을 만들기 위해 추가함.
	*	public boolean signalChecking(byte[] target, byte[] wantChecking)	target과 wantChecking가 같으면 true, 다르면 false 반환
	*	public String SignalByteToString(byte[] wantSignal)					wantSignal 신호이름을 String으로 반환
	*	public byte[] signalStringToByte(String wantSignal)					wantSignal 신호String을 byteArray로 반환 
	*	public boolean toRequest()											요청 신호를 보냄
	*	public boolean toDoRequest(byte[] wantSignal)						원하는 작업(wantSignal) 요청 신호 보냄
	*	public boolean toResponse(byte[] wantSignal)						원하는 신호(wantSignal) 가 들어오면 응답함.
	*	public boolean toConfirm(byte[] responseSignal)						원하는 신호(responseSignal)을 보내기만 한다. 이건 toResponse에게 보내기 위해서 만들었다.
	*
	*	read(byte[] 변수) 는 바이트 배열 스트림이 들어오길 기다린다.
	*	write(byte[] 변수) 는 바이트 배열 스트림을 소켓을 통해 상대방에게 전송한다. (보내길 기다리지 않는다.)
	*
	*/
	
	public SignalData(Socket socket)
	{
		this.socket = socket; 
	}
	
	
	//초기화(초기화 안하며 신호를 보낼수가 없다. 대신 각 신호 메소드에서 초기화를 알아서 시작한다.)
	public boolean initial()
	{
		try {			
			input = new BufferedInputStream(socket.getInputStream());
			output = new BufferedOutputStream(socket.getOutputStream());
			
		} catch (IOException e) {
			System.out.println("초기화 실패");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	//신호 재시도 카운트
	//카운트 이상으로 재시도 일어나면 true 반환
	public boolean signalReCount(boolean countFlag)
	{
		if(countFlag)
			this.reCount++;
		if(this.reCount > this.maxReCount)
			return true;
		return false;	
	}
		
	//신호 equal
	public boolean signalChecking(byte[] target, byte[] wantChecking)
	{
		for(int i=0; i<signalSize; i++)
		{
			if(target[i] == wantChecking[i])
				continue;
			else
				return false;
		}
		return true;
	}
	
	//신호 이름 반환(바이트->문자열)
	public String signalByteToString(byte[] wantSignal)
	{
		switch(wantSignal[0])
		{
		case 0:
			switch(wantSignal[1])
			{
			case 0: return "request";
			case 1: return "response";
			case 2: return "wrong";
			}
		case 1:
			switch(wantSignal[1])
			{
			case 0: return "location";
			case 1: return "camera";
			case 2: return "voice";
			case 3: return "makeRoom";
			case 4: return "delRoom";
			case 5: return "joinRoom";
			case 6: return "exitRoom";
			case 7: return "roomList";
			case 8: return "talk";
			case 9: return "writeYourName";
			}
		case 2:
			switch(wantSignal[1])
			{
			case 0:	return "byteSize";
			case 1: return "byteSend";
			case 2: return "byteReceive";
			}
		case 9:
			switch (wantSignal[1]) {
			case 0:	return "exitServer";				
			}
		default:return null;
		}
	}
	
	//신호바이트반환(원하는 신호가 없으면 null)
	public byte[] signalStringToByte(String wantSignal)
	{		
		if(wantSignal.equals("request")) return request;
		if(wantSignal.equals("response")) return response;
		if(wantSignal.equals("wrong")) return wrong;
		
		if(wantSignal.equals("location")) return location;
		if(wantSignal.equals("camera")) return camera;
		if(wantSignal.equals("voice")) return voice;
		if(wantSignal.equals("makeRoom")) return makeRoom;
		if(wantSignal.equals("joinRoom")) return joinRoom;
		if(wantSignal.equals("exitRoom")) return exitRoom;
		if(wantSignal.equals("roomList")) return roomList;
		if(wantSignal.equals("talk")) return talk;
		if(wantSignal.equals("writeYourName")) return writeYourName;
		
		if(wantSignal.equals("byteSize")) return byteSize;
		if(wantSignal.equals("byteSend")) return byteSend;
		if(wantSignal.equals("byteReceive")) return byteReceive;
		
		if(wantSignal.equals("exitServer"))	return exitServer;
		
		return null;		
	}
	
	//받은 시그널을 byte[]로 리턴
	public byte[] receiveSignalToByteArray()
	{
		byte[] temp = new byte[signalSize];
		try {
			input.read(temp);
		} catch (IOException e) {
			e.printStackTrace();
			return wrong;
		}
		return temp;		
	}
	
	
	//원하는 작업 요청을 하는 메소드 ( 상대방이 signal.response 라 응답해야 요청측에서 상대방이 신호를 받았다고 인지한다.)
	public boolean toDoRequest(byte[] wantSignal)
	{
		byte[] signalByte = new byte[signalSize];
		
		//원하는 신호가 null인 경우 false 리턴 (signal.signalStringToByte(value) 처리 위해)
		if(wantSignal == null)
			return false;
		
		if(input == null | output == null)
		{
			System.out.println("Signal 초기화 안했습니다.");
			return false;
		}
					
		try {
			output.write(wantSignal);		
			output.flush();
		} catch (IOException e) {
			System.out.println("Request 예외");
			e.printStackTrace();
			return false;
		}
		
		try {
			input.read(signalByte);
		} catch (IOException e) {
			System.out.println("Request의 response예외");
			e.printStackTrace();
			return false;
		}		
		if(signalChecking(signalByte, response))
			return true;
		else 
			return false;
	}
	
	//toDoRequest에 대응하는 메소드(이 부분 수정 가해야할듯)
	public boolean toAccept(byte[] wantSignal)
	{
		byte[] signalByte = new byte[signalSize];
		
		if(input == null | output == null)
		{
			System.out.println("Signal 초기화 안했습니다.");
			return false;
		}
					
		try {
			input.read(signalByte);
		} catch (IOException e) {
			System.out.println("response 소켓 입력대기중 예외");
			e.printStackTrace();
			return false;
		}
		if(signalChecking(signalByte, wantSignal))
		{
			try {
				output.write(response);
				output.flush();
			} catch (IOException e) {
				System.out.println("response중 소켓 response보냄 예외");
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		else
		{
			try {
				output.write(wrong);
				output.flush();	
			} catch (IOException e) {
				System.out.println("response중 wrong보냄 예외");
				e.printStackTrace();
			}
			return false;
		}
	}	

	//요청에 대하여 응답(toCatchResponse 대응)
	public boolean toDoResponse(byte[] responseSignal)
	{
		try {
			output.write(responseSignal);
			output.flush();
			return true;
		} catch (IOException e) {
			System.out.println("toConfirm(요청에대한 응답)실패");
			e.printStackTrace();
			return false;
		}
	}
	
	//응답신호를 확인만 함(toDoResponse 대응)
	public boolean toCatchResponse(byte[] OKsignal)
	{
		byte[] temp = new byte[signalSize];
		try {
			input.read(temp);
			if(signalChecking(temp, OKsignal))
				return true;
			else
				return false;
		} catch (IOException e) {
			System.out.println("toDoResponse신호 받는데 예외");
			e.printStackTrace();
			return false;
		}
		
	}
}
/* 혹시모르니 
if(fileSize%unitSize == 0)
counter = (fileSize/unitSize);
else 
{
counter = (fileSize/unitSize) + 1;
extra = fileSize%unitSize;
}
*/