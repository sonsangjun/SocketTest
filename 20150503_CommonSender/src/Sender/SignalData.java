package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import javax.xml.transform.OutputKeys;

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
	
	//BufferedExceptionProcessingThread	=> read()가 오랫동안 블락하는걸 방지
	
	public SignalData(int waitTime)
	{
		this.waitTime = waitTime;		
	}
	
	//신호 체킹
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
	
	//요청 신호 보냄
	public boolean toRequest(BufferedInputStream input, BufferedOutputStream output)
	{
		BufferedExceptionProcessingThread thread = new BufferedExceptionProcessingThread(input, waitTime);
		
		byte[] signalByte = new byte[signalSize];
		
		try {
			output.write(response);			
		} catch (IOException e) {
			System.out.println("Request 예외");
			e.printStackTrace();
			return false;
		}
		
		try {
			thread.start();
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
	public boolean toDoRequest(BufferedInputStream input, BufferedOutputStream output, byte[] wantSignal)
	{
		BufferedExceptionProcessingThread thread = new BufferedExceptionProcessingThread(input, waitTime);
		
		byte[] signalByte = new byte[signalSize];
		
		try {
			output.write(wantSignal);			
		} catch (IOException e) {
			System.out.println("Request 예외");
			e.printStackTrace();
			return false;
		}
		
		try {
			thread.start();
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
	public boolean toResponse(BufferedInputStream input, BufferedOutputStream output, byte[] wantSignal)
	{
		BufferedExceptionProcessingThread thread = new BufferedExceptionProcessingThread(input, waitTime);
		
		byte[] signalByte = new byte[signalSize];
		
		try {
			thread.start();
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