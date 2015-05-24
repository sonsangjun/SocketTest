
public class SharedData {
	boolean DownComplete = false;	//파일 다운 받았는지 보냈는지 확인(true면 완료)
	int counter = 0;				//파일 분할 횟수
	int fileSize = 0;				//클라이언트간 통신을 위해 선언(파일사이즈)	
	int extra = 0;					//파일의 마지막 분할부분의 남은부분(인덱스경계초과 예외 방지)
	byte[] fileSizeArray;
	public SharedData(int fileSize , int fileSizeIndex) {
		this.fileSize = fileSize;
		this.fileSizeArray = new byte[fileSizeIndex];
	}
}
