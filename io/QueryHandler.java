package oxygen.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import oxygen.entity.Entity;
import oxygen.io.standardizer.StandardizerProxy;


/**
 * <p>实体查询器</p>
 * <p>实体查询器进行对实体的查询操作，查询结果返回符合条件的实体的集合。若没有找到符合条件的实体，则返回一个
 * 空集合。若查询中遇到异常而失败，则返回空标记（null）。</p>
 * @author 赖昆
 * @since 1.0, 2007-04-01
 * @version 1.0
 * @param <E> 实体
 */
public class QueryHandler<E extends Entity<E>> extends Handler<E> {
	
	// 要查询的实体
	private final E entity;
	
	// 操作结果
	private final Response<List<E>> response;
	
	QueryHandler( E entity, LookupTable<E> table, Response<List<E>> response ) {
		super( table, null );
		this.entity = entity;
		this.response = response;
	}

	void handle() {
		
		try {

			// 查询目标实体的地址
			HashSet<Long> addressSet = new Querist<E>( entity, table ).query();
			// 保存查询到的实体
			List<E> resultList = new LinkedList<E>();
			
			try {
				table.lock( addressSet );
				FileChannel channel = table.channel();
				ByteBuffer bb = ByteBuffer.allocate( table.size() );
				// 将查询到的地址处的实体实例化
				for ( Long position : addressSet ) {
					channel.read( bb, position );
					E entity = StandardizerProxy.unstandardize( bb.array(), table.name() );
					resultList.add( entity );
					bb.clear();
				}
				// 过滤掉空实体（字段值全部为null的实体）
				resultList.removeAll( table.getEmptyEntityList() );
				
				response.setAddressSet( addressSet );
				response.setResponse( resultList );
			} catch ( IOException e ) {
				e.printStackTrace();
				response.setResponse( null );
			} finally {
				table.unlock();
			}
		
		} catch ( Exception e ) {
			logger.warning( res.getResource( "QueryHandler.handle.warning.UnknownExceptionThrowing" ) );
			e.printStackTrace();
			response.setResponse( null );
		}
	}
}
