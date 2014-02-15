package oxygen.util.i18n;

import java.util.Map;
import java.util.HashMap;

/**
 * <p>��Դ����������</p>
 * <p>------------ �����ռ䣺</p>
 * <p>��Դ���������������ṩ�������ռ�֧�ֵ���Դ��������ͨ������Դ��������һ�����֣�������Դ���������⣩��
 * ����������"/"�ַ��������к������ڰ���ͬ��εĻ��ֵ��ַ�����</p>
 * <p>���磺</p>
 * <p>��oxygen.util.i18n.ResourceLoader���е���Դ����������Ϊ"oxygen/util/i18n"��</p>
 * <p>����û�а����壬����Testû������������ô�����е���Դ����������Ϊ""�����ַ�������</p>
 * <p>��Դ���������в�εģ����ǿ����������֡�ǰ׺��ͬ���϶̵����ֵ���Դ�������ǽϳ����ֵĸ���Դ��������
 * ���磺</p>
 * <p>����Ϊ"oxygen/util"�ļ�����������Ϊ"oxygen/util/i18n"�ļ������ĸ���������
 * ������Ϊ"oxygen"�ļ���������"oxygen/util"�ĸ������������""�����ַ����������м������ĸ���������</p>
 * <p>��һ����Դ�������ǱȽ�����ģ�������Դ������������Ϊnull�����ǲ��������ռ��Լ����</p>
 * <p>------------ �̳����ԣ�</p>
 * <p>��Դ�������ǿ��Լ̳еģ��Ӽ�����ӵ�и���������������Դ��������ĳЩ���ϣ����Խ�����������һ������һЩС�޸ģ�
 * ����Ϊ�Ӽ��������Ǹ����������������Ӽ������ر����ȷ��ʣ�����Ҫ���Ӽ������������е���Դʱ���ص�����������Ѱ�ң�
 * һֱ����������</p>
 * <p>------------ ʹ�÷�����</p>
 * <p>����ҪΪ��oxygen.config.DatabaseConfig������ʻ�֧�֣����Ƚ���Դ����������ΪDatabaseConfig�����վ�̬���Ա��</p>
 * <p>{@code private static final ResourceLoader res = ResourceLoaderProvider.provide(DatabaseConfig.class);}</p>
 * <p>Ȼ���ڰ�oxygen.config��׫д���ʻ��ļ�resource.properties�����properties�����ļ�Ҳʹ��"key=value"����ʽ��д��
 * һ��һ����¼����Java��׼properties�ļ���ͬ���ǣ��������ļ���ʹ�ñ���Ĭ�ϵı����ʽ��ֻ�еȺ�'='�������ַ�������key����Ҫ���еȺţ�
 * ������ڵȺ�ǰ�ӷ�б���ַ�'\'��ʾת�塣��ϣ��key="if.a=b"����ô�����ļ��б���дΪ"if.a\=b"����Ч�Ⱥź����ȫ���ַ���������Ϊ�Ǽ�ֵ��
 * ��һ����¼��</p>
 * <p>hello\=world\=!=Hello,\==World=!</p>
 * <p>����key="hello=world=!"��value="Hello,\==World=!"</p>
 * <p>���ļ���ĳ����¼ȱ�ٵȺŻ������¼�ļ����ظ����������̻��׳�IllegalArgumentException��</p>
 * <p>��Դ�ļ�������Ҳ���Ըı䣬����Դ�ļ�����Ϊlanguage.properties����ô���ô���Դ�����н���Դ��������������Ϊ��������DatabaseConfigΪ������</p>
 * <p>{@code private static final ResourceLoader res = ResourceLoaderProvider.provide(DatabaseConfig.class, "language.properties");}</p>
 * <p>"resource.properties"��Ĭ����Դ�ļ���</p>
 * <p>Ҫ������Դ��ʹ��ResourceLoader��getResource()���������������ǩ����getResource(String key, String... args)��
 * ���Կ�������Դ�����������</p>
 * <p>����Դ�ļ���������һ����¼</p>
 * <p>application.hello=%s�ã�%s��</p>
 * <p>ִ��getResource("application.hello","����","Susan")�����᷵��"���Ϻã�Susan��"��</p>
 * <p>'%s'��ʾһ��������ռλ����s����Сд����</p>
 * <p>'%%'�Ƕ�'%'��ת�塣<b><font color=red>ע�⣺����ֵ���к��е����İٷֺ�'%'�������������ַ���Ͻ���Ϊת�壬
 * ��ô�����дΪ'%%'����������������ƥ���쳣��</font></b></p>
 * <p>���磺</p>
 * <p>ĳ����¼��you.are.right=you're 100%% right (����дΪyou're 100% right)</p>
 * <p></p>
 * <p>������'%%s'����һ���ٷֺű�ʾ�����İٷֺţ������'%s'��ʾ��������ô����дΪ'%%%s'��</p>
 * @author ����
 * @since 1.0, 2006-12-17
 * @version 1.0
 */
