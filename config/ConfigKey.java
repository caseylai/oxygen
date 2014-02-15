package oxygen.config;

/**
 * <p>Oxygen���ݿ������ļ��ļ�ö��</p>
 * <p>������ö�ٳ������滻���е�"_"Ϊ"."��תΪСд��
 * �磺DATABASE_ROOT�ļ���Ϊ"database.root"</p>
 * @author ����
 * @since 1.0, 2006-12-12
 * @version 1.0
 */
public enum ConfigKey {
	
	/**
	 * ���ݿ��Ŀ¼λ��
	 */
	DATABASE_ROOT( null ),
	
	/**
	 * �û���ʵ��jar�ļ�
	 */
	ENTITY_JAR( null ),
	
	/**
	 * ���ݴ洢�ı��뷽ʽ��Ĭ��ΪUTF-8
	 */
	ENCODE( "UTF-8" );

	// �����ļ��еļ�
	private String key;
	
	// �����ļ��м���Ĭ��ֵ
	private String defaultValue;
	
	private ConfigKey( String defaultValue ) {
		this.defaultValue = defaultValue;
		key = name().replaceAll( "_", "." ).toLowerCase();
	}
	
	/**
	 * �õ������ļ��еļ�
	 * @return �����ļ��еļ�
	 */
	public String key() {
		return key;
	}
	
	/**
	 * �õ������ļ��м���Ĭ��ֵ
	 * @return �����ļ��м���Ĭ��ֵ
	 */
	public String defaultValue() {
		return defaultValue;
	}
}
