package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

//신호송수신 자체를 SignalData의 메소드로 작성할 예정이다.
public class SignalData {
	final int signalSize = 2;				//시그널 길이
	final int signalLength = 8;				//시그널 갯수
	final byte[] request =		{ 0,0 };	//요청
	final byte[] response =		{ 0,1 };	//응답함
	final byte[] wrong	=		{ 0,2 };	//올바르지 않음
	
	final byte[] location = 	{ 1,0 }; 	//위치
	final byte[] camera	=		{ 1,1 }; 	//카메라 프리뷰 이미지
	final byte[] voice	=		{ 1,2 }; 	//목소리
	final byte[] makeRoom	=	{ 1,3 };	//방 만들기
	final byte[] delRoom	=	{ 1,4 };	//방 없애기
	
	
	final byte[] byteSend = 	{ 2,0 }; 	//바이트배열보냄
	final byte[] byteReceive=	{ 2,1 }; 	//바이트배열받음
	
	
	
	int waitTime;
	
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
	*	public boolean signalChecking(byte[] target, byte[] wantChecking)	target과 wantChecking가 같으면 true, 다르면 false 반환
	*	public String SignalName(byte[] wantSignal)							wantSignal 신호이름을 String으로 반환
	*	public boolean toRequest()											요청 신호를 보냄
	*	public boolean toDoRequest(byte[] wantSignal)						원하는 작업(wantSignal) 요청 신호 보냄
	*	public boolean toResponse(byte[] wantSignal)						원하는 신호(wantSignal)가 들어오면 응답함.
	*
	*/
	
	public SignalData(Socket socket, int waitTime)
	{
		this.waitTime = waitTime;		
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
	
	//신호 이름 반환
	public String SignalName(byte[] wantSignal)
	{
		switch(wantSignal[0])
		{
		case 0:
			switch(wantSignal[1])
			{
			case 0: return "request";
			case 1: return "response";
			}
		case 1:
			switch(wantSignal[1])
			{
			case 0: return "location";
			case 1: return "camera";
			case 2: return "voice";
			case 3: return "makeRoom";
			case 4: return "delRoom";
			}
		case 2:
			switch(wantSignal[2])
			{
			case 0: return "byteSend";
			case 1: return "byteReceive";
			}
		default:return null;
		}
	}
	
	//요청 신호 보냄 (toDoRequest보다 먼저 보내라)
	public boolean toRequest()	
	{		
		byte[] signalByte = new byte[signalSize];
		
		if(input == null | output == null)
		{
			System.out.println("Signal 초기화 안했습니다.");
			return false;
		}
		
		try {
			output.write(request);			
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
	
	//이 함수는 먼저 request를 보낸 이후에 호출할 수 있는 함수 (모양이 거의 같지만 엄연히 호출 순서가 있다.)
	public boolean toDoRequest(byte[] wantSignal)
	{
		byte[] signalByte = new byte[signalSize];
		
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
			
	//원하는 응답이 오는지 체킹
	
	public boolean toResponse(byte[] wantSignal)
	{
		//신호송수신 전에 버퍼와 소켓스트림 연결
		if(!initial())
		{
			System.out.println("버퍼 스트림 초기화 실패로 Request를 보낼수 없습니다.");
			return false;
		}
		
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