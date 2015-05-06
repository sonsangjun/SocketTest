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
	
	final byte[] fileSend = 	{ 2,0 }; 	//파일보냄
	final byte[] fileReceive=	{ 2,1 }; 	//파일받음
	
	int waitTime;
	
	Socket socket;
	BEPTSharedData shared;	//BufferedExceptionProcessingThread와 통신하기 위해 만든 공유영역
	BufferedInputStream input=null;
	BufferedOutputStream output=null;
	
	
	//20150506
	//BufferedExceptionProcessingThread	=> read()가 오랫동안 블락하는걸 방지
	//문제점은 BEPThread가 본의아니게 신호를 보내는 도중에 닫아버릴수도 있다. 
	//그렇다면 버퍼스트림을 함수 호출때마다 여는건 어떨까 생각하는데, 오버헤드가 크지 않을까?
	//상관없으려나...?
	//BEPT란 공유영역을 만들어서 신호송수신이 완료되면 스레드가 자동으로 죽게끔 만들었다.
	
	
	public SignalData(Socket socket, int waitTime)
	{
		this.waitTime = waitTime;		
		this.socket = socket; 
	}
	
	//초기화(초기화 안하며 신호를 보낼수가 없다. 대신 각 신호 메소드에서 초기화를 알아서 시작한다.)
	private boolean initial()
	{
		try {	//처음 초기화가 아니라면 버퍼가 둘중 하나는 열려있을 수 있으므로 닫아주고 다시 연다.
			if(input != null)
				input.close();
			if(output != null)
				output.close();
			
			input = new BufferedInputStream(socket.getInputStream());
			output = new BufferedOutputStream(socket.getOutputStream());
			
		} catch (IOException e) {
			System.out.println("초기화 실패");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	//버퍼 닫기
	private boolean closeBuffer()
	{
		boolean inputClose = true;
		boolean outputClose = true;
		
		try {
			if(input != null)
				input.close();
		} catch (IOException e) {
			System.out.println("closeBuffer중 input 닫기예외");
			e.printStackTrace();
			inputClose = false;
		}
				
		try {
			if(output != null)
				output.close();
		} catch (IOException e) {
			System.out.println("closeBuffer중 output 닫기예외");
			e.printStackTrace();
			outputClose = false;
		}
		
		if(inputClose && outputClose)
			return true;			
		else
		{
			System.out.println("Signal버퍼 닫기실패");
			return false;
		}		
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
			}
		case 2:
			switch(wantSignal[2])
			{
			case 0: return "fileSend";
			case 1: return "fileReceive";
			}
		default:return null;
		}
	}
	
	//요청 신호 보냄 (toDoRequest보다 먼저 보내라)
	public boolean toRequest()	
	{
		//신호보내기 전에 버퍼와 소켓스트림 연결
		if(!initial())
		{
			System.out.println("버퍼 스트림 초기화 실패로 Request를 보낼수 없습니다.");
			return false;
		}
			
		shared = new BEPTSharedData();
		BufferedExceptionProcessingThread thread = new BufferedExceptionProcessingThread(input, shared,waitTime);
		byte[] signalByte = new byte[signalSize];
		
		if(input == null | output == null)
		{
			System.out.println("Signal 초기화 안했습니다.");
			closeBuffer();
			return false;
		}
		
		try {
			output.write(request);			
			output.flush();
		} catch (IOException e) {
			System.out.println("Request 예외");
			e.printStackTrace();
			closeBuffer();
			return false;
		}
		
		try {
			thread.start();
			input.read(signalByte);
		} catch (IOException e) {
			System.out.println("Request의 response예외");
			e.printStackTrace();
			closeBuffer();
			return false;
		}		
		
		shared.finished = true;
		closeBuffer();
		if(signalChecking(signalByte, response))
			return true;
		else 
			return false;
	}
	
	//이 함수는 먼저 request를 보낸 이후에 호출할 수 있는 함수 (모양이 거의 같지만 엄연히 호출 순서가 있다.)
	public boolean toDoRequest(byte[] wantSignal)
	{
		//신호보내기 전에 버퍼와 소켓스트림 연결
		if(!initial())
		{
			System.out.println("버퍼 스트림 초기화 실패로 Request를 보낼수 없습니다.");
			return false;
		}
		
		shared = new BEPTSharedData();
		BufferedExceptionProcessingThread thread = new BufferedExceptionProcessingThread(input, shared,waitTime);
		byte[] signalByte = new byte[signalSize];
		
		if(input == null | output == null)
		{
			System.out.println("Signal 초기화 안했습니다.");
			closeBuffer();
			return false;
		}
					
		try {
			output.write(wantSignal);		
			output.flush();
		} catch (IOException e) {
			System.out.println("Request 예외");
			e.printStackTrace();
			closeBuffer();
			return false;
		}
		
		try {
			thread.start();
			input.read(signalByte);
		} catch (IOException e) {
			System.out.println("Request의 response예외");
			e.printStackTrace();
			closeBuffer();
			return false;
		}		
		
		shared.finished = true;
		closeBuffer();
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
		
		shared = new BEPTSharedData();
		BufferedExceptionProcessingThread thread = new BufferedExceptionProcessingThread(input, shared,waitTime);
		byte[] signalByte = new byte[signalSize];
		
		if(input == null | output == null)
		{
			System.out.println("Signal 초기화 안했습니다.");
			closeBuffer();
			return false;
		}
					
		try {
			thread.start();
			input.read(signalByte);
		} catch (IOException e) {
			System.out.println("response 소켓 입력대기중 예외");
			e.printStackTrace();
			closeBuffer();
			return false;
		}
		
		shared.finished = true;
		if(signalChecking(signalByte, wantSignal))
		{
			try {
				output.write(response);
				output.flush();
				closeBuffer();		
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
				closeBuffer();		
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