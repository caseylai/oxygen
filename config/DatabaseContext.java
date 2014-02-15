package oxygen.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * ���ݿ������ģ�ά�����ݿ������������Ϣ
 * @author ����
 * @since 1.0, 2006-12-12
 * @version 1.0
 */
public class DatabaseContext {
	
	// ���ݿ������ĵ�ӳ���
	private static Map<ConfigKey, String> propertiesMap;

	private DatabaseContext() {}

	/**
	 * ȡ�������м�Ϊkey��ֵ
	 * @param key ��
	 * @return key��Ӧ��ֵ����key�����������У�����null
	 */
	public static String get( ConfigKey key ) {
		return propertiesMap.get( key );
	}
	
	// �������ļ��ж���������ӳ���ע�ᵽ������
	static void register( Map<String, String> configMap ) throws IllegalArgumentException {
		
		Map<ConfigKey, String> map = new HashMap<ConfigKey, String>();
		
		for( ConfigKey key : ConfigKey.values() ) {
			String value = configMap.get( key.key() );
			if ( value == null ) value = key.defaultValue();
			if ( value == null ) {
				throw new IllegalArgumentException( key.key() );
			} else {
				map.put( key, value );
			}
		}
		
		// ����һ�������޸ĵ�ӳ���
		propertiesMap = Collections.unmodifiableMap( map );
	}
}
