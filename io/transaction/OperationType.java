package oxygen.io.transaction;

/**
 * 实体操作类型
 * @author 赖昆
 * @since 1.0, 2007-04-23
 * @version 1.0
 */
public enum OperationType {
	
	/**
	 * 插入
	 */
	INSERT( (byte) 0 ),
	
	/**
	 * 删除
	 */
	DELETE( (byte) 1 ),
	
	/**
	 * 修改
	 */
	UPDATE( (byte) 2 );
	
	private final byte value;
	
	private OperationType( byte value ) {
		this.value = value;
	}
	
	// 得到实体操作类型的值
	byte value() {
		return value;
	}
}
