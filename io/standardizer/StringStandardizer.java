package oxygen.io.standardizer;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;
import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.ContentConstraint;


/**
 * �ַ������ݱ�׼����
 * @author ����
 * @since 1.0, 2007-04-11
 * @version 1.0
 * @param <E> ʵ��
 */
public class StringStandardizer<E extends Entity<E>> extends AbstractStandardizer<E> {
	
	// ���ݴ洢���뷽ʽ
	private static final String encoding = DatabaseContext.get( ConfigKey.ENCODE );
	
	// �ֶε��ַ���ֵ
	private final String str;
	
	StringStandardizer( E entity, Field field ) {
		super( entity, field );
		str = o == null ? null : o.toString();
	}
	
	// ����ַ���Լ��
	@Override
	void checkConstraint() throws DataFormatException {
		
		// ����Լ�����
		super.checkConstraint();
		
		if ( str != null ) {
			// ��鳤��Լ��
			if ( str.length() > size / TypeSizeDefiner.UTF_MAX_BYTES ) {
				throw new DataFormatException( res.getResource( "StringStandardizer.checkConstraint.exception.SizeConstraintFailed" , entity.getClass().getSimpleName(), field.getName(), str, String.valueOf( str.length() ), String.valueOf( size / TypeSizeDefiner.UTF_MAX_BYTES ) ) );
			}
			// �������Լ��
			ContentConstraint cc = field.getAnnotation( ContentConstraint.class );
			if ( cc != null && !str.matches( cc.value() ) ) {
				throw new DataFormatException( res.getResource( "StringStandardizer.checkConstraint.exception.ContentConstraintFailed", entity.getClass().getSimpleName(), field.getName(), str, cc.value() ) );
			}
		}
	}
	
	// ���Լ������׼���ַ�������
	byte[] standardize() throws DataFormatException {

		// Լ�����
		checkConstraint();
		// ���ַ���Ϊnull����ֱ�ӷ���ȫ����ֽ�����
		if ( str == null ) {
			return new byte[ size ];
		}
		
		// ��׼���ַ���
		try {
			
			StringBuilder sb = new StringBuilder( str );
			int zeroNum = size - str.getBytes( encoding ).length;
			for ( int i = 0 ; i < zeroNum ; i++ ) {
				sb.append( '\u0000' );
			}
			
			return sb.toString().getBytes( encoding );
			
		} catch ( UnsupportedEncodingException e ) {
			return new byte[0];
		}
	}
}
