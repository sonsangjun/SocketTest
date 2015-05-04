package Sender;

import java.io.BufferedInputStream;
import java.io.IOException;

//클래스 명이 긴데, 그냥 오랬동안 입력받는 부분의 블록을 막기위해 만든 스레드(BufferedInputStream.read(byte[]) 이거때문) 
public class BufferedExceptionProcessingThread extends Thread{
	
	int waitTime;
	
	BufferedInputStream input;
	
	
	
	public BufferedExceptionProcessingThread(BufferedInputStream input, int waitTime)
	{
		this.input = input;
		this.waitTime = waitTime;
	}
	
	public void run()
	{
		try {
			this.sleep(waitTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ;
		}
		
		try {
			input.close();
			input = null;
		} catch (IOException e) {
			System.out.println("BEPThread에서 버퍼 강제 닫기 예외발생");
			e.printStackTrace();
			return ;
		}
	}
}
