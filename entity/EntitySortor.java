package oxygen.entity;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;

/**
 * <p>ʵ��������</p>
 * <p>��ʵ�����������ڶ�ʵ���ѯ�Ľ�������������磺</p>
 * <code>
 * <pre>
 * 		// User extends Entity<User>
 * 		User user = EntityProvider.provide( "User" );
 * 		...
 * 		EntitySortor<User> sortor = EntitySortor.getAscSortor( User.class, "name", "age" );
 * 		List<User> sortedList = sortor.sort( user.query() );
 * </pre>
 * </code>
 * @author ����
 * @since 1.0, 2007-06-02
 * @version 1.0
 * @param <E> ʵ��
 */
public abstract class EntitySortor<E extends Entity<E>> implements Comparator<E> {

	// Ҫ������ֶ�
	protected final Field[] fields;
	
	private static final Logger logger = Logger.getLogger( EntitySortor.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( EntitySortor.class );
	
	protected EntitySortor( Field... fields ) {
		this.fields = fields;
	}
	
	// �����ʵ��������
	private static class EntityAscSortor<E extends Entity<E>> extends EntitySortor<E> {
		
		EntityAscSortor( Field... fields ) {
			super( fields );
		}
	}
	
	// �����ʵ��������
	private static class EntityDescSortor<E extends Entity<E>> extends EntitySortor<E> {
		
		EntityDescSortor( Field... fields ) {
			super( fields );
		}
		
		// �������У����Ƚϴ�С�Ľ�����ż���
		@Override
		public int compare( E e1, E e2, int level ) {
			return -1 * super.compare( e1, e2, level );
		}
	}
	
	// ����ָ��ʵ�����о����ֶ����Ƶ������ֶ�
	private static Field[] toFields( Class<?> clazz, String[] fieldNames ) {
		Field[] fields = new Field[ fieldNames.length ];
		try {
			for ( int i = 0 ; i < fieldNames.length ; i++ ) {
				fields[i] = clazz.getDeclaredField( fieldNames[i] );
			}
		} catch ( NoSuchFieldException e ) {
			logger.warning( res.getResource( "EntitySortor.toFields.warning.NotFoundEntityField" ) );
			throw new IllegalArgumentException( res.getResource( "EntitySortor.toFields.warning.NotFoundEntityField" ), e );
		}
		return fields;
	}
	
	/**
	 * ��ȡһ��ָ��ʵ�����͵�����������
	 * @param <E> ʵ��
	 * @param clazz ʵ������
	 * @param fieldNames Ҫ�����ʵ���ֶ����ƣ������������������
	 * @return ����������
	 */
	public static <E extends Entity<E>> EntitySortor<E> getAscSortor( Class<E> clazz, String... fieldNames ) {
		return new EntityAscSortor<E>( toFields( clazz, fieldNames ) );
	}
	
	/**
	 * ��ȡһ��ָ��ʵ�����͵Ľ���������
	 * @param <E> ʵ��
	 * @param clazz ʵ������
	 * @param fieldNames Ҫ�����ʵ���ֶ����ƣ������������������
	 * @return ����������
	 */
	public static <E extends Entity<E>> EntitySortor<E> getDescSortor( Class<E> clazz, String... fieldNames ) {
		return new EntityDescSortor<E>( toFields( clazz, fieldNames ) );
	}
	
	/**
	 * ��ָ���б��е�Ԫ�ؽ�������
	 * @param c Ҫ����������б�
	 * @return �������б�
	 */
	public List<E> sort( List<E> c ) {
		if ( c == null ) throw new NullPointerException();
		Collections.sort( c, this );
		return c;
	}
	
	public int compare( E e1, E e2 ) {
		return compare( e1, e2, 0 );
	}
	
	/*
	 * ���ֶ�����涨�����ȼ����Ƚϴ�С������:
	 * �ֶ�����Ϊ{nameField, ageField}
	 * ����name���Ѿ��Ƚϳ���С���Ͳ�����age�ıȽϣ���name��ͬ���Ž���age�ıȽϡ�����һ���ݹ�Ĺ���
	 */
	@SuppressWarnings("unchecked")
	protected int compare( E e1, E e2, int level ) {
		if ( level < fields.length ) {
			Comparable c = (Comparable) e1.valueOf( fields[level] );
			Object o = e2.valueOf( fields[level] );
			if ( c == null && o == null ) {
				return 0;
			} else if ( c == null ) {
				return -1;
			} else if ( o == null ) {
				return 1;
			} else {
				int result = c.compareTo( o );
				return result == 0 ? compare( e1, e2, level + 1 ) : result;
			}
		} else {
			return 0;
		}
	}
}