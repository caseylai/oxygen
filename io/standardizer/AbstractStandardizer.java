package oxygen.io.standardizer;

import java.lang.reflect.Field;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.NotNullConstraint;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * �������ݱ�׼����
 * @author ����
 * @since 1.0, 2007-04-12
 * @version 1.0
 * @param <E> ʵ��
 */
public abstract class AbstractStandardizer<E extends Entity<E>> {
	
	// ָ����ʵ��
	protected final E entity;
	
	// ָ����ʵ���ֶ�
	protected final Field field;
	
	// ʵ���ֶε�ֵ
	protected Object o;
	
	// ʵ���ֶ�ֵ�ĳ���
	protected int size;
	
	protected static final ResourceLoader res = ResourceLoaderProvider.provide( AbstractStandardizer.class );

	AbstractStandardizer( E entity, Field field ) {
		
		this.entity = entity;
		this.field = field;
		o = entity.valueOf( field );
		size = TypeSizeDefiner.define( field );
	}
	
	// ���õ�Լ�����
	void checkConstraint() throws DataFormatException {
		
		// �ǿռ��
		if ( field.getAnnotation( NotNullConstraint.class ) != null && o == null ) {
			throw new DataFormatException( res.getResource( "AbstractStandardizer.checkConstraint.exception.NotNullConstraintFailed", entity.getClass().getSimpleName(), field.getName() ) );
		}
	}
	
	// ִ��Լ������Լ���׼������
	abstract byte[] standardize() throws DataFormatException;
}
