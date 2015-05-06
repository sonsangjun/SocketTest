package Sender;

/* ByteArray 크기에 따른 파일 사이즈 범위
 * 배열인덱스 약속
 * 인덱스 끝은 당연히 못쓰고, 이 위치수준에 따라 byte배열 선언하기
 * 단위:Byte		Index위치	크기수준(약Max)
 * 	0~100		0			Byte수준(100Byte)	
 * 	100~10000	1			Byte~KByte수준(1KByte)
 * 	10^4~10^6	2			KByte수준(100KByte)
 * 	10^6~10^8	3 			KByte~MByte수준(1MByte)
 * 	10^8~10~10	4			MByte수준(100MByte)
 * 
 * ex)	FileSize 1,000 Byte면 
 * 		Index 위치가 1이므로 배열은 new Byte[2]로 선언해야함.
 * 
 * 	100MByte 이상은 필요없을듯 음성이 커봤자 얼마나 크겠어
 * 	그리고 int 데이터 범위내에서만 변환하고 싶당
 * 
 * 파일 사이즈 변환으로 썼지만, assignClientID변환에도 사용하므로
 * 클래스 명을 바꾸었다.
 */


public class IntegerToByteArray {
	final int divide = 100;
	
	//옳게 출력되는지 테스트 메소드
	public void Test(int intFileSize,byte[] byteFileSize)
	{
		int temp = 0;
		getBytes(intFileSize,byteFileSize);
		System.out.println("파일 사이즈는 "+intFileSize+" Byte\n변환된 배열 : ");
		for(byte i: byteFileSize)
			System.out.printf("%d ",i);
		
		System.out.println("\n바이트배열을 다시 숫자로 변환");
		temp = getInt(byteFileSize);
		System.out.println("변환된 숫자 "+temp+"\n테스트 종료\n");
	}
	
	//파일크기를 바이트배열로 변환
	public void getBytes(int intFileSize,byte[] byteFileSize)
	{
		int temp = intFileSize;
		initialByteArray(byteFileSize);
		
		for(int i=0; i<byteFileSize.length; i++)
		{
			if(temp == 0)
				break;
			
			if(temp % divide == 0)
			{
				byteFileSize[i] = 0;
				temp /= divide;
			}
				
			else
			{
				byteFileSize[i] = (byte)(temp % divide);
				temp /= divide;
			}			
		}
	}
	
	//바이트배열로 표시된 파일 크기를 int로 변환
	public int getInt(byte[] byteFileSize)
	{
		int temp=0;
		if(byteFileSize[0] > 0)
			temp=byteFileSize[0];
		
		for(int i=1; i<byteFileSize.length; i++)
		{
			if(byteFileSize[i] > 0)
				temp += (byteFileSize[i]*(int)(Math.pow(divide, i)));
			else if(byteFileSize[i] == -1)	//종료조건
				break;
		}
		return temp; 
	}
	
	//바이트 배열 -1로 초기화
	public void initialByteArray(byte[] byteFileSize)
	{
		for(int i=0; i<byteFileSize.length; i++)
			byteFileSize[i] = -1;
	}
}