public final class ResourceLoaderProvider {
	
	/**
	 * ��Դ������ӳ���
	 */
	private static final Map<String, ResourceLoader> map = new HashMap<String, ResourceLoader>();
	
	/**
	 * ��������İ�����Դ�ļ�������������ռ����Դ�����������ü������������ռ��Ѿ���ע�ᣬ��ֱ�ӷ��ظü�����
	 * @param clazz ������
	 * @param resourceFileName ��Դ�ļ���
	 * @return �������ռ����Դ������
	 */
	public static ResourceLoader provide( Class clazz, String resourceFileName ) {
		
		String resourceLoaderName = ResourceIO.pathOf( clazz );
		
		if ( map.containsKey( resourceLoaderName ) ) {
			return map.get( resourceLoaderName );
		}
		
		ResourceLoader resourceLoader = ResourceLoader.newInstance( clazz, resourceFileName );
		map.put( resourceLoaderName, resourceLoader );
		
		return resourceLoader;
	}
	
	/**
	 * ��������İ���{@linkplain ResourceLoader#defaultResourceBundle Ĭ����Դ�ļ���}����������ռ����Դ�����������ü������������ռ��Ѿ���ע�ᣬ��ֱ�ӷ��ظü�����
	 * @param clazz ������
	 * @return �������ռ����Դ������
	 */
	public static ResourceLoader provide( Class clazz ) {
		return provide( clazz, ResourceLoader.defaultResourceBundle );
	}	

	/**
	 * <p>������Դ�������ĸ���Դ��������Ѱ�Ҽ������ҵ��򷵻ظü�ֵ��Ӧ����Դ��������������δ�ҵ�����null</p>
	 * <p>�˷�����������Դ������<b>����</b>�ĸ���Դ�������в��Ҽ�����һֱ������Դ������Ҳδ���ҵ������򷵻�null</p>
	 * @param child ����Դ������
	 * @param key ��
	 * @param args ����
	 * @return ���ҵ����ؼ�ֵ��δ�ҵ�����null
	 * @throws NullPointerException ������Դ������child���keyΪnull
	 */
	static String searchResourceInParent( ResourceLoader child, String key, String... args ) {
		
		if ( child == null ) throw new NullPointerException( "The child resource loader cannot be null." );
		if ( key == null ) throw new NullPointerException( "The key for searching in parent resource bundle cannot be null." );
		
		ResourceLoader parent = getParent( child );
		if ( parent == null ) {
			return null;
		} else if ( parent.contains( key ) ){
			return parent.getResource( key, args );
		} else {
			return searchResourceInParent( parent, key, args );
		}
	}
		
	/**
	 * <p>����Դ�������õ�������ĸ���Դ������</p>
	 * <p>�����������null��</p>
	 * <ul><li>����Դ�������������ġ���������Դ������û���Լ��������ռ䣬�����ֵ���Դ�������ķּ���Χ</li>
	 * <li>����Դ�������ǵ�ǰ�����ռ�����߲����Դ��������������Դ������ӵ��""���ַ�����Ϊ����ʱ��
	 * �����������ռ����߲㣬������������Դ�������ĸ���Դ�����������䱾��û�и���Դ������</li>
	 * <li>����Դ��������δע�ᵽ��ResourceLoaderProvider</li>
	 * </ul>
	 * @param child ����Դ������
	 * @return ���ҳɹ����ظ���Դ��������ʧ�ܷ���null
	 */
	private static ResourceLoader getParent( ResourceLoader child ) {
	
		String childName = child.getName();
		
		if ( childName == null || "".equals( childName ) ) return null;
		
		return searchParent( childName );
	}
	
	/**
	 * <p>�������ռ���Ѱ����ΪchildName����Դ�������ĸ�������</p>
	 * <p>���ҵ����򷵻ش���Դ����������û���ҵ������������߲����Դ����������һֱ������Դ��������
	 * ������Դ������Ҳû���ҵ����򷵻�null</p>
	 * @param childName ��Դ����������
	 * @return ����Դ������
	 */
	private static ResourceLoader searchParent( String childName ) {

		if ( childName.indexOf( "/" ) == -1 ) {
			return map.get( "" );
		} else {			
			String parentName = childName.substring( 0, childName.lastIndexOf( "/" ) );
			ResourceLoader parent = map.get( parentName );
			if ( parent != null ) {
				return parent;
			} else {
				return searchParent( parentName );
			}
		}		
	}
}
