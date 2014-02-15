package oxygen.entity;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;

/**
 * <p>实体排序器</p>
 * <p>此实体排序器用于对实体查询的结果进行排序，例如：</p>
 * <code>
 * <pre>
 * 		// User extends Entity<User>
 * 		User user = EntityProvider.provide( "User" );
 * 		...
 * 		EntitySortor<User> sortor = EntitySortor.getAscSortor( User.class, "name", "age" );
 * 		List<User> sortedList = sortor.sort( user.query() );
 * </pre>
 * </code>
 * @author 赖昆
 * @since 1.0, 2007-06-02
 * @version 1.0
 * @param <E> 实体
 */
public abstract class EntitySortor<E extends Entity<E>> implements Comparator<E> {

	// 要排序的字段
	protected final Field[] fields;
	
	private static final Logger logger = Logger.getLogger( EntitySortor.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( EntitySortor.class );
	
	protected EntitySortor( Field... fields ) {
		this.fields = fields;
	}
	
	// 升序的实体排序器
	private static class EntityAscSortor<E extends Entity<E>> extends EntitySortor<E> {
		
		EntityAscSortor( Field... fields ) {
			super( fields );
		}
	}
	
	// 降序的实体排序器
	private static class EntityDescSortor<E extends Entity<E>> extends EntitySortor<E> {
		
		EntityDescSortor( Field... fields ) {
			super( fields );
		}
		
		// 降序排列，将比较大小的结果反号即可
		@Override
		public int compare( E e1, E e2, int level ) {
			return -1 * super.compare( e1, e2, level );
		}
	}
	
	// 返回指定实体类中具有字段名称的所有字段
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
	 * 获取一个指定实体类型的升序排序器
	 * @param <E> 实体
	 * @param clazz 实体类型
	 * @param fieldNames 要排序的实体字段名称，按排序的优先性排列
	 * @return 升序排序器
	 */
	public static <E extends Entity<E>> EntitySortor<E> getAscSortor( Class<E> clazz, String... fieldNames ) {
		return new EntityAscSortor<E>( toFields( clazz, fieldNames ) );
	}
	
	/**
	 * 获取一个指定实体类型的降序排序器
	 * @param <E> 实体
	 * @param clazz 实体类型
	 * @param fieldNames 要排序的实体字段名称，按排序的优先性排列
	 * @return 降序排序器
	 */
	public static <E extends Entity<E>> EntitySortor<E> getDescSortor( Class<E> clazz, String... fieldNames ) {
		return new EntityDescSortor<E>( toFields( clazz, fieldNames ) );
	}
	
	/**
	 * 对指定列表中的元素进行排序
	 * @param c 要进行排序的列表
	 * @return 排序后的列表
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
	 * 按字段数组规定的优先级来比较大小，例如:
	 * 字段数组为{nameField, ageField}
	 * 若按name就已经比较出大小，就不进行age的比较；若name相同，才进行age的比较。这是一个递归的过程
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