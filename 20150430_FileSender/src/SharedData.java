
public class SharedData {
	int fileSize = 0;			//클라이언트간 통신을 위해 선언(파일사이즈)	
	byte[] fileSizeArray;
	public SharedData(int fileSize , int fileSizeIndex) {
		this.fileSize = fileSize;
		this.fileSizeArray = new byte[fileSizeIndex];
	}
}
