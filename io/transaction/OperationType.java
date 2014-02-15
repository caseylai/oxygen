package oxygen.io.transaction;

/**
 * ʵ���������
 * @author ����
 * @since 1.0, 2007-04-23
 * @version 1.0
 */
public enum OperationType {
	
	/**
	 * ����
	 */
	INSERT( (byte) 0 ),
	
	/**
	 * ɾ��
	 */
	DELETE( (byte) 1 ),
	
	/**
	 * �޸�
	 */
	UPDATE( (byte) 2 );
	
	private final byte value;
	
	private OperationType( byte value ) {
		this.value = value;
	}
	
	// �õ�ʵ��������͵�ֵ
	byte value() {
		return value;
	}
}
