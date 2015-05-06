package Sender;

import java.io.BufferedInputStream;
import java.io.IOException;

//클래스 명이 긴데, 그냥 오랬동안 입력받는 부분의 블록을 막기위해 만든 스레드(BufferedInputStream.read(byte[]) 이거때문) 
public class BufferedExceptionProcessingThread extends Thread{
	int waitTime;
	
	BEPTSharedData shared;
	BufferedInputStream input;
	
	public BufferedExceptionProcessingThread(BufferedInputStream input,BEPTSharedData shared,int waitTime)
	{
		this.input = input;
		this.shared = shared;
		this.waitTime = waitTime;
	}
	
	public void run()
	{
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
		if(!shared.finished)
		try {
			System.out.println(this.getName()+"소켓에서 응답이 없어 Output닫습니다.");
			input.close();
			input = null;
		} catch (IOException e) {
			System.out.println("BEPThread에서 버퍼 강제 닫기 예외발생");
			e.printStackTrace();
			return ;
		}
		else
		{
			System.out.println("으앙 쥬금");
			return ;
		}			
	}
}
